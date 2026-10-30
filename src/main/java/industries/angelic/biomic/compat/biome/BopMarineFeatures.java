package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Exact native coral and kelp proposals, committed only when the complete water footprint fits. */
public final class BopMarineFeatures {

    private record Pos(int x, int y, int z) {}

    private record Cell(Block block, int meta) {}

    private record Parameters(String type, Block block, int meta, boolean randomMeta, int groupCount, int minHeight,
        int randomHeight, int generateOffset) {}

    private BopMarineFeatures() {}

    private static Parameters parameters(String feature, WorldGenerator generator) {
        String type = generator.getClass()
            .getName();
        Map<String, Object> fields = new HashMap<>();
        try {
            for (var field : generator.getClass()
                .getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.put(field.getName(), field.get(generator));
                }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect native marine generator", error);
        }
        if (type.equals("biomesoplenty.common.world.features.WorldGenKelp")) {
            if (!fields.keySet()
                .equals(Set.of("minHeight", "randomHeight")))
                throw new IllegalStateException("Native kelp layout changed");
            int min = (Integer) fields.get("minHeight"), random = (Integer) fields.get("randomHeight");
            boolean shortKelp = feature.equals("shortKelpPerChunk");
            if (!(shortKelp && min == 2 && random == 4 || !shortKelp && Set.of("kelpPerChunk", "kelpThickPerChunk")
                .contains(feature) && min == 4 && random == 8))
                throw new IllegalStateException("Native kelp parameters changed: " + feature);
            return new Parameters("kelp", null, 0, false, 0, min, random, installedKelpOffset(generator));
        }
        if (!type.equals("biomesoplenty.common.world.features.WorldGenBOPCoral") || !fields.keySet()
            .equals(Set.of("flora", "floraMeta", "randomMeta", "groupCount")))
            throw new IllegalStateException("Unaudited native marine generator: " + type);
        Block block = (Block) fields.get("flora");
        int meta = (Integer) fields.get("floraMeta"), count = (Integer) fields.get("groupCount");
        boolean random = (Boolean) fields.get("randomMeta");
        String registry = Block.blockRegistry.getNameForObject(block);
        if (feature.equals("seaweedPerChunk")) {
            if (!BopNativeBiomes.SEAWEED.equals(NativeBlockCatalogue.encode(block, meta)) || random || count != 256)
                throw new IllegalStateException("Native seaweed parameters changed");
        } else if (feature.equals("coralPerChunk")) {
            if (!"BiomesOPlenty:coral1".equals(registry) || !random
                || count != 64
                || meta != -1 && meta < 12
                || meta > 15) throw new IllegalStateException("Native random coral parameters changed");
        } else throw new IllegalStateException("Unaudited native coral feature: " + feature);
        return new Parameters("coral", block, meta, random, count, 0, 0, 0);
    }

    private static int installedKelpOffset(WorldGenerator generator) {
        Method x = null, z = null;
        for (Method method : generator.getClass()
            .getDeclaredMethods()) {
            if (method.getName()
                .endsWith("$offsetKelpXBy8")) x = exactOffsetHelper(x, method);
            if (method.getName()
                .endsWith("$offsetKelpZBy8")) z = exactOffsetHelper(z, method);
        }
        if (x == null && z == null) return 0;
        if (x == null || z == null) throw new IllegalStateException("Partial installed Kelp coordinate patch");
        try {
            x.setAccessible(true);
            z.setAccessible(true);
            if ((Integer) x.invoke(generator, 0) != 8 || (Integer) x.invoke(generator, -11) != -3
                || (Integer) z.invoke(generator, 0) != 8
                || (Integer) z.invoke(generator, -11) != -3)
                throw new IllegalStateException("Installed Kelp coordinate patch behavior changed");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot verify installed Kelp coordinate patch", error);
        }
        return 8;
    }

    private static Method exactOffsetHelper(Method prior, Method method) {
        if (prior != null || Modifier.isStatic(method.getModifiers())
            || !Modifier.isPrivate(method.getModifiers())
            || method.getReturnType() != int.class
            || !Arrays.equals(method.getParameterTypes(), new Class<?>[] { int.class }))
            throw new IllegalStateException("Installed Kelp coordinate patch shape changed");
        return method;
    }

