package industries.angelic.biomic.world.cave;

/**
 * Pure sparse regional weight for a future single-field Worley/Greg composition.
 *
 * <p>
 * This class does not sample cave density, inspect terrain or plan excavation. Overlapping active cells are
 * supported: the result is the maximum influence of every cell whose jittered center can reach the queried point.
 * The finite inspection radius is derived from the supplied span and outer radius rather than from a hidden
 * no-overlap assumption.
 * </p>
 */
public final class GregCavernRegionSampler {

    private static final int MAX_NEIGHBOR_CELL_RADIUS = 8;
    private static final long ACTIVE_SALT = 0x4752454741435449L;
    private static final long CENTER_X_SALT = 0x4752454743454E58L;
    private static final long CENTER_Z_SALT = 0x4752454743454E5AL;

    /** All geometry-affecting choices are explicit; there are no production defaults in this unwired sampler. */
    public record Settings(int regionCellSpan, int activationNumerator, int activationDenominator,
        int innerPlateauRadius, int outerRadius) {

        public Settings {
            if (regionCellSpan < 16 || regionCellSpan > 1_048_576) {
                throw new IllegalArgumentException("Greg region-cell span must be within 16..1048576 blocks");
            }
            if (activationDenominator < 1 || activationDenominator > 1_000_000
                || activationNumerator < 0
                || activationNumerator > activationDenominator) {
                throw new IllegalArgumentException("Invalid Greg activation fraction");
            }
            if (innerPlateauRadius < 0 || outerRadius <= innerPlateauRadius || outerRadius > 4_000_000) {
                throw new IllegalArgumentException("Invalid Greg regional radii");
            }
            if (inspectionRadius(regionCellSpan, outerRadius) > MAX_NEIGHBOR_CELL_RADIUS) {
                throw new IllegalArgumentException("Greg regional influence exceeds the bounded neighbor inspection");
            }
        }
    }

    private final long seed;
    private final Settings settings;
    private final int inspectionRadius;

    public GregCavernRegionSampler(long stableSeed, Settings settings) {
        if (settings == null) throw new IllegalArgumentException("Missing Greg regional settings");
        seed = stableSeed;
        this.settings = settings;
        inspectionRadius = inspectionRadius(settings.regionCellSpan(), settings.outerRadius());
    }

    /** Returns a deterministic smooth blend weight in [0,1]. */
    public double weight(int blockX, int blockZ) {
        if (settings.activationNumerator() == 0) return 0.0D;
        long cellX = Math.floorDiv((long) blockX, settings.regionCellSpan());
        long cellZ = Math.floorDiv((long) blockZ, settings.regionCellSpan());
        double maximum = 0.0D;
        for (int offsetX = -inspectionRadius; offsetX <= inspectionRadius; offsetX++) {
            long candidateX = cellX + offsetX;
            for (int offsetZ = -inspectionRadius; offsetZ <= inspectionRadius; offsetZ++) {
                long candidateZ = cellZ + offsetZ;
                if (!activeCell(candidateX, candidateZ)) continue;
                long dx = centerX(candidateX, candidateZ) - (long) blockX;
                long dz = centerZ(candidateX, candidateZ) - (long) blockZ;
                double distance = StrictMath.hypot((double) dx, (double) dz);
                double influence = influence(distance);
                if (influence > maximum) maximum = influence;
                if (maximum == 1.0D) return maximum;
            }
        }
        return maximum;
    }

    private double influence(double distance) {
        if (distance <= settings.innerPlateauRadius()) return 1.0D;
        if (distance >= settings.outerRadius()) return 0.0D;
        double progress = (settings.outerRadius() - distance)
            / (double) (settings.outerRadius() - settings.innerPlateauRadius());
        return progress * progress * (3.0D - 2.0D * progress);
    }

    boolean activeCell(long cellX, long cellZ) {
        if (settings.activationNumerator() == 0) return false;
        if (settings.activationNumerator() == settings.activationDenominator()) return true;
        long value = hash(cellX, cellZ, ACTIVE_SALT);
        return Long.remainderUnsigned(value, settings.activationDenominator()) < settings.activationNumerator();
    }

    long centerX(long cellX, long cellZ) {
        return cellX * settings.regionCellSpan()
            + Long.remainderUnsigned(hash(cellX, cellZ, CENTER_X_SALT), settings.regionCellSpan());
    }

    long centerZ(long cellX, long cellZ) {
        return cellZ * settings.regionCellSpan()
            + Long.remainderUnsigned(hash(cellX, cellZ, CENTER_Z_SALT), settings.regionCellSpan());
    }

    private long hash(long cellX, long cellZ, long salt) {
        long value = seed ^ salt
            ^ mix64(cellX + 0x9E3779B97F4A7C15L)
            ^ Long.rotateLeft(mix64(cellZ + 0xD1B54A32D192ED03L), 29);
        return mix64(value);
    }

    private static int inspectionRadius(int span, int outerRadius) {
        // One additional cell accounts for a center jittered to either edge of its owning cell.
        return (int) (((long) outerRadius + span - 1L) / span) + 1;
    }

    private static long mix64(long value) {
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
