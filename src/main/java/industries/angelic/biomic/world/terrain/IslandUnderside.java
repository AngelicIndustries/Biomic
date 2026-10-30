package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.List;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * What hangs from a floating island (IslandKeel), and its waterfalls. Every write lands in the chunk being populated
 * and is decided from pure inputs (the island spans, their habitats, the raw terrain), so it does not depend on the
 * order chunks are populated in.
 * <ul>
 * <li>Vines on green and lush islands: from 12% of the coast's flank columns (2–8 long), and from 4% of the steps of
 * the keel.</li>
 * <li>Hanging roots under 25% of the thin rim (soil within a few blocks), except on dry islands.</li>
 * <li>Glow lichen under 2% of a lush island's underside.</li>
 * <li>Pointed dripstone under 15% of the hanging tips of dry and rocky islands, 1–3 long.</li>
 * <li>Waterfalls from one chunk in half of each 3x3 chunks with a green or lush coast over water: two or three springs
 * side by side in the rim soil spill over the edge and fall into the sea, a lake or a river below; never onto
 * land.</li>
 * </ul>
 */
final class IslandUnderside {

    /** The world as the pass sees it. */
    interface Cells {

        /** The island in a column, {@code bottom << 16 | top}, or -1. */
        int span(int x, int z);

        /** The kind of the island holding a column (whose top is given), or null. */
        IslandKind kind(int x, int z, int top);

        boolean air(int x, int y, int z);

        /** Natural soil (grass, dirt and the like) a spring may open in. */
        boolean soil(int x, int y, int z);

        /** The raw terrain below the islands (BiomicTerrainSampler materials). */
        byte raw(int x, int y, int z);

        /** Whether a village keeps this cell ({@code wide}: or the margin around it, as for bulky features). */
        boolean guarded(int x, int y, int z, boolean wide);

        void set(int x, int y, int z, String state);
    }

    private static final int[][] SIDES = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    /** The vine face toward each side's support. */
    private static final String[] VINES = { "minecraft:vine[west=true]", "minecraft:vine[east=true]",
        "minecraft:vine[north=true]", "minecraft:vine[south=true]" };
    private static final String LICHEN = "minecraft:glow_lichen[down=false,up=true,north=false,south=false,west=false,"
        + "east=false,waterlogged=false]";
    private static final String ROOTS = "minecraft:hanging_roots[waterlogged=false]";
    private static final String SPRING = "minecraft:water[level=0]", SPILL = "minecraft:water[level=1]",
        FALL = "minecraft:water[level=9]";
    /** A rim column at most this thick hangs roots. */
    private static final int THIN_RIM = 5;
    private static final long VINE_SALT = 0x1A2B3C4D5E6F7081L, UNDER_SALT = 0x7F4A7C159E3779B9L,
        FALL_SALT = 0x3C79AC492BA7B653L;

    private IslandUnderside() {}

    static void decorate(long seed, int chunkX, int chunkZ, Cells cells) {
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        boolean any = false;
        for (int x = baseX - 1; x <= baseX + 16 && !any; x++)
            for (int z = baseZ - 1; z <= baseZ + 16 && !any; z++) any = cells.span(x, z) >= 0;
        if (!any) return;
        // Water first: the vines then keep off its column.
        waterfall(seed, chunkX, chunkZ, cells);
        for (int x = baseX; x < baseX + 16; x++) for (int z = baseZ; z < baseZ + 16; z++) {
            int own = cells.span(x, z);
            if (own >= 0) underneath(seed, x, z, own, cells);
            vine(seed, x, z, own, cells);
        }
    }

    /** Roots, lichen or dripstone under an island column. */
    private static void underneath(long seed, int x, int z, int span, Cells cells) {
        int bottom = span >>> 16, top = span & 0xFFFF, y = bottom - 1;
        if (!cells.air(x, y, z) || cells.guarded(x, y, z, false)) return;
        IslandKind kind = cells.kind(x, z, top);
        if (kind == null) return;
        long hash = hash(seed ^ UNDER_SALT, x, z);
        double roll = unit(hash);
        if (top - bottom <= THIN_RIM) {
            if (kind != IslandKind.DRY && roll < 0.25D) cells.set(x, y, z, ROOTS);
            return;
        }
        if (kind == IslandKind.LUSH && roll < 0.02D) {
            cells.set(x, y, z, LICHEN);
            return;
        }
        if (kind != IslandKind.DRY || roll >= 0.15D) return;
        int[] beside = new int[4];
        for (int side = 0; side < 4; side++) beside[side] = cells.span(x + SIDES[side][0], z + SIDES[side][1]);
        if (!IslandSkin.tip(bottom, beside)) return;
        int length = 1 + (int) Math.floorMod(hash >>> 24, 3L);
        while (length > 1 && !cells.air(x, y - length + 1, z)) length--;
        for (int index = 0; index < length; index++) {
            String thickness = length >= 3 && index == 0 ? "base" : index == length - 1 ? "tip" : "frustum";
            cells.set(
                x,
                y - index,
                z,
                "minecraft:pointed_dripstone[thickness=" + thickness + ",vertical_direction=down,waterlogged=false]");
        }
    }

