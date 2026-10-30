package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.WorldgenContext;

/** Whole authored sky clusters choose habitats; stored aliases retain the floating trait above mainland biomes. */
public final class HostedIslandBiomes {

    public static final String REDWOODS = "regions_unexplored:redwoods";
    public static final String MAPLE = "regions_unexplored:maple_forest";
    private static final String PREFIX = "biomic:hosted/";
    private static final String[] VARIANTS = { "regions_unexplored:wisteria_grove", "regions_unexplored:pine_taiga",
        "regions_unexplored:clover_plains", "regions_unexplored:autumnal_maple_forest",
        "regions_unexplored:silver_birch_forest", "minecraft:forest", "minecraft:taiga", "minecraft:plains",
        // Desert, swamp, rainforest and snowy islands: each kind of scattered structure has islands of its own (RU's
        // tundra is cold but dry, so it never snows there).
        "regions_unexplored:saguaro_desert", "regions_unexplored:bayou", "regions_unexplored:rainforest",
        "minecraft:snowy_plains" };
    private static final Map<String, String> MODERN_KEYS = new LinkedHashMap<>();
    private static final Map<String, String> STORED_KEYS = new LinkedHashMap<>();
    static {
        register(REDWOODS);
        register(MAPLE);
        for (String biome : VARIANTS) register(biome);
        for (String biome : new java.util.TreeSet<>(
            industries.angelic.biomic.compat.biome.BopNativeBiomes.supportedKeys())) register(biome);
    }

    private static void register(String habitat) {
        String stored = PREFIX + habitat.replace(':', '/');
        MODERN_KEYS.put(stored, habitat);
        STORED_KEYS.put(habitat, stored);
    }

    private HostedIslandBiomes() {}

    /**
     * Every habitat an island can store ({@link #storedKey}): those a sky cluster chooses on its own, and the land
     * habitats islands that follow the ground take from below (RegionalHabitatSampler#skyHabitat: vanilla's, Regions
     * Unexplored's, BOP's).
     */
    public static java.util.Set<String> islandHabitats() {
        java.util.Set<String> habitats = new java.util.LinkedHashSet<>(MODERN_KEYS.values());
        for (String habitat : RegionalHabitatSampler.landHabitats())
            if (habitat.startsWith("minecraft:") || habitat.startsWith("regions_unexplored:")
                || industries.angelic.biomic.compat.biome.BopNativeBiomes.handles(habitat)) habitats.add(habitat);
        return habitats;
    }

    /** Every habitat a sky cluster may choose on its own (modern keys), for the biome browser. */
    public static java.util.Collection<String> habitats() {
        return java.util.Collections.unmodifiableCollection(MODERN_KEYS.values());
    }

    public static String habitat(FloatingIslandArchetype archetype) {
        if (archetype == FloatingIslandArchetype.RING_BASIN) return REDWOODS;
        if (archetype == FloatingIslandArchetype.AEROPELAGO) return MAPLE;
        return null;
    }

    /** A cluster key already includes the world seed. No independent X/Z field can bisect one island. */
    public static String habitat(FloatingIslandArchetype archetype, long clusterKey) {
        String original = habitat(archetype);
        if (original == null) return null;
        long variant = WorldgenContext.mix64(clusterKey ^ 0x53C921D7A4B608EFL);
        if ((variant & 1L) == 0L) return original;
        return VARIANTS[(int) ((variant >>> 1) % VARIANTS.length)];
    }

    public static String habitat(FloatingIslandArchetype archetype, long clusterKey, java.util.List<String> nativeBop) {
        String original = habitat(archetype, clusterKey);
        if (original == null) return null;
        String selected = NativeBopHabitatSelector.select(0, clusterKey, -1, nativeBop);
        return selected == null ? original : selected;
    }

    /** Any island habitat: islands that follow the ground take the mainland's, beyond the registered few. */
    public static String storedKey(String habitat) {
        String stored = STORED_KEYS.get(habitat);
        if (stored != null) return stored;
        int colon = habitat.indexOf(':');
        if (colon <= 0 || habitat.indexOf('/') >= 0) throw new IllegalArgumentException(habitat);
        return PREFIX + habitat.replace(':', '/');
    }

