package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/** Horizontal outputs from the Origen-derived province and elevation graph. */
final class TerrainShapeSample {

    private final double offsetHeight;
    private final OrigenBiomeProfile primaryProfile;

    TerrainShapeSample(double offsetHeight, OrigenBiomeProfile primaryProfile) {
        this.offsetHeight = offsetHeight;
        this.primaryProfile = primaryProfile;
    }

    double getOffsetHeight() {
        return offsetHeight;
    }

    OrigenBiomeProfile getPrimaryProfile() {
        return primaryProfile;
    }

}
