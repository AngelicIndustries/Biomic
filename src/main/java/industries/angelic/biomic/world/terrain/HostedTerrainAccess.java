package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.api.ChunkVerticalLayout;

/**
 * Minimal explicit physical-height contract consumed by hosted habitat surface and population adapters.
 * Implementations return complete X/Z/Y-major physical arrays matching {@link #layout()}, and all Y-bearing
 * queries use physical coordinates. Merely implementing this interface does not admit or compose a floating layer.
 */
public interface HostedTerrainAccess {

    ChunkVerticalLayout layout();

    int minimumEcologyY();

    AuthoredVerticalProjection projection();

    byte[] createMaterialArray(int chunkX, int chunkZ);

    int sampleCarvedGroundSurfaceHeight(int blockX, int blockZ);

    FloatingIslandArchetype floatingArchetypeAt(int blockX, int physicalY, int blockZ);

    String floatingHabitatAt(int blockX, int physicalY, int blockZ);
}
