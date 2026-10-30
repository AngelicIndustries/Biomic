package industries.angelic.biomic.world.terrain;

import java.util.Random;

import net.minecraft.world.World;

/** Forge-facing operations used by the pinned TerraScript interpreter. */
interface OrigenStructureEnvironment {

    default int physicalY(int authoredY) {
        return OrigenVerticalProfileSampler.toBiomicBlockY(authoredY);
    }

    boolean placeBlock(World world, int x, int y, int z, int authoredY, String state, boolean overwrite,
        boolean physics, boolean waterlog);

    String getBlockId(World world, int x, int y, int z, int authoredY);

    /** An attachment must have a real full-sized support, not merely a non-air plant or fluid. */
    default boolean supportsCeilingAttachment(World world, int x, int y, int z, int authoredY) {
        return false;
    }

    boolean generateStructure(String id, World world, int originX, int originY, int originZ, int authoredOriginY,
        int offsetX, int offsetY, int offsetZ, int rotationDegrees, Random random, int recursions);

    double sampleNamed(String id, long seed, double x, double y, double z, boolean threeDimensional);

    String checkDensity(double x, double y, double z);

    void spawnEntity(World world, int x, int y, int z, String id);
}
