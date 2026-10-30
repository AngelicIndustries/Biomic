package industries.angelic.biomic.api;

/**
 * Bounded, zero-based layout for a detached chunk column. This is a data-shape descriptor, NOT evidence
 * that a live world, its packets or its renderer support this height. Existing callers retain LEGACY_256;
 * a companion such as Hohenangst must establish live capabilities separately.
 */
public enum ChunkVerticalLayout {

    LEGACY_256(256),
    EXTENDED_512(512);

    private final int height;

    ChunkVerticalLayout(int height) {
        this.height = height;
    }

    public int height() {
        return height;
    }

    public int sectionCount() {
        return height / 16;
    }

    public int blockCount() {
        return 16 * 16 * height;
    }

    public boolean containsY(int y) {
        return y >= 0 && y < height;
    }

    public static ChunkVerticalLayout forHeight(int height) {
        for (ChunkVerticalLayout layout : values()) {
            if (layout.height == height) return layout;
        }
        throw new IllegalArgumentException("Unsupported zero-based column height: " + height);
    }

    /** X-major, then Z, then Y; identical to the legacy 65,536-cell constructor layout at height 256. */
    public int index(int x, int y, int z) {
        if (x < 0 || x >= 16 || z < 0 || z >= 16 || !containsY(y)) {
            throw new IllegalArgumentException("Voxel outside local column: " + x + ", " + y + ", " + z);
        }
        return (x * 16 + z) * height + y;
    }

    public int localX(int index) {
        checkIndex(index);
        return index / height / 16;
    }

    public int localY(int index) {
        checkIndex(index);
        return index % height;
    }

    public int localZ(int index) {
        checkIndex(index);
        return index / height % 16;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= blockCount()) throw new IllegalArgumentException("Column index out of bounds");
    }

    /** Inclusive Y range, clipped to this layout. Section 31 legitimately sets the sign bit. */
    public int sectionMask(int minimumY, int maximumY) {
        int bottom = Math.max(0, minimumY);
        int top = Math.min(height - 1, maximumY);
        if (bottom > top) return 0;
        int first = bottom >> 4;
        int count = (top >> 4) - first + 1;
        return (int) (((1L << count) - 1L) << first);
    }
}
