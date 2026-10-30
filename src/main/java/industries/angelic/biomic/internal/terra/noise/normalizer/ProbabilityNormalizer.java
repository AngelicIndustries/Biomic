package industries.angelic.biomic.internal.terra.noise.normalizer;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

public class ProbabilityNormalizer extends Normalizer {

    public ProbabilityNormalizer(NoiseSampler sampler) {
        super(sampler);
    }

    @Override
    public double normalize(double in) {
        return (in + 1) / 2;
    }
}
