package industries.angelic.biomic.world.modern.noise;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 1.21.1's xoroshiro128++ random source and positional derivation. */
public final class ModernXoroshiroRandom implements ModernRandom {

    private static final long GOLDEN_RATIO_64 = -7046029254386353131L;
    private static final long SILVER_RATIO_64 = 7640891576956012809L;
    private long seedLo;
    private long seedHi;
    private double nextGaussian;
    private boolean hasNextGaussian;

    public ModernXoroshiroRandom(long seed) {
        reset(seed);
    }

    /** Mirrors XoroshiroRandomSource.setSeed. */
    public void reset(long seed) {
        long low = seed ^ SILVER_RATIO_64;
        setSeeds(mixStafford13(low), mixStafford13(low + GOLDEN_RATIO_64));
        hasNextGaussian = false;
    }

    ModernXoroshiroRandom(long seedLo, long seedHi) {
        setSeeds(seedLo, seedHi);
    }

    @Override
    public long nextLong() {
        long low = seedLo;
        long high = seedHi;
        long result = Long.rotateLeft(low + high, 17) + low;
        high ^= low;
        seedLo = Long.rotateLeft(low, 49) ^ high ^ high << 21;
        seedHi = Long.rotateLeft(high, 28);
        return result;
    }

    @Override
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("Bound must be positive");
        long unsigned = Integer.toUnsignedLong((int) nextLong());
        long product = unsigned * bound;
        long low = product & 0xFFFFFFFFL;
        if (low < bound) {
            int threshold = Integer.remainderUnsigned(-bound, bound);
            while (low < Integer.toUnsignedLong(threshold)) {
                unsigned = Integer.toUnsignedLong((int) nextLong());
                product = unsigned * bound;
                low = product & 0xFFFFFFFFL;
            }
        }
        return (int) (product >>> 32);
    }

    @Override
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53D;
    }

    @Override
    public float nextFloat() {
        return (nextLong() >>> 40) * 0x1.0p-24F;
    }

    @Override
    public boolean nextBoolean() {
        return (nextLong() & 1L) != 0L;
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
        return new Positional(nextLong(), nextLong());
    }

    static long mixStafford13(long value) {
        value = (value ^ value >>> 30) * -4658895280553007687L;
        value = (value ^ value >>> 27) * -7723592293110705685L;
        return value ^ value >>> 31;
    }

    private void setSeeds(long low, long high) {
        if ((low | high) == 0L) {
            low = GOLDEN_RATIO_64;
            high = SILVER_RATIO_64;
        }
        seedLo = low;
        seedHi = high;
    }

    private static final class Positional implements ModernPositionalRandom {

        private final long seedLo;
        private final long seedHi;

        private Positional(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
        }

        @Override
        public ModernRandom fromHashOf(String name) {
            byte[] digest;
            try {
                digest = MessageDigest.getInstance("MD5")
                    .digest(name.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException impossible) {
                throw new AssertionError("Every Java runtime must provide MD5", impossible);
            }
            ByteBuffer bytes = ByteBuffer.wrap(digest)
                .order(ByteOrder.BIG_ENDIAN);
            return new ModernXoroshiroRandom(bytes.getLong() ^ seedLo, bytes.getLong() ^ seedHi);
        }

        @Override
        public ModernRandom at(int x, int y, int z) {
            long positionSeed = (long) (x * 3129871) ^ (long) z * 116129781L ^ y;
            positionSeed = positionSeed * positionSeed * 42317861L + positionSeed * 11L;
            positionSeed >>= 16;
            return new ModernXoroshiroRandom(positionSeed ^ seedLo, seedHi);
        }
    }
}
