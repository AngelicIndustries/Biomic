package industries.angelic.biomic.world.alexscaves;

/*
 * Reduced 2D/3D form of Alex's Caves' ACSimplexNoise. The upstream file says this
 * 2012 Stefan Gustavson simplex implementation was placed in the public domain.
 * Exact upstream identity is recorded in assets/biomic/alexscaves/source-manifest.json.
 */
public final class AlexCavesSimplexNoise {

    private static final Grad[] GRADIENTS = { new Grad(1, 1, 0), new Grad(-1, 1, 0), new Grad(1, -1, 0),
        new Grad(-1, -1, 0), new Grad(1, 0, 1), new Grad(-1, 0, 1), new Grad(1, 0, -1), new Grad(-1, 0, -1),
        new Grad(0, 1, 1), new Grad(0, -1, 1), new Grad(0, 1, -1), new Grad(0, -1, -1) };
    private static final short[] SOURCE_PERMUTATION = { 151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233,
        7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62,
        94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74,
        165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55,
        46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196,
        135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147,
        118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248,
        152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224,
        232, 178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51,
        145, 235, 249, 14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4,
        150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180 };
    private static final short[] PERMUTATION = new short[512];
    private static final short[] PERMUTATION_MOD_12 = new short[512];
    private static final double F2 = 0.5D * (Math.sqrt(3.0D) - 1.0D);
    private static final double G2 = (3.0D - Math.sqrt(3.0D)) / 6.0D;
    private static final double F3 = 1.0D / 3.0D;
    private static final double G3 = 1.0D / 6.0D;

    static {
        for (int i = 0; i < 512; i++) {
            PERMUTATION[i] = SOURCE_PERMUTATION[i & 255];
            PERMUTATION_MOD_12[i] = (short) (PERMUTATION[i] % 12);
        }
    }

    private AlexCavesSimplexNoise() {}

    static double sample2D(int x, int z, double scale) {
        return noise2D((x + scale) / scale, (z + scale) / scale);
    }

    public static double sample3D(int x, int y, int z, double scale) {
        return noise3D((x + scale) / scale, (y + scale) / scale, (z + scale) / scale);
    }

    static double noise2D(double x, double y) {
        double skew = (x + y) * F2;
        int i = fastFloor(x + skew);
        int j = fastFloor(y + skew);
        double unskew = (i + j) * G2;
        double x0 = x - (i - unskew);
        double y0 = y - (j - unskew);
        int i1 = x0 > y0 ? 1 : 0;
        int j1 = x0 > y0 ? 0 : 1;
        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0D + 2.0D * G2;
        double y2 = y0 - 1.0D + 2.0D * G2;
        int ii = i & 255;
        int jj = j & 255;
        int g0 = PERMUTATION_MOD_12[ii + PERMUTATION[jj]];
        int g1 = PERMUTATION_MOD_12[ii + i1 + PERMUTATION[jj + j1]];
        int g2 = PERMUTATION_MOD_12[ii + 1 + PERMUTATION[jj + 1]];
        return 70.0D * (contribution2D(g0, x0, y0) + contribution2D(g1, x1, y1) + contribution2D(g2, x2, y2));
    }

    static double noise3D(double x, double y, double z) {
        double skew = (x + y + z) * F3;
        int i = fastFloor(x + skew);
        int j = fastFloor(y + skew);
        int k = fastFloor(z + skew);
        double unskew = (i + j + k) * G3;
        double x0 = x - (i - unskew);
        double y0 = y - (j - unskew);
        double z0 = z - (k - unskew);
        int i1;
        int j1;
        int k1;
        int i2;
        int j2;
        int k2;
        if (x0 >= y0) {
            if (y0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 1;
                k2 = 0;
            } else if (x0 >= z0) {
                i1 = 1;
                j1 = 0;
                k1 = 0;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            } else {
                i1 = 0;
                j1 = 0;
                k1 = 1;
                i2 = 1;
                j2 = 0;
                k2 = 1;
            }
        } else if (y0 < z0) {
            i1 = 0;
            j1 = 0;
            k1 = 1;
            i2 = 0;
            j2 = 1;
            k2 = 1;
        } else if (x0 < z0) {
            i1 = 0;
            j1 = 1;
            k1 = 0;
            i2 = 0;
            j2 = 1;
            k2 = 1;
        } else {
            i1 = 0;
            j1 = 1;
            k1 = 0;
            i2 = 1;
            j2 = 1;
            k2 = 0;
        }
        double x1 = x0 - i1 + G3, y1 = y0 - j1 + G3, z1 = z0 - k1 + G3;
        double x2 = x0 - i2 + 2.0D * G3, y2 = y0 - j2 + 2.0D * G3, z2 = z0 - k2 + 2.0D * G3;
        double x3 = x0 - 1.0D + 3.0D * G3, y3 = y0 - 1.0D + 3.0D * G3, z3 = z0 - 1.0D + 3.0D * G3;
        int ii = i & 255, jj = j & 255, kk = k & 255;
        int g0 = PERMUTATION_MOD_12[ii + PERMUTATION[jj + PERMUTATION[kk]]];
        int g1 = PERMUTATION_MOD_12[ii + i1 + PERMUTATION[jj + j1 + PERMUTATION[kk + k1]]];
        int g2 = PERMUTATION_MOD_12[ii + i2 + PERMUTATION[jj + j2 + PERMUTATION[kk + k2]]];
        int g3 = PERMUTATION_MOD_12[ii + 1 + PERMUTATION[jj + 1 + PERMUTATION[kk + 1]]];
        return 32.0D * (contribution3D(g0, x0, y0, z0) + contribution3D(g1, x1, y1, z1)
            + contribution3D(g2, x2, y2, z2)
            + contribution3D(g3, x3, y3, z3));
    }

    private static double contribution2D(int gradient, double x, double y) {
        double t = 0.5D - x * x - y * y;
        if (t < 0) return 0;
        t *= t;
        Grad g = GRADIENTS[gradient];
        return t * t * (g.x * x + g.y * y);
    }

    private static double contribution3D(int gradient, double x, double y, double z) {
        double t = 0.6D - x * x - y * y - z * z;
        if (t < 0) return 0;
        t *= t;
        Grad g = GRADIENTS[gradient];
        return t * t * (g.x * x + g.y * y + g.z * z);
    }

    private static int fastFloor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private record Grad(double x, double y, double z) {}
}
