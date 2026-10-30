package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.Objects;
import java.util.function.Supplier;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Typed terrain inputs used by Origen feature planning. Implementations expose either the complete legacy hosted
 * terrain semantics or the explicit Origen-only projected terrain; they never infer a projection from storage height.
 */
public interface OrigenFeatureTerrainAccess {

    int AUTHORED_HEIGHT = 384;
    int AUTHORED_CHUNK_LENGTH = BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH * AUTHORED_HEIGHT;

    OrigenFeatureHeightTransport heightTransport();

    /** Fresh physical pre-feature material array in the selected layout. The caller owns the returned array. */
    byte[] createPhysicalMaterials(int chunkX, int chunkZ);

    /** Complete authored column after any configured legacy hosted overlays. */
    byte[] authoredColumn(int blockX, int blockZ, Supplier<byte[]> physicalMaterials);

    /** Applies configured hosted overlays to an already-computed authored chunk. */
    byte[] authoredView(byte[] authored, int blockX, int blockZ, Supplier<byte[]> physicalMaterials,
        BitSet completedCaveMask);

    /**
     * Optional private owner restoration at the original source-getter points. No values or authority
     * are supplied to generic terrain modes; their original authoredView remains literal.
     */
    @FunctionalInterface
    interface NativeMarshPreparationLookup {

        boolean prepare(ShiftedNativeMarshTerrainAccess source, int chunkX, int chunkZ, int point);
    }

    default byte[] authoredViewWithPreparedNativeMarsh(byte[] authored, int x, int z, Supplier<byte[]> physical,
        BitSet caves, NativeMarshPreparationLookup lookup) {
        return authoredView(authored, x, z, physical, caves);
    }

    FloatingIslandArchetype floatingArchetypeAt(int x, int physicalY, int z);

    /** Fails when this terrain mode has no hosted/protected-ground height service. */
    int carvedGroundSurfaceHeight(int x, int z);

    boolean surfaceBoulderIntersectsExpansion(int x, int physicalY, int z);

    boolean isExpansionCavity(int x, int physicalY, int z);

    static OrigenFeatureTerrainAccess legacy(WorldgenContext context, BiomicTerrainSampler terrain) {
        return new Legacy(context, terrain);
    }

    static OrigenFeatureTerrainAccess projectedHosted(WorldgenContext context, OrigenProjectedTerrainSampler terrain,
        HostedTerrainAccess hosted) {
        return new Projected(context, terrain, Objects.requireNonNull(hosted, "Hosted terrain access"));
    }

    /** Explicit complete-intrinsic composition; no existing projected factory selects this path. */
    static OrigenFeatureTerrainAccess projectedIntrinsicHosted(WorldgenContext context,
        OrigenProjectedTerrainSampler terrain, IntrinsicHostedTerrainAccess hosted) {
        return new IntrinsicProjected(context, terrain, hosted);
    }

    /** Explicit saved native-height foundation, followed by independently owned intrinsic environments. */
    static OrigenFeatureTerrainAccess projectedNativeIntrinsicHosted(WorldgenContext context,
        OrigenProjectedTerrainSampler terrain, IntrinsicHostedTerrainAccess hosted,
        ShiftedNativeMarshTerrainAccess marsh) {
        if (!context.getNativeHabitats()
            .shifted512()
            || context.getNativeHabitats()
                .fullMarsh() != (marsh != null))
            throw new IllegalArgumentException(
                "Native feature view requires the matching saved shifted-marsh contract");
        OrigenFeatureTerrainAccess source = new Projected(context, terrain, null);
        return new IntrinsicProjected(
            terrain,
            hosted,
            marsh == null ? source : new NativeMarshProjected(source, marsh));
    }

    final class NativeMarshProjected implements OrigenFeatureTerrainAccess {

        private final OrigenFeatureTerrainAccess source;
        private final ShiftedNativeMarshTerrainAccess marsh;

