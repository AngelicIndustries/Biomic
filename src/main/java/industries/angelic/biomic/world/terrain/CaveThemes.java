package industries.angelic.biomic.world.terrain;

import java.util.EnumMap;
import java.util.Map;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Which kind of cave a place belongs to (reviewed ecology proposal, "caves"), and the rock skins that make it one.
 *
 * <p>
 * Themes are regional: the underground is cut into warped regions about 48 blocks across and 24 high, each with one
 * theme chosen from the climate of the surface above it and its depth (hot zones deep down, frozen caves only under
 * cold land and above authored Y 0, dry caves under arid land, mossy ones under humid land, rooted ceilings under
 * forests, dripstone galleries in the middle band), so a whole cave system shares a look across chunks. The skins
 * are laid into the terrain on the workers before the chunk exists, in patches from a position-hashed value noise,
 * and only on natural rock touching cave air: moss on floors and lower walls, snow and ice,
 * sand drifts, basalt, dripstone and calcite, rooted dirt under the surface, clay around pools and magma beside lava.
 * Everything is a pure function of the seed and position.
 * </p>
 */
public final class CaveThemes {

    public enum Theme {
        BARE,
        DRIPSTONE,
        MOSSY,
        FUNGAL,
        FROZEN,
        HOT,
        DRY,
        ROOTED,
        SPIDER
    }

    private static final int REGION_XZ = 48, REGION_Y = 24, WARP = 14;

    private final long seed;
    private final OrigenDensityField origen;
    private final boolean shifted512;
    private final int height, minY;

    public CaveThemes(WorldgenContext context, boolean shifted512, int physicalHeight) {
        this.seed = context.getSettings()
            .getWorldSeed();
        this.origen = OrigenDensityField.shared(context);
        this.shifted512 = shifted512;
        this.height = physicalHeight;
        this.minY = shifted512 ? 69 : 5;
    }

    int authored(int physicalY) {
        return shifted512 ? physicalY - 128 : (int) Math.round(OrigenDensityField.toUpstreamY(physicalY));
    }

    /** The theme of the cave region holding this cell. */
    public Theme at(int x, int physicalY, int z) {
        int authoredY = authored(physicalY);
        double wx = x + WARP * signed(noise(x, z, 0x77617270L, 40.0D));
        double wz = z + WARP * signed(noise(x + 9871, z - 3313, 0x77617271L, 40.0D));
        int rx = (int) Math.floor(wx / REGION_XZ), rz = (int) Math.floor(wz / REGION_XZ);
        int ry = Math.floorDiv(authoredY, REGION_Y);
        return region(rx, ry, rz);
    }

    private final Map<Long, Theme> regions = new java.util.concurrent.ConcurrentHashMap<>();

    private Theme region(int rx, int ry, int rz) {
        long key = ((long) rx & 0x1FFFFFL) << 42 | ((long) rz & 0x1FFFFFL) << 21 | (ry & 0x1FFFFFL);
        Theme cached = regions.get(key);
        if (cached != null) return cached;
        if (regions.size() > 65536) regions.clear();
        Theme theme = choose(rx, ry, rz);
        regions.put(key, theme);
        return theme;
    }