    public static String modernKey(String stored) {
        String known = MODERN_KEYS.get(stored);
        if (known != null || stored == null || !stored.startsWith(PREFIX)) return known;
        String path = stored.substring(PREFIX.length());
        int slash = path.indexOf('/');
        return slash <= 0 || path.indexOf('/', slash + 1) >= 0 ? null
            : path.substring(0, slash) + ':' + path.substring(slash + 1);
    }

    /** Extend a sky surface's identity through its headroom; leave all lower Origen/cave cells intact. */
    /** Any hosted layout; the volume and the materials share its physical height. */
    public static BiomeVolume project(BiomeVolume original, byte[] materials, int chunkX, int chunkZ,
        HostedTerrainAccess terrain) {
        var layout = terrain.layout();
        if (original.getBlockHeight() != layout.height())
            throw new IllegalArgumentException("Volume/layout height mismatch");
        List<String> palette = new ArrayList<>(original.getPalette());
        short[] cells = original.copyPaletteIndices();
        // Every column of a quart, not only its centre: a quart at an island's edge whose centre misses the island
        // kept the ground's biome, and its island blocks took the ground's grass and leaf colours.
        for (int qz = 0; qz < 4; qz++)
            for (int qx = 0; qx < 4; qx++) for (int column16 = 0; column16 < 16; column16++) {
                int x = qx * 4 + (column16 & 3), z = qz * 4 + (column16 >> 2);
                int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
                byte[] column = new byte[layout.height()];
                for (int y = 0; y < column.length; y++) column[y] = materials[layout.index(x, y, z)];
                for (DecorationSurfaceColumn.Surface surface : DecorationSurfaceColumn
                    .scan(column, layout, y -> terrain.floatingArchetypeAt(wx, y, wz) != null)) {
                    if (!surface.floating) continue;
                    String habitat = terrain.floatingHabitatAt(wx, surface.floorY, wz);
                    if (habitat == null) continue;
                    String key = storedKey(habitat);
                    int index = palette.indexOf(key);
                    if (index < 0) {
                        index = palette.size();
                        palette.add(key);
                    }
                    for (int qy = 0; qy < original.getQuartHeight(); qy++) {
                        int y = qy * 4 + 2;
                        if (y + 1 >= surface.bottomY && y - 2 < surface.ceilingY)
                            cells[(qy * 4 + qz) * 4 + qx] = (short) index;
                    }
                }
            }
        return BiomeVolume.create(original.getGeneratorVersion(), original.getBlockHeight(), palette, cells);
    }

    public static BiomeVolume project(BiomeVolume original, byte[] materials, int chunkX, int chunkZ,
        BiomicTerrainSampler terrain) {
        List<String> palette = new ArrayList<>(original.getPalette());
        short[] cells = original.copyPaletteIndices();
        for (int qz = 0; qz < 4; qz++)
            for (int qx = 0; qx < 4; qx++) for (int column16 = 0; column16 < 16; column16++) {
                int x = qx * 4 + (column16 & 3), z = qz * 4 + (column16 >> 2);
                int wx = (chunkX << 4) + x, wz = (chunkZ << 4) + z;
                byte[] column = new byte[256];
                for (int y = 0; y < 256; y++) column[y] = materials[BiomicTerrainSampler.index(x, y, z)];
                for (DecorationSurfaceColumn.Surface surface : DecorationSurfaceColumn
                    .scan(column, y -> terrain.floatingArchetypeAt(wx, y, wz) != null)) {
                    if (!surface.floating) continue;
                    String habitat = terrain.floatingHabitatAt(wx, surface.floorY, wz);
                    if (habitat == null) continue;
                    String key = storedKey(habitat);
                    int index = palette.indexOf(key);
                    if (index < 0) {
                        index = palette.size();
                        palette.add(key);
                    }
                    for (int qy = 0; qy < 64; qy++) {
                        int y = qy * 4 + 2;
                        if (y + 1 >= surface.bottomY && y - 2 < surface.ceilingY)
                            cells[(qy * 4 + qz) * 4 + qx] = (short) index;
                    }
                }
            }
        return BiomeVolume.create(original.getGeneratorVersion(), palette, cells);
    }
}
