package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.algorithms.sampler.noise.simplex.OpenSimplex2SSampler;

/**
 * Terra 6.4.2-compatible two-dimensional OpenSimplex2S.
 *
 * <p>
 * Seismic 2.5.7 contains a changed corner offset in the smooth 2D implementation. Origen was
 * authored against Terra 6.4.2, so its large-scale climate maps require the older equation.
 * </p>
 */
final class Terra64OpenSimplex2SSampler extends OpenSimplex2SSampler {

    private static final int PRIME_X = 501125321;
    private static final int PRIME_Y = 1136930381;
    private static final double SQRT_3 = 1.7320508075688772935274463415059D;
    private static final double G_2 = (3.0D - SQRT_3) / 6.0D;
    private static final double F_2 = 0.5D * (SQRT_3 - 1.0D);

    Terra64OpenSimplex2SSampler(double frequency, long salt) {
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

        i *= PRIME_X;
        j *= PRIME_Y;
        int i1 = i + PRIME_X;
        int j1 = j + PRIME_Y;

        double unskew = (xi + yi) * G_2;
        double x0 = xi - unskew;
        double y0 = yi - unskew;

        double a0 = (2.0D / 3.0D) - x0 * x0 - y0 * y0;
        double value = fourthPower(a0) * gradient(seed, i, j, x0, y0);

        double a1 = 2.0D * (1.0D - 2.0D * G_2) * (1.0D / G_2 - 2.0D) * unskew
            + (-2.0D * (1.0D - 2.0D * G_2) * (1.0D - 2.0D * G_2) + a0);
        double x1 = x0 - (1.0D - 2.0D * G_2);
        double y1 = y0 - (1.0D - 2.0D * G_2);
        value += fourthPower(a1) * gradient(seed, i1, j1, x1, y1);

        double xMinusYi = xi - yi;
        if (unskew > G_2) {
            if (xi + xMinusYi > 1.0D) {
                value += corner(
                    seed,
                    i + (PRIME_X << 1),
                    j + PRIME_Y,
                    x0 + (3.0D * G_2 - 2.0D),
                    y0 + (3.0D * G_2 - 1.0D));
            } else {
                value += corner(seed, i, j + PRIME_Y, x0 + G_2, y0 + (G_2 - 1.0D));
            }

            if (yi - xMinusYi > 1.0D) {
                value += corner(
                    seed,
                    i + PRIME_X,
                    j + (PRIME_Y << 1),
                    x0 + (3.0D * G_2 - 1.0D),
                    y0 + (3.0D * G_2 - 2.0D));
            } else {
                value += corner(seed, i + PRIME_X, j, x0 + (G_2 - 1.0D), y0 + G_2);
            }
        } else {
            if (xi + xMinusYi < 0.0D) {
                value += corner(seed, i - PRIME_X, j, x0 + (1.0D - G_2), y0 - G_2);
            } else {
                value += corner(seed, i + PRIME_X, j, x0 + (G_2 - 1.0D), y0 + G_2);
            }

            if (yi < xMinusYi) {
                value += corner(seed, i, j - PRIME_Y, x0 - G_2, y0 - (G_2 - 1.0D));
            } else {
                value += corner(seed, i, j + PRIME_Y, x0 + G_2, y0 + (G_2 - 1.0D));
            }
        }

        return value * 18.24196194486065D;
    }

    private static double corner(int seed, int primedX, int primedY, double deltaX, double deltaY) {
        double attenuation = (2.0D / 3.0D) - deltaX * deltaX - deltaY * deltaY;
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
