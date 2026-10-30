package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/** Immutable horizontal controls and surface-rule inputs for one density column. */
final class TerrainColumn {

    private final int surfaceHeight;
    private final int naturalSurfaceHeight;
    private final double riverChannelStrength;
    private final OrigenBiomeProfile primaryProfile;

    TerrainColumn(int surfaceHeight, int naturalSurfaceHeight, double riverChannelStrength,
        OrigenBiomeProfile primaryProfile) {
        this.surfaceHeight = surfaceHeight;
        this.naturalSurfaceHeight = naturalSurfaceHeight;
        this.riverChannelStrength = riverChannelStrength;
        this.primaryProfile = primaryProfile;
    }

    int getSurfaceHeight() {
        return surfaceHeight;
    }

    int getNaturalSurfaceHeight() {
        return naturalSurfaceHeight;
    }

    double getRiverStrength() {
        return riverChannelStrength;
    }

    OrigenBiomeProfile getPrimaryProfile() {
        return primaryProfile;
    }

}
