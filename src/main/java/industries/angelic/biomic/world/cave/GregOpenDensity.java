package industries.angelic.biomic.world.cave;

/**
 * Explicit Biomic adaptation of the source Greg solid-positive field to a normalized positive-is-open field.
 *
 * <p>
 * This is not an Origen density conversion. Each policy supplies a fixed source host-density input and passes
 * legacy block X/Y/Z coordinates unchanged to {@link UltimateGregField}. The regional chamber policy deliberately
 * selects the source's full {@code hostDensity >= 170} branch. The limited policy exists only as a numerical
 * comparison fixture for the source's other branch. Only the chamber policy is reachable, and only through the
 * explicitly saved regional provider; automatic world selection never chooses it.
 * </p>
 */
public final class GregOpenDensity {

    public enum CoordinateFrame {
        /** Integer legacy block coordinates, Y0..255, with no authored-height or physical-height translation. */
        LEGACY_BLOCK_XYZ_V1
    }

    public enum Policy {

        /** Biomic regional chamber candidate: exact Greg full branch at a fixed, explicitly adapted host input. */
        REGIONAL_CHAMBER_V1(256.0D, 128.0D, CoordinateFrame.LEGACY_BLOCK_XYZ_V1, true),
        /** Test/reference policy for comparing the source's limited branch; not a production selection. */
        LIMITED_BRANCH_REFERENCE_V1(128.0D, 128.0D, CoordinateFrame.LEGACY_BLOCK_XYZ_V1, false);

        private final double sourceHostDensity;
        private final double sourceScale;

        Policy(double sourceHostDensity, double sourceScale, CoordinateFrame coordinateFrame,
            boolean chamberCandidate) {
            this.sourceHostDensity = sourceHostDensity;
            this.sourceScale = sourceScale;
            if (!Double.isFinite(sourceHostDensity) || !Double.isFinite(sourceScale)
                || sourceScale <= 0.0D
                || coordinateFrame == null
                || sourceHostDensity < 0.0D
                || chamberCandidate != (sourceHostDensity >= 170.0D)) {
                throw new IllegalArgumentException("Invalid Greg open-density policy");
            }
        }

        public double sourceHostDensity() {
            return sourceHostDensity;
        }

        public double sourceScale() {
            return sourceScale;
        }
    }

    private final UltimateGregField source;
    private final Policy policy;

    public GregOpenDensity(long stableSeed, Policy policy) {
        if (policy == null) throw new IllegalArgumentException("Missing Greg open-density policy");
        source = new UltimateGregField(stableSeed);
        this.policy = policy;
    }

    /** Returns a finite positive-is-open density in [-1,1] in the policy's explicit legacy coordinate frame. */
    public double openDensity(int blockX, int legacyBlockY, int blockZ) {
        if (legacyBlockY < 0 || legacyBlockY > 255) {
            throw new IllegalArgumentException("Greg legacy block Y must be within 0..255");
        }
        double solidPositive = source.density(policy.sourceHostDensity(), blockX, legacyBlockY, blockZ);
        if (!Double.isFinite(solidPositive)) throw new IllegalStateException("Greg source returned non-finite density");
        return clamp(-solidPositive / policy.sourceScale());
    }

    private static double clamp(double value) {
        return Math.max(-1.0D, Math.min(1.0D, value));
    }
}
