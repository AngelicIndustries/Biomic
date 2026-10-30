package industries.angelic.biomic.world.terrain;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.WorldgenContext;

/** Assigns whole connected compatible landscape regions, never a second independently clipped biome noise map. */
public final class RegionalHabitatSampler {

    static final int GRID = 32;
    static final int MIN_CELLS = 24;
    static final int MAX_CELLS = 4096;
    private static final String ORIGEN = "";
    private final OrigenVerticalProfileSampler profiles;
    private final long seed;
    private final boolean saltyMarsh;
    private final java.util.List<String> bopHabitats;
    private final Map<Long, String> regions = new LinkedHashMap<Long, String>(8192, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, String> entry) {
            return size() > 65536;
        }
    };
    private final Map<Long, String> narrowRegions = new LinkedHashMap<Long, String>(8192, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, String> entry) {
            return size() > 65536;
        }
    };

    public RegionalHabitatSampler(WorldgenContext context) {
        profiles = new OrigenVerticalProfileSampler(context);
        seed = context.getSettings()
            .getWorldSeed();
        saltyMarsh = context.getNativeHabitats()
            .saltMarsh();
        bopHabitats = context.getNativeHabitats()
            .bopHabitats();
    }

    /** Column answers are pure; features and ore placement ask for the same columns many times. */
    private static final String ORIGEN_COLUMN = "\u0000origen";
    private final it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<String> columns = new it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap<>(
        65537);

    /** Null means Origen owns this column. Region labels are independent of traversal/chunk order. */
    public synchronized String sample(int x, int z) {
        long column = key(x, z);
        String known = columns.getAndMoveToLast(column);
        if (known != null) return known == ORIGEN_COLUMN ? null : known;
        String sampled = sampleUncached(x, z);
        columns.putAndMoveToLast(column, sampled == null ? ORIGEN_COLUMN : sampled);
        if (columns.size() > 65536) columns.removeFirst();
        return sampled;
    }

    private String sampleUncached(int x, int z) {
        int family = family(profiles.sampleSurface(x, z));
        if (family == 0) return null;
        // Rivers are long, narrow source landforms. A 32-block grid disconnects their channels
        // and wrongly classifies each surviving sample as a micro-biome. Keep this finer graph
        // separate so adding shores never relabels a reviewed inland province.
        int grid = grid(family);
        int gx = Math.floorDiv(x, grid), gz = Math.floorDiv(z, grid);
        // A narrow boundary sliver should inherit its nearest compatible component, not a square lattice cutout.
        int bestX = gx, bestZ = gz, distance = Integer.MAX_VALUE;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int cx = gx + dx, cz = gz + dz;
            int wx = cx * grid + grid / 2, wz = cz * grid + grid / 2;
            int d = (wx - x) * (wx - x) + (wz - z) * (wz - z);
            if (d < distance && familyAt(cx, cz, grid) == family) {
                distance = d;
                bestX = cx;
                bestZ = cz;
            }
        }
        if (distance == Integer.MAX_VALUE) return null;
        String selected = component(bestX, bestZ, family, grid);
        return selected.isEmpty() ? null : selected;
    }

    /**
     * The habitat a sky island over (x, z) takes when it follows the ground (IslandBiomeRules#ownBiomes off): the
     * ground's own where an island can host it, else one of the ground's kind: its family's pool (one pick per family
     * and world, so every island over that ground agrees), or the nearest hosted habitat of an Origen landform. Null
     * over water (beach, river, sea) and over ground nothing matches.
     */
    public synchronized String skyHabitat(int x, int z) {
        OrigenBiomeProfile profile = profiles.sampleSurface(x, z);
        int family = family(profile);
        if (family >= 33 || isMarineFoundation(profile)) return null;
        String ground = sample(x, z);
        if (ground != null && (ground.startsWith("minecraft:") || ground.startsWith("regions_unexplored:")
            || industries.angelic.biomic.compat.biome.BopNativeBiomes.handles(ground))) return ground;
        if (family == 0) return landform(profile);
        String[] pool = java.util.Arrays.stream(family >= 10 ? landmarkOptions(family) : firstWaves(family))
            .filter(habitat -> !habitat.isEmpty())
            .toArray(String[]::new);
        return pool.length == 0 ? null
            : pool[(int) Math.floorMod(WorldgenContext.mix64(seed ^ family * 0x2C1B3C6D5E4F7A8BL), pool.length)];
    }

    /** The ordinary families' habitats, first and second waves together. */
    static String[] firstWaves(int family) {
        return switch (family) {
            case 1 -> new String[] { "minecraft:forest", "regions_unexplored:maple_forest",
                "regions_unexplored:wisteria_grove", "regions_unexplored:autumnal_maple_forest",
                "regions_unexplored:orchard", "regions_unexplored:silver_birch_forest" };
            case 2 -> new String[] { "minecraft:plains", "regions_unexplored:clover_plains",
                "regions_unexplored:flower_fields", "regions_unexplored:poppy_fields", "regions_unexplored:grassland" };
            case 3 -> new String[] { "minecraft:desert", "regions_unexplored:joshua_desert",
                "regions_unexplored:dry_bushland", "regions_unexplored:saguaro_desert", "regions_unexplored:outback" };
            case 4 -> new String[] { "minecraft:taiga", "regions_unexplored:pine_taiga",
                "regions_unexplored:boreal_taiga", "regions_unexplored:blackwood_taiga",
                "regions_unexplored:old_growth_boreal_taiga" };
            default -> new String[0];
        };
    }

    /** The hosted habitat closest to an Origen landform that no family covers; null for water and caves. */
    static String landform(OrigenBiomeProfile profile) {
        return switch (profile) {
            case ARCTIC_ARCHES, SNOWY_MEADOW -> "minecraft:snowy_plains";
            case ICY_INCISIONS -> "regions_unexplored:icy_heights";
            case FERN_FISSURES -> "minecraft:forest";
            case SNOWY_SPIRES, SNOWY_MOUNTAINS -> "minecraft:snowy_slopes";
            case FROZEN_FUNGI -> "minecraft:snowy_taiga";
            case TUNDRA_TRACKS, COLD_STEPPE -> "regions_unexplored:tundra";
            case ALPINE_ASCENDANCY -> "minecraft:grove";
            case BARE_BOULDERFIELDS -> "minecraft:windswept_gravelly_hills";
            case CARVING_CREAKS, OVERGROWN_CLIFFS -> "minecraft:windswept_forest";
            case VINE_VAULT -> "minecraft:jungle";
            case INFERNO_ISLES -> "regions_unexplored:ashen_woodland";
            case MESA_MONUMENTS -> "minecraft:wooded_badlands";
            case TERRACOTTA_TOMBS -> "minecraft:eroded_badlands";
            case SANDY_SPLITS, DESERT_SPIKES_GOLD, OASIS, SALT_FLATS -> "minecraft:desert";
            case SAKURA_STREAMS -> "minecraft:cherry_grove";
            default -> null;
        };
    }

    OrigenBiomeProfile foundationAt(int x, int z) {
        return profiles.sampleSurface(x, z);
    }

    private String component(int x, int z, int family, int grid) {
        Map<Long, String> regions = grid == GRID ? this.regions : narrowRegions;
        long first = key(x, z);
        String known = regions.get(first);
        if (known != null) return known;
        ArrayDeque<Long> queue = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        queue.add(first);
        seen.add(first);
        long anchor = first;
        String selected = null;
        while (!queue.isEmpty()) {
            long next = queue.removeFirst();
            if (next < anchor) anchor = next;
            String cached = regions.get(next);
            if (cached != null) {
                selected = cached;
                break;
            }
            int cx = (int) (next >> 32), cz = (int) next;
            for (int[] offset : NEIGHBORS) {
                int nx = cx + offset[0], nz = cz + offset[1];
                long neighbor = key(nx, nz);
                if (!seen.contains(neighbor) && familyAt(nx, nz, grid) == family) {
                    seen.add(neighbor);
                    queue.add(neighbor);
                }
            }
            // Huge connected systems are left authored, rather than choosing a traversal-dependent partial region.
            if (seen.size() > MAX_CELLS) {
                selected = ORIGEN;
                break;
            }
        }
        // 6,144 square blocks minimum for naturally narrow channels; not little biome patches.
        if (selected == null)
            selected = seen.size() < (grid == GRID ? MIN_CELLS : 96) ? ORIGEN : choose(family, anchor, grid);
        for (long cell : seen) regions.put(cell, selected);
        return selected;
    }

    private static final int[][] NEIGHBORS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };

    private int familyAt(int x, int z, int grid) {
        return family(profiles.sampleSurface(x * grid + grid / 2, z * grid + grid / 2));
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    /** The habitat family of an Origen surface profile; 0 where Origen keeps its own biome. */
    public static int family(OrigenBiomeProfile profile) {
        switch (profile) {
            case FLOWERING_FOREST:
            case BLACK_FOREST:
                return 1;
            case PILLOW_PLAINS_INNER:
            case PILLOW_PLAINS_MIDDLE:
            case PILLOW_PLAINS_OUTER:
            case VERDANT_VALLEYS:
                return 2;
            case DESERT:
                return 3;
            case TALL_TIMBERLAND:
                return 4;
            // Keep these component IDs separate from the original four. Adding a neighboring
            // landmark must not merge/reseed a reviewed ordinary forest or plains province.
            case FOLIAGE_FORTRESS_INNER:
            case FOLIAGE_FORTRESS_MIDDLE:
            case FOLIAGE_FORTRESS_OUTER:
            case PLATEAO_RIVER_INNER:
            case PLATEAO_RIVER_MIDDLE:
                return 10;
            case GLOOMY_GORGE:
            case GLOOMY_GORGE_RIVER:
                return 11;
            case BADLANDS_BALCONIES:
                return 12;
            case LUSH_LOOPS:
            case LOOPS_RIVER:
                return 13;
            case ROCKY_REFUGE:
                return 14;
            case WHITE_WALLOWS:
                return 15;
            case SCARLET_SANCTUARY:
            case SCARLET_SANCTUARY_RIVER:
                return 16;
            case VERTICAL_VISTAS:
            case VISTAS_RIVER:
                return 17;
            case MARINE_MONOLITHS:
                return 18;
            case STONEGATE_SEAS:
                return 19;
            case SINKHOLE_FOREST:
            case SINKHOLE_OUTER:
                return 20;
            case SINKHOLE_JUNGLE:
                return 21;
            case SINKHOLE_FROZEN:
                return 22;
            case BAMBOO_BASIN:
                return 23;
            case MURKY_MARSHLANDS:
                return 24;
            case FOSSILIZED_FENLANDS:
                return 25;
            case MANGROVE_SWAMP:
                return 26;
            case CANOPY_CASCADES:
                return 27;
            case WATERY_WILDS:
                return 28;
            case SECLUDED_VALLEY:
            case SECLUDED_VALLEY_OUTER:
                return 29;
            case FROSTY_FINGERS:
                return 30;
            case MOUNTAIN_MIRRORS:
                return 31;
            case ARID_ARBORETUM:
                return 32;
            case BEACH:
            case PALM_BEACH:
                return 33;
            case SHALE_BEACH:
                return 34;
            case RIVER:
                return 35;
            case FROZEN_RIVER:
                return 36;
            case VERTICAL_JUNGLE_RIVER:
                return 37;
            case TROPICAL_OCEAN:
                return 38;
            case CORAL_OCEAN:
                return 39;
            case TEMPERATE_SEA_ARCHES:
                return 40;
            case DEEP_OCEAN:
            case TROPICAL_DEEP_OCEAN:
                return 41;
            case OCEAN:
                return 42;
            default:
                return 0;
        }
    }

    static boolean isLandmark(OrigenBiomeProfile profile) {
        return family(profile) >= 10;
    }

    /** The cell size of a family's provinces: rivers and shale shores are narrow, so theirs are finer. */
    public static int grid(int family) {
        return family >= 34 && family <= 37 ? 8 : GRID;
    }

    public static boolean isMarineFoundation(OrigenBiomeProfile profile) {
        return profile == OrigenBiomeProfile.MARINE_MONOLITHS || profile == OrigenBiomeProfile.STONEGATE_SEAS;
    }

    /** Distinct ecological pools, using only already replayed/registered habitats. Empty retains Origen. */
    static String[] landmarkOptions(int family) {
        return switch (family) {
            case 10 -> new String[] { ORIGEN, "regions_unexplored:maple_forest", "regions_unexplored:magnolia_woodland",
                "regions_unexplored:wisteria_grove", "regions_unexplored:silver_birch_forest", "minecraft:forest" };
            case 11 -> new String[] { ORIGEN, "regions_unexplored:redwoods", "regions_unexplored:blackwood_taiga",
                "regions_unexplored:willow_forest", "regions_unexplored:boreal_taiga" };
            case 12 -> new String[] { ORIGEN, "regions_unexplored:baobab_savanna", "regions_unexplored:outback",
                "regions_unexplored:joshua_desert", "regions_unexplored:saguaro_desert" };
            case 13, 21 -> new String[] { ORIGEN, "regions_unexplored:sparse_rainforest",
                "regions_unexplored:bamboo_forest", "regions_unexplored:eucalyptus_forest" };
            case 14 -> new String[] { ORIGEN, "regions_unexplored:sparse_redwoods", "regions_unexplored:maple_forest",
                "regions_unexplored:grassland", "regions_unexplored:windswept_maple_forest" };
            case 15 -> new String[] { ORIGEN, "regions_unexplored:magnolia_woodland",
                "regions_unexplored:wisteria_grove", "regions_unexplored:silver_birch_forest" };
            case 16 -> new String[] { ORIGEN, "regions_unexplored:autumnal_maple_forest",
                "regions_unexplored:maple_forest", "regions_unexplored:magnolia_woodland" };
            case 17 -> new String[] { ORIGEN, "regions_unexplored:highland_fields", "regions_unexplored:pine_slopes",
                "regions_unexplored:windswept_maple_forest", "regions_unexplored:clover_plains" };
            case 18, 19 -> new String[] { ORIGEN, "regions_unexplored:windswept_maple_forest",
                "regions_unexplored:silver_birch_forest", "regions_unexplored:grassland",
                "regions_unexplored:willow_forest" };
            case 20 -> new String[] { ORIGEN, "regions_unexplored:maple_forest", "regions_unexplored:pine_taiga",
                "regions_unexplored:silver_birch_forest", "minecraft:forest" };
            case 22 -> new String[] { ORIGEN, "regions_unexplored:pine_slopes",
                "regions_unexplored:old_growth_golden_boreal_taiga", "minecraft:taiga" };
            case 23 -> new String[] { "regions_unexplored:bamboo_forest" };
            case 24 -> new String[] { ORIGEN, "regions_unexplored:bayou", "regions_unexplored:old_growth_bayou",
                "regions_unexplored:fen", "regions_unexplored:marsh" };
            case 25 -> new String[] { ORIGEN, "regions_unexplored:fen", "regions_unexplored:fungal_fen" };
            case 26 -> new String[] { ORIGEN, "regions_unexplored:bayou", "regions_unexplored:old_growth_bayou",
                "regions_unexplored:marsh" };
            case 27 -> new String[] { ORIGEN, "regions_unexplored:rainforest" };
            case 28 -> new String[] { ORIGEN, "regions_unexplored:tropics" };
            case 29 -> new String[] { ORIGEN, "regions_unexplored:old_growth_forest" };
            case 30 -> new String[] { ORIGEN, "regions_unexplored:spires" };
            case 31 -> new String[] { ORIGEN, "regions_unexplored:icy_heights" };
            case 32 -> new String[] { ORIGEN, "regions_unexplored:ashen_woodland" };
            case 33 -> new String[] { ORIGEN, "regions_unexplored:grassy_beach" };
            case 34 -> new String[] { ORIGEN, "regions_unexplored:gravel_beach" };
            case 35 -> new String[] { ORIGEN, "regions_unexplored:muddy_river" };
            case 36 -> new String[] { ORIGEN, "regions_unexplored:cold_river" };
            case 37 -> new String[] { ORIGEN, "regions_unexplored:tropical_river" };
            case 38 -> new String[] { ORIGEN, "regions_unexplored:rocky_reef" };
            case 39 -> new String[] { ORIGEN, "regions_unexplored:hyacinth_deeps" };
            case 40 -> new String[] { ORIGEN, "regions_unexplored:chalk_cliffs" };
            default -> new String[0];
        };
    }

    /**
     * A province's habitat, kept within the host's climate (ClimateClasses.fits): a pick that does not fit is drawn
     * again, up to eight times, then the province stays Origen's.
     */
    private String choose(int family, long anchor, int grid) {
        String first = chooseUnfiltered(family, anchor);
        if (first.isEmpty()) return first;
        // The host and its climate at the province's anchor cell (cells of grid blocks).
        int x = (int) (anchor >> 32) * grid + grid / 2, z = (int) anchor * grid + grid / 2;
        OrigenBiomeProfile host = profiles.sampleSurface(x, z);
        int band = profiles.climateBand(x, z);
        if (ClimateClasses.fits(first, host, band)) return first;
        for (int i = 1; i <= 8; i++) {
            String next = chooseUnfiltered(family, WorldgenContext.mix64(anchor ^ 0x9E3779B97F4A7C15L * i));
            if (next.isEmpty() || ClimateClasses.fits(next, host, band)) return next;
        }
        return ORIGEN;
    }

    private String chooseUnfiltered(int family, long anchor) {
        String selected = chooseBase(family, anchor);
        String parent = selected;
        // The native Savanna child uses only previously unassigned warm acacia
        // provinces. Never relabel a reviewed RU biome as its vanilla parent.
        if (selected.isEmpty() && family == 32) parent = "minecraft:savanna";
        if (parent.startsWith("minecraft:")) {
            long hash = WorldgenContext.mix64(seed ^ anchor ^ family * 0x534255504152454eL);
            String child = industries.angelic.biomic.compat.biome.BopSubBiomePolicy
                .selectExternal(parent, hash, bopHabitats);
            if (!child.equals(parent)) return child;
        }
        return selected;
    }

    private String chooseBase(int family, long anchor) {
        String nativeBop = NativeBopHabitatSelector.select(seed, anchor, family, bopHabitats);
        if (nativeBop != null) return nativeBop;
        if (family == 41 || family == 42) return ORIGEN;
        if (family >= 10) {
            String[] pool = landmarkOptions(family);
            long hash = WorldgenContext.mix64(seed ^ anchor ^ family * 0x425A731EC9D068BFL);
            String selected = pool[(int) Math.floorMod(hash, pool.length)];
            if (saltyMarsh && family == 24 && selected.isEmpty())
                return industries.angelic.biomic.compat.biome.SaltMarshCompat.BIOME;
            // Fill only a previously Origen-owned cliff province, preserving the reviewed v21 habitats.
            if (family == 17 && selected.isEmpty() && Math.floorMod(WorldgenContext.mix64(hash), 3) != 0)
                return "regions_unexplored:towering_cliffs";
            return selected;
        }
        String[] options;
        switch (family) {
            case 1:
                options = new String[] { ORIGEN, "minecraft:forest", "regions_unexplored:maple_forest",
                    "regions_unexplored:wisteria_grove" };
                break;
            case 2:
                options = new String[] { ORIGEN, "minecraft:plains", "regions_unexplored:clover_plains" };
                break;
            case 3:
                options = new String[] { ORIGEN, "minecraft:desert", "regions_unexplored:joshua_desert" };
                break;
            case 4:
                options = new String[] { ORIGEN, "minecraft:taiga", "regions_unexplored:pine_taiga" };
                break;
            default:
                return ORIGEN;
        }
        long hash = WorldgenContext.mix64(seed ^ anchor ^ 0x78B135EFA09C2D61L);
        String original = options[(int) Math.floorMod(hash, options.length)];
        if (!original.isEmpty()) return original; // Preserve the first-wave habitats/coordinates.
        long expansion = WorldgenContext.mix64(hash ^ 0x6284A0D37CEBF519L);
        if (Math.floorMod(expansion, 3L) == 0L) return thirdWave(family, expansion);
        switch (family) {
            case 1:
                options = new String[] { "regions_unexplored:autumnal_maple_forest", "regions_unexplored:orchard",
                    "regions_unexplored:silver_birch_forest" };
                break;
            case 2:
                options = new String[] { "regions_unexplored:flower_fields", "regions_unexplored:poppy_fields",
                    "regions_unexplored:grassland" };
                break;
            case 3:
                options = new String[] { "regions_unexplored:dry_bushland", "regions_unexplored:saguaro_desert",
                    "regions_unexplored:outback" };
                break;
            case 4:
                options = new String[] { "regions_unexplored:boreal_taiga", "regions_unexplored:blackwood_taiga",
                    "regions_unexplored:old_growth_boreal_taiga" };
                break;
            default:
                return ORIGEN;
        }
        return options[(int) Math.floorMod(WorldgenContext.mix64(expansion), options.length)];
    }

    /** The third wave's cold taigas (family 4), Origen's first. */
    private static final String[] COLD_WAVE = { ORIGEN, "regions_unexplored:cold_boreal_taiga",
        "regions_unexplored:frozen_pine_taiga", "regions_unexplored:tundra" };

    /** The third wave's Regions Unexplored biomes per ordinary family, by name. */
    private static String[] thirdWaveOptions(int family) {
        return switch (family) {
            case 1 -> new String[] { "alpha_grove", "bamboo_forest", "eucalyptus_forest", "magnolia_woodland",
                "redwoods", "sparse_redwoods", "sparse_rainforest", "willow_forest", "windswept_maple_forest" };
            case 2 -> new String[] { "highland_fields", "prairie", "shrubland" };
            case 3 -> new String[] { "baobab_savanna" };
            case 4 -> new String[] { "old_growth_golden_boreal_taiga", "pine_slopes" };
            default -> new String[0];
        };
    }

    /** Only split previously retained ordinary regions; both earlier review tours stay stable. */
    private static String thirdWave(int family, long expansion) {
        long hash = WorldgenContext.mix64(expansion ^ 0x793C1BE820A5D64FL);
        if (Math.floorMod(hash, 3L) == 0L) {
            if (family != 4) return ORIGEN;
            return COLD_WAVE[(int) Math.floorMod(WorldgenContext.mix64(hash ^ 0x624EE854BF19231AL), COLD_WAVE.length)];
        }
        String[] options = thirdWaveOptions(family);
        return options.length == 0 ? ORIGEN
            : "regions_unexplored:" + options[(int) Math.floorMod(WorldgenContext.mix64(hash), options.length)];
    }

    /**
     * Every habitat a province of a land family (below 33: no beaches, rivers or seas) can take from this sampler's
     * tables, Origen's own left out, plus the habitats of Origen landforms no family covers ({@link #landform}): the
     * ground that sky islands follow. BOP's habitats come from its own selection (NativeBopHabitatSelector).
     */
    public static java.util.Set<String> landHabitats() {
        java.util.Set<String> habitats = new java.util.LinkedHashSet<>();
        for (int family = 1; family < 33; family++) {
            java.util.Collections.addAll(habitats, family <= 4 ? firstWaves(family) : landmarkOptions(family));
            for (String name : thirdWaveOptions(family)) habitats.add("regions_unexplored:" + name);
        }
        java.util.Collections.addAll(habitats, COLD_WAVE);
        habitats.add("regions_unexplored:towering_cliffs");
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
            String habitat = landform(profile);
            if (habitat != null) habitats.add(habitat);
        }
        habitats.remove(ORIGEN);
        return habitats;
    }

    /** Every habitat of this sampler's tables, water families too (beaches, rivers, seas), Origen's own left out. */
    public static java.util.Set<String> habitats() {
        java.util.Set<String> habitats = landHabitats();
        for (int family = 33; family <= 42; family++) java.util.Collections.addAll(habitats, landmarkOptions(family));
        habitats.remove(ORIGEN);
        return habitats;
    }
}
