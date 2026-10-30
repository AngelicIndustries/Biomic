package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.WorldgenContext;

/** Samples the horizontal controls consumed by Biomic's Origen-derived terrain graph. */
final class TerrainColumnSampler {

    private static final int MIN_FLOOR = 1;
    private static final int MAX_SURFACE = BiomicTerrainSampler.WORLD_HEIGHT - 1;
    private static final int COLUMN_CACHE_SIZE = 32_768;

    private final MacroTerrainSampler macroSampler;
    private final OrigenTerrainGraph terrainGraph;
    private final LongColumnCache columnCache = new LongColumnCache(COLUMN_CACHE_SIZE);

    TerrainColumnSampler(MacroTerrainSampler macroSampler, OrigenTerrainGraph terrainGraph) {
        this.macroSampler = macroSampler;
        this.terrainGraph = terrainGraph;
    }

    TerrainColumn sample(int blockX, int blockZ) {
        long key = coordinateKey(blockX, blockZ);
        TerrainColumn cached = columnCache.get(key);
        if (cached != null) {
            return cached;
        }
        TerrainColumn sampled = sampleUncached(blockX, blockZ);
        return columnCache.putIfAbsent(key, sampled);
    }

    private TerrainColumn sampleUncached(int blockX, int blockZ) {
        MacroTerrainSample fields = macroSampler.sample(blockX, blockZ);
        TerrainShapeSample shape = terrainGraph.sampleColumn(fields, blockX, blockZ);

        int naturalSurfaceHeight = clamp((int) Math.round(shape.getOffsetHeight()), MIN_FLOOR, MAX_SURFACE);
        int surfaceHeight = naturalSurfaceHeight;
        OrigenBiomeProfile profile = shape.getPrimaryProfile();
        double riverStrength = isRiver(profile) ? 1.0D : 0.0D;
        return new TerrainColumn(surfaceHeight, naturalSurfaceHeight, riverStrength, shape.getPrimaryProfile());
    }

    private static long coordinateKey(int blockX, int blockZ) {
        return (long) blockX << 32 ^ blockZ & 0xFFFF_FFFFL;
    }

    /** Fixed-memory cache; a collision causes a safe deterministic resample. */
    private static final class LongColumnCache {

        private final long[] keys;
        private final TerrainColumn[] values;
        private final int mask;

        private LongColumnCache(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Terrain column cache size must be a power of two");
            }
            keys = new long[size];
            values = new TerrainColumn[size];
            mask = size - 1;
        }

        private synchronized TerrainColumn get(long key) {
            int index = (int) WorldgenContext.mix64(key) & mask;
            return values[index] != null && keys[index] == key ? values[index] : null;
        }

        private synchronized TerrainColumn putIfAbsent(long key, TerrainColumn value) {
            int index = (int) WorldgenContext.mix64(key) & mask;
            if (values[index] != null && keys[index] == key) {
                return values[index];
            }
            keys[index] = key;
            values[index] = value;
            return value;
        }
    }

    private static boolean isRiver(OrigenBiomeProfile profile) {
        return profile.name()
            .contains("RIVER");
    }

    private static int clamp(int value, int lower, int upper) {
        return Math.max(lower, Math.min(upper, value));
    }
}
