package industries.angelic.biomic.internal.terra.noise.samplers.arithmetic;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

/** Terra 6.4.2's binary ADD sampler. */
public final class AdditionSampler extends BinaryArithmeticSampler {

    public AdditionSampler(NoiseSampler left, NoiseSampler right) {
        super(left, right);
    }

    @Override
    public double operate(double left, double right) {
        return left + right;
    }
}
