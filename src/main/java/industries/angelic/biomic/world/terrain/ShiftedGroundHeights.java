package industries.angelic.biomic.world.terrain;

import java.util.Arrays;

import industries.angelic.biomic.world.WorldgenContext;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/**
 * Shifted512 ground heights for structure admission, one column at a time from Origen's authored density, without
 * building chunks (a village footprint or mineshaft layout touches dozens of them). Carvers are ignored: they cost a
 * per-chunk lattice and rarely open the surface. Physical Y = authored Y + 128.
 */
public final class ShiftedGroundHeights {

    private static final int SHIFT = 128;
    private static final int MIN_AUTHORED = OrigenDensityField.UPSTREAM_MIN_Y;
    private static final int MAX_AUTHORED = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - 1;
    private static final int COARSE_STEP = 8;
    private static final int CACHE_SIZE = 1 << 16;
    /** Origen's lattice spacing (OrigenDensityField's Terra grid) and its rows over the authored range. */
    private static final int GRID = 4;
    private static final int MIN_LATTICE = Math.floorDiv(MIN_AUTHORED, GRID) * GRID;
    private static final int LATTICE_ROWS = (Math.floorDiv(MAX_AUTHORED + 1, GRID) * GRID + GRID - MIN_LATTICE) / GRID
        + 1;
    private static final int ROW_CACHE_CELLS = 512;
    /** Covers rounding in the interpolation: a sample is skipped only when clearly below zero. */
    private static final double BOUND_MARGIN = 1.0E-6D;
    private final OrigenDensityField field;
    private final Long2IntLinkedOpenHashMap heights = new Long2IntLinkedOpenHashMap(CACHE_SIZE + 1);
    private final ThreadLocal<Long2ObjectLinkedOpenHashMap<double[]>> rowCache = ThreadLocal
        .withInitial(() -> new Long2ObjectLinkedOpenHashMap<>(ROW_CACHE_CELLS + 1));

    public ShiftedGroundHeights(WorldgenContext context) {
        field = OrigenDensityField.shared(context);
        heights.defaultReturnValue(Integer.MIN_VALUE);
    }

    /** Highest solid physical Y of the column, or 63 (below Origen's floor) when it has none. */
    public int ground(int x, int z) {
        long key = (long) x << 32 ^ (z & 0xFFFF_FFFFL);
        synchronized (heights) {
            int known = heights.getAndMoveToLast(key);
            if (known != Integer.MIN_VALUE) return known;
        }
        int height = compute(x, z);
        synchronized (heights) {
            heights.putAndMoveToLast(key, height);
            if (heights.size() > CACHE_SIZE) heights.removeFirstInt();
        }
        return height;
    }

    private int compute(int x, int z) {
        double[] rows = rows(x >> 2, z >> 2);
        double elevation = field.sampleElevation(x, z);
        int y = MAX_AUTHORED;
        while (y >= MIN_AUTHORED && !solid(x, y, z, rows, elevation)) y -= COARSE_STEP;
        if (y < MIN_AUTHORED) return SHIFT + MIN_AUTHORED - 1;
        while (y < MAX_AUTHORED && solid(x, y + 1, z, rows, elevation)) y++;
        return y + SHIFT;
    }

    /**
     * Whether the interpolated density is positive. The density is a trilinear blend of its lattice cell's corners
     * plus the column's elevation, so it cannot exceed the highest corner around the sample plus the elevation:
     * where that is negative (the open air above the ground) the sample is skipped, with the same answer.
     */
    private boolean solid(int x, int authoredY, int z, double[] rows, double elevation) {
        int lower = Math.floorDiv(authoredY, GRID) * GRID;
        double bound = row(rows, x, lower, z);
        if (authoredY != lower) bound = Math.max(bound, row(rows, x, lower + GRID, z));
        if (bound + elevation < -BOUND_MARGIN) return false;
        return field.sampleUpstreamInterpolated(x, authoredY, z) > 0.0D;
    }

    /** The highest of the four lattice corners around a column at one lattice row, cached per cell. */
    private double row(double[] rows, int x, int latticeY, int z) {
        int index = (latticeY - MIN_LATTICE) / GRID;
        double known = rows[index];
        if (!Double.isNaN(known)) return known;
        int lowerX = x >> 2 << 2, lowerZ = z >> 2 << 2;
        double highest = Math.max(
            Math.max(
                field.sampleBlendedLattice(lowerX, latticeY, lowerZ),
                field.sampleBlendedLattice(lowerX + GRID, latticeY, lowerZ)),
            Math.max(
                field.sampleBlendedLattice(lowerX, latticeY, lowerZ + GRID),
                field.sampleBlendedLattice(lowerX + GRID, latticeY, lowerZ + GRID)));
        rows[index] = highest;
        return highest;
    }

    /** One lattice cell's row maxima, per thread: the 16 columns of a cell share them. */
    private double[] rows(int cellX, int cellZ) {
        Long2ObjectLinkedOpenHashMap<double[]> cells = rowCache.get();
        long key = (long) cellX << 32 ^ (cellZ & 0xFFFF_FFFFL);
        double[] rows = cells.getAndMoveToLast(key);
        if (rows == null) {
            rows = new double[LATTICE_ROWS];
            Arrays.fill(rows, Double.NaN);
            cells.putAndMoveToLast(key, rows);
            if (cells.size() > ROW_CACHE_CELLS) cells.removeFirst();
        }
        return rows;
    }
}
