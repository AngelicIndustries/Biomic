package industries.angelic.biomic.world.terrain;

/**
 * Projects Origen's authored -64..319 world into the 0..255 Anvil envelope.
 *
 * <p>
 * The visible part of an Origen structure is much less tolerant of vertical resampling than deep rock or the
 * mostly-empty space above its ordinary terrain. Keep the authored sea level through Y=158 as a one-block
 * translation, retain the existing even compression underground, and absorb the remaining upper-world squeeze
 * with a smooth quadratic curve. The upper curve is tangent to the one-to-one band, so terrain does not acquire a
 * hard shelf where compression begins.
 * </p>
 */
final class OrigenVerticalTransform {

    static final double AUTHORED_MIN_Y = -64.0D;
    static final double AUTHORED_SEA_LEVEL = 62.0D;
    static final double AUTHORED_VISUAL_CEILING = 158.0D;
    static final double AUTHORED_MAX_Y_EXCLUSIVE = 320.0D;

    static final double PHYSICAL_MIN_Y = 0.0D;
    static final double PHYSICAL_SEA_LEVEL = 63.0D;
    static final double PHYSICAL_VISUAL_CEILING = 159.0D;
    static final double PHYSICAL_MAX_Y_EXCLUSIVE = 256.0D;

    private static final double UNDERGROUND_AUTHORED_SPAN = AUTHORED_SEA_LEVEL - AUTHORED_MIN_Y;
    private static final double UNDERGROUND_PHYSICAL_SPAN = PHYSICAL_SEA_LEVEL - PHYSICAL_MIN_Y;
    private static final double UPPER_AUTHORED_SPAN = AUTHORED_MAX_Y_EXCLUSIVE - AUTHORED_VISUAL_CEILING;
    private static final double UPPER_COMPRESSION = AUTHORED_MAX_Y_EXCLUSIVE + 1.0D - PHYSICAL_MAX_Y_EXCLUSIVE;
    private static final double UPPER_CURVATURE = UPPER_COMPRESSION / (UPPER_AUTHORED_SPAN * UPPER_AUTHORED_SPAN);
    private static final double[] INTEGER_PHYSICAL_TO_AUTHORED = createIntegerInverseTable();

    private OrigenVerticalTransform() {}

    static double toPhysicalY(double authoredY) {
        if (authoredY <= AUTHORED_SEA_LEVEL) {
            return PHYSICAL_MIN_Y
                + (authoredY - AUTHORED_MIN_Y) * UNDERGROUND_PHYSICAL_SPAN / UNDERGROUND_AUTHORED_SPAN;
        }
        if (authoredY <= AUTHORED_VISUAL_CEILING) {
            return authoredY + 1.0D;
        }
        double upperOffset = authoredY - AUTHORED_VISUAL_CEILING;
        return authoredY + 1.0D - UPPER_CURVATURE * upperOffset * upperOffset;
    }

    static double toAuthoredY(double physicalY) {
        int integerY = (int) physicalY;
        if (physicalY == integerY && integerY >= (int) PHYSICAL_MIN_Y && integerY <= (int) PHYSICAL_MAX_Y_EXCLUSIVE) {
            return INTEGER_PHYSICAL_TO_AUTHORED[integerY];
        }
        return invertContinuous(physicalY);
    }

    private static double invertContinuous(double physicalY) {
        if (physicalY <= PHYSICAL_SEA_LEVEL) {
            return AUTHORED_MIN_Y
                + (physicalY - PHYSICAL_MIN_Y) * UNDERGROUND_AUTHORED_SPAN / UNDERGROUND_PHYSICAL_SPAN;
        }
        if (physicalY <= PHYSICAL_VISUAL_CEILING) {
            return physicalY - 1.0D;
        }

        // physicalOffset = authoredOffset - curvature * authoredOffset^2. The smaller quadratic root is the
        // monotone branch covering Origen's authored visual ceiling through its Y=320 upper bound.
        double physicalOffset = physicalY - PHYSICAL_VISUAL_CEILING;
        double discriminant = Math.max(0.0D, 1.0D - 4.0D * UPPER_CURVATURE * physicalOffset);
        double authoredOffset = (1.0D - Math.sqrt(discriminant)) / (2.0D * UPPER_CURVATURE);
        return AUTHORED_VISUAL_CEILING + authoredOffset;
    }

    private static double[] createIntegerInverseTable() {
        double[] result = new double[(int) PHYSICAL_MAX_Y_EXCLUSIVE + 1];
        for (int physicalY = 0; physicalY < result.length; physicalY++) {
            result[physicalY] = invertContinuous(physicalY);
        }
        return result;
    }
}
