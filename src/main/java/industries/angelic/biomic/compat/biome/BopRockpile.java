package industries.angelic.biomic.compat.biome;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native rock geometry, admitted only as one supported, non-overwriting proposal. */
public final class BopRockpile {

    private record Pos(int x, int y, int z) {}

    private record Cell(Block block, int meta) {}

    private static final int[][] DIRECTIONS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private BopRockpile() {}

    static void validateGenerator(WorldGenerator generator) {
        if (!generator.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features.WorldGenBOPBlob"))
            throw new IllegalArgumentException("Unaudited native rock generator");
        try {
            int blocks = 0, radii = 0;
            for (var field : generator.getClass()
                .getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                if (field.getType() == Block.class && field.get(generator) == Blocks.cobblestone) blocks++;
                else if (field.getType() == int.class && field.getInt(generator) == 0) radii++;
                else throw new IllegalStateException("Unaudited native rock parameter");
            }
            if (blocks != 1 || radii != 1) throw new IllegalStateException("Incomplete native rock parameters");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot validate native rock parameters", error);
        }
    }

    /** Caller buffers writes and discards them on false/exception, including habitat/ceiling rejection. */
    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenerator generator, int x, int y, int z) {
        validateGenerator(generator);
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var view = new NativeFeatureCapture.Access() {

            private void check(int px, int pz) {
                if (Math.abs((long) px - x) > 4 || Math.abs((long) pz - z) > 4)
                    throw NativeFeatureCapture.outside("Native rock escaped its source envelope");
            }

            public Block block(int px, int py, int pz) {
                check(px, pz);
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.block(px, py, pz) : c.block;
            }

            public int metadata(int px, int py, int pz) {
                check(px, pz);
                Cell c = proposal.get(new Pos(px, py, pz));
                return c == null ? physical.metadata(px, py, pz) : c.meta;
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                check(px, pz);
                if (meta != 0 || block != Blocks.cobblestone || proposal.size() >= 81)
                    throw new IllegalStateException("Unaudited native rock write");
                proposal.put(new Pos(px, py, pz), new Cell(block, meta));
                return true;
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
        };
        if (!NativeFeatureCapture
            .run(world, new BopSurfaceCapture(view, random), () -> generator.generate(world, random, x, y, z))
            || proposal.isEmpty()) return false;
        var attached = new HashSet<Pos>();
        var frontier = new ArrayDeque<Pos>();
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            if (!NativeFeatureCapture.containsFeatureY(physical, p.y)) return false;
            Block existing = physical.block(p.x, p.y, p.z);
            // Do not remove trees, fungi, water or one half of a tall ground plant.
            if (existing != Blocks.air && !ground(existing) && existing != Blocks.cobblestone) return false;
            if (ground(existing) || ground(physical.block(p.x, p.y - 1, p.z))) {
                attached.add(p);
                frontier.add(p);
            }
        }
        while (!frontier.isEmpty()) {
            Pos p = frontier.removeFirst();
            for (int[] d : DIRECTIONS) {
                Pos neighbor = new Pos(p.x + d[0], p.y + d[1], p.z + d[2]);
                if (proposal.containsKey(neighbor) && attached.add(neighbor)) frontier.add(neighbor);
            }
        }
        if (attached.size() != proposal.size()) return false;
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, c.block, c.meta)) return false;
        }
        return true;
    }

    private static boolean ground(Block block) {
        return block == Blocks.grass || block == Blocks.dirt || block == Blocks.stone;
    }
}
