package industries.angelic.biomic.internal.terra.noise.samplers.arithmetic;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

public class MultiplicationSampler extends BinaryArithmeticSampler {

    public MultiplicationSampler(NoiseSampler left, NoiseSampler right) {
        super(left, right);
    }

    @Override
    public double operate(double left, double right) {
        return left * right;
    }
}
