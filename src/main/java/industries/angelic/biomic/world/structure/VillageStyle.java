package industries.angelic.biomic.world.structure;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.ru.RuRuntimeManifest;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;
import industries.angelic.biomic.world.terrain.OrigenSurfaces;

/**
 * How a village looks: Village Names' village type (its building set) and material set, and the biome its layout is
 * built in. Chosen from what is at the village's own site, in its 3D biome (a sky village's is its island's habitat):
 * an Origen province's ground palette at the village's height and its trees, an RU habitat's surface, climate and
 * trees, or a native biome's top block, climate and dictionary types.
 */
record VillageStyle(String type, String material, BiomeGenBase biome) {

    enum Ground {
        SNOW,
        SAND,
        RED,
        MUD,
        MYCELIUM,
        SOIL
    }

    enum Wood {
        OAK,
        SPRUCE,
        BIRCH,
        JUNGLE,
        ACACIA,
        DARK_OAK
    }

    /**
     * What the site shows: {@code wood} is its commonest tree wood (null without trees), {@code forested} a real
     * forest.
     */
    record Evidence(Ground ground, float temperature, float downfall, Wood wood, boolean forested, boolean swamp,
        boolean jungle) {}

    boolean desert() {
        return "DESERT".equals(type);
    }

    static VillageStyle choose(BiomeGenBase biome, Evidence site) {
        Wood wood = site.wood();
        if (site.ground() == Ground.SNOW || site.temperature() < 0.2F)
            return !site.forested() ? new VillageStyle("SNOWY", "SNOW", biome)
                : new VillageStyle("TAIGA", wood == null || wood == Wood.OAK ? "SPRUCE" : wood.name(), biome);
        if (site.swamp() || site.ground() == Ground.MUD)
            return new VillageStyle("SWAMP", wood == Wood.DARK_OAK ? "DARK_OAK" : "OAK", biome);
        if (site.ground() == Ground.RED) return new VillageStyle("DESERT", "MESA", biome);
        if (site.ground() == Ground.SAND) return new VillageStyle("DESERT", "SAND", biome);
        if (site.jungle() || wood == Wood.JUNGLE && site.temperature() >= 0.9F)
            return new VillageStyle("JUNGLE", "JUNGLE", biome);
        if (wood == Wood.ACACIA || site.temperature() >= 1.0F && site.downfall() < 0.3F)
            return new VillageStyle("SAVANNA", "ACACIA", biome);
        if (wood == Wood.SPRUCE && site.forested()) return new VillageStyle("TAIGA", "SPRUCE", biome);
        if (site.ground() == Ground.MYCELIUM) return new VillageStyle("PLAINS", "MUSHROOM", biome);
        return new VillageStyle("PLAINS", wood == null || wood == Wood.SPRUCE ? "OAK" : wood.name(), biome);
    }

