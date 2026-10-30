package industries.angelic.biomic.world.modern.noise;

/** Improved Perlin octave used by modern Minecraft's density and climate fields. */
final class ModernImprovedNoise {

    private static final int[][] GRADIENT = { { 1, 1, 0 }, { -1, 1, 0 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, 0, 1 },
        { -1, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 1, 1 }, { 0, -1, 1 }, { 0, 1, -1 }, { 0, -1, -1 }, { 1, 1, 0 },
        { 0, -1, 1 }, { -1, 1, 0 }, { 0, -1, -1 } };

    private final byte[] permutation = new byte[256];
    private final double xOffset;
    private final double yOffset;
    private final double zOffset;

    ModernImprovedNoise(ModernRandom random) {
        xOffset = random.nextDouble() * 256.0D;
        yOffset = random.nextDouble() * 256.0D;
        zOffset = random.nextDouble() * 256.0D;
        for (int index = 0; index < 256; index++) permutation[index] = (byte) index;
        for (int index = 0; index < 256; index++) {
            int swap = index + random.nextInt(256 - index);
            byte value = permutation[index];
            permutation[index] = permutation[swap];
            permutation[swap] = value;
        }
    }

    double sample(double x, double y, double z) {
        return sample(x, y, z, 0.0D, 0.0D);
    }

    double sample(double x, double y, double z, double yScale, double yMax) {
        double shiftedX = x + xOffset;
        double shiftedY = y + yOffset;
        double shiftedZ = z + zOffset;
        int floorX = floor(shiftedX);
        int floorY = floor(shiftedY);
        int floorZ = floor(shiftedZ);
        double localX = shiftedX - floorX;
        double localY = shiftedY - floorY;
        double localZ = shiftedZ - floorZ;
        double quantizedY = 0.0D;
        if (yScale != 0.0D) {
            double limitedY = yMax >= 0.0D && yMax < localY ? yMax : localY;
            quantizedY = Math.floor(limitedY / yScale + 1.0000000116860974E-7D) * yScale;
        }
        return sampleAndLerp(floorX, floorY, floorZ, localX, localY - quantizedY, localZ, localY);
    }

    private double sampleAndLerp(int floorX, int floorY, int floorZ, double x, double y, double z, double fadeY) {
        int px0 = p(floorX);
        int px1 = p(floorX + 1);
        int p00 = p(px0 + floorY);
        int p01 = p(px0 + floorY + 1);
        int p10 = p(px1 + floorY);
        int p11 = p(px1 + floorY + 1);
        double v000 = grad(p(p00 + floorZ), x, y, z);
        double v100 = grad(p(p10 + floorZ), x - 1.0D, y, z);
        double v010 = grad(p(p01 + floorZ), x, y - 1.0D, z);
        double v110 = grad(p(p11 + floorZ), x - 1.0D, y - 1.0D, z);
        double v001 = grad(p(p00 + floorZ + 1), x, y, z - 1.0D);
        double v101 = grad(p(p10 + floorZ + 1), x - 1.0D, y, z - 1.0D);
        double v011 = grad(p(p01 + floorZ + 1), x, y - 1.0D, z - 1.0D);
        double v111 = grad(p(p11 + floorZ + 1), x - 1.0D, y - 1.0D, z - 1.0D);
        return lerp3(smoothstep(x), smoothstep(fadeY), smoothstep(z), v000, v100, v010, v110, v001, v101, v011, v111);
    }

    private int p(int value) {
        return permutation[value & 0xFF] & 0xFF;
    }

    private static double grad(int hash, double x, double y, double z) {
        int[] gradient = GRADIENT[hash & 0xF];
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double smoothstep(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double lerp2(double x, double y, double v00, double v10, double v01, double v11) {
        return lerp(y, lerp(x, v00, v10), lerp(x, v01, v11));
    }

    private static double lerp3(double x, double y, double z, double v000, double v100, double v010, double v110,
        double v001, double v101, double v011, double v111) {
        return lerp(z, lerp2(x, y, v000, v100, v010, v110), lerp2(x, y, v001, v101, v011, v111));
    }
}
