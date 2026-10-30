package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Detached Origen-only terrain transport with explicit authored projection and physical array layout.
 * Includes the existing authored surface palettes, carvers and orphan polish, but no feature population,
 * RU/other-mod overlays or Biomic floating islands. This service does not enable a world type or choose a
 * saved revision. The live caller must establish those capabilities before publishing its output.
 */
public final class OrigenProjectedTerrainSampler {

    private static final int SOURCE_HEIGHT = 384;
    private final OrigenSurfaceRuntime surface;
    private final AuthoredVerticalProjection projection;
    private final ChunkVerticalLayout layout;

    public OrigenProjectedTerrainSampler(WorldgenContext context, AuthoredVerticalProjection projection,
        ChunkVerticalLayout layout) {
        if (context == null || layout == null
            || (projection != AuthoredVerticalProjections.origenCompressed256()
                && projection != AuthoredVerticalProjections.shifted512())) {
            throw new IllegalArgumentException("Origen terrain requires a canonical Origen projection and a layout");
        }
        if (!projection.supportsStorageHeight(layout.height())) {
            throw new IllegalArgumentException("Physical layout cannot contain the authored Origen envelope");
        }
        this.projection = projection;
        this.layout = layout;
        surface = OrigenSurfaceRuntime.shared(context);
    }

    public AuthoredVerticalProjection projection() {
        return projection;
    }

    public ChunkVerticalLayout layout() {
        return layout;
    }

    /** Returned X/Z/Y-major array belongs to the caller; all unauthored physical cells are AIR. */
    public byte[] createMaterialArray(int chunkX, int chunkZ) {
        if (projection == AuthoredVerticalProjections.origenCompressed256()) {
            // Preserve the legacy palette-depth, dry-surface and upper interpolation decisions exactly.
            byte[] compressed = new byte[ChunkVerticalLayout.LEGACY_256.blockCount()];
            surface.applyStrict(compressed, chunkX, chunkZ);
            if (layout == ChunkVerticalLayout.LEGACY_256) return compressed;
            byte[] physical = new byte[layout.blockCount()];
            for (int column = 0; column < 256; column++) {
                System.arraycopy(compressed, column * 256, physical, column * layout.height(), 256);
            }
            return physical;
        }
        byte[] authored = surface.createAuthoredChunk(chunkX, chunkZ);
        byte[] physical = new byte[layout.blockCount()];
        for (int column = 0; column < 256; column++) {
            System.arraycopy(
                authored,
                column * SOURCE_HEIGHT,
                physical,
                column * layout.height() + projection.physicalMinY(),
                SOURCE_HEIGHT);
        }
        return physical;
    }

    /**
     * Shifted512 only: where Origen's density is positive, by authored row (X/Z-major columns of 384 rows from
     * authored Y -64). Rock or a cave Origen's own carver opened; any other air is open air.
     */
    public java.util.BitSet groundMask(int chunkX, int chunkZ) {
        if (projection != AuthoredVerticalProjections.shifted512())
            throw new IllegalStateException("The authored ground mask is laid out for shifted512");
        return surface.groundMask(chunkX, chunkZ);
    }

    /** Independent mutable copy for feature planning; it never exposes the shared source-terrain cache. */
    public byte[] copyAuthoredMaterialArray(int chunkX, int chunkZ) {
        return surface.createAuthoredChunk(chunkX, chunkZ)
            .clone();
    }
}
