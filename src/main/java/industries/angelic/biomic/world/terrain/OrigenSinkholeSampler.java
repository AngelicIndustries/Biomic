package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.algorithms.sampler.DomainWarpedSampler;
import com.dfsek.seismic.algorithms.sampler.noise.cellular.CellularStyleSampler.CellularReturnType;
import com.dfsek.seismic.type.DistanceFunction;
import com.dfsek.seismic.type.sampler.Sampler;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/** Pinned Origen add_sinkholes.yml mask and customization defaults, opt-in to the blended recipe. */
final class OrigenSinkholeSampler {

    private final Sampler distance;
    private final Sampler variation;

    OrigenSinkholeSampler() {
        Sampler cells = new Terra64CellularSampler(
            .0025,
            0,
            Sampler.zero(),
            DistanceFunction.Euclidean,
            CellularReturnType.Distance,
            .3);
        Sampler values = new Terra64CellularSampler(
            .0025,
            0,
            Sampler.zero(),
            DistanceFunction.EuclideanSq,
            CellularReturnType.CellValue,
            .3);
        distance = warped(cells);
        variation = warped(values);
    }

    private static Sampler warped(Sampler sampler) {
        return new DomainWarpedSampler(new Sampler() {

            @Override
            public double getSample(long seed, double x, double z) {
                return sampler.getSample(seed, x / 2.5, z);
            }

            @Override
            public double getSample(long seed, double x, double y, double z) {
                return getSample(seed, x, z);
            }
        }, new Terra64OpenSimplex2Sampler(.01, 0), 20);
    }

    OrigenBiomeProfile sample(long seed, int x, int z) {
        // Preserve the authored pair of rotate() expressions, including its asymmetric second argument.
        double rx = x * Math.cos(.435) - z * Math.sin(.435);
        double rz = z * Math.cos(.435) - x * Math.sin(.435);
        double depth = -distance.getSample(seed, rx, rz) - .83;
        if (depth + .02 <= 0) return null;
        int slot = Math.max(0, Math.min(20, (int) Math.floor((variation.getSample(seed, rx, rz) + 1) * .5 * 21)));
        if (slot < 18) return null;
        if (depth <= 0) return OrigenBiomeProfile.SINKHOLE_OUTER;
        return switch (slot) {
            case 18 -> OrigenBiomeProfile.SINKHOLE_FROZEN;
            case 19 -> OrigenBiomeProfile.SINKHOLE_FOREST;
            default -> OrigenBiomeProfile.SINKHOLE_JUNGLE;
        };
    }
}
