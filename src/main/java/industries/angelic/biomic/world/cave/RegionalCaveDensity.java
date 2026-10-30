package industries.angelic.biomic.world.cave;

import java.util.Objects;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Detached, single-field Worley/Greg composition for the explicit regional cave provider.
 *
 * <p>
 * The two source fields are blended before excavation. {@link #plan} presents exactly one density field to
 * exactly one whole-clearance planner; it never ORs independently carved masks. This class has no automatic save
 * selection, World or chunk-loading behavior.
 * </p>
 */
public final class RegionalCaveDensity {

    public record Settings(UltimateWorleyField.Settings worley, boolean continuousWorley,
        GregOpenDensity.Policy gregPolicy, GregCavernRegionSampler.Settings regions, CaveComposition.Limits clearance) {

        public Settings {
            Objects.requireNonNull(worley, "worley");
            Objects.requireNonNull(gregPolicy, "gregPolicy");
            Objects.requireNonNull(regions, "regions");
            Objects.requireNonNull(clearance, "clearance");
            if (worley.minY() > clearance.minY() || worley.maxY() < clearance.maxY()) {
                throw new IllegalArgumentException("Worley field does not cover the clearance planning envelope");
            }
        }
    }

    /** One immutable X/Z sample; its regional influence is never recomputed for different Y values. */
    public interface Column {

        double openDensity(int legacyBlockY);
    }

    @FunctionalInterface
    interface ScalarDensity {

        double at(int x, int y, int z);
    }

    private final UltimateWorleyField worley;
    private final GregOpenDensity greg;
    private final GregCavernRegionSampler regions;

    public RegionalCaveDensity(long stableSeed, Settings settings) {
        worley = new UltimateWorleyField(stableSeed, settings.worley(), settings.continuousWorley());
        greg = new GregOpenDensity(stableSeed, settings.gregPolicy());
        regions = new GregCavernRegionSampler(stableSeed, settings.regions());
    }

    /** Worley chunk fields from the caller's longer-lived cache; they must be this field's {@code chunk} output. */
    CaveComposition.Density createPlanningDensity(ChunkFields fields) {
        return productionField(java.util.Objects.requireNonNull(fields, "fields"))::at;
    }

    /** Per-chunk Worley lattice values, X/Z/Y-major over 16x16x256. */
    @FunctionalInterface
    public interface ChunkFields {

        float[] at(int chunkX, int chunkZ);
    }

    private PlanField productionField(ChunkFields shared) {
        // A field is confined to one finite planning operation. Hit probes must not allocate boxed
        // world coordinates or a new capturing miss function for every density sample.
        var chunks = new Long2ObjectOpenHashMap<float[]>();
        var columns = new Long2ObjectOpenHashMap<Column>();
        ScalarDensity cachedWorley = (x, y, z) -> {
            int chunkX = Math.floorDiv(x, 16), chunkZ = Math.floorDiv(z, 16);
            long key = key(chunkX, chunkZ);
            float[] values = chunks.get(key);
            if (values == null) {
                values = shared == null ? worley.chunk(chunkX, chunkZ) : shared.at(chunkX, chunkZ);
                chunks.put(key, values);
            }
            return values[((Math.floorMod(x, 16) * 16 + Math.floorMod(z, 16)) * 256) + y];
        };
        // Greg's noise caves were a terrain-grid kernel: sample them on the 4x8x4 noise lattice and interpolate,
        // as the source terrain did, instead of three octave-noise stacks at every probed block.
        var gregChunks = new Long2ObjectOpenHashMap<float[]>();
        ScalarDensity gregField = (x, y, z) -> {
            int chunkX = Math.floorDiv(x, 16), chunkZ = Math.floorDiv(z, 16);
            long key = key(chunkX, chunkZ);
            float[] lattice = gregChunks.get(key);
            if (lattice == null) {
                lattice = gregLattice(chunkX, chunkZ);
                gregChunks.put(key, lattice);
            }
            return interpolateGreg(lattice, Math.floorMod(x, 16), y, Math.floorMod(z, 16));
        };
        return (x, y, z) -> {
            long key = key(x, z);
            Column known = columns.get(key);
            if (known == null) {
                known = column(x, z, checkedWeight(regions.weight(x, z)), cachedWorley, gregField);
                columns.put(key, known);
            }
            return known.openDensity(y);
        };
    }

    private static final int GREG_NODES_XZ = 5, GREG_NODES_Y = 33;

    private float[] gregLattice(int chunkX, int chunkZ) {
        float[] lattice = new float[GREG_NODES_XZ * GREG_NODES_XZ * GREG_NODES_Y];
        for (int i = 0; i < GREG_NODES_XZ; i++) for (int k = 0; k < GREG_NODES_XZ; k++)
            for (int j = 0; j < GREG_NODES_Y; j++) lattice[(i * GREG_NODES_XZ + k) * GREG_NODES_Y
                + j] = (float) gregAt(chunkX * 16 + i * 4, Math.min(255, j * 8), chunkZ * 16 + k * 4);
        return lattice;
    }

    private static double interpolateGreg(float[] lattice, int localX, int y, int localZ) {
        int i = localX >> 2, k = localZ >> 2, j = y >> 3;
        double fx = (localX & 3) / 4.0D, fz = (localZ & 3) / 4.0D, fy = (y & 7) / 8.0D;
        double c00 = lerp(fy, node(lattice, i, j, k), node(lattice, i, j + 1, k));
        double c10 = lerp(fy, node(lattice, i + 1, j, k), node(lattice, i + 1, j + 1, k));
        double c01 = lerp(fy, node(lattice, i, j, k + 1), node(lattice, i, j + 1, k + 1));
        double c11 = lerp(fy, node(lattice, i + 1, j, k + 1), node(lattice, i + 1, j + 1, k + 1));
        return lerp(fz, lerp(fx, c00, c10), lerp(fx, c01, c11));
    }

    private static double node(float[] lattice, int i, int j, int k) {
        return lattice[(i * GREG_NODES_XZ + k) * GREG_NODES_Y + j];
    }

    private static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }

    private static Column column(int x, int z, double weight, ScalarDensity worley, ScalarDensity greg) {
        return new Column() {

            @Override
            public double openDensity(int y) {
                if (y < 0 || y > 255) throw new IllegalArgumentException("Legacy cave Y must be within 0..255");
                if (weight == 0.0D) return checkedDensity(worley.at(x, y, z));
                if (weight == 1.0D) return checkedDensity(greg.at(x, y, z));
                return CaveComposition
                    .blend(checkedDensity(worley.at(x, y, z)), checkedDensity(greg.at(x, y, z)), weight);
            }
        };
    }

    private double gregAt(int x, int y, int z) {
        return greg.openDensity(x, y, z);
    }

    private static double checkedWeight(double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("Regional Greg influence must be finite and within 0..1");
        }
        return value;
    }

    private static double checkedDensity(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Regional cave density must be finite");
        return value;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    @FunctionalInterface
    private interface PlanField {

        double at(int x, int y, int z);
    }
}
