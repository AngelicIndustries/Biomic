package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/** Origen's profile at one horizontal position. */
public final class OrigenProvinceSample {

    private final OrigenBiomeProfile primary;

    OrigenProvinceSample(OrigenBiomeProfile primary) {
        this.primary = primary;
    }

    public OrigenBiomeProfile getPrimary() {
        return primary;
    }

}
