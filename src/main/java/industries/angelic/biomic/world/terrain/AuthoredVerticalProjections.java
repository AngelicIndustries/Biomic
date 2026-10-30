package industries.angelic.biomic.world.terrain;

import java.util.OptionalInt;

/**
 * Pure projection definitions; saved selection belongs to the provider's immutable world policy.
 *
 * <p>
 * The hidden modern/RU tall pilot uses a separately saved revision and explicit peer checks. Other
 * providers retain their legacy mappings. Storage height alone must never select {@link #shifted512()}.
 * </p>
 */
public final class AuthoredVerticalProjections {

    private static final AuthoredVerticalProjection ORIGEN_COMPRESSED = new Projection(
        "origen-authored-to-legacy-v1",
        0,
        256,
        256,
        63,
        64,
        y -> clamp((int) Math.round(OrigenVerticalTransform.toPhysicalY(y)), 0, 255),
        y -> clamp((int) Math.round(OrigenVerticalTransform.toAuthoredY(y)), -64, 319));
    private static final AuthoredVerticalProjection SHIFTED_512 = new Projection(
        "shifted-512-v1",
        64,
        448,
        512,
        190,
        191,
        y -> y + 128,
        y -> y - 128);

    private AuthoredVerticalProjections() {}

    public static AuthoredVerticalProjection origenCompressed256() {
        return ORIGEN_COMPRESSED;
    }

    public static AuthoredVerticalProjection shifted512() {
        return SHIFTED_512;
    }

    @FunctionalInterface
    private interface IntProjection {

        int apply(int value);
    }

    private record Projection(String revision, int physicalMinY, int physicalMaxYExclusive, int requiredStorageHeight,
        int physicalWaterTop, int physicalSeaBoundary, IntProjection physicalBlock, IntProjection sourceBlock)
        implements AuthoredVerticalProjection {

        @Override
        public int sourceMinY() {
            return -64;
        }

        @Override
        public int sourceMaxYExclusive() {
            return 320;
        }

        @Override
        public boolean containsSourceBlockY(int y) {
            return y >= sourceMinY() && y < sourceMaxYExclusive();
        }

        @Override
        public boolean containsPhysicalBlockY(int y) {
            return y >= physicalMinY && y < physicalMaxYExclusive;
        }

        @Override
        public OptionalInt toPhysicalBlockY(int y) {
            return containsSourceBlockY(y) ? OptionalInt.of(physicalBlock.apply(y)) : OptionalInt.empty();
        }

        @Override
        public OptionalInt toSourceBlockY(int y) {
            return containsPhysicalBlockY(y) ? OptionalInt.of(sourceBlock.apply(y)) : OptionalInt.empty();
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
