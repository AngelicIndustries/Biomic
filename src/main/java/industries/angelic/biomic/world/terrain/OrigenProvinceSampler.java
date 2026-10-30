package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;

/** Adapts the exact Origen biome pipeline to the terrain graph's horizontal sample contract. */
final class OrigenProvinceSampler {

    private final OrigenDensityField densityField;

    OrigenProvinceSampler(WorldgenContext context) {
        densityField = OrigenDensityField.shared(context);
    }

    OrigenProvinceSample sample(int blockX, int blockZ) {
        return densityField.sampleProvince(blockX, blockZ);
    }

}
