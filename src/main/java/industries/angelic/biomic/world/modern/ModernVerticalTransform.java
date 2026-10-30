package industries.angelic.biomic.world.modern;

/**
 * Explicit 1.21.1 Overworld-to-1.7.10 height compromise for the RU parity world type.
 *
 * <p>
 * The sea surface remains at Y=63. The 127 modern blocks below it are compressed into the 63
 * legacy blocks below it, while the 256 modern blocks above it are compressed into the 192 legacy
 * blocks above it. Density functions are sampled at {@link #toModernY(double)}; feature anchors use
 * {@link #toLegacyY(int)}. Keeping this transform isolated prevents accidental Origen height rules
 * from leaking into the modern-parity generator.
 * </p>
 */
public final class ModernVerticalTransform {

    public static final int MODERN_MIN_Y = -64;
    public static final int MODERN_SEA_LEVEL = 63;
    public static final int MODERN_MAX_Y = 319;
    public static final int LEGACY_MIN_Y = 0;
    public static final int LEGACY_SEA_LEVEL = 63;
    public static final int LEGACY_MAX_Y = 255;

    private static final double BELOW_SCALE = (double) (LEGACY_SEA_LEVEL - LEGACY_MIN_Y)
        / (MODERN_SEA_LEVEL - MODERN_MIN_Y);
    private static final double ABOVE_SCALE = (double) (LEGACY_MAX_Y - LEGACY_SEA_LEVEL)
        / (MODERN_MAX_Y - MODERN_SEA_LEVEL);
    private static final int[] LOWEST_MODERN_Y = new int[LEGACY_MAX_Y + 1];
    private static final int[] HIGHEST_MODERN_Y = new int[LEGACY_MAX_Y + 1];

    static {
        java.util.Arrays.fill(LOWEST_MODERN_Y, Integer.MAX_VALUE);
        java.util.Arrays.fill(HIGHEST_MODERN_Y, Integer.MIN_VALUE);
        for (int modernY = MODERN_MIN_Y; modernY <= MODERN_MAX_Y; modernY++) {
            int legacyY = toLegacyY(modernY);
            LOWEST_MODERN_Y[legacyY] = Math.min(LOWEST_MODERN_Y[legacyY], modernY);
            HIGHEST_MODERN_Y[legacyY] = Math.max(HIGHEST_MODERN_Y[legacyY], modernY);
        }
    }

    private ModernVerticalTransform() {}

    public static double toLegacyY(double modernY) {
        if (modernY <= MODERN_SEA_LEVEL) {
            return LEGACY_MIN_Y + (modernY - MODERN_MIN_Y) * BELOW_SCALE;
        }
        return LEGACY_SEA_LEVEL + (modernY - MODERN_SEA_LEVEL) * ABOVE_SCALE;
    }

    public static int toLegacyY(int modernY) {
        return clampLegacy((int) Math.floor(toLegacyY((double) modernY) + 0.5D));
    }

    public static double toModernY(double legacyY) {
        if (legacyY <= LEGACY_SEA_LEVEL) {
            return MODERN_MIN_Y + (legacyY - LEGACY_MIN_Y) / BELOW_SCALE;
        }
        return MODERN_SEA_LEVEL + (legacyY - LEGACY_SEA_LEVEL) / ABOVE_SCALE;
    }

    public static int toModernY(int legacyY) {
        return clampModern((int) Math.floor(toModernY((double) legacyY) + 0.5D));
    }

    private static int clampLegacy(int y) {
        return Math.max(LEGACY_MIN_Y, Math.min(LEGACY_MAX_Y, y));
    }

    private static int clampModern(int y) {
        return Math.max(MODERN_MIN_Y, Math.min(MODERN_MAX_Y, y));
    }
}
