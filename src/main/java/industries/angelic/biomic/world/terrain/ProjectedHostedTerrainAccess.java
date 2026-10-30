package industries.angelic.biomic.world.terrain;

import java.util.LinkedHashMap;
import java.util.Map;

import industries.angelic.biomic.api.ChunkVerticalLayout;

/**
 * Full-height physical terrain view for hosted native-height vegetation. This adapter never uncompresses a legacy
 * array: its source must already have rasterized the canonical authored envelope into 512-block storage.
 * Construction does not activate a world preset, a vegetation pass, or an optional layer.
 */
public final class ProjectedHostedTerrainAccess implements HostedTerrainAccess {

    public static final int MAX_CACHED_CHUNKS = 128;
    private static final ChunkVerticalLayout LAYOUT = ChunkVerticalLayout.EXTENDED_512;

    @FunctionalInterface
    public interface ChunkSource {

        /** Complete X/Z/Y-major physical chunk. The adapter copies this array before retaining it. */
        byte[] physicalMaterials(int chunkX, int chunkZ);
    }

    private final ChunkSource source;
    private final Map<Long, byte[]> chunks = new LinkedHashMap<>(32, .75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, byte[]> eldest) {
            return size() > MAX_CACHED_CHUNKS;
        }
    };

    /** Origen mainland-only view; it deliberately claims no floating ownership. */
    public ProjectedHostedTerrainAccess(OrigenProjectedTerrainSampler source) {
        this.source = requireShifted(source)::createMaterialArray;
    }

    private static OrigenProjectedTerrainSampler requireShifted(OrigenProjectedTerrainSampler source) {
        if (source == null || source.projection() != AuthoredVerticalProjections.shifted512()
            || source.layout() != LAYOUT) {
            throw new IllegalArgumentException("Hosted tall terrain requires canonical shifted-512 source terrain");
        }
        return source;
    }

    @Override
    public ChunkVerticalLayout layout() {
        return LAYOUT;
    }

    @Override
    public int minimumEcologyY() {
        return 64;
    }

    @Override
    public AuthoredVerticalProjection projection() {
        return AuthoredVerticalProjections.shifted512();
    }

    @Override
    public byte[] createMaterialArray(int chunkX, int chunkZ) {
        return cached(chunkX, chunkZ).clone();
    }

    /** Highest actual solid mainland support; floating-owned surfaces never conceal the ground surface. */
    @Override
    public int sampleCarvedGroundSurfaceHeight(int blockX, int blockZ) {
        byte[] materials = cached(Math.floorDiv(blockX, 16), Math.floorDiv(blockZ, 16));
        int x = Math.floorMod(blockX, 16), z = Math.floorMod(blockZ, 16);
        for (int y = projection().physicalMaxYExclusive() - 1; y >= minimumEcologyY(); y--) {
            byte material = materials[LAYOUT.index(x, y, z)];
            if (material != BiomicTerrainSampler.AIR && material != BiomicTerrainSampler.WATER
                && material != BiomicTerrainSampler.LAVA) {
                return y;
            }
        }
        // No eligible ground. A bare deep-below surface is not a vegetation root or a fabricated sea floor.
        return minimumEcologyY() - 1;
    }

    @Override
    public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
        return null;
    }

    @Override
    public String floatingHabitatAt(int x, int y, int z) {
        return null;
    }

    /** The (pure) chunk is computed outside the lock so concurrent callers never queue behind one another. */
    private byte[] cached(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) | (chunkZ & 0xffffffffL);
        synchronized (this) {
            byte[] known = chunks.get(key);
            if (known != null) return known;
        }
        byte[] supplied = source.physicalMaterials(chunkX, chunkZ);
        if (supplied == null || supplied.length != LAYOUT.blockCount()) {
            throw new IllegalArgumentException("Hosted physical source must supply all 512 layers");
        }
        byte[] result = supplied.clone();
        synchronized (this) {
            byte[] raced = chunks.get(key);
            if (raced != null) return raced;
            chunks.put(key, result);
        }
        return result;
    }

}
