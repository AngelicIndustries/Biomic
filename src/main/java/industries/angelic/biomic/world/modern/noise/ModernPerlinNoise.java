package industries.angelic.biomic.world.modern.noise;

/** Exact modern multi-octave Perlin composition for non-legacy noise settings. */
public final class ModernPerlinNoise {

    private static final double WRAP = 3.3554432E7D;

    private final ModernImprovedNoise[] levels;
    private final double[] amplitudes;
    private final double lowestFrequencyInputFactor;
    private final double lowestFrequencyValueFactor;

    public ModernPerlinNoise(ModernRandom random, int firstOctave, double... amplitudes) {
        if (random == null || amplitudes == null || amplitudes.length == 0) {
            throw new IllegalArgumentException("Modern Perlin noise requires a random source and amplitudes");
        }
        this.amplitudes = amplitudes.clone();
        this.levels = new ModernImprovedNoise[amplitudes.length];
        ModernPositionalRandom positional = random.forkPositional();
        for (int index = 0; index < amplitudes.length; index++) {
            if (amplitudes[index] != 0.0D) {
                levels[index] = new ModernImprovedNoise(positional.fromHashOf("octave_" + (firstOctave + index)));
            }
        }
        int zeroOctaveIndex = -firstOctave;
        lowestFrequencyInputFactor = Math.pow(2.0D, -zeroOctaveIndex);
        lowestFrequencyValueFactor = Math.pow(2.0D, amplitudes.length - 1) / (Math.pow(2.0D, amplitudes.length) - 1.0D);
    }

    public double sample(double x, double y, double z) {
        double value = 0.0D;
        double inputFactor = lowestFrequencyInputFactor;
        double valueFactor = lowestFrequencyValueFactor;
        for (int index = 0; index < levels.length; index++) {
            ModernImprovedNoise level = levels[index];
            if (level != null) {
                value += amplitudes[index]
                    * level.sample(wrap(x * inputFactor), wrap(y * inputFactor), wrap(z * inputFactor))
                    * valueFactor;
            }
            inputFactor *= 2.0D;
            valueFactor /= 2.0D;
        }
        return value;
    }

    private static double wrap(double value) {
        return value - Math.floor(value / WRAP + 0.5D) * WRAP;
    }
}
