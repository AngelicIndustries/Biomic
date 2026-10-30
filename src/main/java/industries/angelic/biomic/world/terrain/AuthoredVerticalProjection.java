package industries.angelic.biomic.world.terrain;

import java.util.OptionalInt;

/**
 * Pure coordinate-domain contract between a modern authored envelope and physical block storage.
 *
 * <p>
 * Block conversions are checked and never clamp an out-of-domain coordinate. Continuous conversions
 * accept block-center coordinates from the inclusive minimum through inclusive maximum block Y; they do
 * not claim that a many-to-one compressed projection round-trips. Water top is the highest authored or
 * physical water block, while sea boundary is the first block above that water column.
 * </p>
 */
public interface AuthoredVerticalProjection {

    String revision();

    int sourceMinY();

    int sourceMaxYExclusive();

    int physicalMinY();

    int physicalMaxYExclusive();

    int requiredStorageHeight();

    /** True only for supported physical layouts large enough for this projection's authored envelope. */
    default boolean supportsStorageHeight(int height) {
        return (height == 256 || height == 512) && height >= requiredStorageHeight();
    }

    int physicalWaterTop();

    int physicalSeaBoundary();

    boolean containsSourceBlockY(int y);

    boolean containsPhysicalBlockY(int y);

    OptionalInt toPhysicalBlockY(int sourceY);

    OptionalInt toSourceBlockY(int physicalY);
}
