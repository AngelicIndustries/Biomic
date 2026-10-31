package industries.angelic.biomic.world.terrain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Explicit shifted native-wetland terrain stage. The input must be the pristine shifted mainland/deep-below
 * foundation, before intrinsic cave or floating ownership is composed. This adapter does not select a save policy.
 */
public final class ShiftedNativeMarshTerrainAccess implements HostedTerrainAccess {

    public static final int MAX_CACHED_CHUNKS = 128;
    private final HostedTerrainAccess foundation;
    private final HostedMarshTerrain marsh;
    private final Map<Long, byte[]> chunks = new LinkedHashMap<Long, byte[]>(32, .75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, byte[]> entry) {
            return size() > MAX_CACHED_CHUNKS;
        }
    };

    public ShiftedNativeMarshTerrainAccess(WorldgenContext context, HostedTerrainAccess pristineFoundation) {
        this(pristineFoundation, requireContext(context));
    }

    private ShiftedNativeMarshTerrainAccess(HostedTerrainAccess foundation, WorldgenContext context) {
        this(foundation, HostedMarshTerrain.shifted512(context));
        if (!context.getNativeHabitats()
            .shifted512()
            || !context.getNativeHabitats()
                .fullMarsh())
            throw new IllegalArgumentException("Native marsh terrain requires the saved shifted full-marsh revision");
    }

    ShiftedNativeMarshTerrainAccess(HostedTerrainAccess foundation, HostedMarshTerrain marsh) {
        this.foundation = Objects.requireNonNull(foundation, "Native marsh foundation");
        if (foundation.layout() != ChunkVerticalLayout.EXTENDED_512
            || foundation.projection() != AuthoredVerticalProjections.shifted512()
            || foundation.minimumEcologyY() != 64)
            throw new IllegalArgumentException("Native marsh terrain requires canonical shifted pristine terrain");
        this.marsh = Objects.requireNonNull(marsh, "Shifted marsh transform");
    }

    private static WorldgenContext requireContext(WorldgenContext context) {
        return Objects.requireNonNull(context, "Worldgen context");
    }

    @Override
    public ChunkVerticalLayout layout() {
        return ChunkVerticalLayout.EXTENDED_512;
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

    /** Lock-free apart from the cache lookups: a miss builds the chunk while other threads keep reading. */
    @Override
    public int sampleCarvedGroundSurfaceHeight(int x, int z) {
        int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16), lx = Math.floorMod(x, 16), lz = Math.floorMod(z, 16);
        byte[] values = cached(cx, cz);
        for (int y = 447; y >= 64; y--) {
            byte value = values[layout().index(lx, y, lz)];
            if (value != BiomicTerrainSampler.AIR && value != BiomicTerrainSampler.WATER
                && value != BiomicTerrainSampler.LAVA) return y;
        }
        return 63;
    }

    @Override
    public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
        return foundation.floatingArchetypeAt(x, y, z);
    }

    @Override
    public String floatingHabitatAt(int x, int y, int z) {
        return foundation.floatingHabitatAt(x, y, z);
    }

    /** Reconciles only changed marsh cells into the authored feature view; callers retain all other source bytes. */
    public byte[] authoredView(byte[] authored, int blockX, int blockZ) {
        int cx = Math.floorDiv(blockX, 16), cz = Math.floorDiv(blockZ, 16);
        Supplier<byte[]> source = () -> createMaterialArray(cx, cz);
        return marsh.authoredView(authored, blockX, blockZ, source, () -> restore(cx, cz));
    }

    /**
     * Private population caller reauthenticates its exact frame/source/ticket at each original getter.
     * Original authored validation and habitat callbacks remain inside HostedMarshTerrain before source.get.
     */
    byte[] authoredView(byte[] authored, int blockX, int blockZ,
        OrigenFeatureTerrainAccess.NativeMarshPreparationLookup lookup) {
        int cx = Math.floorDiv(blockX, 16), cz = Math.floorDiv(blockZ, 16);
        Supplier<byte[]> source = () -> {
            lookup.prepare(this, cx, cz, 1);
            return createMaterialArray(cx, cz);
        };
        return marsh.authoredView(
            authored,
            blockX,
            blockZ,
            source,
            () -> { if (!lookup.prepare(this, cx, cz, 2)) restore(cx, cz); });
    }

    private void restore(int cx, int cz) {
        synchronized (this) {
            chunks.remove((long) cx << 32 ^ (cz & 0xffffffffL));
        }
        cached(cx, cz);
    }

    /**
     * The (pure) chunk is computed outside the lock so worker threads never queue behind one another; callers must
     * not hold it either. A stored chunk is never written again.
     */
    private byte[] cached(int cx, int cz) {
        long key = (long) cx << 32 ^ (cz & 0xffffffffL);
        synchronized (this) {
            byte[] known = chunks.get(key);
            if (known != null) return known;
        }
        byte[] made = foundation.createMaterialArray(cx, cz);
        if (made == null || made.length != layout().blockCount())
            throw new IllegalArgumentException("Native marsh foundation returned an incomplete physical chunk");
        made = made.clone();
        byte[] origins = new byte[made.length];
        for (int i = 0; i < made.length; i++)
            if (made[i] != BiomicTerrainSampler.AIR && made[i] != BiomicTerrainSampler.WATER
                && made[i] != BiomicTerrainSampler.LAVA) origins[i] = TerrainDensitySampler.GROUND_ORIGIN;
        marsh.apply(made, origins, cx, cz);
        synchronized (this) {
            byte[] raced = chunks.get(key);
            if (raced != null) return raced;
            chunks.put(key, made);
        }
        return made;
    }
}