        private NativeMarshProjected(OrigenFeatureTerrainAccess source, ShiftedNativeMarshTerrainAccess marsh) {
            this.source = source;
            this.marsh = marsh;
        }

        @Override
        public OrigenFeatureHeightTransport heightTransport() {
            return source.heightTransport();
        }

        @Override
        public byte[] createPhysicalMaterials(int x, int z) {
            return marsh.createMaterialArray(x, z);
        }

        private byte[] preparedPhysicalMaterials(int x, int z, NativeMarshPreparationLookup lookup) {
            lookup.prepare(marsh, x, z, 0);
            return createPhysicalMaterials(x, z); // Original getter still runs exactly once.
        }

        private byte[] preparedAuthoredView(byte[] authored, int x, int z, NativeMarshPreparationLookup lookup) {
            return marsh.authoredView(authored, x, z, lookup);
        }

        @Override
        public byte[] authoredColumn(int x, int z, Supplier<byte[]> physical) {
            return marsh.authoredView(source.authoredColumn(x, z, physical), x, z);
        }

        @Override
        public byte[] authoredView(byte[] authored, int x, int z, Supplier<byte[]> physical, BitSet caves) {
            return marsh.authoredView(authored, x, z);
        }

        @Override
        public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
            return marsh.floatingArchetypeAt(x, y, z);
        }

        @Override
        public int carvedGroundSurfaceHeight(int x, int z) {
            return marsh.sampleCarvedGroundSurfaceHeight(x, z);
        }

        @Override
        public boolean surfaceBoulderIntersectsExpansion(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean isExpansionCavity(int x, int y, int z) {
            return false;
        }
    }

    final class IntrinsicProjected implements OrigenFeatureTerrainAccess {

        private final OrigenFeatureTerrainAccess source;
        private final IntrinsicHostedTerrainAccess hosted;

        private IntrinsicProjected(WorldgenContext context, OrigenProjectedTerrainSampler terrain,
            IntrinsicHostedTerrainAccess hosted) {
            this(terrain, hosted, new Projected(context, terrain, null));
        }

        private IntrinsicProjected(OrigenProjectedTerrainSampler terrain, IntrinsicHostedTerrainAccess hosted,
            OrigenFeatureTerrainAccess source) {
            if (terrain == null || terrain.layout() != ChunkVerticalLayout.EXTENDED_512
                || terrain.projection() != AuthoredVerticalProjections.shifted512()
                || hosted == null
                || hosted.layout() != ChunkVerticalLayout.EXTENDED_512
                || hosted.projection() != AuthoredVerticalProjections.shifted512()
                || hosted.minimumEcologyY() != 64)
                throw new IllegalArgumentException("Invalid intrinsic shifted terrain source");
            this.source = source;
            this.hosted = hosted;
        }

        @Override
        public OrigenFeatureHeightTransport heightTransport() {
            return source.heightTransport();
        }

        @Override
        public byte[] createPhysicalMaterials(int cx, int cz) {
            byte[] original = source.createPhysicalMaterials(cx, cz);
            byte[] composed = hosted.createMaterialArray(cx, cz);
            requireIntrinsicChanges(original, composed, cx, cz, hosted);
            return composed;
        }

        @Override
        public byte[] authoredColumn(int x, int z, Supplier<byte[]> physical) {
            byte[] result = source.authoredColumn(x, z, physical)
                .clone();
            byte[] cells = requirePhysical(physical.get());
            requireIntrinsicColumnChanges(result, cells, x, z, hosted);
            for (int y = 64; y < 448; y++)
                if (y < 80 || hosted.ownsIntrinsicEnvironment(x, y, z) && hosted.authoredVisible(x, y, z))
                    result[y - 64] = cells[ChunkVerticalLayout.EXTENDED_512.index(x & 15, y, z & 15)];
            return result;
        }

