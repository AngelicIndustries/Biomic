package industries.angelic.biomic.world.modern.noise;

/** Modern double-Perlin normal noise. */
public final class ModernNormalNoise {

    private static final double INPUT_FACTOR = 1.0181268882175227D;

    private final ModernPerlinNoise first;
    private final ModernPerlinNoise second;
    private final double valueFactor;

    public ModernNormalNoise(ModernRandom random, int firstOctave, double... amplitudes) {
        if (amplitudes == null || amplitudes.length == 0) {
            throw new IllegalArgumentException("Modern normal noise requires amplitudes");
        }
        first = new ModernPerlinNoise(random, firstOctave, amplitudes);
        second = new ModernPerlinNoise(random, firstOctave, amplitudes);
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (int index = 0; index < amplitudes.length; index++) {
            if (amplitudes[index] == 0.0D) continue;
            minimum = Math.min(minimum, index);
            maximum = Math.max(maximum, index);
        }
        if (minimum == Integer.MAX_VALUE) throw new IllegalArgumentException("At least one amplitude must be nonzero");
        valueFactor = (1.0D / 6.0D) / (0.1D * (1.0D + 1.0D / (maximum - minimum + 1.0D)));
    }

    public double sample(double x, double y, double z) {
        return (first.sample(x, y, z) + second.sample(x * INPUT_FACTOR, y * INPUT_FACTOR, z * INPUT_FACTOR))
            * valueFactor;
    }

    public static ModernNormalNoise named(long worldSeed, String identifier, int firstOctave, double... amplitudes) {
        ModernPositionalRandom root = new ModernXoroshiroRandom(worldSeed).forkPositional();
        return new ModernNormalNoise(root.fromHashOf(identifier), firstOctave, amplitudes);
    }
}
