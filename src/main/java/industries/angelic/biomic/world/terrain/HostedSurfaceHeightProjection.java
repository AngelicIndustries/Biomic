package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.modern.ModernVerticalTransform;

/** Explicit physical-to-source coordinate policy for hosted modern/RU surface rules. */
public final class HostedSurfaceHeightProjection {

    private static final HostedSurfaceHeightProjection LEGACY = new HostedSurfaceHeightProjection(null);
    private final AuthoredVerticalProjection projection;

    private HostedSurfaceHeightProjection(AuthoredVerticalProjection projection) {
        this.projection = projection;
    }

    public static HostedSurfaceHeightProjection legacy() {
        return LEGACY;
    }

    public static HostedSurfaceHeightProjection shifted512(AuthoredVerticalProjection projection) {
        if (projection != AuthoredVerticalProjections.shifted512()) {
            throw new IllegalArgumentException("Hosted shifted surfaces require the canonical shifted-512 projection");
        }
        return new HostedSurfaceHeightProjection(projection);
    }

    public int sourceY(int physicalY) {
        if (projection == null) return ModernVerticalTransform.toModernY(physicalY);
        return projection.toSourceBlockY(physicalY)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Hosted surface Y is outside the shifted authored envelope: " + physicalY));
    }

    public int sourceDepth(int upperPhysicalY, int lowerPhysicalY) {
        if (lowerPhysicalY > upperPhysicalY) throw new IllegalArgumentException("Surface depth is inverted");
        return sourceY(upperPhysicalY) - sourceY(lowerPhysicalY);
    }

    /** Retains the old floor-layers>0 condition exactly; shifted mode uses the actual authored envelope. */
    public boolean containsLowerLayer(int physicalY) {
        return projection == null ? physicalY > 0 : projection.containsPhysicalBlockY(physicalY);
    }

    /** Legacy used literal physical 62; shifted mode uses the projected authored water-top block. */
    public int slopeFloorPhysicalY() {
        return projection == null ? 62 : projection.physicalWaterTop();
    }
}