        @Override
        public byte[] authoredView(byte[] authored, int blockX, int blockZ, Supplier<byte[]> physical, BitSet caves) {
            byte[] cells = physical.get();
            requireIntrinsicChanges(
                source.createPhysicalMaterials(blockX >> 4, blockZ >> 4),
                cells,
                blockX >> 4,
                blockZ >> 4,
                hosted);
            return intrinsicAuthoredView(
                source.authoredView(authored, blockX, blockZ, physical, caves),
                cells,
                blockX,
                blockZ,
                hosted);
        }

        @Override
        public byte[] authoredViewWithPreparedNativeMarsh(byte[] authored, int blockX, int blockZ,
            Supplier<byte[]> physical, BitSet caves, NativeMarshPreparationLookup lookup) {
            if (lookup == null || !(source instanceof NativeMarshProjected nativeMarsh))
                return authoredView(authored, blockX, blockZ, physical, caves);
            byte[] cells = physical.get();
            requireIntrinsicChanges(
                nativeMarsh.preparedPhysicalMaterials(blockX >> 4, blockZ >> 4, lookup),
                cells,
                blockX >> 4,
                blockZ >> 4,
                hosted);
            // Original authored length/alignment/habitat checks precede any further optional restoration.
            return intrinsicAuthoredView(
                nativeMarsh.preparedAuthoredView(authored, blockX, blockZ, lookup),
                cells,
                blockX,
                blockZ,
                hosted);
        }

        @Override
        public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
            return hosted.floatingArchetypeAt(x, y, z);
        }

        @Override
        public int carvedGroundSurfaceHeight(int x, int z) {
            return hosted.sampleCarvedGroundSurfaceHeight(x, z);
        }

        @Override
        public boolean surfaceBoulderIntersectsExpansion(int x, int y, int z) {
            return hosted.ownsIntrinsicEnvironment(x, y, z) && hosted.authoredVisible(x, y, z);
        }

