package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenClay;
import net.minecraft.world.gen.feature.WorldGenSand;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Installed shoreline generators, with whole-proposal gravity validation. No added footings. */
public final class BopShorePatch {

    private record Pos(int x, int y, int z) {}

    private record Parameters(Block block, int radius, boolean clay) {}

    private BopShorePatch() {}

    private static Parameters parameters(WorldGenerator generator) {
        Class<?> type = generator.getClass();
        boolean sand = type == WorldGenSand.class, clay = type == WorldGenClay.class;
        boolean bop = type.getName()
            .equals("biomesoplenty.common.world.features.WorldGenWaterside");
        if (!sand && !clay && !bop) throw new IllegalArgumentException("Unaudited shoreline generator");
        Block block = null;
        Integer radius = null;
        List<?> substrates = null;
        try {
            // Role-based fields also work with shipped SRG names.
            for (var field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(generator);
                if (field.getType() == Block.class && block == null) block = (Block) value;
                else if (field.getType() == int.class && radius == null) radius = (Integer) value;
                else if (bop && value instanceof List<?>list && substrates == null) substrates = list;
                else throw new IllegalStateException("Unexpected shoreline generator layout");
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot inspect shoreline generator", error);
        }
        boolean valid = radius != null
            && (sand && (block == Blocks.sand && radius == 7 || block == Blocks.gravel && radius == 6)
                || clay && block == Blocks.clay && radius == 4
                || bop && (block == Blocks.gravel || BopNativeBiomes.MUD.equals(NativeBlockCatalogue.encode(block, 0)))
                    && radius == 7
                    && List.of(Blocks.dirt, Blocks.grass)
                        .equals(substrates));
        if (!valid) throw new IllegalStateException("Unaudited shoreline parameters");
        return new Parameters(block, radius, clay);
    }

    static String identity(WorldGenerator generator) {
        Parameters p = parameters(generator);
        return generator.getClass()
            .getName() + "{block="
            + Block.blockRegistry.getNameForObject(p.block)
            + ", radius="
            + p.radius
            + "}";
    }

    public static String encode(WorldGenerator generator, Block block, int meta) {
        Parameters p = parameters(generator);
        if (meta != 0 || block != p.block) throw new IllegalStateException("Unaudited shoreline write");
        return Block.blockRegistry.getNameForObject(block);
    }

    /** Caller must buffer and discard writes when this returns false or throws. */
    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, int x, int y, int z) {
        Parameters parameters = parameters(generator);
        Map<Pos, Block> proposal = new LinkedHashMap<>();
        var view = new NativeFeatureCapture.Access() {

            void check(int px, int pz) {
                if (Math.abs((long) px - x) >= parameters.radius || Math.abs((long) pz - z) >= parameters.radius)
                    throw NativeFeatureCapture.outside("Native shoreline escaped source envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, pz);
                Block written = proposal.get(new Pos(px, py, pz));
                return written == null ? physical.block(px, py, pz) : written;
            }

            public int metadata(int px, int py, int pz) {
                check(px, pz);
                return proposal.containsKey(new Pos(px, py, pz)) ? 0 : physical.metadata(px, py, pz);
            }

            public int heightValue(int px, int pz) {
                check(px, pz);
                return physical.heightValue(px, pz);
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, pz);
                if (block != parameters.block || meta != 0
                    || Math.abs((long) py - y) > (parameters.clay ? 1 : 2)
                    || proposal.size() >= 625) throw new IllegalStateException("Unaudited native shoreline write");
                proposal.put(new Pos(px, py, pz), block);
                return true;
            }
        };
        boolean valid = NativeFeatureCapture.run(world, new BopSurfaceCapture(view, random), () -> {
            if (!generator.generate(world, random, x, y, z) || proposal.isEmpty()) return false;
            for (Pos p : proposal.keySet()) {
                if (!NativeFeatureCapture.containsFeatureY(physical, p.y)) return false;
                Block existing = physical.block(p.x, p.y, p.z);
                if (existing != Blocks.dirt && existing != (parameters.clay ? Blocks.clay : Blocks.grass)) return false;
                if (BopNativeBiomes.MUD.equals(NativeBlockCatalogue.encode(parameters.block, 0))
                    && !proposal.containsKey(new Pos(p.x, p.y + 1, p.z))) {
                    var above = physical.block(p.x, p.y + 1, p.z)
                        .getMaterial();
                    if (above == net.minecraft.block.material.Material.plants
                        || above == net.minecraft.block.material.Material.vine
                        || above == net.minecraft.block.material.Material.wood
                        || above == net.minecraft.block.material.Material.leaves) return false;
                }
                if (parameters.clay) continue;
                // Check the final proposal, including stacks of existing/new falling blocks.
                // A water pocket or air gap under any member rejects the entire patch.
                int below = p.y - 1;
                while (below > 0 && view.block(p.x, below, p.z) instanceof BlockFalling) below--;
                if (view.block(p.x, below, p.z) instanceof BlockFalling
                    || BlockFalling.func_149831_e(world, p.x, below, p.z)) return false;
            }
            return true;
        });
        if (!valid) return false;
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            if (!physical.set(p.x, p.y, p.z, entry.getValue(), 0)) return false;
        }
        return true;
    }
}