    /** One vine strand in an open column, on the first island face beside it whose roll admits one. */
    private static void vine(long seed, int x, int z, int own, Cells cells) {
        for (int side = 0; side < 4; side++) {
            int nx = x + SIDES[side][0], nz = z + SIDES[side][1];
            int span = cells.span(nx, nz);
            if (span < 0) continue;
            int bottom = span >>> 16, top = span & 0xFFFF;
            int start = own < 0 ? top - 1 : Math.min(top - 1, (own >>> 16) - 1);
            if (start < bottom) continue; // No face of that island beside this column.
            long hash = hash(seed ^ VINE_SALT ^ side, x, z);
            if (unit(hash) >= (own < 0 ? 0.12D : 0.04D)) continue;
            IslandKind kind = cells.kind(nx, nz, top);
            if (kind == null || !kind.verdant() || cells.guarded(x, start, z, false)) continue;
            int length = 2 + (int) Math.floorMod(hash >>> 24, 7L);
            for (int down = 0; down < length && cells.air(x, start - down, z); down++)
                cells.set(x, start - down, z, VINES[side]);
            return;
        }
    }

    /**
     * At most one waterfall in a chunk, two or three blocks wide: a row of springs in rim columns, each walled by
     * island
     * behind and at the row's ends, all spilling to the same open side into columns that are clear down to at least two
     * blocks of water.
     */
    private static void waterfall(long seed, int chunkX, int chunkZ, Cells cells) {
        // One chunk of each 3x3 may hold one, in half of them: falls stand apart, a few to a large island.
        int cellX = Math.floorDiv(chunkX, 3), cellZ = Math.floorDiv(chunkZ, 3);
        long cell = WorldgenContext.mix64(seed ^ FALL_SALT ^ cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL);
        if (unit(cell) >= 0.5D || Math.floorMod(cell, 9L) != Math.floorMod(chunkX, 3) * 3 + Math.floorMod(chunkZ, 3))
            return;
        long chunk = hash(cell, chunkX, chunkZ);
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        int[] open = new int[256];
        java.util.Arrays.fill(open, -1);
        List<long[]> candidates = new ArrayList<>();
        for (int x = baseX; x < baseX + 16; x++) for (int z = baseZ; z < baseZ + 16; z++) {
            int span = cells.span(x, z);
            if (span < 0) continue;
            int top = span & 0xFFFF, side = -1;
            for (int s = 0; s < 4 && side != -2; s++) {
                int nx = x + SIDES[s][0], nz = z + SIDES[s][1];
                int beside = cells.span(nx, nz);
                if (beside < 0) {
                    boolean inside = nx >= baseX && nx < baseX + 16 && nz >= baseZ && nz < baseZ + 16;
                    side = side == -1 && inside ? s : -2;
                } else if (beside >>> 16 > top || (beside & 0xFFFF) < top) side = -2;
            }
            if (side >= 0) {
                open[(x - baseX) * 16 + z - baseZ] = side;
                candidates.add(new long[] { hash(chunk, x, z), x, z, side });
            }
        }
        candidates.sort((a, b) -> Long.compareUnsigned(a[0], b[0]));
        for (int tried = 0; tried < Math.min(6, candidates.size()); tried++) {
            long[] candidate = candidates.get(tried);
            int x = (int) candidate[1], z = (int) candidate[2], side = (int) candidate[3];
            int top = cells.span(x, z) & 0xFFFF;
            // The row runs across the flow: along z for a fall to the west or east, along x for one to the north or
            // south.
            int px = side < 2 ? 0 : 1, pz = side < 2 ? 1 : 0;
            List<int[]> row = new ArrayList<>();
            row.add(new int[] { x, z });
            for (int dir : new int[] { 1, -1 }) for (int step = 1; step <= 2 && row.size() < 3; step++) {
                int rx = x + dir * step * px, rz = z + dir * step * pz;
                if (rx < baseX || rx >= baseX + 16
                    || rz < baseZ
                    || rz >= baseZ + 16
                    || open[(rx - baseX) * 16 + rz - baseZ] != side
                    || (cells.span(rx, rz) & 0xFFFF) != top) break;
                row.add(new int[] { rx, rz });
            }
            if (row.size() < 2) continue;
            IslandKind kind = cells.kind(x, z, top);
            if (kind == null || !kind.verdant()) continue;
            int[] surfaces = new int[row.size()];
            boolean fits = true;
            for (int i = 0; i < row.size() && fits; i++) {
                int rx = row.get(i)[0], rz = row.get(i)[1], fx = rx + SIDES[side][0], fz = rz + SIDES[side][1];
                fits = cells.soil(rx, top, rz) && cells.air(rx, top + 1, rz)
                    && !cells.guarded(rx, top, rz, true)
                    && !cells.guarded(fx, top, fz, true);
                int surface = top;
                while (fits && surface > 0 && cells.raw(fx, surface, fz) == BiomicTerrainSampler.AIR) surface--;
                fits &= surface > 0 && cells.raw(fx, surface, fz) == BiomicTerrainSampler.WATER
                    && cells.raw(fx, surface - 1, fz) == BiomicTerrainSampler.WATER;
                for (int y = top; y > surface && fits; y--) fits = cells.air(fx, y, fz);
                surfaces[i] = surface;
            }
            if (!fits) continue;
            for (int i = 0; i < row.size(); i++) {
                int rx = row.get(i)[0], rz = row.get(i)[1], fx = rx + SIDES[side][0], fz = rz + SIDES[side][1];
                cells.set(rx, top, rz, SPRING);
                cells.set(fx, top, fz, SPILL);
                for (int y = top - 1; y > surfaces[i]; y--) cells.set(fx, y, fz, FALL);
            }
            return;
        }
    }

    private static long hash(long seed, int x, int z) {
        return WorldgenContext.mix64(seed ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL);
    }

    /** 24 bits of a hash in [0, 1). */
    private static double unit(long hash) {
        return (hash >>> 40) / (double) (1L << 24);
    }
}
