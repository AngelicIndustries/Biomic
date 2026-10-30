package industries.angelic.biomic.compat.biome;

import java.util.Objects;
import java.util.Random;

import industries.angelic.biomic.world.modern.ModernVerticalTransform;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjection;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjections;

/**
 * Transports BOP's legacy decorator Y draws without scaling native feature geometry.
 * The random draw is made in the original 0..255 scheduling domain, then mapped once.
 */
public final class BopHeightSchedule {

    private static final BopHeightSchedule LEGACY = new BopHeightSchedule(
        AuthoredVerticalProjections.origenCompressed256(),
        true);
    private static final BopHeightSchedule SHIFTED = new BopHeightSchedule(
        AuthoredVerticalProjections.shifted512(),
        false);

    private final AuthoredVerticalProjection projection;
    private final boolean legacy;

    private BopHeightSchedule(AuthoredVerticalProjection projection, boolean legacy) {
        this.projection = Objects.requireNonNull(projection, "projection");
        this.legacy = legacy;
    }

    public static BopHeightSchedule forProjection(AuthoredVerticalProjection projection) {
        if (projection == AuthoredVerticalProjections.origenCompressed256()) return LEGACY;
        if (projection == AuthoredVerticalProjections.shifted512()) return SHIFTED;
        throw new IllegalArgumentException("BOP height scheduling requires a canonical hosted projection");
    }

    /**
     * Whether a resolved physical surface can seed this schedule. A missing WORLD_SURFACE result is represented by
     * zero by the hosted view; shifted generation must skip that attempt rather than clamp it into the authored
     * envelope and create a truncated native feature. Legacy keeps its exact historical 0..255 domain.
     */
    public boolean acceptsPhysicalSurfaceY(int physicalSurfaceY) {
        return legacy ? physicalSurfaceY >= 0 && physicalSurfaceY < 256
            : projection.containsPhysicalBlockY(physicalSurfaceY);
    }

    /** Exact replacement for HostedIslandRuntime's Height switch. */
    public int select(BopNativeBiomes.Height height, Random random, int physicalSurfaceY) {
        Objects.requireNonNull(height, "height");
        Objects.requireNonNull(random, "random");
        if (height == BopNativeBiomes.Height.SURFACE) {
            legacySurfaceY(physicalSurfaceY); // Validate the selected projection without lossy round-tripping.
            return physicalSurfaceY;
        }
        int surface = legacySurfaceY(physicalSurfaceY);
        int scheduled = switch (height) {
            case SURFACE -> throw new AssertionError("handled above");
            case RANDOM_64 -> random.nextInt(64);
            case RANDOM_256 -> random.nextInt(256);
            case RANDOM_DOUBLE_SURFACE -> random.nextInt(Math.max(1, Math.multiplyExact(surface, 2)));
            case RANDOM_SURFACE_PLUS_32 -> random.nextInt(Math.addExact(surface, 32));
            case SURFACE_PLUS_64 -> Math.addExact(surface, random.nextInt(64));
            case SURFACE_CENTERED_128 -> Math.addExact(Math.subtractExact(surface, 64), random.nextInt(128));
        };
        return physicalY(scheduled);
    }

    public int legacySurfaceY(int physicalSurfaceY) {
        if (legacy) {
            if (!acceptsPhysicalSurfaceY(physicalSurfaceY))
                throw new IllegalArgumentException("Legacy BOP surface outside 0..255: " + physicalSurfaceY);
            return physicalSurfaceY;
        }
        int source = projection.toSourceBlockY(physicalSurfaceY)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Shifted BOP surface outside authored terrain: " + physicalSurfaceY));
        return ModernVerticalTransform.toLegacyY(source);
    }

    /** Source-coordinate anchor for legacy generators whose own clearance code hard-codes y<256. */
    public int nativeGeneratorY(int physicalY) {
        return legacy ? physicalY : legacySurfaceY(physicalY);
    }

    /** Maps an old decorator candidate; out-of-envelope source draws remain out of envelope, never clamp. */
    public int physicalY(int legacyY) {
        if (legacy) return legacyY;
        if (legacyY < 0) return Math.addExact(projection.physicalMinY(), legacyY);
        if (legacyY > ModernVerticalTransform.LEGACY_MAX_Y)
            return Math.addExact(projection.physicalMaxYExclusive(), legacyY - 256);
        int source = ModernVerticalTransform.toModernY(legacyY);
        return projection.toPhysicalBlockY(source)
            .orElseThrow();
    }
}
