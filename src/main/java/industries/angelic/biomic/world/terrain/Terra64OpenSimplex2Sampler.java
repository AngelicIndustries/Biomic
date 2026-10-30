package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.algorithms.sampler.noise.simplex.OpenSimplex2Sampler;

/** Terra 6.4.2-compatible two-dimensional OpenSimplex2 implementation. */
final class Terra64OpenSimplex2Sampler extends OpenSimplex2Sampler {

    private static final int PRIME_X = 501125321;
    private static final int PRIME_Y = 1136930381;
    private static final double SQRT_3 = 1.7320508075688772935274463415059D;
    private static final double G_2 = (3.0D - SQRT_3) / 6.0D;
    private static final double F_2 = 0.5D * (SQRT_3 - 1.0D);

    Terra64OpenSimplex2Sampler(double frequency, long salt) {
        super(frequency, salt);
    }

    @Override
    @SuppressWarnings("NumericOverflow")
    public double getNoiseRaw(long saltedSeed, double x, double y) {
        int seed = (int) saltedSeed;
        double skew = (x + y) * F_2;
        x += skew;
        y += skew;

        int i = (int) Math.floor(x);
        int j = (int) Math.floor(y);
        double xi = x - i;
        double yi = y - j;

        double unskew = (xi + yi) * G_2;
        double x0 = xi - unskew;
        double y0 = yi - unskew;
        i *= PRIME_X;
        j *= PRIME_Y;

        double n0 = contribution(seed, i, j, x0, y0);

        double a = 0.5D - x0 * x0 - y0 * y0;
        double c = 2.0D * (1.0D - 2.0D * G_2) * (1.0D / G_2 - 2.0D) * unskew
            + (-2.0D * (1.0D - 2.0D * G_2) * (1.0D - 2.0D * G_2) + a);
        double n2 = 0.0D;
        if (c > 0.0D) {
            double x2 = x0 + (2.0D * G_2 - 1.0D);
            double y2 = y0 + (2.0D * G_2 - 1.0D);
            n2 = fourthPower(c) * gradient(seed, i + PRIME_X, j + PRIME_Y, x2, y2);
        }

        double n1;
        if (y0 > x0) {
            n1 = contribution(seed, i, j + PRIME_Y, x0 + G_2, y0 + (G_2 - 1.0D));
        } else {
            n1 = contribution(seed, i + PRIME_X, j, x0 + (G_2 - 1.0D), y0 + G_2);
        }
        return (n0 + n1 + n2) * 99.83685446303647F;
    }

    private static double contribution(int seed, int primedX, int primedY, double deltaX, double deltaY) {
        double attenuation = 0.5D - deltaX * deltaX - deltaY * deltaY;
        if (attenuation <= 0.0D) return 0.0D;
        return fourthPower(attenuation) * gradient(seed, primedX, primedY, deltaX, deltaY);
    }

    private static double fourthPower(double value) {
        double square = value * value;
        return square * square;
    }

    private static double gradient(int seed, int primedX, int primedY, double deltaX, double deltaY) {
        return gradCoord(GRADIENTS_2D, seed, primedX, primedY, deltaX, deltaY);
    }
}
