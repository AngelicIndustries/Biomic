package industries.angelic.biomic.storage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import industries.angelic.biomic.Biomic;

/** Immutable quart-resolution biome identities for one 1.7.10 chunk. */
public final class BiomeVolume {

    public static final int QUART_WIDTH = 4;
    public static final int QUART_HEIGHT = 64;
    /** Stored coverage, not the live world's build limit. Schema 1 remains a 0..255 volume. */
    public static final int BLOCK_HEIGHT = QUART_HEIGHT * 4;
    public static final int CELL_COUNT = QUART_WIDTH * QUART_WIDTH * QUART_HEIGHT;
    public static final int FULL_BLOCK_HEIGHT = 512;
    public static final int FULL_QUART_HEIGHT = FULL_BLOCK_HEIGHT / 4;
    public static final int MAX_CELL_COUNT = QUART_WIDTH * QUART_WIDTH * FULL_QUART_HEIGHT;
    public static final int MAX_PALETTE_ENTRIES = 64;
    public static final int MAX_KEY_BYTES = 128;
    public static final String SURFACE_KEY = Biomic.MODID + ":surface";

    private final int generatorVersion;
    private final int blockHeight, quartHeight;
    private final List<String> palette;
    private final short[] paletteIndices;

    private BiomeVolume(int generatorVersion, int blockHeight, List<String> palette, short[] paletteIndices) {
        this.generatorVersion = generatorVersion;
        this.blockHeight = blockHeight;
        this.quartHeight = blockHeight / 4;
        this.palette = Collections.unmodifiableList(new ArrayList<>(palette));
        this.paletteIndices = paletteIndices.clone();
    }

    public static BiomeVolume create(int generatorVersion, List<String> palette, short[] paletteIndices) {
        return create(generatorVersion, BLOCK_HEIGHT, palette, paletteIndices);
    }

    public static BiomeVolume create(int generatorVersion, int blockHeight, List<String> palette,
        short[] paletteIndices) {
        validate(generatorVersion, blockHeight, palette, paletteIndices);
        return new BiomeVolume(generatorVersion, blockHeight, palette, paletteIndices);
    }

    public static BiomeVolume surfaceOnly(int generatorVersion) {
        return create(generatorVersion, Collections.singletonList(SURFACE_KEY), new short[CELL_COUNT]);
    }

    public static BiomeVolume surfaceOnly(int generatorVersion, int blockHeight) {
        return create(
            generatorVersion,
            blockHeight,
            Collections.singletonList(SURFACE_KEY),
            new short[cellCount(blockHeight)]);
    }

    public int getGeneratorVersion() {
        return generatorVersion;
    }

    public int getBlockHeight() {
        return blockHeight;
    }

    public int getQuartHeight() {
        return quartHeight;
    }

    public boolean containsBlockY(int blockY) {
        return blockY >= 0 && blockY < getBlockHeight();
    }

    public List<String> getPalette() {
        return palette;
    }

    public int getPaletteIndex(int quartX, int quartY, int quartZ) {
        return paletteIndices[index(quartX, quartY, quartZ)] & 0xFFFF;
    }

    public String getBiomeKey(int quartX, int quartY, int quartZ) {
        return palette.get(getPaletteIndex(quartX, quartY, quartZ));
    }

    public short[] copyPaletteIndices() {
        return paletteIndices.clone();
    }

    public BiomeVolume copy() {
        return new BiomeVolume(generatorVersion, blockHeight, palette, paletteIndices);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BiomeVolume)) {
            return false;
        }
        BiomeVolume that = (BiomeVolume) other;
        return generatorVersion == that.generatorVersion && blockHeight == that.blockHeight
            && palette.equals(that.palette)
            && Arrays.equals(paletteIndices, that.paletteIndices);
    }

    @Override
    public int hashCode() {
        int result = 31 * (31 * generatorVersion + blockHeight) + palette.hashCode();
        return 31 * result + Arrays.hashCode(paletteIndices);
    }

    private static void validate(int generatorVersion, int blockHeight, List<String> palette, short[] paletteIndices) {
        if (generatorVersion < 0) {
            throw new IllegalArgumentException("Generator version cannot be negative");
        }
        if (palette == null || palette.isEmpty() || palette.size() > MAX_PALETTE_ENTRIES) {
            throw new IllegalArgumentException(
                "Biome palette must contain between 1 and " + MAX_PALETTE_ENTRIES + " entries");
        }
        if (!SURFACE_KEY.equals(palette.get(0))) {
            throw new IllegalArgumentException("Biome palette entry 0 must be " + SURFACE_KEY);
        }

        Set<String> uniqueKeys = new HashSet<>();
        for (String key : palette) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("Biome palette keys cannot be null or empty");
            }
            if (key.getBytes(StandardCharsets.UTF_8).length > MAX_KEY_BYTES) {
                throw new IllegalArgumentException(
                    "Biome palette key exceeds " + MAX_KEY_BYTES + " UTF-8 bytes: " + key);
            }
            if (!uniqueKeys.add(key)) {
                throw new IllegalArgumentException("Duplicate biome palette key: " + key);
            }
        }

        int cells = cellCount(blockHeight);
        if (paletteIndices == null || paletteIndices.length != cells) {
            throw new IllegalArgumentException("Biome volume must contain exactly " + cells + " cells");
        }
        for (short paletteIndex : paletteIndices) {
            int unsignedIndex = paletteIndex & 0xFFFF;
            if (unsignedIndex >= palette.size()) {
                throw new IllegalArgumentException(
                    "Biome volume palette index " + unsignedIndex + " exceeds palette size " + palette.size());
            }
        }
    }

    private int index(int quartX, int quartY, int quartZ) {
        if (quartX < 0 || quartX >= QUART_WIDTH
            || quartZ < 0
            || quartZ >= QUART_WIDTH
            || quartY < 0
            || quartY >= quartHeight) {
            throw new IndexOutOfBoundsException(
                "Quart coordinates must be x/z in [0, 3] and y in [0, " + (quartHeight - 1)
                    + "], got ("
                    + quartX
                    + ", "
                    + quartY
                    + ", "
                    + quartZ
                    + ")");
        }
        return (quartY * QUART_WIDTH + quartZ) * QUART_WIDTH + quartX;
    }

    private static int cellCount(int blockHeight) {
        if (blockHeight != BLOCK_HEIGHT && blockHeight != FULL_BLOCK_HEIGHT)
            throw new IllegalArgumentException("Biome volume block height must be 256 or 512");
        return QUART_WIDTH * QUART_WIDTH * (blockHeight / 4);
    }
}
