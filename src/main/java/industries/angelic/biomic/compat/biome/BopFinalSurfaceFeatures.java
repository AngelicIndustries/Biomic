package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Whole-proposal adapters for Wasteland formations and the Volcano lava spout. */
public final class BopFinalSurfaceFeatures {

    private record Pos(int x, int y, int z) {}

    private record Cell(Block block, int meta) {}

    private BopFinalSurfaceFeatures() {}

    public static String identity(String feature, WorldGenerator generator) {
        String type = generator.getClass()
            .getName();
        if (feature.equals("lavaSpoutsPerChunk")) {
            if (!type.equals("biomesoplenty.common.world.features.WorldGenLavaSpout") || instanceFields(generator) != 0)
                throw new IllegalStateException("Native lava-spout generator changed");
            return type + "{ashStone,flowingLava,cross5}";
        }
        if (feature.equals("wastelandRockPilesPerChunk")) {
            if (!type.equals("biomesoplenty.common.world.features.WorldGenBOPBlob"))
                throw new IllegalStateException("Native Wasteland rock-pile class changed");
            int blocks = 0, ints = 0;
            try {
                for (var field : generator.getClass()
                    .getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        if (field.getType() == Block.class && BopNativeBiomes.DRIED_DIRT
                            .equals(NativeBlockCatalogue.encode((Block) field.get(generator), 0))) blocks++;
                        else if (field.getType() == int.class && field.getInt(generator) == 0) ints++;
                        else throw new IllegalStateException("Native Wasteland rock-pile parameter changed");
                    }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Cannot inspect Wasteland rock pile", error);
            }
            if (blocks != 1 || ints != 1) throw new IllegalStateException("Incomplete Wasteland rock-pile parameters");
            return type + "{driedDirt,radius0}";
        }
        if (!Set.of("wasteland1PerChunk", "wasteland2PerChunk", "wasteland3PerChunk", "wasteland4PerChunk")
            .contains(feature)
            || !type.equals("biomesoplenty.common.world.features.WorldGenWasteland" + feature.charAt(9))
                && !(feature.equals("wasteland1PerChunk")
                    && type.equals("biomesoplenty.common.world.features.WorldGenWasteland"))
            || instanceFields(generator) != 0)
            throw new IllegalStateException("Native Wasteland formation changed: " + feature);
        return type + "{driedDirt,wholeFormation}";
    }

    private static int instanceFields(Object value) {
        int count = 0;
        for (var field : value.getClass()
            .getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers())) count++;
        return count;
    }

    public static String encode(String feature, WorldGenerator generator, Block block, int meta) {
        identity(feature, generator);
        if (meta != 0) throw new IllegalStateException("Unexpected final surface-feature metadata");
        if (feature.equals("lavaSpoutsPerChunk") && block == Blocks.flowing_lava) return "minecraft:lava[level=0]";
        if (feature.equals("wasteland4PerChunk") && block == Blocks.air) return "minecraft:air";
        if (BopNativeBiomes.DRIED_DIRT.equals(NativeBlockCatalogue.encode(block, meta)))
            return BopNativeBiomes.DRIED_DIRT;
        throw new IllegalStateException("Unexpected final surface-feature write");
    }

    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random, String feature,
        WorldGenerator generator, int x, int y, int z) {
        identity(feature, generator);
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var view = new NativeFeatureCapture.Access() {

            void check(int px, int py, int pz) {
                if (Math.abs((long) px - x) > 3 || Math.abs((long) pz - z) > 3 || py < 0 || py >= 512)
                    throw NativeFeatureCapture.outside("Native final surface feature escaped its source envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, py, pz);
                if (!NativeFeatureCapture.containsPhysicalY(physical, py)) return Blocks.air;
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.block(px, py, pz) : c.block;
            }

            public int metadata(int px, int py, int pz) {
                check(px, py, pz);
                if (!NativeFeatureCapture.containsPhysicalY(physical, py)) return 0;
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.metadata(px, py, pz) : c.meta;
            }

            public int heightValue(int px, int pz) {
                check(px, y, pz);
                return physical.heightValue(px, pz);
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, py, pz);
                if (!NativeFeatureCapture.containsFeatureY(physical, py))
                    throw NativeFeatureCapture.outside("Native final surface feature wrote outside physical envelope");
                encode(feature, generator, block, meta);
                if (proposal.size() >= 256)
                    throw new IllegalStateException("Native final surface feature exceeded write budget");
                proposal.put(new Pos(px, py, pz), new Cell(block, meta));
                return true;
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }
        };
        if (!NativeFeatureCapture
            .run(world, new BopSurfaceCapture(view, random), () -> generator.generate(world, random, x, y, z))
            || proposal.isEmpty()) return false;
        Block dried = NativeBlockCatalogue.resolve(BopNativeBiomes.DRIED_DIRT)
            .block(),
            ash = NativeBlockCatalogue.resolve(BopNativeBiomes.ASH_STONE)
                .block();
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            Block old = physical.block(p.x, p.y, p.z);
            if (feature.equals("lavaSpoutsPerChunk")) {
                if (old != Blocks.air && old != ash) return false;
            } else if (feature.equals("wastelandRockPilesPerChunk")) {
                if (old != Blocks.air && old != dried
                    && old != Blocks.stone
                    && old != Blocks.dirt
                    && old != Blocks.grass) return false;
            } else if (c.block == Blocks.air) {
                if (old != dried) return false;
            } else if (old != Blocks.air && old != dried) return false;
        }
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, c.block, c.meta)) return false;
        }
        return true;
    }
}