        @Override
        public boolean isExpansionCavity(int x, int y, int z) {
            return hosted.ownsIntrinsicEnvironment(x, y, z) && hosted.authoredVisible(x, y, z);
        }
    }

    /** Tests and benches set this to fail on an ownership mismatch; a live server logs it and keeps generating. */
    String STRICT_OWNERSHIP_PROPERTY = "biomic.strictTerrainOwnership";
    java.util.concurrent.atomic.AtomicInteger UNOWNED_REPORTS = new java.util.concurrent.atomic.AtomicInteger();

    /**
     * A composed cell that differs from the source with no intrinsic owner. It means two derivations of the same chunk
     * disagree (seen once on a review server, 2026-10-22, not reproduced in benches); the authored view then keeps the
     * source cell. Never worth a crash: the first few are logged with their coordinates.
     */
    private static void unowned(String what, int x, int y, int z, byte source, byte composed) {
        String message = "Intrinsic host modified unowned terrain " + what
            + " at "
            + x
            + ","
            + y
            + ","
            + z
            + " source="
            + (source & 255)
            + " composed="
            + (composed & 255)
            + " owner=false";
        if (Boolean.getBoolean(STRICT_OWNERSHIP_PROPERTY)) throw new IllegalStateException(message);
        int n = UNOWNED_REPORTS.incrementAndGet();
        if (n <= 5) industries.angelic.biomic.Biomic.LOG.warn("{} (report {}; generation continues)", message, n);
    }

    static void requireIntrinsicChanges(byte[] original, byte[] composed, int chunkX, int chunkZ,
        IntrinsicHostedTerrainAccess hosted) {
        requirePhysical(original);
        requirePhysical(composed);
        int minX = chunkX << 4, minZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 80; y < 512; y++) {
            int i = ChunkVerticalLayout.EXTENDED_512.index(x, y, z);
            if (original[i] != composed[i] && !CaveThemes.isSkin(original[i], composed[i])
                && !hosted.ownsIntrinsicEnvironment(minX + x, y, minZ + z)) {
                unowned("cell", minX + x, y, minZ + z, original[i], composed[i]);
                return;
            }
        }
    }

    static void requireIntrinsicColumnChanges(byte[] sourceColumn, byte[] composed, int blockX, int blockZ,
        IntrinsicHostedTerrainAccess hosted) {
        if (sourceColumn == null || sourceColumn.length != AUTHORED_HEIGHT)
            throw new IllegalArgumentException("Expected complete authored column");
        requirePhysical(composed);
        for (int y = 64; y < 448; y++) {
            int physicalIndex = ChunkVerticalLayout.EXTENDED_512.index(blockX & 15, y, blockZ & 15);
            if (sourceColumn[y - 64] != composed[physicalIndex] && y >= 80
                && !CaveThemes.isSkin(sourceColumn[y - 64], composed[physicalIndex])
                && !hosted.ownsIntrinsicEnvironment(blockX, y, blockZ)) {
                unowned("column", blockX, y, blockZ, sourceColumn[y - 64], composed[physicalIndex]);
                return;
            }
        }
    }

    static byte[] intrinsicAuthoredView(byte[] original, byte[] physical, int blockX, int blockZ,
        IntrinsicHostedTerrainAccess hosted) {
        byte[] result = requireAuthoredChunk(original).clone();
        requirePhysical(physical);
        // Copying an identical cell is a no-op, so ownership is consulted only where the two differ.
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 64; y < 448; y++) {
            int a = (x * 16 + z) * 384 + y - 64, p = ChunkVerticalLayout.EXTENDED_512.index(x, y, z);
            if (result[a] != physical[p] && (y < 80 || CaveThemes.isSkin(result[a], physical[p])
                || hosted.ownsIntrinsicEnvironment(blockX + x, y, blockZ + z)
                    && hosted.authoredVisible(blockX + x, y, blockZ + z)))
                result[a] = physical[p];
        }
        return result;
    }

    // Public interface helper avoids a private-interface nest bridge in the Java8 downgraded runtime.
    static byte[] requirePhysical(byte[] physical) {
        if (physical == null || physical.length != ChunkVerticalLayout.EXTENDED_512.blockCount())
            throw new IllegalArgumentException("Expected complete physical512 terrain");
        return physical;
    }

    static byte[] requireAuthoredChunk(byte[] authored) {
        if (authored == null || authored.length != AUTHORED_CHUNK_LENGTH) {
            throw new IllegalArgumentException("Authored terrain must contain one 384-layer chunk");
        }
        return authored;
    }

    final class Legacy implements OrigenFeatureTerrainAccess {

        private final BiomicTerrainSampler terrain;
        private final OrigenSurfaceRuntime surface;
        private final OrigenFeatureHeightTransport height = new OrigenFeatureHeightTransport(
            AuthoredVerticalProjections.origenCompressed256(),
            ChunkVerticalLayout.LEGACY_256);

        private Legacy(WorldgenContext context, BiomicTerrainSampler terrain) {
            surface = OrigenSurfaceRuntime.shared(Objects.requireNonNull(context, "Worldgen context"));
            this.terrain = Objects.requireNonNull(terrain, "Legacy terrain sampler");
        }

        @Override
        public OrigenFeatureHeightTransport heightTransport() {
            return height;
        }

        @Override
        public byte[] createPhysicalMaterials(int chunkX, int chunkZ) {
            return terrain.createMaterialArray(chunkX, chunkZ);
        }

        @Override
        public byte[] authoredColumn(int blockX, int blockZ, Supplier<byte[]> physicalMaterials) {
            Objects.requireNonNull(physicalMaterials, "Physical material supplier");
            return terrain.oceanAuthoredView(
                surface.createAuthoredMaterialColumn(blockX, blockZ),
                blockX,
                blockZ,
                physicalMaterials);
        }

        @Override
        public byte[] authoredView(byte[] authored, int blockX, int blockZ, Supplier<byte[]> physicalMaterials,
            BitSet completedCaveMask) {
            requireAuthoredChunk(authored);
            Objects.requireNonNull(physicalMaterials, "Physical material supplier");
            return terrain.oceanAuthoredView(authored, blockX, blockZ, physicalMaterials, completedCaveMask);
        }

        @Override
        public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
            return terrain.floatingArchetypeAt(x, y, z);
        }

        @Override
        public int carvedGroundSurfaceHeight(int x, int z) {
            return terrain.sampleCarvedGroundSurfaceHeight(x, z);
        }

        @Override
        public boolean surfaceBoulderIntersectsExpansion(int x, int y, int z) {
            return terrain.surfaceBoulderIntersectsExpansion(x, y, z);
        }

        @Override
        public boolean isExpansionCavity(int x, int y, int z) {
            return terrain.isExpansionCavity(x, y, z);
        }
    }

    final class Projected implements OrigenFeatureTerrainAccess {

        private final OrigenProjectedTerrainSampler terrain;
        private final HostedTerrainAccess hosted;
        private final OrigenSurfaceRuntime surface;
        private final OrigenFeatureHeightTransport height;

        private Projected(WorldgenContext context, OrigenProjectedTerrainSampler terrain, HostedTerrainAccess hosted) {
            surface = OrigenSurfaceRuntime.shared(Objects.requireNonNull(context, "Worldgen context"));
            this.terrain = Objects.requireNonNull(terrain, "Projected terrain sampler");
            height = new OrigenFeatureHeightTransport(terrain.projection(), terrain.layout());
            if (hosted != null && (hosted.layout() != terrain.layout() || hosted.projection() != terrain.projection()
                || hosted.minimumEcologyY() != 64)) {
                throw new IllegalArgumentException("Hosted and projected Origen terrain disagree on shifted storage");
            }
            this.hosted = hosted;
        }

        @Override
        public OrigenFeatureHeightTransport heightTransport() {
            return height;
        }

        @Override
        public byte[] createPhysicalMaterials(int chunkX, int chunkZ) {
            byte[] projected = terrain.createMaterialArray(chunkX, chunkZ);
            if (hosted == null) return projected;
            byte[] shared = hosted.createMaterialArray(chunkX, chunkZ);
            if (!java.util.Arrays.equals(projected, shared)) {
                throw new IllegalStateException("Hosted mainland does not share the projected Origen physical source");
            }
            return shared;
        }

        @Override
        public byte[] authoredColumn(int blockX, int blockZ, Supplier<byte[]> physicalMaterials) {
            Objects.requireNonNull(physicalMaterials, "Physical material supplier");
            // The published chunk includes orphan polish; raw column sampling does not. Using the latter
            // falsely attributes removed grass/stone to an unowned intrinsic overlay (and locates plants on it).
            byte[] authored = surface.createAuthoredChunk(blockX >> 4, blockZ >> 4);
            int offset = ((blockX & 15) * 16 + (blockZ & 15)) * AUTHORED_HEIGHT;
            return java.util.Arrays.copyOfRange(authored, offset, offset + AUTHORED_HEIGHT);
        }

        @Override
        public byte[] authoredView(byte[] authored, int blockX, int blockZ, Supplier<byte[]> physicalMaterials,
            BitSet completedCaveMask) {
            Objects.requireNonNull(physicalMaterials, "Physical material supplier");
            return requireAuthoredChunk(authored);
        }

        @Override
        public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
            return hosted == null ? null : hosted.floatingArchetypeAt(x, y, z);
        }

        @Override
        public int carvedGroundSurfaceHeight(int x, int z) {
            if (hosted == null)
                throw new IllegalStateException("Projected Origen terrain has no hosted ground-height service");
            return hosted.sampleCarvedGroundSurfaceHeight(x, z);
        }

        @Override
        public boolean surfaceBoulderIntersectsExpansion(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean isExpansionCavity(int x, int y, int z) {
            return false;
        }
    }
}
