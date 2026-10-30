package industries.angelic.biomic.world.modern.noise;

/** Mutable WorldgenRandom wrapper used by 1.21.1 biome decoration. */
public final class ModernWorldgenRandom implements ModernRandom {

    private final ModernXoroshiroRandom delegate = new ModernXoroshiroRandom(0L);
    private double nextGaussian;
    private boolean hasNextGaussian;

    public long setDecorationSeed(long worldSeed, int blockX, int blockZ) {
        setSeed(worldSeed);
        long xMultiplier = nextLong() | 1L;
        long zMultiplier = nextLong() | 1L;
        long seed = (long) blockX * xMultiplier + (long) blockZ * zMultiplier ^ worldSeed;
        setSeed(seed);
        return seed;
    }

    public void setFeatureSeed(long decorationSeed, int featureIndex, int decorationStep) {
        setSeed(decorationSeed + featureIndex + 10000L * decorationStep);
    }

    public void setSeed(long seed) {
        delegate.reset(seed);
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
        return delegate.forkPositional();
    }

    private int nextBits(int bits) {
        return (int) (delegate.nextLong() >>> 64 - bits);
    }
}