    public static String identity(String feature, WorldGenerator generator) {
        Parameters p = parameters(feature, generator);
        return generator.getClass()
            .getName() + "{feature="
            + feature
            + ",block="
            + (p.block == null ? "coral1" : Block.blockRegistry.getNameForObject(p.block))
            + ",meta="
            + p.meta
            + ",randomMeta="
            + p.randomMeta
            + ",groupCount="
            + p.groupCount
            + ",height="
            + p.minHeight
            + '+'
            + p.randomHeight
            + (p.type.equals("kelp") ? ",installedGenerateOffset=" + p.generateOffset : "")
            + '}';
    }

    public static String encode(String feature, WorldGenerator generator, Block block, int meta) {
        return encode(parameters(feature, generator), feature, block, meta);
    }

    private static String encode(Parameters p, String feature, Block block, int meta) {
        String state = NativeBlockCatalogue.encode(block, meta);
        if (p.type.equals("kelp")) {
            if (!Set.of(8, 9, 10)
                .contains(meta) || !("biomesoplenty:coral1[legacy_meta=" + meta + "]").equals(state))
                throw new IllegalStateException("Unexpected native kelp write");
        } else if (feature.equals("seaweedPerChunk")) {
            if (!BopNativeBiomes.SEAWEED.equals(state))
                throw new IllegalStateException("Unexpected native seaweed write");
        } else if (meta < 12 || meta > 15 || !("biomesoplenty:coral1[legacy_meta=" + meta + "]").equals(state))
            throw new IllegalStateException("Unexpected native coral write");
        return state;
    }

    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random, String feature,
        WorldGenerator generator, int x, int y, int z) {
        Parameters parameters = parameters(feature, generator);
        boolean kelp = parameters.type.equals("kelp");
        int minHorizontal = kelp ? parameters.generateOffset - 3 : -7,
            maxHorizontal = kelp ? parameters.generateOffset + 3 : 7;
        int minWriteY = kelp ? y : y - 3,
            maxWriteY = kelp ? y + parameters.minHeight + parameters.randomHeight - 2 : y + 3;
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var view = new NativeFeatureCapture.Access() {

            void checkColumn(int px, int pz) {
                long dx = (long) px - x, dz = (long) pz - z;
                // WorldGenKelp owns a +/-3 draw. When installed, Hodgepodge's two
                // verified merged helpers first shift generate's x/z arguments by +8.
                if (dx < minHorizontal || dx > maxHorizontal || dz < minHorizontal || dz > maxHorizontal)
                    throw NativeFeatureCapture
                        .outside("Native marine read escaped source envelope: feature=" + feature);
            }

            void checkWrite(int px, int py, int pz) {
                long dx = (long) px - x, dz = (long) pz - z;
                if (dx < minHorizontal || dx > maxHorizontal
                    || dz < minHorizontal
                    || dz > maxHorizontal
                    || py < minWriteY
                    || py > maxWriteY)
                    throw NativeFeatureCapture
                        .outside("Native marine write escaped source envelope: feature=" + feature);
            }

            public Block block(int px, int py, int pz) {
                checkColumn(px, pz);
                if (!NativeFeatureCapture.containsPhysicalY(physical, py)) return Blocks.air;
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.block(px, py, pz) : c.block;
            }

            public int metadata(int px, int py, int pz) {
                checkColumn(px, pz);
                if (!NativeFeatureCapture.containsPhysicalY(physical, py)) return 0;
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.metadata(px, py, pz) : c.meta;
            }

            public int heightValue(int px, int pz) {
                checkColumn(px, pz);
                return physical.heightValue(px, pz);
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                checkWrite(px, py, pz);
                encode(parameters, feature, block, meta);
                if (proposal.size() >= 512) throw new IllegalStateException("Native marine write budget exceeded");
                proposal.put(new Pos(px, py, pz), new Cell(block, meta));
                return true;
            }
        };
        boolean generated = NativeFeatureCapture
            .run(world, new BopSurfaceCapture(view, random), () -> generator.generate(world, random, x, y, z));
        if (!generated) return false;
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            if (p.y <= physical.minimumY() || p.y >= physical.maximumYExclusive() - 1
                || physical.block(p.x, p.y, p.z) != Blocks.water) return false;
        }
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, c.block, c.meta)) return false;
        }
        return true;
    }
}
