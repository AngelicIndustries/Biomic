package industries.angelic.biomic.compat.biome;

import industries.angelic.biomic.world.terrain.AuthoredVerticalProjection;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjections;

/** The physical counterpart of the marsh's source sea boundary (63); feature geometry remains 1:1. */
public record SaltMarshHeight(int physicalSeaBoundary) {

    private static final SaltMarshHeight LEGACY = new SaltMarshHeight(63);
    private static final SaltMarshHeight SHIFTED = new SaltMarshHeight(191);

    public static SaltMarshHeight forProjection(AuthoredVerticalProjection projection) {
        if (projection == AuthoredVerticalProjections.origenCompressed256()) return LEGACY;
        if (projection == AuthoredVerticalProjections.shifted512()) return SHIFTED;
        throw new IllegalArgumentException("Salty marsh height requires canonical hosted projection");
    }
}