    /** Ground a biome's name promises where its sampled surface does not show it (badlands' wooded tops). */
    private static Ground namedGround(String name, Ground sampled) {
        if (sampled != Ground.SOIL) return sampled;
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("badlands") || n.contains("mesa")) return Ground.RED;
        if (n.contains("desert") || n.contains("sandy") || n.contains("dune")) return Ground.SAND;
        return sampled;
    }

    /** Village Names' biome mapping as this village sees it: its own biome, with this style. */
    Map<String, ArrayList<String>> villageNamesMapping() {
        Map<String, ArrayList<String>> mapping = new HashMap<>();
        mapping.put("BiomeNames", single(biome.biomeName));
        mapping.put("VillageTypes", single(type.toLowerCase(Locale.ROOT)));
        mapping.put("MaterialTypes", single(material.toLowerCase(Locale.ROOT)));
        mapping.put("DisallowModSubs", single(""));
        return mapping;
    }

    private static ArrayList<String> single(String value) {
        ArrayList<String> list = new ArrayList<>(1);
        list.add(value);
        return list;
    }

    /** The style of a village whose ground is at {@code groundY} in {@code biome}. */
    static VillageStyle at(WorldgenContext context, StructureTerrain terrain, BiomeGenBase biome, int x, int groundY,
        int z) {
        OrigenBiomeProfile profile = Origen.profile(biome);
        if (profile != null) return choose(biome, Origen.evidence(context, terrain, profile, biome, x, groundY, z));
        RuRuntimeManifest.BiomeEntry ru = Ru.entry(biome);
        return choose(biome, ru != null ? Ru.evidence(ru) : Native.evidence(biome));
    }

    /** The wood a tree, log or feature name points to, or null when it names none (dead trees, bushes). */
    static Wood wood(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("dead") || n.contains("bush")
            || n.contains("shrub")
            || n.contains("bioshroom")
            || n.contains("mushroom")) return null;
        if (n.contains("dark_oak") || n.contains("roofed")) return Wood.DARK_OAK;
        if (n.contains("birch")) return Wood.BIRCH;
        if (n.contains("spruce") || n.contains("pine")
            || n.contains("larch")
            || n.contains("redwood")
            || n.contains("boreal")
            || n.contains("blackwood")
            || n.contains("black_forest")
            || n.contains("evergreen")
            || n.contains("taiga")
            || n.contains("conifer")
            || n.contains("cedar")) return Wood.SPRUCE;
        if (n.contains("acacia") || n.contains("baobab")
            || n.contains("socotra")
            || n.contains("ashen")
            || n.contains("savanna")) return Wood.ACACIA;
        if (n.contains("jungle") || n.contains("kapok")
            || n.contains("palm")
            || n.contains("eucalyptus")
            || n.contains("mahogany")
            || n.contains("bamboo")
            || n.contains("mangrove")) return Wood.JUNGLE;
        if (n.contains("oak") || n.contains("maple")
            || n.contains("magnolia")
            || n.contains("alpha")
            || n.contains("wisteria")
            || n.contains("cherry")
            || n.contains("sakura")
            || n.contains("willow")
            || n.contains("cypress")
            || n.contains("apple")
            || n.contains("tree")) return Wood.OAK;
        return null;
    }

    private static boolean swampName(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("swamp") || n.contains(
            "mangrove") || n.contains("cypress") || n.contains("bayou") || n.contains("marsh") || n.contains("fen");
    }

    /** The commonest wood among weighted names, or null where there are none. */
    private static Wood dominant(Map<Wood, Integer> weights) {
        Wood best = null;
        for (var entry : weights.entrySet())
            if (best == null || entry.getValue() > weights.get(best)) best = entry.getKey();
        return best;
    }

    private static int total(Map<Wood, Integer> weights) {
        int total = 0;
        for (int weight : weights.values()) total += weight;
        return total;
    }

    static Ground ground(byte material) {
        if (material == BiomicTerrainSampler.SNOW || material == BiomicTerrainSampler.POWDER_SNOW
            || material == BiomicTerrainSampler.ICE
            || material == BiomicTerrainSampler.BLUE_ICE
            || material == BiomicTerrainSampler.PACKED_ICE) return Ground.SNOW;
        if (material == BiomicTerrainSampler.SAND || material == BiomicTerrainSampler.SANDSTONE
            || material == BiomicTerrainSampler.SMOOTH_SANDSTONE) return Ground.SAND;
        if (material == BiomicTerrainSampler.RED_SAND || material == BiomicTerrainSampler.RED_SANDSTONE
            || material == BiomicTerrainSampler.TERRACOTTA
            || material == BiomicTerrainSampler.RED_TERRACOTTA
            || material == BiomicTerrainSampler.ORANGE_TERRACOTTA
            || material == BiomicTerrainSampler.BROWN_TERRACOTTA
            || material == BiomicTerrainSampler.YELLOW_TERRACOTTA
            || material == BiomicTerrainSampler.WHITE_TERRACOTTA
            || material == BiomicTerrainSampler.LIGHT_GRAY_TERRACOTTA
            || material == BiomicTerrainSampler.PINK_TERRACOTTA) return Ground.RED;
        if (material == BiomicTerrainSampler.MUD || material == BiomicTerrainSampler.PACKED_MUD) return Ground.MUD;
        if (material == BiomicTerrainSampler.MYCELIUM) return Ground.MYCELIUM;
        return Ground.SOIL;
    }

    /** Origen provinces: the ground palette at the village's own height around its start, and the province's trees. */
    private static final class Origen {

        private static volatile Map<BiomeGenBase, OrigenBiomeProfile> profiles;

        static OrigenBiomeProfile profile(BiomeGenBase biome) {
            Map<BiomeGenBase, OrigenBiomeProfile> known = profiles;
            if (known == null) {
                known = new HashMap<>();
                for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
                    BiomeGenBase origen = BiomicBiomes.getOrigenBiome(profile);
                    if (origen != null) known.put(origen, profile);
                }
                profiles = known;
            }
            return known.get(biome);
        }

        static Evidence evidence(WorldgenContext context, StructureTerrain terrain, OrigenBiomeProfile profile,
            BiomeGenBase biome, int x, int groundY, int z) {
            int authoredY = terrain.authoredY(groundY);
            Map<Ground, Integer> grounds = new EnumMap<>(Ground.class);
            for (int dx = -16; dx <= 16; dx += 8) for (int dz = -16; dz <= 16; dz += 8) grounds
                .merge(ground(OrigenSurfaces.groundTop(context, profile, x + dx, authoredY, z + dz)), 1, Integer::sum);
            Ground ground = Ground.SOIL;
            for (var entry : grounds.entrySet())
                if (entry.getValue() > grounds.getOrDefault(ground, 0)) ground = entry.getKey();
            // Mixed palettes (grass with red sand) read as their sand when it is a large share.
            int sand = grounds.getOrDefault(Ground.SAND, 0), red = grounds.getOrDefault(Ground.RED, 0), samples = 25;
            if (ground == Ground.SOIL && (sand + red) * 10 >= samples * 3)
                ground = red >= sand ? Ground.RED : Ground.SAND;
            ground = namedGround(profile.getDisplayName(), ground);
            Map<Wood, Integer> woods = new EnumMap<>(Wood.class);
            boolean swamp = swampName(profile.name());
            for (String tree : OrigenSurfaces.treeFeatures(context, profile)) {
                Wood wood = wood(tree);
                if (wood != null) woods.merge(wood, 1, Integer::sum);
                swamp |= swampName(tree);
            }
            float temperature = switch (profile.getClimate()) {
                case COLD -> 0.05F;
                case TEMPERATE -> 0.7F;
                case WARM -> 1.1F;
            };
            return new Evidence(ground, temperature, biome.rainfall, dominant(woods), !woods.isEmpty(), swamp, false);
        }
    }

    /** RU habitats: their declared surface, climate and tags, and the logs of the trees they actually grow. */
    private static final class Ru {

        private static volatile Map<String, RuRuntimeManifest.BiomeEntry> entries;

        static RuRuntimeManifest.BiomeEntry entry(BiomeGenBase biome) {
            Map<String, RuRuntimeManifest.BiomeEntry> known = entries;
            if (known == null) {
                known = new HashMap<>();
                for (RuRuntimeManifest.BiomeEntry entry : RuRuntimeManifest.biomes()) known.put(entry.getKey(), entry);
                entries = known;
            }
            String key = BiomicBiomes.getKey(biome);
            return key == null ? null : known.get(key);
        }

        static Evidence evidence(RuRuntimeManifest.BiomeEntry entry) {
            Ground ground = switch (entry.getSurfaceStyle()) {
                case "SNOW" -> Ground.SNOW;
                case "SAND", "JOSHUA_DESERT" -> Ground.SAND;
                case "BADLANDS" -> Ground.RED;
                case "MUD", "PEAT", "SILT", "BAYOU", "OLD_GROWTH_BAYOU" -> Ground.MUD;
                default -> Ground.SOIL;
            };
            List<String> tags = entry.getTags();
            if (ground == Ground.SOIL && tags.contains("DESERT")) ground = Ground.SAND;
            Map<Wood, Integer> woods = new EnumMap<>(Wood.class);
            for (RuRuntimeManifest.DecorationEntry decoration : entry.getDecorations())
                for (RuRuntimeManifest.TerminalEntry terminal : decoration.getTerminals())
                    for (String state : terminal.getStates()) {
                        if (!state.contains("_log")) continue;
                        Wood wood = wood(state);
                        if (wood != null) woods.merge(wood, Math.max(1, decoration.getAttempts()), Integer::sum);
                    }
            // Trees per chunk: tundra's scattered few (4) are not a forest, a birch wood's 3 tall ones set its wood.
            return new Evidence(
                ground,
                entry.getTemperature(),
                entry.getDownfall(),
                dominant(woods),
                total(woods) >= 5,
                tags.contains("SWAMP"),
                tags.contains("JUNGLE"));
        }
    }

    /** Other mods' biomes: their top block, climate, dictionary types and name. */
    private static final class Native {

        static Evidence evidence(BiomeGenBase biome) {
            Ground ground = Ground.SOIL;
            if (biome.topBlock == Blocks.snow || biome.topBlock == Blocks.ice
                || biome.topBlock == Blocks.packed_ice
                || biome.getEnableSnow()) ground = Ground.SNOW;
            else if (biome.topBlock == Blocks.sand) ground = biome.field_150604_aj == 1 ? Ground.RED : Ground.SAND;
            else if (biome.topBlock == Blocks.hardened_clay || biome.topBlock == Blocks.stained_hardened_clay)
                ground = Ground.RED;
            else if (biome.topBlock == Blocks.mycelium) ground = Ground.MYCELIUM;
            else if (biome.topBlock.getUnlocalizedName()
                .toLowerCase(Locale.ROOT)
                .contains("mud")) ground = Ground.MUD;
            boolean swamp = false, jungle = false, forested = false;
            Map<Wood, Integer> woods = new EnumMap<>(Wood.class);
            for (BiomeDictionary.Type type : BiomeDictionary.getTypesForBiome(biome)) {
                switch (type) {
                    case SWAMP -> swamp = true;
                    case JUNGLE -> jungle = true;
                    case CONIFEROUS -> {
                        woods.merge(Wood.SPRUCE, 2, Integer::sum);
                        forested = true;
                    }
                    case SAVANNA -> woods.merge(Wood.ACACIA, 2, Integer::sum);
                    case FOREST, DENSE -> {
                        woods.merge(Wood.OAK, 1, Integer::sum);
                        forested = true;
                    }
                    case SANDY -> {
                        if (ground == Ground.SOIL) ground = Ground.SAND;
                    }
                    case MESA -> ground = Ground.RED;
                    case MUSHROOM -> ground = Ground.MYCELIUM;
                    default -> {}
                }
            }
            ground = namedGround(biome.biomeName, ground);
            Wood named = wood(biome.biomeName);
            if (named != null && named != Wood.OAK) woods.merge(named, 3, Integer::sum);
            swamp |= swampName(biome.biomeName);
            return new Evidence(ground, biome.temperature, biome.rainfall, dominant(woods), forested, swamp, jungle);
        }
    }
}
