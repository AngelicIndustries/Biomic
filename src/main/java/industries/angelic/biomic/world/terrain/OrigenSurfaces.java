package industries.angelic.biomic.world.terrain;

import java.util.List;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.WorldgenContext;

/** Read-only facts about Origen's authored surfaces, for choosing how structures on them look. */
public final class OrigenSurfaces {

    private OrigenSurfaces() {}

    /** The top block (a BiomicTerrainSampler material) of a profile's ground palette at one authored-height point. */
    public static byte groundTop(WorldgenContext context, OrigenBiomeProfile profile, int x, int authoredY, int z) {
        return OrigenSurfaceRuntime.shared(context)
            .groundTop(profile, x, authoredY, z);
    }

    /** The profile's tree features (names such as SPRUCE_TREES or BLACK_FOREST_TREES). */
    public static List<String> treeFeatures(WorldgenContext context, OrigenBiomeProfile profile) {
        return OrigenSurfaceRuntime.shared(context)
            .treeFeatures(profile);
    }
}