    private Theme choose(int rx, int ry, int rz) {
        int cx = rx * REGION_XZ + REGION_XZ / 2, cz = rz * REGION_XZ + REGION_XZ / 2;
        int authoredY = ry * REGION_Y + REGION_Y / 2;
        OrigenBiomeProfile surface = origen.sampleSurfaceProfile(cx, cz);
        String name = surface.name();
        boolean cold = surface.getClimate() == OrigenBiomeProfile.Climate.COLD;
        boolean arid = name.matches(".*(DESERT|MESA|BADLANDS|BALCON|SANDY|SALT|TERRACOTTA|ARID|CARVING|INFERNO).*");
        boolean humid = name.matches(
            ".*(SWAMP|MARSH|LUSH|JUNGLE|CANOPY|VINE|WATERY|MANGROVE|FEN|LOOPS|OVERGROWN|GLOOMY|VERDANT|BAMBOO).*");
        boolean forest = name
            .matches(".*(FOREST|TIMBER|SAKURA|SCARLET|BLACK|SECLUDED|CANOPY|FOLIAGE|JUNGLE|WALLOWS).*");
        Map<Theme, Integer> weights = new EnumMap<>(Theme.class);
        weights.put(Theme.BARE, 4);
        if (authoredY < -36) {
            weights.put(Theme.HOT, 4);
            weights.put(Theme.FUNGAL, 2);
            weights.put(Theme.DRIPSTONE, 1);
        } else if (authoredY <= 40) {
            weights.put(Theme.DRIPSTONE, !arid && !cold ? 3 : 1);
            if (humid && !cold) weights.put(Theme.MOSSY, 4);
            else if (!cold && !arid) weights.put(Theme.MOSSY, 1);
            if (!cold) weights.put(Theme.FUNGAL, humid || forest ? 3 : 1);
            if (cold && authoredY > 0) weights.put(Theme.FROZEN, 5);
            if (arid) weights.put(Theme.DRY, 5);
            weights.put(Theme.SPIDER, 1);
        } else {
            if (humid && !cold) weights.put(Theme.MOSSY, 4);
            else if (!cold && !arid) weights.put(Theme.MOSSY, 2);
            if (forest && !arid) weights.put(Theme.ROOTED, 4);
            if (cold) weights.put(Theme.FROZEN, 6);
            if (arid) weights.put(Theme.DRY, 5);
            if (!cold && !arid) weights.put(Theme.FUNGAL, 1);
            weights.put(Theme.DRIPSTONE, 1);
        }
        int total = 0;
        for (int w : weights.values()) total += w;
        long h = WorldgenContext.mix64(
            seed ^ rx * 0x9E3779B97F4A7C15L
                ^ WorldgenContext.mix64(rz * 0xC2B2AE3D27D4EB4FL ^ ry * 0x165667B19E3779F9L ^ 0x546865L));
        int roll = (int) Math.floorMod(h, (long) total);
        for (Map.Entry<Theme, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) return entry.getKey();
        }
        return Theme.BARE;
    }

    /**
     * Lays the cave skins into one chunk's terrain (physical layout, X/Z-major columns of {@code height} rows).
     * {@code floating}: cells of sky islands (never cave). {@code authored}/{@code authoredOffset}: the shifted512
     * authored companion to keep in step (row = physical Y - offset), or null.
     */
    public void skin(int chunkX, int chunkZ, byte[] materials, BiomeVolume volume,
        java.util.function.IntPredicate floating, byte[] authored, int authoredHeight, int authoredOffset) {
        int[] top = new int[256];
        for (int column = 0; column < 256; column++) {
            int base = column * height, y = height - 1;
            while (y > 0 && (materials[base + y] == BiomicTerrainSampler.AIR || floating.test(base + y))) y--;
            top[column] = y;
        }
        for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
            int column = lx * 16 + lz, base = column * height;
            int x = (chunkX << 4) + lx, z = (chunkZ << 4) + lz;
            for (int y = minY; y < top[column] - 1; y++) {
                byte cell = materials[base + y];
                if (cell != BiomicTerrainSampler.AIR && cell != BiomicTerrainSampler.WATER
                    && cell != BiomicTerrainSampler.LAVA) continue;
                if (volume != null && volume.containsBlockY(y)
                    && !BiomeVolume.SURFACE_KEY.equals(volume.getBiomeKey(lx >> 2, y >> 2, lz >> 2))) continue;
                Theme theme = at(x, y, z);
                if (cell == BiomicTerrainSampler.WATER) {
                    // Pools: clay under and around still water, except where it is frozen or hot.
                    if (theme == Theme.FROZEN || theme == Theme.HOT) continue;
                    if (unit(x, y, z, 0x436C6179L) < 0.45D) {
                        swap(
                            materials,
                            authored,
                            authoredHeight,
                            authoredOffset,
                            lx,
                            y - 1,
                            lz,
                            BiomicTerrainSampler.CLAY);
                    }
                    continue;
                }
                if (cell == BiomicTerrainSampler.LAVA) {
                    if (theme == Theme.FROZEN) continue;
                    int[][] sides = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
                    for (int[] d : sides) {
                        int nx = lx + d[0], nz = lz + d[1];
                        if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                        if (unit(x + d[0], y, z + d[1], 0x4D61676DL) < 0.4D) swap(
                            materials,
                            authored,
                            authoredHeight,
                            authoredOffset,
                            nx,
                            y,
                            nz,
                            BiomicTerrainSampler.MAGMA);
                    }
                    if (unit(x, y, z, 0x4D616742L) < 0.4D) swap(
                        materials,
                        authored,
                        authoredHeight,
                        authoredOffset,
                        lx,
                        y - 1,
                        lz,
                        BiomicTerrainSampler.MAGMA);
                    continue;
                }
                skinAir(theme, materials, authored, authoredHeight, authoredOffset, lx, y, lz, x, z, top[column] - y);
            }
        }
    }

    private void skinAir(Theme theme, byte[] materials, byte[] authored, int authoredHeight, int authoredOffset, int lx,
        int y, int lz, int x, int z, int cover) {
        int base = (lx * 16 + lz) * height;
        boolean floor = rock(materials[base + y - 1]);
        boolean ceiling = rock(materials[base + y + 1]);
        boolean lowWall = floor || rock(materials[base + y - 2]) && materials[base + y - 1] == BiomicTerrainSampler.AIR;
        double patch = noise(x, z, theme.ordinal() * 0x51ED27L + y / 6, 5.0D);
        switch (theme) {
            case MOSSY -> {
                if (floor && patch < 0.62D)
                    swap(materials, authored, authoredHeight, authoredOffset, lx, y - 1, lz, BiomicTerrainSampler.MOSS);
                if (lowWall && patch < 0.45D)
                    walls(materials, authored, authoredHeight, authoredOffset, lx, y, lz, BiomicTerrainSampler.MOSS);
                if (ceiling && patch < 0.18D)
                    swap(materials, authored, authoredHeight, authoredOffset, lx, y + 1, lz, BiomicTerrainSampler.MOSS);
            }
            case FROZEN -> {
                if (floor && patch < 0.45D) swap(
                    materials,
                    authored,
                    authoredHeight,
                    authoredOffset,
                    lx,
                    y - 1,
                    lz,
                    patch < 0.15D ? BiomicTerrainSampler.PACKED_ICE : BiomicTerrainSampler.SNOW);
                if (ceiling && patch < 0.2D) swap(
                    materials,
                    authored,
                    authoredHeight,
                    authoredOffset,
                    lx,
                    y + 1,
                    lz,
                    BiomicTerrainSampler.PACKED_ICE);
            }
            case HOT -> {
                if (floor && patch < 0.35D) swap(
                    materials,
                    authored,
                    authoredHeight,
                    authoredOffset,
                    lx,
                    y - 1,
                    lz,
                    patch < 0.1D ? BiomicTerrainSampler.SMOOTH_BASALT : BiomicTerrainSampler.BASALT);
            }
            case DRY -> {
                if (floor && patch < 0.5D)
                    swap(materials, authored, authoredHeight, authoredOffset, lx, y - 1, lz, BiomicTerrainSampler.SAND);
            }
            case DRIPSTONE -> {
                if (floor && patch < 0.3D) swap(
                    materials,
                    authored,
                    authoredHeight,
                    authoredOffset,
                    lx,
                    y - 1,
                    lz,
                    BiomicTerrainSampler.DRIPSTONE_BLOCK);
                if (ceiling && patch < 0.4D) swap(
                    materials,
                    authored,
                    authoredHeight,
                    authoredOffset,
                    lx,
                    y + 1,
                    lz,
                    BiomicTerrainSampler.DRIPSTONE_BLOCK);
                if (!floor && !ceiling && unit(x, y, z, 0x43616C63L) < 0.12D)
                    walls(materials, authored, authoredHeight, authoredOffset, lx, y, lz, BiomicTerrainSampler.CALCITE);
            }
            case ROOTED -> {
                if (ceiling && cover <= 10 && patch < 0.45D) swap(
                    materials,
                    authored,
                    authoredHeight,
                    authoredOffset,
                    lx,
                    y + 1,
                    lz,
                    BiomicTerrainSampler.ROOTED_DIRT);
            }
            case BARE, SPIDER -> {
                if (floor && patch < 0.06D) swap(
                    materials,
                    authored,
                    authoredHeight,
                    authoredOffset,
                    lx,
                    y - 1,
                    lz,
                    BiomicTerrainSampler.GRAVEL);
            }
        }
    }

    private void walls(byte[] materials, byte[] authored, int authoredHeight, int authoredOffset, int lx, int y, int lz,
        byte skin) {
        if (lx > 0) swap(materials, authored, authoredHeight, authoredOffset, lx - 1, y, lz, skin);
        if (lx < 15) swap(materials, authored, authoredHeight, authoredOffset, lx + 1, y, lz, skin);
        if (lz > 0) swap(materials, authored, authoredHeight, authoredOffset, lx, y, lz - 1, skin);
        if (lz < 15) swap(materials, authored, authoredHeight, authoredOffset, lx, y, lz + 1, skin);
    }

    /** Natural rock at (lx, y, lz) becomes the skin, in the physical terrain and its authored companion. */
    private void swap(byte[] materials, byte[] authored, int authoredHeight, int authoredOffset, int lx, int y, int lz,
        byte skin) {
        if (y < 1 || y >= height) return;
        int index = (lx * 16 + lz) * height + y;
        if (!rock(materials[index])) return;
        materials[index] = skin;
        if (authored != null) {
            int row = y - authoredOffset;
            if (row >= 0 && row < authoredHeight) {
                int a = (lx * 16 + lz) * authoredHeight + row;
                if (rock(authored[a])) authored[a] = skin;
            }
        }
    }

    private static final boolean[] SKINS = new boolean[256];
    static {
        for (byte skin : new byte[] { BiomicTerrainSampler.MOSS, BiomicTerrainSampler.ICE,
            BiomicTerrainSampler.PACKED_ICE, BiomicTerrainSampler.SNOW, BiomicTerrainSampler.BASALT,
            BiomicTerrainSampler.SMOOTH_BASALT, BiomicTerrainSampler.SAND, BiomicTerrainSampler.DRIPSTONE_BLOCK,
            BiomicTerrainSampler.CALCITE, BiomicTerrainSampler.ROOTED_DIRT, BiomicTerrainSampler.GRAVEL,
            BiomicTerrainSampler.CLAY, BiomicTerrainSampler.MAGMA }) SKINS[skin & 255] = true;
    }

    /** Whether a change from {@code before} to {@code after} is a cave skin: natural rock become a skin material. */
    static boolean isSkin(byte before, byte after) {
        return rock(before) && SKINS[after & 255];
    }

    static boolean rock(byte material) {
        return material == BiomicTerrainSampler.STONE || material == BiomicTerrainSampler.DEEPSLATE
            || material == BiomicTerrainSampler.TUFF
            || material == BiomicTerrainSampler.GRANITE
            || material == BiomicTerrainSampler.DIORITE
            || material == BiomicTerrainSampler.ANDESITE;
    }

    /** Smooth 0..1 value noise over the horizontal plane (bilinear between hashed lattice points). */
    double noise(int x, int z, long salt, double scale) {
        double fx = x / scale, fz = z / scale;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double a = lattice(x0, z0, salt), b = lattice(x0 + 1, z0, salt), c = lattice(x0, z0 + 1, salt),
            d = lattice(x0 + 1, z0 + 1, salt);
        return (a + (b - a) * tx) * (1 - tz) + (c + (d - c) * tx) * tz;
    }

    private double lattice(int x, int z, long salt) {
        return (WorldgenContext.mix64(seed ^ salt ^ x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL) >>> 11)
            * 0x1.0p-53;
    }

    double unit(int x, int y, int z, long salt) {
        return (WorldgenContext.mix64(
            seed ^ salt
                ^ x * 0x9E3779B97F4A7C15L
                ^ WorldgenContext.mix64(z * 0xC2B2AE3D27D4EB4FL ^ y * 0x165667B19E3779F9L))
            >>> 11) * 0x1.0p-53;
    }

    private static double signed(double unit) {
        return unit * 2 - 1;
    }
}
