package industries.angelic.biomic.world.modern.noise;

/** Modern LegacyRandomSource, retained by state providers and coordinate-stable predicates. */
public final class ModernLegacyRandom implements ModernRandom {

    private static final long MASK = (1L << 48) - 1L;
    private static final long MULTIPLIER = 25214903917L;
    private static final long INCREMENT = 11L;

    private long seed;
    private double nextGaussian;
    private boolean hasNextGaussian;

    public ModernLegacyRandom(long seed) {
        reset(seed);
    }

    public void reset(long value) {
        seed = (value ^ MULTIPLIER) & MASK;
        hasNextGaussian = false;
    }

    @Override
    public long nextLong() {
        int high = nextBits(32);
        int low = nextBits(32);
        return ((long) high << 32) + (long) low;
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("Bound must be positive");
        if ((bound & bound - 1) == 0) return (int) ((long) bound * nextBits(31) >> 31);
        int bits;
        int value;
        do {
            bits = nextBits(31);
            value = bits % bound;
        } while (bits - value + (bound - 1) < 0);
        return value;
    }

    @Override
    public double nextDouble() {
        long bits = ((long) nextBits(26) << 27) + nextBits(27);
        return bits * 0x1.0p-53D;
    }

    @Override
    public float nextFloat() {
        return nextBits(24) * 0x1.0p-24F;
    }

    @Override
    public boolean nextBoolean() {
        return nextBits(1) != 0;
    }

    @Override
    public double nextGaussian() {
        if (hasNextGaussian) {
            hasNextGaussian = false;
            return nextGaussian;
        }
        double x;
        double y;
        double radiusSquared;
        do {
            x = 2.0D * nextDouble() - 1.0D;
            y = 2.0D * nextDouble() - 1.0D;
            radiusSquared = x * x + y * y;
        } while (radiusSquared >= 1.0D || radiusSquared == 0.0D);
        double multiplier = Math.sqrt(-2.0D * Math.log(radiusSquared) / radiusSquared);
        nextGaussian = y * multiplier;
        hasNextGaussian = true;
        return x * multiplier;
    }

    @Override
    public ModernPositionalRandom forkPositional() {
        return new Positional(nextLong());
    }

    private int nextBits(int bits) {
        seed = seed * MULTIPLIER + INCREMENT & MASK;
        return (int) (seed >>> 48 - bits);
    }

    private static final class Positional implements ModernPositionalRandom {

        private final long seed;

        private Positional(long seed) {
            this.seed = seed;
        }

        @Override
        public ModernRandom at(int x, int y, int z) {
            long positionSeed = (long) (x * 3129871) ^ (long) z * 116129781L ^ y;
            positionSeed = positionSeed * positionSeed * 42317861L + positionSeed * 11L;
            positionSeed >>= 16;
            return new ModernLegacyRandom(positionSeed ^ seed);
        }

        @Override
        public ModernRandom fromHashOf(String name) {
            return new ModernLegacyRandom((long) name.hashCode() ^ seed);
        }

    }
}
