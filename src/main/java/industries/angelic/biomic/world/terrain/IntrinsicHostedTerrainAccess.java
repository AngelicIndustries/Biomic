package industries.angelic.biomic.world.terrain;

/** Hosted physical terrain containing a complete intrinsic environment with exclusive cell ownership. */
public interface IntrinsicHostedTerrainAccess extends HostedTerrainAccess {

    boolean ownsIntrinsicEnvironment(int x, int physicalY, int z);

    /**
     * Whether an owned cell belongs in Origen's authored terrain view (reshaped or carved terrain). A foreign layer
     * such as a sky island is owned for reconciliation but stays invisible to Origen, as in the compressed blend.
     */
    default boolean authoredVisible(int x, int physicalY, int z) {
        return true;
    }

    /** Terrain reconciliation ownership need not exclude ecology on transformed, ordinary ocean water. */
    default boolean blocksHostedEcology(int x, int physicalY, int z) {
        return ownsIntrinsicEnvironment(x, physicalY, z);
    }
}
