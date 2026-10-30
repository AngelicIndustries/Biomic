package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * The rock of a floating island (IslandKeel). The core is stone. The visible underside and flanks are speckled, in
 * four-block bands every third of which is darker, and the last blocks of hanging tips are dark rock. The speckle
 * follows the island's kind: stone, tuff, andesite, cobblestone and deepslate with 8% mossy cobblestone on green
 * islands (16% on lush ones), sandstone, granite and terracotta bands on dry ones, diorite and calcite on snowy ones.
 * The deck's top blocks stay stone for the habitat's soil.
 */
final class IslandSkin {

    /** Blocks under the top left to the habitat's soil pass. */
    private static final int SOIL = 3;
    private static final long SPECKLE_SALT = 0x4F1BBCDCBFA53E0AL, BAND_SALT = 0x1F83D9ABFB41BD6BL;

    private IslandSkin() {}

    /**
     * The material of an island cell.
     *
     * @param exposed the cell has open sky beside it (a flank)
     * @param tip     the column hangs lower than all four beside it
     */
    static byte material(long seed, int x, int y, int z, int top, int bottom, boolean exposed, boolean tip,
        IslandKind kind) {
        if (top - y < SOIL) return BiomicTerrainSampler.STONE;
        int above = y - bottom;
        if (tip && above <= 2)
            return (hash(seed, x, y, z) & 1) == 0 ? BiomicTerrainSampler.DEEPSLATE : BiomicTerrainSampler.TUFF;
        if (above > 1 && !exposed) return BiomicTerrainSampler.STONE;
        double band = Math.floor((y + 2.0D * ValueNoise2D.fractal(seed ^ BAND_SALT, x, z, 24.0D, 1, 0.5D)) / 4.0D);
        long roll = Math.floorMod(hash(seed, x, y, z), 100L);
        boolean dark = Math.floorMod((long) band, 3L) == 0L;
        return switch (kind) {
            case DRY -> dark
                ? pick(
                    roll,
                    45,
                    BiomicTerrainSampler.TERRACOTTA,
                    75,
                    BiomicTerrainSampler.BROWN_TERRACOTTA,
                    BiomicTerrainSampler.TUFF)
                : roll < 40 ? BiomicTerrainSampler.STONE
                    : roll < 60 ? BiomicTerrainSampler.SANDSTONE
                        : roll < 75 ? BiomicTerrainSampler.ANDESITE
                            : roll < 85 ? BiomicTerrainSampler.TUFF
                                : roll < 94 ? BiomicTerrainSampler.GRANITE : BiomicTerrainSampler.COBBLESTONE;
            case SNOWY -> dark
                ? pick(
                    roll,
                    50,
                    BiomicTerrainSampler.ANDESITE,
                    85,
                    BiomicTerrainSampler.TUFF,
                    BiomicTerrainSampler.DEEPSLATE)
                : roll < 45 ? BiomicTerrainSampler.STONE
                    : roll < 60 ? BiomicTerrainSampler.DIORITE
                        : roll < 72 ? BiomicTerrainSampler.CALCITE
                            : roll < 84 ? BiomicTerrainSampler.ANDESITE
                                : roll < 94 ? BiomicTerrainSampler.TUFF : BiomicTerrainSampler.COBBLESTONE;
            default -> dark
                ? pick(
                    roll,
                    50,
                    BiomicTerrainSampler.ANDESITE,
                    85,
                    BiomicTerrainSampler.TUFF,
                    BiomicTerrainSampler.DEEPSLATE)
                : roll < (kind == IslandKind.LUSH ? 33 : 41) ? BiomicTerrainSampler.STONE
                    : roll < 49 ? BiomicTerrainSampler.MOSSY_COBBLESTONE
                        : roll < 67 ? BiomicTerrainSampler.TUFF
                            : roll < 84 ? BiomicTerrainSampler.ANDESITE
                                : roll < 94 ? BiomicTerrainSampler.COBBLESTONE : BiomicTerrainSampler.DEEPSLATE;
        };
    }

    private static byte pick(long roll, int first, byte a, int second, byte b, byte c) {
        return roll < first ? a : roll < second ? b : c;
    }

    /** Whether a cell has sky beside it: a neighbouring column with no island at its height. */
    static boolean exposed(int y, int... neighbours) {
        for (int span : neighbours) if (span < 0 || y < span >>> 16 || y > (span & 0xFFFF)) return true;
        return false;
    }

    /** Whether a column hangs lower than all four beside it (neighbours without an island don't hold it up). */
    static boolean tip(int bottom, int... neighbours) {
        for (int span : neighbours) if (span >= 0 && span >>> 16 <= bottom) return false;
        return true;
    }

    private static long hash(long seed, int x, int y, int z) {
        return WorldgenContext
            .mix64(seed ^ SPECKLE_SALT ^ x * 0x9E3779B97F4A7C15L ^ y * 0xBF58476D1CE4E5B9L ^ z * 0xC2B2AE3D27D4EB4FL);
    }
}
