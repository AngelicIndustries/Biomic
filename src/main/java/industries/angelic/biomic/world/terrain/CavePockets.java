package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * Solid rock pockets (review, 2026-10-24: "not having the entire map be swiss cheese, but leave some solid spots where
 * a
 * mineshaft can easily fit"). In seven 384-block regions in ten, a rounded, squarish block of rock about 216 blocks
 * across and up to 72 tall stands at mineshaft depth. Origen's caves wholly inside it are filled (one reaching out of
 * it,
 * into a sinkhole or the sky, stays open), and the expansion's tunnels and caverns keep out of it and their guard
 * distance from it. A mineshaft starts at a pocket's centre and, with vanilla's reach of 80 blocks, stays inside;
 * other mods' buried structures find solid rock there too.
 * Heights are authored (Origen's frame); every answer is a pure function of the seed and the column.
 */
public final class CavePockets {

    public static final int REGION = 384;
    private static final double CHANCE = 0.7D;
    /** Half the side of the squarish outline, how far the outline wanders, and the half height at the centre. */
    private static final int RADIUS = 108, RADIUS_NOISE = 8, HALF_HEIGHT = 36;
    /** The authored height of a pocket's middle (physical 142 at 512: mineshaft depth, well under the sea). */
    private static final int CENTER_Y = 14, CENTER_Y_JITTER = 6;
    private static final long SALT = 0x3A8F05C5B9D2E417L, OUTLINE_SALT = 0x6C8E9CF570932BD5L;
    private static final int PACK = 1024;

    /** A pocket's centre; {@code y} is authored. */
    public record Pocket(int x, int y, int z) {}

    private final long seed;

    public CavePockets(long seed) {
        this.seed = seed;
    }

    /** The pocket of a region, or null where the region has none. */
    public Pocket at(int regionX, int regionZ) {
        long hash = WorldgenContext.mix64(seed ^ SALT ^ regionX * 0x9E3779B97F4A7C15L ^ regionZ * 0xC2B2AE3D27D4EB4FL);
        if ((hash >>> 11) * 0x1.0p-53 >= CHANCE) return null;
        // Centres stay far enough inside their region that a pocket never reaches into the next one.
        int spread = REGION / 2 - RADIUS - RADIUS_NOISE - 4;
        long jitter = WorldgenContext.mix64(hash);
        int x = regionX * REGION + REGION / 2 + (int) Math.floorMod(jitter, 2L * spread + 1) - spread;
        int z = regionZ * REGION + REGION / 2 + (int) Math.floorMod(jitter >>> 20, 2L * spread + 1) - spread;
        int y = CENTER_Y + (int) Math.floorMod(jitter >>> 40, 2L * CENTER_Y_JITTER + 1) - CENTER_Y_JITTER;
        return new Pocket(x, y, z);
    }

    /** The pocket a column lies in, or null. */
    public Pocket containing(int x, int z) {
        return at(Math.floorDiv(x, REGION), Math.floorDiv(z, REGION));
    }

    /**
     * The authored heights a column's pocket spans, packed (see {@link #low}, {@link #high}), or -1 outside every
     * pocket. The outline is a superellipse (squarish, so a mineshaft's corners fit) with a wandering edge; the pocket
     * is tallest at its centre and rounds off towards its rim.
     */
    public int range(int x, int z) {
        Pocket pocket = containing(x, z);
        if (pocket == null) return -1;
        double radius = RADIUS + RADIUS_NOISE * ValueNoise2D.fractal(seed ^ OUTLINE_SALT, x, z, 40, 2, .5);
        double dx = Math.abs(x - pocket.x) / radius, dz = Math.abs(z - pocket.z) / radius;
        double outline = dx * dx * dx * dx + dz * dz * dz * dz;
        if (outline >= 1.0D) return -1;
        int half = (int) Math.round(HALF_HEIGHT * Math.sqrt(Math.sqrt(1.0D - outline)));
        if (half < 2) return -1;
        return (pocket.y - half + PACK) << 16 | (pocket.y + half + PACK);
    }

    public static int low(int range) {
        return (range >>> 16) - PACK;
    }

    public static int high(int range) {
        return (range & 0xFFFF) - PACK;
    }
}
