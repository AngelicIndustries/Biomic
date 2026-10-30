package industries.angelic.biomic.world.modern.noise;

import java.util.Arrays;
import java.util.Random;

/** The legacy-random PerlinSimplexNoise retained by Minecraft 1.21.1 for biome temperature. */
public final class ModernPerlinSimplexNoise {

    private final Simplex[] levels;
    private final double highestFrequencyInputFactor;
    private final double highestFrequencyValueFactor;

    public ModernPerlinSimplexNoise(long seed, int... octaves) {
        if (octaves.length == 0) throw new IllegalArgumentException("Need some octaves");
        int[] sorted = octaves.clone();
        Arrays.sort(sorted);
        int first = sorted[0];
        int last = sorted[sorted.length - 1];
        int zeroIndex = -first;
        int count = zeroIndex + last + 1;
        if (count < 1) throw new IllegalArgumentException("Total octaves must be positive");

        Random random = new Random(seed);
        Simplex zero = new Simplex(random);
        levels = new Simplex[count];
        if (last >= 0 && last < count && contains(sorted, 0)) levels[last] = zero;
        for (int index = last + 1; index < count; index++) {
            if (index >= 0 && contains(sorted, last - index)) levels[index] = new Simplex(random);
            else consume(random, 262);
        }
        if (last > 0) {
            long derivedSeed = (long) (zero.value(zero.xOffset, zero.yOffset) * 9.223372036854776E18D);
            random = new Random(derivedSeed);
            for (int index = last - 1; index >= 0; index--) {
                if (index < count && contains(sorted, last - index)) levels[index] = new Simplex(random);
                else consume(random, 262);
            }
        }
        highestFrequencyInputFactor = Math.pow(2.0D, last);
        highestFrequencyValueFactor = 1.0D / (Math.pow(2.0D, count) - 1.0D);
    }

    public double value(double x, double z) {
        double result = 0.0D;
        double inputFactor = highestFrequencyInputFactor;
        double valueFactor = highestFrequencyValueFactor;
        for (Simplex level : levels) {
            if (level != null) result += level.value(x * inputFactor, z * inputFactor) * valueFactor;
            inputFactor /= 2.0D;
            valueFactor *= 2.0D;
        }
        return result;
    }

    private static boolean contains(int[] values, int target) {
        return Arrays.binarySearch(values, target) >= 0;
    }

    private static void consume(Random random, int count) {
        for (int index = 0; index < count; index++) random.nextInt();
    }

    private static final class Simplex {

        private static final int[][] GRADIENT = { { 1, 1, 0 }, { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, 0, 1 },
            { -1, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 1, 1 }, { 0, -1, 1 }, { 0, 1, -1 }, { 0, -1, -1 },
            { 1, 1, 0 }, { 0, -1, 1 }, { -1, 1, 0 }, { 0, -1, -1 } };
        private static final double SQRT_THREE = Math.sqrt(3.0D);
        private static final double F2 = 0.5D * (SQRT_THREE - 1.0D);
        private static final double G2 = (3.0D - SQRT_THREE) / 6.0D;

        private final int[] permutation = new int[256];
        private final double xOffset;
        private final double yOffset;

        private Simplex(Random random) {
            xOffset = random.nextDouble() * 256.0D;
            yOffset = random.nextDouble() * 256.0D;
            random.nextDouble(); // zOffset is initialized by modern SimplexNoise even for 2D sampling.
            for (int index = 0; index < 256; index++) permutation[index] = index;
            for (int index = 0; index < 256; index++) {
                int selected = index + random.nextInt(256 - index);
                int swap = permutation[index];
                permutation[index] = permutation[selected];
                permutation[selected] = swap;
            }
        }

        private double value(double x, double z) {
            double skew = (x + z) * F2;
            int cellX = floor(x + skew);
            int cellZ = floor(z + skew);
            double unskew = (cellX + cellZ) * G2;
            double dx0 = x - (cellX - unskew);
            double dz0 = z - (cellZ - unskew);
            int stepX;
            int stepZ;
            if (dx0 > dz0) {
                stepX = 1;
                stepZ = 0;
            } else {
                stepX = 0;
                stepZ = 1;
            }
            double dx1 = dx0 - stepX + G2;
            double dz1 = dz0 - stepZ + G2;
            double dx2 = dx0 - 1.0D + 2.0D * G2;
            double dz2 = dz0 - 1.0D + 2.0D * G2;
            int wrappedX = cellX & 255;
            int wrappedZ = cellZ & 255;
            int gradient0 = p(wrappedX + p(wrappedZ)) % 12;
            int gradient1 = p(wrappedX + stepX + p(wrappedZ + stepZ)) % 12;
            int gradient2 = p(wrappedX + 1 + p(wrappedZ + 1)) % 12;
            return 70.0D * (corner(gradient0, dx0, dz0) + corner(gradient1, dx1, dz1) + corner(gradient2, dx2, dz2));
        }

        private int p(int index) {
            return permutation[index & 255];
        }

        private static double corner(int gradient, double x, double z) {
            double attenuation = 0.5D - x * x - z * z;
            if (attenuation < 0.0D) return 0.0D;
            attenuation *= attenuation;
            int[] direction = GRADIENT[gradient];
            return attenuation * attenuation * (direction[0] * x + direction[1] * z);
        }

        private static int floor(double value) {
            int integer = (int) value;
            return value < integer ? integer - 1 : integer;
        }
    }
}
