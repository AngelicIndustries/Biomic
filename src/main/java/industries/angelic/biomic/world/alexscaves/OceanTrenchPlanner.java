package industries.angelic.biomic.world.alexscaves;

/**
 * Detached whole-footprint projection of Alex's Caves' pinned Ocean Trench piece.
 *
 * <p>
 * The source formulas are evaluated in modern Y and only their resulting writes cross the
 * save-pinned {@code modern-overworld-to-legacy-v1} boundary. A 512-high physical layout remains
 * storage headroom; it does not select an uncompressed coordinate system.
 * </p>
 */
public final class OceanTrenchPlanner {

    public static final int SOURCE_CENTER_Y = -16, SOURCE_MIN_Y = -64, SOURCE_SEA_LEVEL = 63;
    public static final int SOURCE_RADIUS = 150, SOURCE_HEIGHT_RADIUS = 64, MAX_REALIZED_RADIUS = 182;

    private OceanTrenchPlanner() {}

    /**
     * One column of a trench centred on {@code centerX,centerZ}: the source dig and sea-mount tests with every
     * y-independent noise term sampled once. Equivalent to {@link #shouldDig} and {@link #seaMount} cell by cell.
     */
    public static final class Column {

        private final int floor, lowest;
        private final double distance, radiusSquared;
        private final float below, df1, df2, mountTarget, mountSample;

        public Column(int centerX, int centerZ, int radius, int x, int z, int floor) {
            this.floor = floor;
            lowest = lowestDigY(x, z);
            double dx = (double) x - centerX, dz = (double) z - centerZ;
            distance = dx * dx + 1D + dz * dz;
            radiusSquared = radiusSquared(radius, x, z);
            below = smin((SOURCE_SEA_LEVEL - floor) / 120F, 1F, .2F);
            df1 = noise3(x, 0, z, 100) * .6F;
            df2 = noise3(x - 450, 0, z + 450, 300) * .25F;
            mountTarget = 20F + noise3(x - 440, 0, z + 412, 30) * 10F + noise3(x - 110, 0, z + 110, 10) * 3F;
            mountSample = noise3(x, 0, z, 50) + noise3(x - 440, 0, z + 412, 11) * .2F
                + noise3(x - 100, 0, z - 400, 100) * .3F
                - .1F;
        }

        /** Source Y range the trench may dig in this column; empty (low &gt; high) on land and shallows. */
        public int digLow() {
            return lowest;
        }

        public int digHigh() {
            return floor >= SOURCE_SEA_LEVEL - 3 ? Integer.MIN_VALUE : Math.min(floor + 3, SOURCE_SEA_LEVEL - 2);
        }

        public boolean digs(int sourceY) {
            int j = SOURCE_MIN_Y - sourceY;
            if (sourceY >= SOURCE_SEA_LEVEL + 1 || j > 0 || floor >= SOURCE_SEA_LEVEL - 3) return false;
            float bedrock = smin(Math.abs(j) / 50F - .1F, 1F, .2F);
            float yDistance = smin(below * (bedrock - df1) - df2, .9F, .2F) - df2;
            double corner = radiusSquared - distance;
            if (corner > 0D && corner <= 100D) yDistance *= corner / 100F;
            return distance <= yDistance * radiusSquared;
        }

        public boolean seaMount(int sourceY) {
            float heightScale = (mountTarget - (sourceY + 64)) / (mountTarget + 15F);
            return mountSample >= .4F * Math.max(0F, 1F - heightScale);
        }
    }

    /** Lowest source Y the trench may dig in this column (the source's noisy floor margin above bedrock). */
    public static int lowestDigY(int x, int z) {
        return (int) (SOURCE_MIN_Y + 2 + (1F + noise2(x - 800, z - 212, 20)) * 5F);
    }

    private static double radiusSquared(int radius, int x, int z) {
        float a = noise2(x, z, 30), b = noise2(x + 1000, z - 1000, 100);
        float multiplier = .8F + .2F * (1F + a + b) * .5F;
        return multiplier * radius * radius;
    }

    private static float smin(float a, float b, float k) {
        float h = Math.max(k - Math.abs(a - b), 0F) / k;
        return Math.min(a, b) - h * h * k * .25F;
    }

    private static float noise2(int x, int z, float scale) {
        return (float) AlexCavesSimplexNoise.sample2D(x, z, scale);
    }

    private static float noise3(int x, int y, int z, float scale) {
        return (float) AlexCavesSimplexNoise.sample3D(x, y, z, scale);
    }
}
