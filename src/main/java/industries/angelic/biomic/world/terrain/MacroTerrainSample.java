package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.placement.SurfaceLandform;

/** Immutable shared macro fields for one horizontal position. */
public final class MacroTerrainSample {

    private final SurfaceTopology topology;
    private final SurfaceLandform landform;
    private final OrigenProvinceSample origenProvince;

    MacroTerrainSample(SurfaceTopology topology, SurfaceLandform landform, OrigenProvinceSample origenProvince) {
        this.topology = topology;
        this.landform = landform;
        this.origenProvince = origenProvince;
    }

    public SurfaceTopology getTopology() {
        return topology;
    }

    public SurfaceLandform getLandform() {
        return landform;
    }

    public OrigenProvinceSample getOrigenProvince() {
        return origenProvince;
    }

}
