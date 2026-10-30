package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenFlowers;
import net.minecraft.world.gen.feature.WorldGenMelon;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenShrub;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTallGrass;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenWaterlily;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.BiomeManager;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.compat.flora.NativeFeatureCapture;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;

/** Optional native BOP 2.1.0.2308 surface adapters. Never invokes the full decorator, ores, caves or structures. */
public final class BopNativeBiomes {

    public static final String GROVE = "biomesoplenty:grove", CHERRY = "biomesoplenty:cherry_blossom_grove",
        LAVENDER = "biomesoplenty:lavender_fields", DECIDUOUS = "biomesoplenty:deciduous_forest",
        PRAIRIE = "biomesoplenty:prairie", FLOWER_FIELD = "biomesoplenty:flower_field",
        WOODLAND = "biomesoplenty:woodland", HEATHLAND = "biomesoplenty:heathland",
        SHRUBLAND = "biomesoplenty:shrubland", BOREAL = "biomesoplenty:boreal_forest",
        MAPLE = "biomesoplenty:maple_woods", SEASONAL = "biomesoplenty:seasonal_forest",
        THICKET = "biomesoplenty:thicket", CONIFEROUS = "biomesoplenty:coniferous_forest",
        SNOWY_CONIFEROUS = "biomesoplenty:snowy_coniferous_forest", FROST = "biomesoplenty:frost_forest",
        MEADOW = "biomesoplenty:meadow", TEMPERATE_RAINFOREST = "biomesoplenty:temperate_rainforest",
        STEPPE = "biomesoplenty:steppe", DENSE_FOREST = "biomesoplenty:dense_forest", SHIELD = "biomesoplenty:shield",
        HIGHLAND = "biomesoplenty:highland", TUNDRA = "biomesoplenty:tundra", ARCTIC = "biomesoplenty:arctic",
        ALPS = "biomesoplenty:alps", BRUSHLAND = "biomesoplenty:brushland", CHAPARRAL = "biomesoplenty:chaparral",
        GRASSLAND = "biomesoplenty:grassland", ALPS_FOREST = "biomesoplenty:alps_forest",
        MEADOW_FOREST = "biomesoplenty:meadow_forest", SEASONAL_CLEARING = "biomesoplenty:seasonal_forest_clearing",
        MOUNTAIN = "biomesoplenty:mountain", GLACIER = "biomesoplenty:glacier", CRAG = "biomesoplenty:crag",
        EUCALYPTUS = "biomesoplenty:eucalyptus_forest", RAINFOREST = "biomesoplenty:rainforest",
        TROPICAL_RAINFOREST = "biomesoplenty:tropical_rainforest", BAMBOO_FOREST = "biomesoplenty:bamboo_forest",
        REDWOOD = "biomesoplenty:redwood_forest", JADE_CLIFFS = "biomesoplenty:jade_cliffs",
        DEAD_FOREST = "biomesoplenty:dead_forest", OMINOUS_WOODS = "biomesoplenty:ominous_woods",
        FUNGI_FOREST = "biomesoplenty:fungi_forest", MYSTIC_GROVE = "biomesoplenty:mystic_grove",
        GARDEN = "biomesoplenty:garden";
    public static final String LONG_GRASS = "biomesoplenty:longgrass[legacy_meta=0]";
    public static final String ORIGIN_VALLEY = "biomesoplenty:origin_valley",
        ORIGIN_GRASS = "biomesoplenty:origingrass[legacy_meta=0]";
    public static final String OUTBACK = "biomesoplenty:outback", XERIC_SHRUBLAND = "biomesoplenty:xeric_shrubland",
        HARD_SAND = "biomesoplenty:hardsand[legacy_meta=0]";
    public static final String LUSH_DESERT = "biomesoplenty:lush_desert";
    public static final String MARSH = "biomesoplenty:marsh", MUD = "biomesoplenty:mud[legacy_meta=0]";
    public static final String DEAD_SWAMP = "biomesoplenty:dead_swamp";
    public static final String MOOR = "biomesoplenty:moor";
    public static final String FEN = "biomesoplenty:fen";
    public static final String BOG = "biomesoplenty:bog";
    public static final String LUSH_SWAMP = "biomesoplenty:lush_swamp";
    public static final String BAYOU = "biomesoplenty:bayou";
    public static final String WETLAND = "biomesoplenty:wetland";
    public static final String SLUDGEPIT = "biomesoplenty:sludgepit";
    public static final String QUAGMIRE = "biomesoplenty:quagmire", SILKGLADES = "biomesoplenty:silkglades";
    public static final String CANYON = "biomesoplenty:canyon", CANYON_RAVINE = "biomesoplenty:canyon_ravine",
        HARD_DIRT = "biomesoplenty:harddirt[legacy_meta=0]";
    public static final String LAND_OF_LAKES = "biomesoplenty:land_of_lakes",
        LAND_OF_LAKES_MARSH = "biomesoplenty:land_of_lakes_marsh";
    public static final String LOAMY_GRASS = "biomesoplenty:newbopgrass[legacy_meta=0]",
        LOAMY_DIRT = "biomesoplenty:newbopdirt[legacy_meta=0]", SEAWEED = "biomesoplenty:coral2[legacy_meta=8]",
        HARD_ICE = "biomesoplenty:hardice[legacy_meta=0]", CRAG_ROCK = "biomesoplenty:cragrock[legacy_meta=0]";
    public static final String SACRED_SPRINGS = "biomesoplenty:sacred_springs", WASTELAND = "biomesoplenty:wasteland",
        ORCHARD = "biomesoplenty:orchard", OASIS = "biomesoplenty:oasis", SCRUBLAND = "biomesoplenty:scrubland",
        SPRUCE_WOODS = "biomesoplenty:spruce_woods", CORAL_REEF = "biomesoplenty:coral_reef",
        KELP_FOREST = "biomesoplenty:kelp_forest", MANGROVE = "biomesoplenty:mangrove",
        TROPICS = "biomesoplenty:tropics", VOLCANO = "biomesoplenty:volcano",
        DRIED_DIRT = "biomesoplenty:drieddirt[legacy_meta=0]", ASH_STONE = "biomesoplenty:ashstone[legacy_meta=0]";
    private static final Map<String, String> CLASSES = Map.ofEntries(
        Map.entry(ORIGIN_VALLEY, "BiomeGenOriginValley"),
        Map.entry(OUTBACK, "BiomeGenOutback"),
        Map.entry(XERIC_SHRUBLAND, "BiomeGenXericShrubland"),
        Map.entry(LUSH_DESERT, "BiomeGenLushDesert"),
        Map.entry(MARSH, "BiomeGenMarsh"),
        Map.entry(DEAD_SWAMP, "BiomeGenDeadSwamp"),
        Map.entry(MOOR, "BiomeGenMoor"),
        Map.entry(FEN, "BiomeGenFen"),
        Map.entry(BOG, "BiomeGenBog"),
        Map.entry(LUSH_SWAMP, "BiomeGenLushSwamp"),
        Map.entry(BAYOU, "BiomeGenBayou"),
        Map.entry(WETLAND, "BiomeGenWetland"),
        Map.entry(SLUDGEPIT, "BiomeGenSludgepit"),
        Map.entry(QUAGMIRE, "BiomeGenQuagmire"),
        Map.entry(SILKGLADES, "BiomeGenSilkglades"),
        Map.entry(CANYON, "BiomeGenCanyon"),
        Map.entry(CANYON_RAVINE, "BiomeGenCanyonRavine"),
        Map.entry(LAND_OF_LAKES, "BiomeGenLandOfLakes"),
        Map.entry(LAND_OF_LAKES_MARSH, "BiomeGenLandOfLakesMarsh"),
        Map.entry(GROVE, "BiomeGenGrove"),
        Map.entry(CHERRY, "BiomeGenCherryBlossomGrove"),
        Map.entry(LAVENDER, "BiomeGenLavenderFields"),
        Map.entry(DECIDUOUS, "BiomeGenDeciduousForest"),
        Map.entry(PRAIRIE, "BiomeGenPrairie"),
        Map.entry(FLOWER_FIELD, "BiomeGenFlowerField"),
        Map.entry(WOODLAND, "BiomeGenWoodland"),
        Map.entry(HEATHLAND, "BiomeGenHeathland"),
        Map.entry(SHRUBLAND, "BiomeGenShrubland"),
        Map.entry(BOREAL, "BiomeGenBorealForest"),
        Map.entry(MAPLE, "BiomeGenMapleWoods"),
        Map.entry(SEASONAL, "BiomeGenSeasonalForest"),
        Map.entry(THICKET, "BiomeGenThicket"),
        Map.entry(CONIFEROUS, "BiomeGenConiferousForest"),
        Map.entry(SNOWY_CONIFEROUS, "BiomeGenConiferousForestSnow"),
        Map.entry(FROST, "BiomeGenFrostForest"),
        Map.entry(MEADOW, "BiomeGenMeadow"),
        Map.entry(TEMPERATE_RAINFOREST, "BiomeGenTemperateRainforest"),
        Map.entry(STEPPE, "BiomeGenSteppe"),
        Map.entry(DENSE_FOREST, "BiomeGenDenseForest"),
        Map.entry(SHIELD, "BiomeGenShield"),
        Map.entry(HIGHLAND, "BiomeGenHighland"),
        Map.entry(TUNDRA, "BiomeGenTundra"),
        Map.entry(ARCTIC, "BiomeGenArctic"),
        Map.entry(ALPS, "BiomeGenAlps"),
        Map.entry(BRUSHLAND, "BiomeGenBrushland"),
        Map.entry(CHAPARRAL, "BiomeGenChaparral"),
        Map.entry(GRASSLAND, "BiomeGenGrassland"),
        Map.entry(ALPS_FOREST, "BiomeGenAlpsForest"),
        Map.entry(MEADOW_FOREST, "BiomeGenMeadowForest"),
        Map.entry(SEASONAL_CLEARING, "BiomeGenSeasonalForestClearing"),
        Map.entry(MOUNTAIN, "BiomeGenMountain"),
        Map.entry(GLACIER, "BiomeGenGlacier"),
        Map.entry(CRAG, "BiomeGenCrag"),
        Map.entry(EUCALYPTUS, "BiomeGenEucalyptusForest"),
        Map.entry(RAINFOREST, "BiomeGenRainforest"),
        Map.entry(TROPICAL_RAINFOREST, "BiomeGenTropicalRainforest"),
        Map.entry(BAMBOO_FOREST, "BiomeGenBambooForest"),
        Map.entry(REDWOOD, "BiomeGenRedwoodForest"),
        Map.entry(JADE_CLIFFS, "BiomeGenJadeCliffs"),
        Map.entry(DEAD_FOREST, "BiomeGenDeadForest"),
        Map.entry(OMINOUS_WOODS, "BiomeGenOminousWoods"),
        Map.entry(FUNGI_FOREST, "BiomeGenFungiForest"),
        Map.entry(MYSTIC_GROVE, "BiomeGenMysticGrove"),
        Map.entry(GARDEN, "BiomeGenGarden"),
        Map.entry(SACRED_SPRINGS, "BiomeGenSacredSprings"),
        Map.entry(WASTELAND, "BiomeGenWasteland"),
        Map.entry(ORCHARD, "BiomeGenOrchard"),
        Map.entry(OASIS, "BiomeGenOasis"),
        Map.entry(SCRUBLAND, "BiomeGenScrubland"),
        Map.entry(SPRUCE_WOODS, "BiomeGenSpruceWoods"),
        Map.entry(CORAL_REEF, "BiomeGenCoralReef"),
        Map.entry(KELP_FOREST, "BiomeGenKelpForest"),
        Map.entry(MANGROVE, "BiomeGenMangrove"),
        Map.entry(TROPICS, "BiomeGenTropics"),
        Map.entry(VOLCANO, "BiomeGenVolcano"));
    private static final Set<String> SIMPLE = Set.of(
        "WorldGenBOPFlora",
        "WorldGenBOPDoubleFlora",
        "WorldGenBOPTallGrass",
        "WorldGenWaterReeds",
        "WorldGenBOPLily",
        "WorldGenMoss",
        "WorldGenRiverCane");
    private static final Set<String> SURFACE_FEATURES = Set
        .of("WorldGenWaterside", "WorldGenLog", "WorldGenBOPCoral", "WorldGenBOPBlob");
    private static final Set<String> EXTENDED_VANILLA = Set.of(
        "BiomeGenWoodland",
        "BiomeGenHeathland",
        "BiomeGenShrubland",
        "BiomeGenBorealForest",
        "BiomeGenMapleWoods",
        "BiomeGenSeasonalForest",
        "BiomeGenThicket",
        "BiomeGenConiferousForest",
        "BiomeGenConiferousForestSnow",
        "BiomeGenFrostForest",
        "BiomeGenMeadow",
        "BiomeGenTemperateRainforest",
        "BiomeGenSteppe",
        "BiomeGenDenseForest",
        "BiomeGenShield",
        "BiomeGenHighland",
        "BiomeGenTundra",
        "BiomeGenArctic",
        "BiomeGenAlps",
        "BiomeGenBrushland",
        "BiomeGenChaparral",
        "BiomeGenGrassland",
        "BiomeGenAlpsForest",
        "BiomeGenMeadowForest",
        "BiomeGenSeasonalForestClearing",
        "BiomeGenMountain",
        "BiomeGenGlacier",
        "BiomeGenCrag",
        "BiomeGenEucalyptusForest",
        "BiomeGenRainforest",
        "BiomeGenTropicalRainforest",
        "BiomeGenBambooForest",
        "BiomeGenRedwoodForest",
        "BiomeGenJadeCliffs");
    private static final Set<String> OWNED_ELSEWHERE = Set
        .of("waterSpringsPerChunk", "lavaSpringsPerChunk", "minersDelightPerChunk", "rootsPerChunk");
    private static final Set<String> TREES = Set.of(
        "WorldGenMiniShrub",
        "WorldGenPoplar",
        "WorldGenPoplar2",
        "WorldGenBOPBigTree",
        "WorldGenMixedTree",
        "WorldGenOriginalTree",
        "WorldGenBulbTree",
        "WorldGenBOPTaiga2",
        "WorldGenBOPTaiga3",
        "WorldGenBOPShrub",
        "WorldGenBOPSwampTree");
    private static Map<String, Binding> bindings = Map.of();
    private static String status = "not inspected";
    private static boolean inspected;

    private BopNativeBiomes() {}

    public static boolean handles(String key) {
        return CLASSES.containsKey(key);
    }

    public static Set<String> supportedKeys() {
        return CLASSES.keySet();
    }

    public static boolean openWinter(String key) {
        return ARCTIC.equals(key) || ALPS.equals(key);
    }

    public static boolean winter(String key) {
        return DEAD_FOREST.equals(key) || SNOWY_CONIFEROUS
            .equals(key) || FROST.equals(key) || ALPS_FOREST.equals(key) || GLACIER.equals(key) || openWinter(key);
    }

    public static boolean geological(String key) {
        return GLACIER.equals(key) || CRAG.equals(key);
    }

    public static String geologicalState(String key) {
        if (GLACIER.equals(key)) return HARD_ICE;
        if (CRAG.equals(key)) return CRAG_ROCK;
        throw new IllegalArgumentException("Not a native geological profile: " + key);
    }

    public static boolean barrenSurface(String key) {
        return openWinter(key) || geological(key) || VOLCANO.equals(key);
    }

    public static boolean loamy(String key) {
        return SLUDGEPIT.equals(key) || WETLAND.equals(key)
            || MOOR.equals(key)
            || DEAD_SWAMP.equals(key)
            || OMINOUS_WOODS.equals(key)
            || CONIFEROUS.equals(key)
            || SNOWY_CONIFEROUS.equals(key);
    }

    public static boolean lowlandWetland(String key) {
        return landOfLakes(key) || SILKGLADES.equals(key)
            || QUAGMIRE.equals(key)
            || MARSH.equals(key)
            || DEAD_SWAMP.equals(key)
            || FEN.equals(key)
            || BOG.equals(key)
            || LUSH_SWAMP.equals(key)
            || BAYOU.equals(key)
            || WETLAND.equals(key)
            || SLUDGEPIT.equals(key);
    }

    public static boolean landOfLakes(String key) {
        return LAND_OF_LAKES.equals(key) || LAND_OF_LAKES_MARSH.equals(key);
    }

    public static boolean nativeMudBanks(String key) {
        return MOOR.equals(key) || lowlandWetland(key);
    }

    public static boolean patchySoil(String key) {
        return DENSE_FOREST.equals(key) || SHIELD.equals(key);
    }

    public static boolean usesSurfaceNoise(String key) {
        return patchySoil(key) || REDWOOD.equals(key) || LUSH_DESERT.equals(key);
    }

    public static boolean largeForestBatch(String key) {
        return REDWOOD.equals(key) || JADE_CLIFFS.equals(key);
    }

    public static boolean dryScrub(String key) {
        return BRUSHLAND.equals(key) || CHAPARRAL.equals(key);
    }

    public static boolean arid(String key) {
        return OUTBACK.equals(key) || XERIC_SHRUBLAND.equals(key);
    }

    public static boolean canyon(String key) {
        return CANYON.equals(key) || CANYON_RAVINE.equals(key);
    }

    public static boolean humidForest(String key) {
        return EUCALYPTUS.equals(key) || RAINFOREST.equals(key) || TROPICAL_RAINFOREST.equals(key);
    }

    public static boolean marine(String key) {
        return CORAL_REEF.equals(key) || KELP_FOREST.equals(key)
            || MANGROVE.equals(key)
            || TROPICS.equals(key)
            || VOLCANO.equals(key);
    }

    public static boolean oceanChild(String key) {
        return CORAL_REEF.equals(key) || KELP_FOREST.equals(key);
    }

    public static boolean vanillaLandChild(String key) {
        return OASIS.equals(key) || SCRUBLAND.equals(key) || SPRUCE_WOODS.equals(key);
    }

    public static boolean finalBatch(String key) {
        return SACRED_SPRINGS.equals(key) || WASTELAND.equals(key)
            || ORCHARD.equals(key)
            || vanillaLandChild(key)
            || marine(key);
    }

    public static boolean ownsOreSubstrate(String key, int depth, int soilLayers) {
        if (key == null) return false;
        if (nativeMudBanks(key) || GRASSLAND.equals(key)
            || OMINOUS_WOODS.equals(key)
            || MYSTIC_GROVE.equals(key)
            || LUSH_DESERT.equals(key)
            || Set.of(WASTELAND, OASIS, MANGROVE, VOLCANO)
                .contains(key))
            return soilLayers > 0 && depth >= 0 && depth < 8;
        return reservesOreSubstrate(key) && depth >= 0 && depth < soilLayers;
    }

    public static boolean reservesOreSubstrate(String key) {
        return canyon(key) || nativeMudBanks(key)
            || LUSH_DESERT.equals(key)
            || arid(key)
            || ORIGIN_VALLEY.equals(key)
            || GARDEN.equals(key)
            || MYSTIC_GROVE.equals(key)
            || FUNGI_FOREST.equals(key)
            || OMINOUS_WOODS.equals(key)
            || DEAD_FOREST.equals(key)
            || TUNDRA.equals(key)
            || openWinter(key)
            || dryScrub(key)
            || GRASSLAND.equals(key)
            || MOUNTAIN.equals(key)
            || geological(key)
            || humidForest(key)
            || BAMBOO_FOREST.equals(key)
            || largeForestBatch(key)
            || finalBatch(key)
            || BopSubBiomePolicy.isChild(key);
    }

    public static synchronized void discover() {
        if (inspected) return;
        if (!NativeFeatureCapture.installed()) {
            status = "Native feature capture unavailable";
            return;
        }
        inspected = true;
        try {
            List<?>[] nativePools = (List<?>[]) Class.forName("biomesoplenty.common.world.BOPBiomeManager")
                .getField("overworldBiomes")
                .get(null);
            Set<BiomeGenBase> enabled = Collections.newSetFromMap(new IdentityHashMap<>());
            for (var pool : nativePools) if (pool != null) for (var item : pool) {
                var entry = (BiomeManager.BiomeEntry) item;
                if (entry.itemWeight > 0) enabled.add(entry.biome);
            }
            Map<String, Binding> found = new TreeMap<>();
            var unavailable = new TreeMap<String, String>();
            for (String key : new TreeSet<>(CLASSES.keySet())) {
                if (BopSubBiomePolicy.isChild(key)) continue;
                var nativeEntry = NativeBiomeCatalogue.get(key);
                if (nativeEntry == null || !enabled.contains(nativeEntry.biome)) continue;
                try {
                    if (!nativeEntry.version.equals("2.1.0") || !nativeEntry.biome.getClass()
                        .getName()
                        .equals("biomesoplenty.common.biome.overworld." + CLASSES.get(key)))
                        throw new IllegalStateException("Unsupported BOP binding: " + key);
                    found.put(key, bind(nativeEntry.biome));
                } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                    // An unsupported new profile must not disable unrelated, already saved profiles.
                    unavailable.put(key, error.toString());
                    Biomic.LOG.warn("BOP profile unavailable: {}: {}", key, error.toString());
                }
            }
            // Read native registration, not mere existence in the catalogue. Disabled parents or
            // children stay disabled, and failure of a new child never invalidates its saved parent.
            List<?>[] nativeSubPools = null;
            try {
                nativeSubPools = (List<?>[]) Class.forName("biomesoplenty.common.world.BOPBiomeManager")
                    .getField("overworldSubBiomes")
                    .get(null);
                if (nativeSubPools == null) throw new IllegalStateException("Missing native sub-biome pools");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                unavailable.put("sub-biome-registration", error.toString());
                Biomic.LOG
                    .warn("BOP sub-biome registration unavailable; keeping primary profiles: {}", error.toString());
            }
            if (nativeSubPools != null) for (String key : new TreeSet<>(CLASSES.keySet())) {
                var rule = BopSubBiomePolicy.rule(key);
                if (rule == null || !BopSubBiomePolicy.externalParent(key) && !found.containsKey(rule.parent()))
                    continue;
                var nativeEntry = NativeBiomeCatalogue.get(key);
                if (nativeEntry == null) continue;
                try {
                    BiomeGenBase parent = BopSubBiomePolicy.externalParent(key) ? vanillaParent(rule.parent())
                        : found.get(rule.parent()).biome;
                    if (parent.biomeID < 0 || parent.biomeID >= nativeSubPools.length)
                        throw new IllegalStateException("Invalid native parent ID");
                    int weight = configuredSubBiomeWeight(nativeSubPools[parent.biomeID], nativeEntry.biome);
                    if (weight <= 0) continue;
                    if ((SLUDGEPIT.equals(rule.parent()) || CANYON.equals(rule.parent())
                        || LAND_OF_LAKES.equals(rule.parent())
                        || BopSubBiomePolicy.externalParent(key)) && weight != 10)
                        throw new IllegalStateException("Native child weight changed: " + rule.parent());
                    if (!nativeEntry.version.equals("2.1.0") || !nativeEntry.biome.getClass()
                        .getName()
                        .equals(expectedClassName(key)))
                        throw new IllegalStateException("Unsupported native sub-biome: " + key);
                    Class<?> type = Class.forName("biomesoplenty.common.biome.BOPSubBiome");
                    if (type.getField("zoom")
                        .getDouble(nativeEntry.biome) != rule.sourceZoom()
                        || type.getField("threshold")
                            .getDouble(nativeEntry.biome) != rule.sourceThreshold())
                        throw new IllegalStateException("Native sub-biome parameters changed");
                    Binding bound = bind(nativeEntry.biome);
                    found.put(
                        key,
                        new Binding(
                            bound.biome,
                            bound.slots,
                            bound.signature + BopSubBiomePolicy.identity(rule) + "|source-sub-weight=" + weight));
                } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                    unavailable.put(key, error.toString());
                    Biomic.LOG.warn("BOP sub-biome unavailable: {}: {}", key, error.toString());
                }
            }
            if (found.isEmpty()) {
                status = "No enabled supported BOP habitats";
                return;
            }
            var signatures = new TreeMap<String, String>();
            found.forEach((key, b) -> signatures.put(key, b.signature));
            bindings = Collections.unmodifiableMap(found);
            status = "native BOP surface profiles: " + found.keySet()
                + (unavailable.isEmpty() ? "" : "; rejected profiles: " + unavailable);
        } catch (ClassNotFoundException absent) {
            status = "Biomes O' Plenty absent";
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            status = error.toString();
            Biomic.LOG.warn("BOP surface adapter unavailable: {}", status);
        }
    }

    private static String expectedClassName(String key) {
        String packageName = oceanChild(key) ? "biomesoplenty.common.biome.overworld.ocean."
            : "biomesoplenty.common.biome.overworld.sub.";
        return packageName + CLASSES.get(key);
    }

    private static BiomeGenBase vanillaParent(String key) {
        return switch (key) {
            case BopSubBiomePolicy.OCEAN -> BiomeGenBase.ocean;
            case BopSubBiomePolicy.DEEP_OCEAN -> BiomeGenBase.deepOcean;
            case BopSubBiomePolicy.DESERT -> BiomeGenBase.desert;
            case BopSubBiomePolicy.SAVANNA -> BiomeGenBase.savanna;
            case BopSubBiomePolicy.FOREST -> BiomeGenBase.forest;
            default -> throw new IllegalArgumentException("Unknown native source parent: " + key);
        };
    }

    /** Registry-object identity matters: a child listed under another parent is not enabled here. */
    public static int configuredSubBiomeWeight(List<?> parentPool, BiomeGenBase child) {
        if (child == null) throw new IllegalArgumentException("Missing native child");
        if (parentPool == null) return 0;
        int weight = 0, matches = 0;
        for (Object item : parentPool) {
            var entry = (BiomeManager.BiomeEntry) item;
            if (entry.biome == child) {
                weight = entry.itemWeight;
                matches++;
            }
        }
        if (matches > 1) throw new IllegalStateException("Duplicate native sub-biome registration");
        return matches == 1 ? Math.max(0, weight) : 0;
    }

    /** Validate only the profiles a save selected, so future batches cannot silently enroll or invalidate it. */
    public static String manifest(Collection<String> selected) {
        discover();
        var signatures = new TreeMap<String, String>();
        for (String key : selected) {
            Binding binding = bindings.get(key);
            if (binding == null) return "";
            signatures.put(key, binding.signature);
        }
        return fingerprint(signatures);
    }

    static String fingerprint(Map<String, String> profiles) {
        if (profiles.isEmpty()) return "";
        var identity = new StringBuilder("bop-2308-surface-v1\n");
        new TreeMap<>(profiles).forEach(
            (key, signature) -> identity.append(key)
                .append(':')
                .append(signature)
                .append('\n'));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(
                    identity.toString()
                        .getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public static String status() {
        discover();
        return status;
    }

    public static Set<String> enabledKeys() {
        discover();
        return bindings.keySet();
    }

    public static Binding binding(String key) {
        discover();
        Binding result = bindings.get(key);
        if (result == null) throw new IllegalStateException("Unavailable BOP habitat: " + key + " (" + status + ")");
        return result;
    }

    public static String soil(String key, int depth, double surfaceNoise) {
        if (!handles(key)) throw new IllegalArgumentException(key);
        if (!Double.isFinite(surfaceNoise)) throw new IllegalArgumentException("Non-finite native surface noise");
        if (geological(key)) return geologicalState(key);
        if (WASTELAND.equals(key)) return DRIED_DIRT;
        if (VOLCANO.equals(key)) return ASH_STONE;
        if (MANGROVE.equals(key)) return "minecraft:sand";
        if (ALPS.equals(key)) return "minecraft:snow_block";
        if (QUAGMIRE.equals(key)) return MUD;
        if (canyon(key)) return HARD_DIRT;
        if (OUTBACK.equals(key)) return HARD_SAND;
        if (XERIC_SHRUBLAND.equals(key)) return "minecraft:sand";
        // Native noise<=-.5 leaves mutable prior-column state; use constructor clay,
        // never make one column depend on whichever neighbor was sampled first.
        if (LUSH_DESERT.equals(key))
            return surfaceNoise > 1.75 ? (depth == 0 ? "minecraft:grass_block[snowy=false]" : "minecraft:dirt")
                : "minecraft:terracotta";
        if (ORIGIN_VALLEY.equals(key)) return depth == 0 ? ORIGIN_GRASS : "minecraft:dirt";
        if (GARDEN.equals(key)) return depth == 0 ? LONG_GRASS : "minecraft:dirt";
        if (BAMBOO_FOREST.equals(key)) return depth == 0 ? "minecraft:podzol" : "minecraft:dirt";
        if (REDWOOD.equals(key)) return depth == 0
            ? (surfaceNoise > -.95 && surfaceNoise <= 1.75 ? "minecraft:podzol" : "minecraft:grass_block[snowy=false]")
            : "minecraft:dirt";
        if (loamy(key)) return depth == 0 ? LOAMY_GRASS : LOAMY_DIRT;
        if (surfaceNoise > 1.75) {
            if (SHIELD.equals(key)) return "minecraft:stone";
            if (DENSE_FOREST.equals(key) && depth == 0) return "minecraft:dirt[legacy_meta=1]";
        }
        // Native Dense Forest/Shield leave top/filler mutable at noise<=-.95. Use their
        // constructor's grass/dirt defaults, never the previously generated column's state.
        // Earlier profiles retain their ordinary grass/dirt and saved output.
        return depth == 0 ? "minecraft:grass_block[snowy=false]" : "minecraft:dirt";
    }

    public enum Height {
        SURFACE,
        RANDOM_64,
        RANDOM_256,
        RANDOM_DOUBLE_SURFACE,
        RANDOM_SURFACE_PLUS_32,
        SURFACE_PLUS_64,
        SURFACE_CENTERED_128
    }

    public enum Kind {
        TREE,
        VANILLA_GRASS,
        VANILLA_FLOWER,
        WEIGHTED,
        SIMPLE,
        PUMPKIN,
        MELON,
        SHORE_PATCH,
        FALLEN_LOG,
        DEAD_BUSH,
        BROWN_MUSHROOM,
        RED_MUSHROOM,
        SUBMERGED_PLANT,
        WATERLILY,
        MOSS,
        ROCKPILE,
        VANILLA_SHORE,
        SPLOTCH,
        POND,
        REEDS,
        RIVER_CANE,
        GIANT_MUSHROOM,
        SPLATTER,
        CACTUS,
        OASIS_BANK,
        WEB_NEST,
        ORCHARD_TREE,
        MARINE,
        WASTELAND_FORMATION,
        VOLCANO_SPOUT
    }

    public record Slot(String name, int attempts, Height height, Kind kind, WorldGenerator generator,
        NativeWeightedTable<WorldGenerator> weights) {}

    public static final class Binding {

        public final BiomeGenBase biome;
        public final List<Slot> slots;
        public final String signature;

        Binding(BiomeGenBase biome, List<Slot> slots, String signature) {
            this.biome = biome;
            this.slots = List.copyOf(slots);
            this.signature = signature;
        }
    }

    private static Binding bind(BiomeGenBase biome) throws ReflectiveOperationException {
        // BOP hides BiomeGenBase.theBiomeDecorator with a distinct field. Read the BOP field explicitly: by its
        // development
        // name, or the SRG name BOP's release jar gives it (reobfuscation renamed it with the vanilla field it hides).
        Object decorator = cpw.mods.fml.relauncher.ReflectionHelper
            .findField(Class.forName("biomesoplenty.api.biome.BOPBiome"), "theBiomeDecorator", "field_76760_I")
            .get(biome);
        var vanilla = (BiomeDecorator) decorator;
        Object features = decorator.getClass()
            .getField("bopFeatures")
            .get(decorator);
        List<Slot> slots = new ArrayList<>();
        StringBuilder signature = new StringBuilder(
            biome.getClass()
                .getName());
        boolean tropical = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenTropicalRainforest");
        boolean humid = tropical || Set.of("BiomeGenEucalyptusForest", "BiomeGenRainforest")
            .contains(
                biome.getClass()
                    .getSimpleName());
        boolean bamboo = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenBambooForest");
        boolean redwood = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenRedwoodForest");
        boolean jade = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenJadeCliffs");
        boolean deadForest = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenDeadForest");
        boolean ominous = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenOminousWoods");
        boolean fungi = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenFungiForest");
        boolean mystic = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenMysticGrove");
        boolean garden = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenGarden");
        boolean origin = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenOriginValley");
        boolean outback = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenOutback");
        boolean xeric = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenXericShrubland"), arid = outback || xeric;
        boolean lushDesert = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenLushDesert");
        boolean marsh = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenMarsh");
        boolean deadSwamp = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenDeadSwamp");
        boolean moor = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenMoor");
        boolean fen = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenFen");
        boolean bog = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenBog");
        boolean lushSwamp = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenLushSwamp");
        boolean bayou = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenBayou");
        boolean wetland = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenWetland");
        boolean sludgepit = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenSludgepit");
        boolean quagmire = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenQuagmire");
        boolean silkglades = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenSilkglades");
        boolean canyonRavine = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenCanyonRavine");
        boolean canyon = canyonRavine || biome.getClass()
            .getSimpleName()
            .equals("BiomeGenCanyon");
        boolean lakeMarsh = biome.getClass()
            .getSimpleName()
            .equals("BiomeGenLandOfLakesMarsh");
        boolean lakes = lakeMarsh || biome.getClass()
            .getSimpleName()
            .equals("BiomeGenLandOfLakes");
        String simpleName = biome.getClass()
            .getSimpleName();
        boolean sacred = simpleName.equals("BiomeGenSacredSprings"), wasteland = simpleName.equals("BiomeGenWasteland"),
            orchard = simpleName.equals("BiomeGenOrchard");
        boolean oasis = simpleName.equals("BiomeGenOasis"), scrubland = simpleName.equals("BiomeGenScrubland"),
            spruceWoods = simpleName.equals("BiomeGenSpruceWoods");
        boolean coralReef = simpleName.equals("BiomeGenCoralReef"),
            kelpForest = simpleName.equals("BiomeGenKelpForest"), mangrove = simpleName.equals("BiomeGenMangrove");
        boolean tropics = simpleName.equals("BiomeGenTropics"), volcano = simpleName.equals("BiomeGenVolcano"),
            marine = coralReef || kelpForest || mangrove || tropics || volcano;
        boolean finalProfile = sacred || wasteland || orchard || oasis || scrubland || spruceWoods || marine;
        if (finalProfile) {
            Block expectedTop = wasteland ? NativeBlockCatalogue.resolve(DRIED_DIRT)
                .block()
                : volcano ? NativeBlockCatalogue.resolve(ASH_STONE)
                    .block() : mangrove ? Blocks.sand : Blocks.grass;
            Block expectedFiller = wasteland || volcano || mangrove ? expectedTop : Blocks.dirt;
            if (biome.topBlock != expectedTop || biome.fillerBlock != expectedFiller || biome.field_150604_aj != 0)
                throw new IllegalStateException("Native final-profile soil changed: " + simpleName);
            float expectedTemperature = sacred ? .5F
                : wasteland ? 1F
                    : orchard ? .7F
                        : oasis ? 2F
                            : scrubland ? 1.2F
                                : spruceWoods ? .7F
                                    : coralReef || kelpForest ? .5F : mangrove ? .8F : tropics ? 1.2F : 2F;
            float expectedRain = sacred ? .5F
                : wasteland ? .05F
                    : orchard ? .7F
                        : oasis ? .3F
                            : scrubland ? 0F
                                : spruceWoods ? .8F
                                    : coralReef || kelpForest ? .9F : mangrove ? .9F : tropics ? 1F : .05F;
            if (biome.temperature != expectedTemperature || biome.rainfall != expectedRain)
                throw new IllegalStateException("Native final-profile climate changed: " + simpleName);
            int expectedTrees = sacred ? 30
                : wasteland || volcano || coralReef || kelpForest ? 0
                    : orchard ? 2 : oasis ? 3 : scrubland ? 7 : spruceWoods ? 10 : mangrove ? 6 : 12;
            if (vanilla.treesPerChunk != expectedTrees)
                throw new IllegalStateException("Native final-profile tree schedule changed: " + simpleName);
            signature.append(
                "|native-final-overworld-v1|native-soil3-v1|immutable-native-soil-ore-reservation-v1|shallow-bed-max12-v1");
            if (sacred) signature.append(
                "|native-sacred-springs-v1|native-sacred-oak-1-in-450-height50-trunk5x5-minus-corners-v1|whole-tree-envelope-v1|source-sized-sacred-oak-capture-budget-v1|bounded-source-ground-scan-rejection-v1");
            if (wasteland) signature.append(
                "|native-wasteland-v1|native-dried-dirt-formations-and-hollows-v1|closed-native-water2-poison10-v1|native-zero-plus-1-in-10-dead-tree-v1");
            if (orchard)
                signature.append("|native-orchard-v1|native-fruit-selector-with-single-external-provider-owner-v1");
            if (oasis) signature.append(
                "|native-oasis-v1|native-palm-cactus-reeds-quicksand-melon-v1|vanilla-desert-parent-v1|unsupported-thin-shelf-sand-base-sandstone-v1|bounded-source-ground-scan-rejection-v1");
            if (scrubland) signature.append(
                "|native-scrubland-v1|native-shrub-original-tree-v1|vanilla-savanna-parent-v1|bounded-source-ground-scan-rejection-v1");
            if (spruceWoods)
                signature.append("|native-spruce-woods-v1|native-oak-spruce-tree-v1|vanilla-forest-parent-v1");
            if (coralReef) signature.append(
                "|native-coral-reef-v1|native-coral-short-kelp-sponge-v1|vanilla-ocean-parent-v1|marine-carrier-v1");
            if (kelpForest) signature
                .append("|native-kelp-forest-v1|native-kelp-densities-v1|vanilla-ocean-parent-v1|marine-carrier-v1");
            if (mangrove) signature.append(
                "|native-mangrove-v1|native-whole-mangrove-roots-v1|closed-native-water10-v1|vanilla-deep-ocean-parent-v1|marine-carrier-v1|unsupported-thin-shelf-sand-base-sandstone-v1|bounded-source-ground-scan-rejection-v1");
            if (tropics) signature.append(
                "|native-tropics-v1|native-palm-and-shrub-trees-v1|vanilla-deep-ocean-parent-v1|marine-carrier-v1|bounded-source-ground-scan-rejection-v1");
            if (volcano) signature.append(
                "|native-volcano-v1|native-ash-and-lava-features-v1|vanilla-deep-ocean-parent-v1|marine-carrier-v1");
            BopFinalProfile.validate(biome, vanilla, features);
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
            if (oasis) add(
                slots,
                new Slot(
                    "vanilla_cacti",
                    count(vanilla.cactiPerChunk),
                    Height.RANDOM_DOUBLE_SURFACE,
                    Kind.CACTUS,
                    null,
                    null));
        }
        if (lakes) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .5F
                || biome.rainfall != .9F
                || biome.rootHeight != (lakeMarsh ? -.1F : .1F)
                || biome.heightVariation != (lakeMarsh ? .05F : .2F)
                || vanilla.treesPerChunk != (lakeMarsh ? 1 : 50)
                || vanilla.flowersPerChunk != 3
                || vanilla.grassPerChunk != 25
                || vanilla.sandPerChunk != (lakeMarsh ? 1 : -999)
                || vanilla.sandPerChunk2 != (lakeMarsh ? 1 : -999)
                || features.getClass()
                    .getField("wildRicePerChunk")
                    .getInt(features) != (lakeMarsh ? 8 : 2)
                || features.getClass()
                    .getField("bopGrassPerChunk")
                    .getInt(features) != 25
                || features.getClass()
                    .getField("waterReedsPerChunk")
                    .getInt(features) != (lakeMarsh ? 15 : 30)
                || features.getClass()
                    .getField("gravelPerChunk")
                    .getInt(features) != (lakeMarsh ? 0 : 9)
                || features.getClass()
                    .getField("mudPerChunk")
                    .getInt(features) != (lakeMarsh ? 9 : 0)
                || features.getClass()
                    .getField("waterLakesPerChunk")
                    .getInt(features) != (lakeMarsh ? 10 : 0))
                throw new IllegalStateException("Native Land of Lakes soil/climate/schedule changed");
            for (String field : List
                .of("seaweedPerChunk", "algaePerChunk", "leafPilesPerChunk", "deadLeafPilesPerChunk"))
                if (features.getClass()
                    .getField(field)
                    .getInt(features) != 15)
                    throw new IllegalStateException("Native Land of Lakes flora changed: " + field);
            signature.append(
                "|native-land-of-lakes-v1|native-whole-oak-birch-spruce-selection-v1|native-wild-rice-ground-only-v1|native-grass-dirt-soil3-v1|native-plant-noise-colors-v1|pond-shore-tree-flora-order-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-parent-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (canyon) {
            if (!HARD_DIRT.equals(NativeBlockCatalogue.encode(biome.topBlock, 0)) || biome.fillerBlock != biome.topBlock
                || biome.field_150604_aj != 0
                || biome.temperature != 1F
                || biome.rainfall != .3F
                || biome.rootHeight != (canyonRavine ? -.1F : 5F)
                || biome.heightVariation != (canyonRavine ? .4F : .025F)
                || vanilla.treesPerChunk != (canyonRavine ? 1 : 3)
                || vanilla.grassPerChunk != 5
                || vanilla.flowersPerChunk != -999
                || !Integer.valueOf(3)
                    .equals(
                        features.getClass()
                            .getField("bromeliadsPerChunk")
                            .get(features))
                || !Integer.valueOf(4)
                    .equals(
                        features.getClass()
                            .getField("grassSplatterPerChunk")
                            .get(features))
                || !Integer.valueOf(4)
                    .equals(
                        features.getClass()
                            .getField("waterReedsPerChunk")
                            .get(features))
                || !Boolean.FALSE.equals(
                    features.getClass()
                        .getField("generatePumpkins")
                        .get(features)))
                throw new IllegalStateException("Native Canyon soil/climate/schedule changed");
            signature.append(
                "|native-canyon-v1|native-harddirt-soil3-v1|native-pine-and-acacia-shrub-v1|native-shrub-absolute-height64..256-v1|whole-trees-and-grass-splatters-v1|native-bromeliad-substrate-v1|shore-patch-tree-flora-order-v1|ore-skin-reservation-v1|shallow-bed-max12-v1|arid-landmark-hosts12-32-and-sky-v1|unsupported-mainland-host-sand-base-sandstone-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (silkglades) {
            if (biome.temperature != .5F || biome.rainfall != .9F
                || biome.rootHeight != .1F
                || biome.heightVariation != .2F
                || biome.field_150604_aj != 0
                || biome.topBlock != Blocks.grass
                || biome.fillerBlock != Blocks.dirt
                || vanilla.treesPerChunk != 6
                || vanilla.grassPerChunk != 2
                || vanilla.mushroomsPerChunk != 4
                || vanilla.flowersPerChunk != -999
                || vanilla.reedsPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Silkglades soil/climate/schedule changed");
            signature.append(
                "|native-silkglades-v1|native-hanging-leaf-and-dead-trees-v1|bounded-native-water-descent16-v1|native-whole-web-nests-no-entity-side-effects-v1|native-supported-ground-canopy-webs-v1|shore-tree-nest-web-flora-order-v1|grass-dirt-soil3-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-parent-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (quagmire) {
            if (biome.temperature != .8F || biome.rainfall != .9F
                || biome.rootHeight != 0F
                || biome.heightVariation != .1F
                || biome.field_150604_aj != 0
                || !MUD.equals(NativeBlockCatalogue.encode(biome.topBlock, 0))
                || biome.fillerBlock != biome.topBlock
                || vanilla.treesPerChunk != 0
                || vanilla.grassPerChunk != 10
                || vanilla.mushroomsPerChunk != 3
                || vanilla.flowersPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Quagmire soil/climate/schedule changed");
            signature.append(
                "|native-quagmire-v1|native-mud-soil3-v1|native-grass-splotch48-and-splatter5-v1|native-zero-tree-plus-bonus10-v1|whole-dead-tree-grass-only-v1|shore-splotch-splatter-tree-flora-order-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-parent-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (sludgepit) {
            if (biome.temperature != .8F || biome.rainfall != .9F
                || biome.rootHeight != 0F
                || biome.heightVariation != .1F
                || biome.field_150604_aj != 0
                || !(biome.topBlock == Blocks.grass && biome.fillerBlock == Blocks.dirt
                    || LOAMY_GRASS.equals(NativeBlockCatalogue.encode(biome.topBlock, 0))
                        && LOAMY_DIRT.equals(NativeBlockCatalogue.encode(biome.fillerBlock, 0)))
                || vanilla.treesPerChunk != 30
                || vanilla.grassPerChunk != 15
                || vanilla.mushroomsPerChunk != 8
                || vanilla.deadBushPerChunk != 5
                || vanilla.flowersPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Sludgepit soil/climate/schedule changed");
            signature.append(
                "|native-sludgepit-v1|native-two-whole-bog-trees-v1|native-canopy-vine-support-preservation-v1|bounded-native-water-descent16-v1|stateless-loamy-soil3-v1|native-loam-vanilla-grass-support-v1|native-poison5-mud5-clay1-v1|pond-shore-tree-flora-order-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (wetland) {
            if (biome.temperature != .6F || biome.rainfall != .9F
                || biome.rootHeight != -.1F
                || biome.heightVariation != .2F
                || biome.field_150604_aj != 0
                || !(biome.topBlock == Blocks.grass && biome.fillerBlock == Blocks.dirt
                    || LOAMY_GRASS.equals(NativeBlockCatalogue.encode(biome.topBlock, 0))
                        && LOAMY_DIRT.equals(NativeBlockCatalogue.encode(biome.fillerBlock, 0)))
                || vanilla.treesPerChunk != 6
                || vanilla.mushroomsPerChunk != 8
                || vanilla.reedsPerChunk != 15
                || vanilla.clayPerChunk != 2
                || vanilla.waterlilyPerChunk != 4
                || vanilla.flowersPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Wetland soil/climate/schedule changed");
            signature.append(
                "|native-wetland-v1|native-taiga6-and-hanging-leaf-swamp-tree-v1|bounded-native-water-descent16-v1|stateless-loamy-soil3-v1|native-loam-vanilla-grass-support-v1|native-mud5-clay2-v1|native-ground-moss20-v1|shore-tree-flora-order-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (bayou) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .8F
                || biome.rainfall != .9F
                || biome.rootHeight != -.1F
                || biome.heightVariation != .1F
                || vanilla.treesPerChunk != 15
                || vanilla.grassPerChunk != 5
                || vanilla.mushroomsPerChunk != 4
                || vanilla.reedsPerChunk != 25
                || vanilla.waterlilyPerChunk != 4
                || vanilla.flowersPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Bayou soil/climate/schedule changed");
            Block willow = NativeBlockCatalogue.resolve("biomesoplenty:willow[legacy_meta=1]")
                .block();
            if (!willow.getClass()
                .getName()
                .equals("biomesoplenty.common.blocks.BlockWillow"))
                throw new IllegalStateException("Native Bayou willow changed");
            signature.append(
                "|native-bayou-v1|native-three-whole-buttressed-willows-v1|native-canopy-vine-support-preservation-v1|bounded-native-water-descent16-v1|native-water5-mud1-clay1-v1|native-ground-moss20-v1|pond-shore-tree-flora-order-v1|grass-dirt-soil3-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (lushSwamp) {
            signature.append("|native-canopy-vine-support-preservation-v1|bounded-native-water-descent16-v1");
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .7F
                || biome.rainfall != 1F
                || biome.rootHeight != 0F
                || biome.heightVariation != .1F
                || vanilla.treesPerChunk != 10
                || vanilla.grassPerChunk != 4
                || vanilla.mushroomsPerChunk != 8
                || vanilla.reedsPerChunk != 16
                || vanilla.waterlilyPerChunk != 4)
                throw new IllegalStateException("Native Lush Swamp soil/climate/schedule changed");
            Block ivy = NativeBlockCatalogue.resolve("biomesoplenty:ivy[legacy_meta=1]")
                .block();
            if (!ivy.getClass()
                .getName()
                .equals("biomesoplenty.common.blocks.BlockIvy"))
                throw new IllegalStateException("Native Lush Swamp ivy changed");
            signature.append(
                "|native-lush-swamp-v1|native-swamp-tree8+6-supported-ivy-v1|native-cattails10-high5-lilies-cane-v1|native-poison2-v1|pond-shore-tree-flora-order-v1|grass-dirt-soil3-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (bog) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .5F
                || biome.rainfall != .9F
                || biome.rootHeight != .1F
                || biome.heightVariation != .2F
                || vanilla.treesPerChunk != 12
                || vanilla.grassPerChunk != 5
                || vanilla.mushroomsPerChunk != 8
                || vanilla.flowersPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Bog soil/climate/schedule changed");
            signature.append(
                "|native-bog-v1|native-two-cypress-and-bog-bush-v1|persistent-native-bush-leaves4-v1|native-closed-water6-poison2-v1|native-cane-reeds-litter-blue-milk-v1|pond-shore-tree-flora-order-v1|grass-dirt-soil3-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (bog) signature.append("|persistent-bog-bush-tree-journal-v1");
        if (fen) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .4F
                || biome.rainfall != .4F
                || biome.rootHeight != .1F
                || biome.heightVariation != .1F
                || vanilla.treesPerChunk != 10
                || vanilla.grassPerChunk != 5
                || vanilla.mushroomsPerChunk != 8
                || vanilla.waterlilyPerChunk != 2
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Fen soil/climate/schedule changed");
            signature.append(
                "|native-wooded-fen-v1|native-taiga1-taiga2-bare-tree-v1|native-lilies-cattails-cane-v1|paired-cattails-and-grass-v1|native-ground-moss20-v1|grass-dirt-soil3-v1|mud-gravel-clay-before-trees-flora-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (moor) {
            if (biome.temperature != .5F || biome.rainfall != 1F
                || biome.rootHeight != 1.5F
                || biome.heightVariation != .025F
                || biome.field_150604_aj != 0
                || !(biome.topBlock == Blocks.grass && biome.fillerBlock == Blocks.dirt
                    || LOAMY_GRASS.equals(NativeBlockCatalogue.encode(biome.topBlock, 0))
                        && LOAMY_DIRT.equals(NativeBlockCatalogue.encode(biome.fillerBlock, 0)))
                || vanilla.treesPerChunk != -999
                || vanilla.flowersPerChunk != -999
                || vanilla.grassPerChunk != 4
                || vanilla.mushroomsPerChunk != 2
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Moor soil/climate/schedule changed");
            signature.append(
                "|native-raised-moor-v1|treeless-loamy-soil3-v1|native-loam-vanilla-grass-support-v1|closed-native-water-ponds10-v1|native-mud-grass-dirt-only-v1|native-swampflower-koru-flax-and-grasses-v1|pond-shore-flora-order-v1|ore-horizon8-v1|shallow-bed-max12-v1|raised-hosts14-17-no-sky-no-lowland-flatten-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (deadSwamp) {
            if (biome.temperature != .7F || biome.rainfall != .9F
                || biome.field_150604_aj != 0
                || !(biome.topBlock == Blocks.grass && biome.fillerBlock == Blocks.dirt
                    || LOAMY_GRASS.equals(NativeBlockCatalogue.encode(biome.topBlock, 0))
                        && LOAMY_DIRT.equals(NativeBlockCatalogue.encode(biome.fillerBlock, 0)))
                || vanilla.treesPerChunk != 1
                || vanilla.grassPerChunk != 5
                || vanilla.flowersPerChunk != -999
                || vanilla.reedsPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Dead Swamp soil/climate/schedule changed");
            signature.append(
                "|native-dead-swamp-v1|native-eight-bare-tree-forms-v1|stateless-loamy-soil3-v1|native-loam-vanilla-grass-support-v1|native-mud-grass-dirt-only-v1|native-cane-and-water-flora-v1|shore-before-trees-before-flora-v1|ore-horizon8-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (marsh) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .5F
                || biome.rainfall != .9F
                || vanilla.treesPerChunk != -999
                || vanilla.flowersPerChunk != -999
                || vanilla.grassPerChunk != 15
                || vanilla.reedsPerChunk != -999
                || vanilla.sandPerChunk != -999
                || vanilla.sandPerChunk2 != -999)
                throw new IllegalStateException("Native Marsh soil/climate/schedule changed");
            signature.append(
                "|native-treeless-marsh-v1|native-water-lakes100-v1|native-mud-banks2-v1|native-tall-grass200-v1|water-reeds50-seaweed25-algae5-v1|grass-dirt-soil3-pond-horizon8-v1|pond-bank-flora-order-v1|shallow-bed-max12-v1|lowland-hummocks63-amplitude2-scale42-octaves3-v1|edge64-grid16-height50..96-no-sky-v1|wetland-hosts24-25-28-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (lushDesert) {
            signature.append("|native-clay-only-desert-grass-v1|mainland-host-sandstone-bases-v1");
            if (biome.topBlock != Blocks.hardened_clay || biome.fillerBlock != Blocks.hardened_clay
                || biome.field_150604_aj != 0
                || biome.temperature != 1.1F
                || biome.rainfall != .5F
                || vanilla.treesPerChunk != 6
                || vanilla.cactiPerChunk != 20)
                throw new IllegalStateException("Native Lush Desert soil/climate/tree schedule changed");
            signature.append(
                "|native-lushdesert-clay-grass-noise-v1|low-noise-constructor-fallback-v1|native-cypress7+10-alt2-four-tree-forms-v1|native-closed-water-ponds-v1|native-waterside-grass999-v1|native-rock-only-sand-v1|soil3-pond-horizon8-v1|warm-arid-landmark-hosts-v1|shallow-bed-max12-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
            add(
                slots,
                new Slot(
                    "vanilla_cacti",
                    count(vanilla.cactiPerChunk),
                    Height.RANDOM_DOUBLE_SURFACE,
                    Kind.CACTUS,
                    null,
                    null));
        }
        if (arid) {
            Block expected = outback ? NativeBlockCatalogue.resolve(HARD_SAND)
                .block() : Blocks.sand;
            if (biome.topBlock != expected || biome.fillerBlock != expected
                || biome.field_150604_aj != 0
                || biome.temperature != (outback ? 1.3F : 1F)
                || biome.rainfall != (outback ? .05F : .2F))
                throw new IllegalStateException("Native arid soil/climate changed");
            signature.append(
                "|native-arid-skin3-v1|native-whole-arid-splatters-v1|native-side-supported-cactus-v1|shore-splatters-trees-flora-v1|immutable-native-soil-ore-reservation-v1|shallow-bed-max12-v1");
            signature.append(
                outback ? "|native-hardsand-acacia-shrubs-v1|red-sand-cactus-substrate-v1|warm-arid-landmark-hosts-v1"
                    : "|native-sand-treeless-xeric-v1|thin-overhang-sandstone-base-v1|dry-desert-hosts-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
            add(
                slots,
                new Slot(
                    "vanilla_cacti",
                    count(vanilla.cactiPerChunk),
                    Height.RANDOM_DOUBLE_SURFACE,
                    Kind.CACTUS,
                    null,
                    null));
        }
        if (origin) {
            var soil = NativeBlockCatalogue.resolve(ORIGIN_GRASS);
            if (biome.topBlock != soil.block() || !biome.topBlock.getClass()
                .getName()
                .equals("biomesoplenty.common.blocks.BlockOriginGrass")
                || biome.field_150604_aj != 0
                || biome.fillerBlock != Blocks.dirt
                || biome.temperature != .7F
                || biome.rainfall != .8F
                || vanilla.sandPerChunk != 0
                || vanilla.sandPerChunk2 != 0
                || vanilla.clayPerChunk != 0
                || vanilla.treesPerChunk != 4
                || vanilla.grassPerChunk != -999
                || vanilla.flowersPerChunk != -999)
                throw new IllegalStateException("Native Origin Valley soil/climate/sparse ecology changed");
            signature.append(
                "|native-origin-grass-dirt-soil3-v1|native-complete-classic-trees-v1|native-rose-dandelion-patches-v1|native-dandelion-support-v1|no-grass-or-shore-or-pumpkin-pass-v1|immutable-native-soil-ore-reservation-v1|temperate-valley-hosts-v1|shallow-bed-max12-v1");
        }
        if (garden) {
            var soil = NativeBlockCatalogue.resolve(LONG_GRASS);
            if (biome.topBlock != soil.block() || !biome.topBlock.getClass()
                .getName()
                .equals("biomesoplenty.common.blocks.BlockLongGrass")
                || biome.field_150604_aj != 0
                || biome.fillerBlock != Blocks.dirt
                || biome.temperature != .7F
                || biome.rainfall != .8F
                || vanilla.sandPerChunk > 0
                || vanilla.sandPerChunk2 > 0)
                throw new IllegalStateException("Native Garden soil/climate/shore changed");
            signature.append(
                "|native-longgrass-dirt-soil3-v1|native-two-whole-giant-flowers-and-oak-shrubs-v1|strict-flower-envelope-v1|native-four-flower-five-grass-choices-v1|native-vanilla-groundcover-on-longgrass-v1|native-melon-vanilla-grass-only-v1|shore-before-flora-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|open-flowering-hosts-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (mystic) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .7F
                || biome.rainfall != 1F
                || vanilla.sandPerChunk > 0
                || vanilla.sandPerChunk2 > 0)
                throw new IllegalStateException("Native Mystic Grove soil/climate/shore changed");
            Block vine = NativeBlockCatalogue.resolve("biomesoplenty:flowervine[legacy_meta=1]")
                .block();
            if (!vine.getClass()
                .getName()
                .equals("biomesoplenty.common.blocks.BlockFlowerVine"))
                throw new IllegalStateException("Native flower vine changed");
            signature.append(
                "|native-five-mystic-tree-forms-v1|native-supported-flower-vines-v1|native-six-weighted-flowers-v1|closed-native-poison-pools-v1|native-triple-nested-lake-y-v1|grass-dirt-soil3-v1|pond-before-clay-before-flora-v1|immutable-pond-horizon8-ore-reservation-v1|shallow-bed-max12-v1|flowering-forest-hosts-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (fungi) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .9F
                || biome.rainfall != 1F
                || vanilla.sandPerChunk > 0
                || vanilla.sandPerChunk2 > 0)
                throw new IllegalStateException("Native Fungi Forest soil/climate/shore changed");
            Block ivy = NativeBlockCatalogue.resolve("biomesoplenty:ivy[legacy_meta=1]")
                .block();
            if (!ivy.getClass()
                .getName()
                .equals("biomesoplenty.common.blocks.BlockIvy")) throw new IllegalStateException("Native ivy changed");
            signature.append(
                "|native-fungi-jungle-shrub-v1|native-supported-ivy-v1|native-whole-vanilla-mushrooms-v1|exact-legacy-cap-and-stem-v1|native-mycelium-size32-ten-source128-v1|mycelium-clay-trees-mushrooms-flora-v1|dormant-y128-ivy-hook-v1|grass-dirt-soil3-v1|immutable-native-soil-ore-reservation-v1|shallow-bed-max12-v1|warm-fungal-forest-hosts-v1");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            signature.append('|')
                .append(BopGiantMushroom.identity(vanilla.bigMushroomGen));
            signature.append("|unfused-whole-mushroom-envelope-v1");
            add(
                slots,
                new Slot(
                    "vanilla_giant_mushrooms",
                    count(vanilla.bigMushroomsPerChunk),
                    Height.SURFACE,
                    Kind.GIANT_MUSHROOM,
                    vanilla.bigMushroomGen,
                    null));
        }
        if (ominous) {
            if (biome.temperature != .5F || biome.rainfall != .9F
                || biome.field_150604_aj != 0
                || !(biome.topBlock == Blocks.grass && biome.fillerBlock == Blocks.dirt
                    || LOAMY_GRASS.equals(NativeBlockCatalogue.encode(biome.topBlock, 0))
                        && LOAMY_DIRT.equals(NativeBlockCatalogue.encode(biome.fillerBlock, 0))))
                throw new IllegalStateException("Native Ominous Woods soil/climate changed");
            Block moss = NativeBlockCatalogue.resolve("biomesoplenty:treemoss[legacy_meta=1]")
                .block();
            if (!moss.getClass()
                .getName()
                .equals("biomesoplenty.common.blocks.BlockTreeMoss"))
                throw new IllegalStateException("Native tree moss changed");
            signature.append(
                "|native-complete-ominous-trees-v1|native-supported-tree-moss-faces-v1|stateless-loamy-soil3-v1|closed-native-poison-pools-v1|native-triple-nested-lake-y-v1|immutable-pond-horizon8-ore-reservation-v1|pond-before-clay-before-flora-v1|shallow-bed-max12-v1|cool-dark-forest-hosts-v1");
            if (vanilla.sandPerChunk > 0 || vanilla.sandPerChunk2 > 0)
                throw new IllegalStateException("Ominous Woods acquired sand/gravel shore passes");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (deadForest) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.field_150604_aj != 0
                || biome.temperature != .4F
                || biome.rainfall != .3F
                || !biome.getEnableSnow()) throw new IllegalStateException("Native Dead Forest soil/climate changed");
            signature.append(
                "|native-complete-dead-forest-trees-v1|native-eight-dead-branch-forms-v1|native-winter-altitude-temperature-v1|grass-dirt-soil3-v1|supported-shore-before-flora-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|cool-forest-hosts-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (redwood || jade) {
            if (biome.fillerBlock != Blocks.dirt || !(biome.topBlock == Blocks.grass && biome.field_150604_aj == 0
                || redwood && biome.topBlock == Blocks.dirt && biome.field_150604_aj == 2))
                throw new IllegalStateException("Native large-forest soil changed");
            signature.append(
                "|native-complete-forest-trees-v1|supported-shore-before-flora-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1");
            signature.append(
                redwood
                    ? "|native-three-redwood-sizes-v1|native-surface-noise-absolute-v1|stateless-podzol-interval-soil3-v1|temperate-forest-hosts-v1"
                    : "|native-jade-pine-shrub-v1|raised-cliff-hosts-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (bamboo) {
            // The constructor starts as grass; its terrain override always selects vanilla dirt:2.
            // Do not mutate this global biome object merely to obtain its eventual surface material.
            if (biome.fillerBlock != Blocks.dirt || !(biome.topBlock == Blocks.grass && biome.field_150604_aj == 0
                || biome.topBlock == Blocks.dirt && biome.field_150604_aj == 2))
                throw new IllegalStateException("Native bamboo soil changed");
            signature.append(
                "|native-bamboo-bulb-shrub-v1|stateless-podzol-soil3-v1|native-stalk-support-v1|humid-basin-hosts-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|supported-shore-before-flora-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (humid) {
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt || biome.field_150604_aj != 0)
                throw new IllegalStateException("Native humid forest soil changed");
            signature.append(
                "|native-humid-whole-trees-v1|vine-cocoa-faces-v1|native-tree-journal-attachments-v1|source-vine-horizontal-support-v1|humid-landform-hosts-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|supported-shore-before-flora-v1");
            if (tropical) signature.append(
                "|source-parameterized-jungle-trees-v1|native-hollow-branched-trees-v1|native-supported-splotches-before-flora-v1|native-melons-reeds-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (Set.of("BiomeGenGlacier", "BiomeGenCrag")
            .contains(
                biome.getClass()
                    .getSimpleName())) {
            boolean glacier = biome.getClass()
                .getSimpleName()
                .equals("BiomeGenGlacier");
            String state = glacier ? HARD_ICE : CRAG_ROCK;
            if (!state.equals(NativeBlockCatalogue.encode(biome.topBlock, biome.field_150604_aj))
                || !state.equals(NativeBlockCatalogue.encode(biome.fillerBlock, 0)))
                throw new IllegalStateException("Native geological top/filler changed");
            if (vanilla.treesPerChunk > 0 || vanilla.grassPerChunk > 0 || vanilla.flowersPerChunk > 0)
                throw new IllegalStateException("Native geological flora schedule changed");
            signature.append(
                "|native-geological-skin3-v1|native-bare-surface-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|supported-shore-before-flora-v1");
            signature
                .append(glacier ? "|native-winter-v2-thin-soil-snow-cap|arctic-parent-host-v1" : "|raised-hosts-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenMountain")) {
            signature.append(
                "|native-whole-mountain-trees-v1|supported-shore-before-flora-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|raised-hosts-v1");
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt || biome.field_150604_aj != 0)
                throw new IllegalStateException("Native Mountain soil changed");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (Set.of("BiomeGenAlpsForest", "BiomeGenMeadowForest", "BiomeGenSeasonalForestClearing")
            .contains(
                biome.getClass()
                    .getSimpleName())) {
            signature.append(
                "|native-parent-variant-v1|grass-dirt-soil-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|supported-shore-before-flora-v1");
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt || biome.field_150604_aj != 0)
                throw new IllegalStateException("Native parent variant soil changed");
            if (biome.getClass()
                .getSimpleName()
                .equals("BiomeGenAlpsForest")) signature.append("|native-winter-v2-thin-soil-snow-cap");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenConiferousForest"))
            signature.append("|loamy-soil-v1|shallow-bed-max12-v1|taiga3-whole-feature-v1");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenConiferousForestSnow"))
            signature.append(
                "|loamy-soil-v1|shallow-bed-max12-v1|taiga3-whole-feature-v1|native-winter-v2-thin-soil-snow-cap");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenFrostForest"))
            signature.append("|shallow-bed-max12-v1|native-winter-v2-thin-soil-snow-cap");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenMeadow")) signature.append("|shallow-bed-max12-v1");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenTemperateRainforest"))
            signature.append("|shallow-bed-max12-v1|native-moss-floor-relative63-v1|native-swamp-tree-v1");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenDenseForest"))
            signature.append(
                "|native-surface-noise-absolute-v1|stateless-legacy-dirt1-soil-v1|shallow-bed-max12-v1|intact-native-huge-tree-v1");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenShield"))
            signature.append(
                "|native-surface-noise-absolute-v1|stateless-stone-soil-v1|shallow-bed-max12-v1|native-moss-floor-relative63-v1|intact-native-pine-v1");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenHighland")) signature.append("|native-attached-rock-before-flora-v1|raised-landforms-v1");
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenGrassland")) {
            signature.append(
                "|native-closed-water-ponds-v1|pond-floor-relative64-nested-y-v1|pond-earth-foundation8-v1|native-intact-cane-reeds-v1|shallow-bed-max12-v1|pond-before-shore-before-flora-v1|immutable-pond-horizon8-ore-reservation-v1|open-hosts-v1");
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt
                || biome.temperature != 0.6F
                || biome.field_150604_aj != 0)
                throw new IllegalStateException("Unsupported native Grassland soil/climate");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (Set.of("BiomeGenBrushland", "BiomeGenChaparral")
            .contains(
                biome.getClass()
                    .getSimpleName())) {
            signature.append(
                "|native-whole-scrub-trees-v1|native-supported-splotches-before-flora-v1|splotch-floor-relative64-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1|warm-open-hosts-v1");
            if (biome.topBlock != Blocks.grass || biome.fillerBlock != Blocks.dirt || biome.field_150604_aj != 0)
                throw new IllegalStateException("Native dry scrub soil changed");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (Set.of("BiomeGenArctic", "BiomeGenAlps")
            .contains(
                biome.getClass()
                    .getSimpleName())) {
            signature.append(
                "|native-winter-v2-thin-soil-snow-cap|native-supported-clay-v1|shallow-bed-max12-v1|immutable-native-soil-ore-reservation-v1");
            boolean alps = biome.getClass()
                .getSimpleName()
                .equals("BiomeGenAlps");
            signature.append(
                alps ? "|snow-geological-skin3-v1|frozen-mountain-hosts-v1"
                    : "|grass-dirt-geological-skin3-v1|frozen-sinkhole-hosts-v1");
            if (biome.topBlock != (alps ? Blocks.snow : Blocks.grass)
                || biome.fillerBlock != (alps ? Blocks.snow : Blocks.dirt)
                || biome.field_150604_aj != 0) throw new IllegalStateException("Native open winter soil changed");
            if (vanilla.treesPerChunk >= 0 || vanilla.flowersPerChunk >= 0
                || vanilla.grassPerChunk >= 0
                || vanilla.sandPerChunk >= 0
                || vanilla.sandPerChunk2 >= 0) throw new IllegalStateException("Native open winter ecology changed");
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
        }
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenTundra")) {
            signature.append(
                "|native-supported-shore-before-rock-and-flora-v1|shallow-bed-max12-v1|open-hosts-v1|immutable-native-soil-ore-reservation-v1");
            addShore(slots, signature, "vanilla_shore_sand", vanilla.sandPerChunk2, vanilla.sandGen);
            addShore(slots, signature, "vanilla_shore_clay", vanilla.clayPerChunk, vanilla.clayGen);
            addShore(slots, signature, "vanilla_shore_gravel", vanilla.sandPerChunk, vanilla.gravelAsSandGen);
        }
        if (quagmire || wasteland) slots.add(new Slot("trees", 0, Height.SURFACE, Kind.TREE, null, null));
        else add(
            slots,
            new Slot(
                "trees",
                count(vanilla.treesPerChunk),
                Height.SURFACE,
                orchard ? Kind.ORCHARD_TREE : Kind.TREE,
                null,
                null));
        add(
            slots,
            new Slot(
                "vanilla_flowers",
                count(vanilla.flowersPerChunk),
                Height.RANDOM_SURFACE_PLUS_32,
                Kind.VANILLA_FLOWER,
                null,
                null));
        add(
            slots,
            new Slot(
                "vanilla_grass",
                count(vanilla.grassPerChunk),
                Height.RANDOM_DOUBLE_SURFACE,
                Kind.VANILLA_GRASS,
                null,
                null));
        // The six earlier profiles retain their saved slot identities/output. New profiles include
        // their explicitly configured vanilla dead-bush/mushroom passes; not a second full decorator.
        if (lakes || canyon
            || silkglades
            || quagmire
            || sludgepit
            || wetland
            || bayou
            || lushSwamp
            || bog
            || fen
            || moor
            || deadSwamp
            || marsh
            || arid
            || lushDesert
            || origin
            || garden
            || mystic
            || fungi
            || ominous
            || deadForest
            || finalProfile
            || EXTENDED_VANILLA.contains(
                biome.getClass()
                    .getSimpleName())) {
            add(
                slots,
                new Slot(
                    "vanilla_dead_bush",
                    count(vanilla.deadBushPerChunk),
                    Height.RANDOM_DOUBLE_SURFACE,
                    Kind.DEAD_BUSH,
                    null,
                    null));
            add(
                slots,
                new Slot(
                    "vanilla_brown_mushrooms",
                    count(vanilla.mushroomsPerChunk),
                    Height.SURFACE,
                    Kind.BROWN_MUSHROOM,
                    null,
                    null));
            add(
                slots,
                new Slot(
                    "vanilla_red_mushrooms",
                    count(vanilla.mushroomsPerChunk),
                    Height.RANDOM_DOUBLE_SURFACE,
                    Kind.RED_MUSHROOM,
                    null,
                    null));
            if (wetland || bayou
                || lushSwamp
                || fen
                || mystic
                || fungi
                || humid
                || sacred
                || biome.getClass()
                    .getSimpleName()
                    .equals("BiomeGenTemperateRainforest"))
                add(
                    slots,
                    new Slot(
                        "vanilla_waterlily",
                        count(vanilla.waterlilyPerChunk),
                        Height.RANDOM_DOUBLE_SURFACE,
                        Kind.WATERLILY,
                        null,
                        null));
            else if (vanilla.waterlilyPerChunk > 0)
                throw new IllegalStateException("Unadapted configured waterlily pass");
            if (wetland || bayou
                || lushSwamp
                || tropical
                || oasis
                || tropics
                || biome.getClass()
                    .getSimpleName()
                    .equals("BiomeGenGrassland"))
                add(
                    slots,
                    new Slot(
                        "vanilla_reeds",
                        count(vanilla.reedsPerChunk),
                        Height.RANDOM_DOUBLE_SURFACE,
                        Kind.REEDS,
                        new WorldGenReed(),
                        null));
            else if (vanilla.reedsPerChunk > 0) throw new IllegalStateException("Unadapted configured vanilla reeds");
            if (!arid && !lushDesert && !oasis && vanilla.cactiPerChunk > 0
                || !fungi && vanilla.bigMushroomsPerChunk > 0)
                throw new IllegalStateException(
                    "Unadapted configured vanilla surface feature in " + biome.getClass()
                        .getSimpleName());
        }
        var names = new TreeSet<String>(
            (List<String>) features.getClass()
                .getMethod("getFeatureNames")
                .invoke(features));
        Method associated = Class.forName("biomesoplenty.common.world.generation.WorldGenFieldAssociation")
            .getMethod("getAssociatedFeature", String.class);
        for (String name : names) {
            Object value = features.getClass()
                .getField(name)
                .get(features);
            signature.append('|')
                .append(name)
                .append('=')
                .append(value);
            if (OWNED_ELSEWHERE.contains(name) || Boolean.FALSE.equals(value)
                || value instanceof Number && ((Number) value).intValue() <= 0) continue;
            if (name.equals("generatePumpkins")) {
                slots.add(new Slot(name, 1, Height.RANDOM_DOUBLE_SURFACE, Kind.PUMPKIN, null, null));
                continue;
            }
            if (quagmire && name.equals("generateQuagmire") && Boolean.TRUE.equals(value)) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopSurfaceSplotch.identity(name, generator));
                slots.add(new Slot(name, 15, Height.SURFACE_PLUS_64, Kind.SPLOTCH, generator, null));
                continue;
            }
            if (fungi && name.equals("generateMycelium") && Boolean.TRUE.equals(value)) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopSurfaceSplotch.identity(name, generator));
                slots.add(new Slot(name, 10, Height.SURFACE_CENTERED_128, Kind.SPLOTCH, generator, null));
                continue;
            }
            if ((tropical || garden || oasis || tropics) && name.equals("generateMelons")
                && Boolean.TRUE.equals(value)) {
                Object association = associated.invoke(null, name);
                Object wrapper = association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                if (!wrapper.getClass()
                    .getName()
                    .equals("biomesoplenty.common.world.forcedgenerators.MelonForcedGenerator"))
                    throw new IllegalStateException("Unaudited native melon wrapper");
                var field = wrapper.getClass()
                    .getSuperclass()
                    .getDeclaredField("worldGenerator");
                field.setAccessible(true);
                var generator = (WorldGenerator) field.get(wrapper);
                if (generator.getClass() != WorldGenMelon.class)
                    throw new IllegalStateException("Unaudited native melon generator");
                slots.add(new Slot(name, 1, Height.RANDOM_DOUBLE_SURFACE, Kind.MELON, generator, null));
                signature.append("|MelonForcedGenerator{WorldGenMelon}");
                continue;
            }
            if ((tropical || oasis
                || Set.of("BiomeGenBrushland", "BiomeGenChaparral")
                    .contains(
                        biome.getClass()
                            .getSimpleName()))
                && Set.of("generateQuicksand", "generateStoneInGrass")
                    .contains(name)
                && Boolean.TRUE.equals(value)) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopSurfaceSplotch.identity(name, generator));
                slots.add(
                    new Slot(
                        name,
                        name.equals("generateQuicksand") ? 5 : 15,
                        Height.SURFACE_PLUS_64,
                        Kind.SPLOTCH,
                        generator,
                        null));
                continue;
            }
            if ((coralReef && name.equals("generateSponge") || volcano && name.equals("generateAsh"))
                && Boolean.TRUE.equals(value)) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopSurfaceSplotch.identity(name, generator));
                slots.add(
                    new Slot(
                        name,
                        coralReef ? 5 : 10,
                        coralReef ? Height.RANDOM_64 : Height.SURFACE_CENTERED_128,
                        Kind.SPLOTCH,
                        generator,
                        null));
                continue;
            }
            if (lushDesert && name.equals("generateSand") && Boolean.TRUE.equals(value)) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopSurfaceSplotch.identity(name, generator));
                slots.add(new Slot(name, 15, Height.SURFACE_CENTERED_128, Kind.SPLOTCH, generator, null));
                continue;
            }
            if (!(value instanceof Integer)) throw new IllegalStateException("Unadapted BOP feature: " + name);
            int attempts = count((Integer) value);
            if ((wasteland && Set.of("waterLakesPerChunk", "poisonLakesPerChunk")
                .contains(name)) || (oasis || mangrove) && name.equals("waterLakesPerChunk")
                || volcano && name.equals("lavaLakesPerChunk")) {
                Object association = associated.invoke(null, name);
                Object wrapper = association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                var profile = pondProfile(simpleName, name);
                var generator = BopNativePond.unwrap(wrapper, profile);
                signature.append('|')
                    .append(BopNativePond.identity(generator, profile));
                slots.add(new Slot(name, attempts, Height.SURFACE, Kind.POND, generator, null));
                continue;
            }
            if (marine
                && Set.of("coralPerChunk", "seaweedPerChunk", "kelpPerChunk", "kelpThickPerChunk", "shortKelpPerChunk")
                    .contains(name)) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopMarineFeatures.identity(name, generator));
                slots.add(
                    new Slot(
                        name,
                        attempts,
                        generator.getClass()
                            .getSimpleName()
                            .equals("WorldGenKelp") ? Height.RANDOM_64 : Height.RANDOM_DOUBLE_SURFACE,
                        Kind.MARINE,
                        generator,
                        null));
                continue;
            }
            if (wasteland && (name.startsWith("wasteland") && name.endsWith("PerChunk"))) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopFinalSurfaceFeatures.identity(name, generator));
                slots.add(
                    new Slot(name, attempts, Height.RANDOM_DOUBLE_SURFACE, Kind.WASTELAND_FORMATION, generator, null));
                continue;
            }
            if (volcano && name.equals("lavaSpoutsPerChunk")) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopFinalSurfaceFeatures.identity(name, generator));
                slots.add(new Slot(name, attempts, Height.RANDOM_DOUBLE_SURFACE, Kind.VOLCANO_SPOUT, generator, null));
                continue;
            }
            if (silkglades && name.equals("cobwebNestsPerChunk")) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                if (attempts != 2) throw new IllegalStateException("Native Silkglades nest schedule changed");
                signature.append('|')
                    .append(BopWebNest.identity(generator));
                slots.add(new Slot(name, attempts, Height.RANDOM_DOUBLE_SURFACE, Kind.WEB_NEST, generator, null));
                continue;
            }
            if (bog && Set.of("waterLakesPerChunk", "poisonLakesPerChunk")
                .contains(name)) {
                Object association = associated.invoke(null, name);
                Object wrapper = association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                var profile = name.equals("waterLakesPerChunk") ? BopNativePond.Profile.BOG_WATER
                    : BopNativePond.Profile.BOG_POISON;
                var generator = BopNativePond.unwrap(wrapper, profile);
                signature.append('|')
                    .append(BopNativePond.identity(generator, profile));
                slots.add(new Slot(name, attempts, Height.SURFACE, Kind.POND, generator, null));
                continue;
            }
            if (lushDesert && name.equals("oasesPerChunk")) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopOasisBank.identity(generator));
                slots.add(new Slot(name, attempts, Height.SURFACE, Kind.OASIS_BANK, generator, null));
                continue;
            }
            if ((lakeMarsh || bayou || moor || marsh || lushDesert) && name.equals("waterLakesPerChunk")) {
                Object association = associated.invoke(null, name);
                Object wrapper = association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                var profile = lakeMarsh ? BopNativePond.Profile.LAND_OF_LAKES_MARSH_WATER
                    : bayou ? BopNativePond.Profile.BAYOU_WATER
                        : moor ? BopNativePond.Profile.MOOR_WATER
                            : marsh ? BopNativePond.Profile.MARSH_WATER : BopNativePond.Profile.LUSH_DESERT_WATER;
                var generator = BopNativePond.unwrap(wrapper, profile);
                signature.append('|')
                    .append(BopNativePond.identity(generator, profile));
                slots.add(new Slot(name, attempts, Height.SURFACE, Kind.POND, generator, null));
                continue;
            }
            if ((arid || (quagmire || canyon) && name.equals(BopAridFeatures.GRASS))
                && BopAridFeatures.splatter(name)) {
                Object association = associated.invoke(null, name);
                var generator = (WorldGenerator) association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                signature.append('|')
                    .append(BopAridFeatures.identity(name, generator));
                slots.add(
                    new Slot(
                        name,
                        attempts,
                        name.equals(BopAridFeatures.RED_SAND) ? Height.RANDOM_256 : Height.RANDOM_DOUBLE_SURFACE,
                        Kind.SPLATTER,
                        generator,
                        null));
                continue;
            }
            if ((sludgepit || lushSwamp || ominous || mystic) && name.equals("poisonLakesPerChunk")) {
                Object association = associated.invoke(null, name);
                Object wrapper = association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                var profile = sludgepit ? BopNativePond.Profile.SLUDGEPIT_POISON
                    : lushSwamp ? BopNativePond.Profile.LUSH_SWAMP_POISON
                        : mystic ? BopNativePond.Profile.MYSTIC_POISON : BopNativePond.Profile.OMINOUS_POISON;
                var generator = BopNativePond.unwrap(wrapper, profile);
                signature.append('|')
                    .append(BopNativePond.identity(generator, profile));
                slots.add(new Slot(name, attempts, Height.SURFACE, Kind.POND, generator, null));
                continue;
            }
            if (biome.getClass()
                .getSimpleName()
                .equals("BiomeGenGrassland") && name.equals("waterLakesPerChunk")) {
                Object association = associated.invoke(null, name);
                Object wrapper = association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                var generator = BopNativePond.unwrap(wrapper);
                signature.append('|')
                    .append(BopNativePond.identity(generator));
                slots.add(new Slot(name, attempts, Height.SURFACE, Kind.POND, generator, null));
                continue;
            }
            if (name.equals("bopFlowersPerChunk") || name.equals("bopGrassPerChunk")) {
                var weighted = weighted(
                    (Map<?, ?>) features.getClass()
                        .getField(name.equals("bopFlowersPerChunk") ? "weightedFlowerGen" : "weightedGrassGen")
                        .get(features));
                for (var entry : weighted.entries()) signature.append('|')
                    .append(entry.key())
                    .append('@')
                    .append(entry.weight());
                if (!weighted.entries()
                    .isEmpty())
                    slots.add(
                        new Slot(
                            name,
                            attempts,
                            name.equals("bopFlowersPerChunk") ? Height.RANDOM_SURFACE_PLUS_32 : Height.RANDOM_256,
                            Kind.WEIGHTED,
                            null,
                            weighted));
            } else {
                Object association = associated.invoke(null, name);
                if (association == null) throw new IllegalStateException("Unbound BOP feature: " + name);
                Object generator = association.getClass()
                    .getMethod("getBOPWorldGenerator")
                    .invoke(association);
                String id = generatorIdentity((WorldGenerator) generator);
                signature.append('|')
                    .append(id);
                String type = generator.getClass()
                    .getSimpleName();
                Kind kind = type.equals("WorldGenWaterside") ? Kind.SHORE_PATCH
                    : type.equals("WorldGenLog") ? Kind.FALLEN_LOG
                        : type.equals("WorldGenBOPCoral") ? Kind.SUBMERGED_PLANT
                            : type.equals("WorldGenBOPBlob") ? Kind.ROCKPILE
                                : type.equals("WorldGenRiverCane") ? Kind.RIVER_CANE : Kind.SIMPLE;
                if (kind == Kind.ROCKPILE) BopRockpile.validateGenerator((WorldGenerator) generator);
                Height height = kind == Kind.SHORE_PATCH ? Height.SURFACE
                    : kind == Kind.FALLEN_LOG || kind == Kind.SUBMERGED_PLANT
                        || kind == Kind.ROCKPILE
                        || kind == Kind.RIVER_CANE
                        || Set.of("WorldGenBOPDoubleFlora", "WorldGenWaterReeds")
                            .contains(type) ? Height.RANDOM_DOUBLE_SURFACE : Height.RANDOM_256;
                slots.add(new Slot(name, attempts, height, kind, (WorldGenerator) generator, null));
            }
        }
        if (wetland || bayou
            || fen
            || Set.of("BiomeGenTemperateRainforest", "BiomeGenShield", "BiomeGenEucalyptusForest")
                .contains(
                    biome.getClass()
                        .getSimpleName())) {
            var moss = (WorldGenerator) Class.forName("biomesoplenty.common.world.features.WorldGenMoss")
                .getConstructor()
                .newInstance();
            signature.append('|')
                .append(generatorIdentity(moss));
            slots.add(new Slot("native_moss", 20, Height.SURFACE, Kind.MOSS, moss, null));
        }
        // Only newly admitted rock profiles change ordering: rocks cannot sever double plants
        // planted earlier, and subsequent flora sees the real stone instead of phantom grass.
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenHighland"))
            slots.sort(Comparator.comparingInt(slot -> slot.kind() == Kind.ROCKPILE ? 0 : 1));
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenTundra")) {
            for (var slot : slots) if (slot.kind() == Kind.SHORE_PATCH) signature.append('|')
                .append(BopShorePatch.identity(slot.generator()));
            slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : slot.kind() == Kind.ROCKPILE ? 1 : 2));
        }
        if (Set.of("BiomeGenBrushland", "BiomeGenChaparral")
            .contains(
                biome.getClass()
                    .getSimpleName()))
            slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : slot.kind() == Kind.SPLOTCH ? 1 : 2));
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenGrassland"))
            slots.sort(Comparator.comparingInt(slot -> slot.kind() == Kind.POND ? 0 : shore(slot) ? 1 : 2));
        if (Set.of("BiomeGenAlpsForest", "BiomeGenMeadowForest", "BiomeGenSeasonalForestClearing")
            .contains(
                biome.getClass()
                    .getSimpleName()))
            slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : 1));
        if (biome.getClass()
            .getSimpleName()
            .equals("BiomeGenMountain")) {
            for (var slot : slots) if (slot.kind() == Kind.SHORE_PATCH) signature.append('|')
                .append(BopShorePatch.identity(slot.generator()));
            slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : 1));
        }
        if (Set.of("BiomeGenGlacier", "BiomeGenCrag")
            .contains(
                biome.getClass()
                    .getSimpleName()))
            slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : 1));
        if (humid) slots
            .sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : tropical && slot.kind() == Kind.SPLOTCH ? 1 : 2));
        if (bamboo) slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : 1));
        if (redwood || jade) slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : 1));
        if (deadForest) slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : 1));
        if (ominous) slots.sort(Comparator.comparingInt(slot -> slot.kind() == Kind.POND ? 0 : shore(slot) ? 1 : 2));
        if (mystic) slots.sort(Comparator.comparingInt(slot -> slot.kind() == Kind.POND ? 0 : shore(slot) ? 1 : 2));
        if (garden) slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : 1));
        if (arid || canyon) slots.sort(
            Comparator.comparingInt(
                slot -> shore(slot) ? 0 : slot.kind() == Kind.SPLATTER ? 1 : slot.kind() == Kind.TREE ? 2 : 3));
        if (lushDesert) slots.sort(
            Comparator.comparingInt(
                slot -> slot.kind() == Kind.POND ? 0
                    : slot.kind() == Kind.SPLOTCH ? 1 : shore(slot) ? 2 : slot.kind() == Kind.OASIS_BANK ? 3 : 4));
        if (marsh || moor) {
            for (var slot : slots) if (slot.kind() == Kind.SHORE_PATCH) signature.append('|')
                .append(BopShorePatch.identity(slot.generator()));
            slots.sort(Comparator.comparingInt(slot -> slot.kind() == Kind.POND ? 0 : shore(slot) ? 1 : 2));
        }
        if (wetland || deadSwamp || fen) {
            for (var slot : slots) if (slot.kind() == Kind.SHORE_PATCH) signature.append('|')
                .append(BopShorePatch.identity(slot.generator()));
            slots.sort(Comparator.comparingInt(slot -> shore(slot) ? 0 : slot.kind() == Kind.TREE ? 1 : 2));
        }
        if (lakes || sludgepit || bayou || bog || lushSwamp) {
            for (var slot : slots) if (slot.kind() == Kind.SHORE_PATCH) signature.append('|')
                .append(BopShorePatch.identity(slot.generator()));
            slots.sort(
                Comparator.comparingInt(
                    slot -> slot.kind() == Kind.POND ? 0 : shore(slot) ? 1 : slot.kind() == Kind.TREE ? 2 : 3));
        }
        if (fungi) slots.sort(
            Comparator.comparingInt(
                slot -> slot.kind() == Kind.SPLOTCH ? 0
                    : shore(slot) ? 1 : slot.kind() == Kind.TREE ? 2 : slot.kind() == Kind.GIANT_MUSHROOM ? 3 : 4));
        if (quagmire) slots.sort(
            Comparator.comparingInt(
                slot -> shore(slot) ? 0
                    : slot.kind() == Kind.SPLOTCH ? 1
                        : slot.kind() == Kind.SPLATTER ? 2 : slot.kind() == Kind.TREE ? 3 : 4));
        if (silkglades) slots.sort(
            Comparator.comparingInt(
                slot -> shore(slot) ? 0
                    : slot.kind() == Kind.TREE ? 1
                        : slot.kind() == Kind.WEB_NEST ? 2
                            : slot.name()
                                .equals("cobwebsPerChunk") ? 3 : 4));
        if (finalProfile) slots.sort(
            Comparator.comparingInt(
                slot -> slot.kind() == Kind.POND ? 0
                    : shore(slot) ? 1
                        : slot.kind() == Kind.TREE || slot.kind() == Kind.ORCHARD_TREE ? 2
                            : slot.kind() == Kind.SPLOTCH ? 3 : slot.kind() == Kind.MARINE ? 4 : 5));
        for (var slot : slots) signature.append('|')
            .append(slot.name())
            .append(':')
            .append(slot.attempts())
            .append(':')
            .append(slot.height());
        return new Binding(biome, slots, signature.toString());
    }

    private static void add(List<Slot> slots, Slot slot) {
        if (slot.attempts() > 0) slots.add(slot);
    }

    private static void addShore(List<Slot> slots, StringBuilder signature, String name, int attempts,
        WorldGenerator generator) {
        signature.append('|')
            .append(name)
            .append('=')
            .append(BopShorePatch.identity(generator));
        add(slots, new Slot(name, count(attempts), Height.SURFACE, Kind.VANILLA_SHORE, generator, null));
    }

    private static int count(int value) {
        if (value > 2048) throw new IllegalArgumentException("Excessive BOP feature count");
        return Math.max(0, value);
    }

    private static NativeWeightedTable<WorldGenerator> weighted(Map<?, ?> map) throws ReflectiveOperationException {
        List<NativeWeightedTable.Entry<WorldGenerator>> values = new ArrayList<>();
        for (var entry : map.entrySet()) {
            var generator = (WorldGenerator) entry.getKey();
            values.add(
                new NativeWeightedTable.Entry<>(
                    generatorIdentity(generator),
                    ((Number) entry.getValue()).doubleValue(),
                    generator));
        }
        return new NativeWeightedTable<>(values);
    }

    private static String generatorIdentity(WorldGenerator generator) throws ReflectiveOperationException {
        if (generator == null || generator.getClass() != WorldGenTallGrass.class && !generator.getClass()
            .getName()
            .startsWith("biomesoplenty.common.world.features.") || !auditedFeature(generator.getClass()))
            throw new IllegalStateException("Unaudited BOP surface generator: " + generator);
        if (generator.getClass() == WorldGenTallGrass.class) {
            // Minecraft private field names differ between MCP and shipped SRG runtimes.
            // This audited generator has exactly one Block and one int, both immutable during generate.
            var values = new TreeMap<String, String>();
            for (Field field : WorldGenTallGrass.class.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                String role = field.getType() == Block.class ? "block"
                    : field.getType() == int.class ? "metadata" : null;
                if (role == null || values.containsKey(role))
                    throw new IllegalStateException("Unexpected vanilla grass generator layout");
                Object value = field.get(generator);
                values.put(
                    role,
                    value instanceof Block ? Block.blockRegistry.getNameForObject(value) : String.valueOf(value));
            }
            if (values.size() != 2) throw new IllegalStateException("Incomplete vanilla grass identity");
            return "net.minecraft.world.gen.feature.WorldGenTallGrass" + values;
        }
        if (generator.getClass()
            .getSimpleName()
            .equals("WorldGenBOPCoral")) {
            Class<?> type = generator.getClass();
            String state = NativeBlockCatalogue.encode(
                (Block) type.getField("flora")
                    .get(generator),
                type.getField("floraMeta")
                    .getInt(generator));
            if (!SEAWEED.equals(state)) throw new IllegalStateException(
                "Only fixed coral2:8 seaweed is audited; no mutable random coral selector");
        }
        var fields = new TreeMap<String, String>();
        for (Class<?> type = generator.getClass(); auditedFeature(type); type = type.getSuperclass())
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                field.setAccessible(true);
                Object value = field.get(generator);
                if (type.getSimpleName()
                    .equals("WorldGenWaterside")
                    && field.getName()
                        .equals("sideBlocks")) {
                    if (!(value instanceof List<?>blocks) || blocks.stream()
                        .anyMatch(b -> !(b instanceof Block)))
                        throw new IllegalStateException("Unexpected shoreline substrate list");
                    fields.put(
                        type.getSimpleName() + '.' + field.getName(),
                        blocks.stream()
                            .map(Block.blockRegistry::getNameForObject)
                            .toList()
                            .toString());
                    continue;
                }
                if (value != null && !(value instanceof Block)
                    && !(value instanceof Number)
                    && !(value instanceof Boolean)) throw new IllegalStateException("Unexpected BOP generator state");
                fields.put(
                    type.getSimpleName() + '.' + field.getName(),
                    value instanceof Block ? Block.blockRegistry.getNameForObject(value) : String.valueOf(value));
            }
        return generator.getClass()
            .getName() + fields;
    }

    private static boolean auditedFeature(Class<?> type) {
        return type != null && (type == WorldGenTallGrass.class || SIMPLE.contains(type.getSimpleName())
            || SURFACE_FEATURES.contains(type.getSimpleName()));
    }

    public static boolean woody(Slot slot) {
        return slot.kind() == Kind.TREE || slot.kind() == Kind.ORCHARD_TREE || slot.kind() == Kind.FALLEN_LOG;
    }

    public static BopNativePond.Profile bogPondProfile(Slot slot) {
        if (slot.kind() != Kind.POND) throw new IllegalArgumentException("Not a Bog pond slot");
        return switch (slot.name()) {
            case "waterLakesPerChunk" -> BopNativePond.Profile.BOG_WATER;
            case "poisonLakesPerChunk" -> BopNativePond.Profile.BOG_POISON;
            default -> throw new IllegalArgumentException("Unaudited Bog pond slot");
        };
    }

    public static BopNativePond.Profile pondProfile(String habitat, Slot slot) {
        if (slot.kind() != Kind.POND) throw new IllegalArgumentException("Not a native pond slot");
        if (BOG.equals(habitat)) return bogPondProfile(slot);
        return pondProfile(CLASSES.get(habitat), slot.name());
    }

    private static BopNativePond.Profile pondProfile(String simpleName, String feature) {
        if ("BiomeGenWasteland".equals(simpleName))
            return feature.equals("waterLakesPerChunk") ? BopNativePond.Profile.WASTELAND_WATER
                : BopNativePond.Profile.WASTELAND_POISON;
        if ("BiomeGenMangrove".equals(simpleName)) return BopNativePond.Profile.MANGROVE_WATER;
        if ("BiomeGenOasis".equals(simpleName)) return BopNativePond.Profile.OASIS_WATER;
        if ("BiomeGenVolcano".equals(simpleName)) return BopNativePond.Profile.VOLCANO_LAVA;
        return switch (simpleName) {
            case "BiomeGenLandOfLakesMarsh" -> BopNativePond.Profile.LAND_OF_LAKES_MARSH_WATER;
            case "BiomeGenSludgepit" -> BopNativePond.Profile.SLUDGEPIT_POISON;
            case "BiomeGenBayou" -> BopNativePond.Profile.BAYOU_WATER;
            case "BiomeGenLushSwamp" -> BopNativePond.Profile.LUSH_SWAMP_POISON;
            case "BiomeGenMoor" -> BopNativePond.Profile.MOOR_WATER;
            case "BiomeGenMarsh" -> BopNativePond.Profile.MARSH_WATER;
            case "BiomeGenLushDesert" -> BopNativePond.Profile.LUSH_DESERT_WATER;
            case "BiomeGenOminousWoods" -> BopNativePond.Profile.OMINOUS_POISON;
            case "BiomeGenMysticGrove" -> BopNativePond.Profile.MYSTIC_POISON;
            default -> BopNativePond.Profile.GRASSLAND_WATER;
        };
    }

    public static boolean shore(Slot slot) {
        return slot.kind() == Kind.SHORE_PATCH || slot.kind() == Kind.VANILLA_SHORE;
    }

    /** A native feature that reaches outside its audited envelope is rejected whole (see NativeFeatureCapture). */
    public static boolean place(String key, Slot slot, World world, NativeFeatureCapture.Access access, Random random,
        int x, int y, int z) {
        try {
            return placeAudited(key, slot, world, access, random, x, y, z);
        } catch (NativeFeatureCapture.OutsideEnvelope rejected) {
            return false;
        }
    }

    private static boolean placeAudited(String key, Slot slot, World world, NativeFeatureCapture.Access access,
        Random random, int x, int y, int z) {
        Binding binding = binding(key);
        if (LAND_OF_LAKES_MARSH.equals(key) && slot.kind() == Kind.POND) return BopNativePond.place(
            world,
            access,
            random,
            slot.generator(),
            binding.biome,
            x,
            y,
            z,
            BopNativePond.Profile.LAND_OF_LAKES_MARSH_WATER);
        if (landOfLakes(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (tree.getClass() != WorldGenTaiga2.class && !tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenOriginalTree"))
                throw new IllegalStateException("Unaudited native Land of Lakes tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (SILKGLADES.equals(key) && slot.kind() == Kind.WEB_NEST)
            return BopWebNest.place(world, access, random, slot.generator(), x, y, z);
        if (slot.kind() == Kind.ORCHARD_TREE)
            throw new IllegalStateException("Orchard tree selection requires the saved single-provider runtime");
        if (slot.kind() == Kind.MARINE)
            return BopMarineFeatures.place(world, access, random, slot.name(), slot.generator(), x, y, z);
        if (slot.kind() == Kind.WASTELAND_FORMATION || slot.kind() == Kind.VOLCANO_SPOUT)
            return BopFinalSurfaceFeatures.place(world, access, random, slot.name(), slot.generator(), x, y, z);
        if (slot.kind() == Kind.TREE && WASTELAND.equals(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            if (!tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenDeadTree"))
                throw new IllegalStateException("Unaudited native Wasteland tree");
            return BopWholeTree.placeFinal(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && SACRED_SPRINGS.equals(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            if (tree.getClass() != WorldGenShrub.class && !tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenSacredOak"))
                throw new IllegalStateException("Unaudited native Sacred Springs tree");
            return BopWholeTree.placeFinal(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && Set.of(OASIS, SCRUBLAND, SPRUCE_WOODS, MANGROVE, TROPICS)
            .contains(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            String type = tree.getClass()
                .getName();
            Set<String> expected = OASIS.equals(key)
                ? Set.of("biomesoplenty.common.world.features.trees.WorldGenPalmTree1")
                : SCRUBLAND.equals(key) ? Set
                    .of(WorldGenShrub.class.getName(), "biomesoplenty.common.world.features.trees.WorldGenOriginalTree")
                    : SPRUCE_WOODS.equals(key) ? Set.of(WorldGenTrees.class.getName(), WorldGenTaiga2.class.getName())
                        : MANGROVE.equals(key)
                            ? Set.of(
                                "biomesoplenty.common.world.features.trees.WorldGenBOPShrub",
                                "biomesoplenty.common.world.features.trees.WorldGenMangrove")
                            : Set.of(
                                "biomesoplenty.common.world.features.trees.WorldGenPalmTree1",
                                "biomesoplenty.common.world.features.trees.WorldGenTropicsShrub",
                                WorldGenShrub.class.getName());
            if (!expected.contains(type))
                throw new IllegalStateException("Unaudited native final-profile tree: " + type);
            if (tree.getClass() == WorldGenTrees.class) tree = new WorldGenTrees(false);
            if (TROPICS.equals(key)) return BopHumidTree.placeFinal(world, access, random, tree, x, y, z);
            return BopWholeTree.placeFinal(world, access, random, tree, x, y, z);
        }
        if (SILKGLADES.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "biomesoplenty.common.world.features.trees.WorldGenBOPSwampTree",
                    "biomesoplenty.common.world.features.trees.WorldGenDeadTree")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Native Silkglades tree changed");
            return BopWholeTree.place(world, access, random, tree, x, y, z, 16);
        }
        if (QUAGMIRE.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenDeadTree"))
                throw new IllegalStateException("Native Quagmire tree changed");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (SLUDGEPIT.equals(key) && slot.kind() == Kind.POND) return BopNativePond.place(
            world,
            access,
            random,
            slot.generator(),
            binding.biome,
            x,
            y,
            z,
            BopNativePond.Profile.SLUDGEPIT_POISON);
        if (SLUDGEPIT.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "biomesoplenty.common.world.features.trees.WorldGenBogTree1",
                    "biomesoplenty.common.world.features.trees.WorldGenBogTree2")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Unaudited native Sludgepit tree family");
            return BopHumidTree.placeKeepingCanopy(world, access, random, tree, x, y, z, Blocks.vine);
        }
        if (WETLAND.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "biomesoplenty.common.world.features.trees.WorldGenBOPTaiga2",
                    "biomesoplenty.common.world.features.trees.WorldGenBOPSwampTree")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Unaudited native Wetland tree family");
            // This swamp tree hangs actual leaves, not face-mask vines. Preserve the
            // complete native geometry and metadata instead of running a vine filter.
            return BopWholeTree.place(world, access, random, tree, x, y, z, 16);
        }
        if (BAYOU.equals(key) && slot.kind() == Kind.POND) return BopNativePond
            .place(world, access, random, slot.generator(), binding.biome, x, y, z, BopNativePond.Profile.BAYOU_WATER);
        if (BAYOU.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "biomesoplenty.common.world.features.trees.WorldGenBayou1",
                    "biomesoplenty.common.world.features.trees.WorldGenBayou2",
                    "biomesoplenty.common.world.features.trees.WorldGenBayou3")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Unaudited native Bayou tree family");
            return BopHumidTree.placeKeepingCanopy(
                world,
                access,
                random,
                tree,
                x,
                y,
                z,
                NativeBlockCatalogue.resolve("biomesoplenty:willow[legacy_meta=1]")
                    .block());
        }
        if (LUSH_SWAMP.equals(key) && slot.kind() == Kind.POND) return BopNativePond.place(
            world,
            access,
            random,
            slot.generator(),
            binding.biome,
            x,
            y,
            z,
            BopNativePond.Profile.LUSH_SWAMP_POISON);
        if (LUSH_SWAMP.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenBOPSwampTree"))
                throw new IllegalStateException("Unaudited native Lush Swamp tree family");
            return BopHumidTree.placeKeepingCanopy(
                world,
                access,
                random,
                tree,
                x,
                y,
                z,
                NativeBlockCatalogue.resolve("biomesoplenty:ivy[legacy_meta=1]")
                    .block());
        }
        if (BOG.equals(key) && slot.kind() == Kind.POND) return BopNativePond
            .place(world, access, random, slot.generator(), binding.biome, x, y, z, bogPondProfile(slot));
        if (BOG.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "biomesoplenty.common.world.features.trees.WorldGenCypress",
                    "biomesoplenty.common.world.features.trees.WorldGenBogBush")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Unaudited native Bog tree family");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if ((canyon(key) || arid(key) || QUAGMIRE.equals(key)) && slot.kind() == Kind.SPLATTER)
            return BopAridFeatures.placeSplatter(world, access, random, slot.name(), slot.generator(), x, y, z);
        if (canyon(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "biomesoplenty.common.world.features.trees.WorldGenPineTree",
                    "biomesoplenty.common.world.features.trees.WorldGenBOPShrub")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Unaudited native Canyon tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if ((arid(key) || LUSH_DESERT.equals(key) || OASIS.equals(key)) && slot.kind() == Kind.CACTUS)
            return BopAridFeatures.placeCactus(world, access, random, x, y, z);
        if (LUSH_DESERT.equals(key) && slot.kind() == Kind.OASIS_BANK)
            return BopOasisBank.place(world, access, random, slot.generator(), x, y, z);
        if (LUSH_DESERT.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "net.minecraft.world.gen.feature.WorldGenTrees",
                    "net.minecraft.world.gen.feature.WorldGenShrub",
                    "biomesoplenty.common.world.features.trees.WorldGenCypress",
                    "biomesoplenty.common.world.features.trees.WorldGenDeadTree")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Unaudited Lush Desert tree family");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (OUTBACK.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!Set
                .of(
                    "biomesoplenty.common.world.features.trees.WorldGenMiniShrub",
                    "biomesoplenty.common.world.features.trees.WorldGenBOPShrub")
                .contains(
                    tree.getClass()
                        .getName()))
                throw new IllegalStateException("Unaudited native Outback shrub");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (ORIGIN_VALLEY.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (!tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenOriginalTree"))
                throw new IllegalStateException("Unaudited native Origin Valley tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (GARDEN.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (tree.getClass() == WorldGenShrub.class)
                return BopWholeTree.place(world, BopGiantFlower.preservingPetals(access), random, tree, x, y, z);
            return BopGiantFlower.place(world, access, random, tree, x, y, z);
        }
        if (MYSTIC_GROVE.equals(key) && slot.kind() == Kind.TREE) {
            var selected = binding.biome.func_150567_a(random);
            var tree = selected.getClass() == WorldGenBigTree.class ? new WorldGenBigTree(false) : selected;
            if (tree.getClass() != WorldGenBigTree.class && (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !Set.of("WorldGenOriginalTree", "WorldGenBOPSwampTree", "WorldGenMixedTree")
                    .contains(
                        tree.getClass()
                            .getSimpleName())))
                throw new IllegalStateException("Unaudited native Mystic Grove tree");
            return BopHumidTree.place(
                world,
                access,
                random,
                tree,
                x,
                y,
                z,
                NativeBlockCatalogue.resolve("biomesoplenty:flowervine[legacy_meta=1]")
                    .block());
        }
        if (FUNGI_FOREST.equals(key) && slot.kind() == Kind.GIANT_MUSHROOM)
            return BopGiantMushroom.place(world, access, random, slot.generator(), x, y, z);
        if (FUNGI_FOREST.equals(key) && slot.kind() == Kind.TREE) {
            var tree = binding.biome.func_150567_a(random);
            if (tree.getClass() != WorldGenShrub.class && !tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenBOPJungle"))
                throw new IllegalStateException("Unaudited native Fungi Forest tree");
            return BopHumidTree.place(
                world,
                access,
                random,
                tree,
                x,
                y,
                z,
                NativeBlockCatalogue.resolve("biomesoplenty:ivy[legacy_meta=1]")
                    .block());
        }
        if (slot.kind() == Kind.TREE && OMINOUS_WOODS.equals(key)) {
            var tree = binding.biome.func_150567_a(random);
            if (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !Set.of("WorldGenDeadTree", "WorldGenBOPSwampTree", "WorldGenBOPTaiga2")
                    .contains(
                        tree.getClass()
                            .getSimpleName()))
                throw new IllegalStateException("Unaudited native Ominous tree");
            return BopHumidTree.place(
                world,
                access,
                random,
                tree,
                x,
                y,
                z,
                NativeBlockCatalogue.resolve("biomesoplenty:treemoss[legacy_meta=1]")
                    .block());
        }
        if (slot.kind() == Kind.TREE && FEN.equals(key)) {
            var tree = binding.biome.func_150567_a(random);
            if (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !Set.of("WorldGenBOPTaiga1", "WorldGenBOPTaiga2", "WorldGenDeadTree")
                    .contains(
                        tree.getClass()
                            .getSimpleName()))
                throw new IllegalStateException("Unaudited native Fen tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && (DEAD_FOREST.equals(key) || DEAD_SWAMP.equals(key))) {
            var tree = binding.biome.func_150567_a(random);
            if (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !(DEAD_SWAMP.equals(key) ? Set.of("WorldGenDeadTree")
                    : Set.of("WorldGenDeadTree", "WorldGenBOPTaiga2", "WorldGenOriginalTree")).contains(
                        tree.getClass()
                            .getSimpleName()))
                throw new IllegalStateException("Unaudited native Dead Forest tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && largeForestBatch(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            var types = REDWOOD.equals(key)
                ? Set.of("WorldGenRedwoodTree", "WorldGenRedwoodTree2", "WorldGenRedwoodTree3")
                : Set.of("WorldGenPineTree");
            if (tree.getClass() != WorldGenShrub.class && (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !types.contains(
                    tree.getClass()
                        .getSimpleName())))
                throw new IllegalStateException("Unaudited native large-forest tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && BAMBOO_FOREST.equals(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            if (tree.getClass() != WorldGenShrub.class && !tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenBulbTree"))
                throw new IllegalStateException("Unaudited native bamboo tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && humidForest(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            if (RAINFOREST.equals(key) && tree.getClass() == WorldGenBigTree.class) tree = new WorldGenBigTree(false);
            // Tropical's selector creates a fresh, parameterized jungle tree. Keep its height,
            // jungle wood/leaves and vine flag; the generic default constructor loses all three.
            else if (TROPICAL_RAINFOREST.equals(key) && tree.getClass() == WorldGenTrees.class) {} else {
                Set<String> expected = EUCALYPTUS.equals(key) ? Set.of("WorldGenBulbTree", "WorldGenBOPShrub")
                    : TROPICAL_RAINFOREST.equals(key) ? Set.of("WorldGenRainforestTree1")
                        : Set.of("WorldGenOriginalTree", "WorldGenMixedTree");
                if (!tree.getClass()
                    .getName()
                    .startsWith("biomesoplenty.common.world.features.trees.")
                    || !expected.contains(
                        tree.getClass()
                            .getSimpleName()))
                    throw new IllegalStateException("Unaudited native humid tree");
            }
            return BopHumidTree.place(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.POND) return BopNativePond
            .place(world, access, random, slot.generator(), binding.biome, x, y, z, pondProfile(key, slot));
        if (slot.kind() == Kind.REEDS || slot.kind() == Kind.RIVER_CANE) return BopNativeCane
            .place(world, access, random, slot.generator(), slot.kind() == Kind.RIVER_CANE, x, y, z);
        if (slot.kind() == Kind.SPLOTCH)
            return BopSurfaceSplotch.place(world, access, random, slot.name(), slot.generator(), x, y, z);
        if (slot.kind() == Kind.ROCKPILE) return BopRockpile.place(world, access, random, slot.generator(), x, y, z);
        if (reservesOreSubstrate(key) && shore(slot))
            return BopShorePatch.place(world, access, random, slot.generator(), x, y, z);
        if (slot.kind() == Kind.TREE && MOUNTAIN.equals(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            if (tree.getClass() == WorldGenTrees.class) tree = new WorldGenTrees(false);
            else if (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !Set.of("WorldGenOriginalTree", "WorldGenPineTree")
                    .contains(
                        tree.getClass()
                            .getSimpleName()))
                throw new IllegalStateException("Unaudited native Mountain tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && dryScrub(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            Set<String> expected = BRUSHLAND.equals(key)
                ? Set.of("WorldGenBrush1", "WorldGenBrush2", "WorldGenMiniShrub")
                : Set.of("WorldGenMiniShrub", "WorldGenBOPShrub", "WorldGenChaparral3");
            if (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !expected.contains(
                    tree.getClass()
                        .getSimpleName()))
                throw new IllegalStateException("Unaudited native scrub tree");
            return BopWholeTree.place(world, access, random, tree, x, y, z);
        }
        if (slot.kind() == Kind.TREE && patchySoil(key)) {
            WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
            if (tree.getClass()
                .getName()
                .equals("biomesoplenty.common.world.features.trees.WorldGenBOPHugeTree")
                || tree.getClass()
                    .getName()
                    .equals("biomesoplenty.common.world.features.trees.WorldGenPineTree"))
                return BopWholeTree.place(world, access, random, tree, x, y, z);
            if (tree.getClass() != WorldGenShrub.class && (!tree.getClass()
                .getName()
                .startsWith("biomesoplenty.common.world.features.trees.")
                || !TREES.contains(
                    tree.getClass()
                        .getSimpleName())))
                throw new IllegalStateException("Unaudited BOP tree: " + tree.getClass());
            return NativeFeatureCapture.run(world, access, () -> {
                tree.setScale(1, 1, 1);
                return tree.generate(world, random, x, y, z);
            });
        }
        if (slot.kind() == Kind.MOSS) {
            if (!(access instanceof NativeFeatureCapture.SurfaceAccess surface))
                throw new IllegalArgumentException("Moss needs isolated surface access");
            // Native scan is58..79 around sea-level soil63. Move its whole view, not just its origin.
            var translated = new BopSurfaceYOffset(surface, y - 64);
            return NativeFeatureCapture.run(
                world,
                translated,
                () -> slot.generator()
                    .generate(world, random, x, 58, z));
        }
        return NativeFeatureCapture.run(world, access, () -> {
            WorldGenerator generator;
            switch (slot.kind()) {
                case TREE -> {
                    WorldGenAbstractTree tree = binding.biome.func_150567_a(random);
                    if (tree.getClass() == WorldGenBigTree.class) tree = new WorldGenBigTree(false);
                    else if (tree.getClass() == WorldGenTrees.class) tree = new WorldGenTrees(false);
                    // Deciduous Forest's two shrub variants are freshly constructed by its native selector.
                    else if (tree.getClass() == WorldGenShrub.class) { /* no shared mutable generator state */ } else
                        if (!tree.getClass()
                            .getName()
                            .startsWith("biomesoplenty.common.world.features.trees.")
                            || !TREES.contains(
                                tree.getClass()
                                    .getSimpleName()))
                            throw new IllegalStateException("Unaudited BOP tree: " + tree.getClass());
                    tree.setScale(1, 1, 1);
                    return tree.generate(world, random, x, y, z);
                }
                case VANILLA_GRASS -> generator = binding.biome.getRandomWorldGenForGrass(random);
                case VANILLA_FLOWER -> {
                    String flower = binding.biome.func_150572_a(random, x, y, z);
                    var block = net.minecraft.block.BlockFlower.func_149857_e(flower);
                    var flowers = new WorldGenFlowers(block);
                    flowers.func_150550_a(block, net.minecraft.block.BlockFlower.func_149856_f(flower));
                    generator = flowers;
                }
                case WEIGHTED -> generator = slot.weights()
                    .choose(random);
                case SIMPLE, SHORE_PATCH, FALLEN_LOG, SUBMERGED_PLANT, MELON -> generator = slot.generator();
                case DEAD_BUSH -> generator = new WorldGenDeadBush(Blocks.deadbush);
                case WATERLILY -> {
                    int surfaceY = y;
                    while (surfaceY > 0 && world.isAirBlock(x, surfaceY - 1, z)) surfaceY--;
                    return new WorldGenWaterlily().generate(world, random, x, surfaceY, z);
                }
                case BROWN_MUSHROOM -> {
                    if (random.nextInt(4) != 0) return false;
                    generator = new WorldGenFlowers(Blocks.brown_mushroom);
                }
                case RED_MUSHROOM -> {
                    if (random.nextInt(8) != 0) return false;
                    generator = new WorldGenFlowers(Blocks.red_mushroom);
                }
                case PUMPKIN -> {
                    if (random.nextInt(32) != 0) return false;
                    generator = new WorldGenPumpkin();
                }
                default -> throw new IllegalStateException("Unknown BOP slot");
            }
            return generator.generate(world, random, x, y, z);
        });
    }

    public static String encodeWrite(Block block, int meta, Block below, int belowMeta) {
        if (block.hasTileEntity(meta)) throw new IllegalStateException("BOP surface tile requires a separate adapter");
        if (block == Blocks.dirt && meta == 1) return "minecraft:dirt[legacy_meta=1]";
        String nativeState = NativeBlockCatalogue.encode(block, meta);
        if (nativeState != null) {
            Material material = block.getMaterial();
            if (!nativeState.startsWith("biomesoplenty:") || material != Material.wood && material != Material.leaves
                && material != Material.plants
                && material != Material.vine)
                throw new IllegalStateException("Unexpected BOP surface material: " + nativeState);
            return nativeState;
        }
        if (block != Blocks.dirt && block != Blocks.grass
            && block != Blocks.log
            && block != Blocks.log2
            && block != Blocks.leaves
            && block != Blocks.leaves2
            && block != Blocks.tallgrass
            && block != Blocks.double_plant
            && block != Blocks.red_flower
            && block != Blocks.yellow_flower
            && block != Blocks.pumpkin
            && block != Blocks.deadbush
            && block != Blocks.brown_mushroom
            && block != Blocks.red_mushroom)
            throw new IllegalStateException(
                "Unexpected native BOP write: " + Block.blockRegistry.getNameForObject(block) + ':' + meta);
        String state = ModernStateCompatibility.modernState(block, meta, below, belowMeta);
        if ((block == Blocks.log || block == Blocks.log2) && (meta & 12) != 12)
            state += "[axis=" + ((meta & 12) == 4 ? "x" : (meta & 12) == 8 ? "z" : "y") + "]";
        if (block == Blocks.pumpkin)
            state += "[facing=" + new String[] { "south", "west", "north", "east" }[meta & 3] + "]";
        return state;
    }

    /** Shoreline replacement is not a general license for tree/plant generators to write geology. */
    public static String encodeWrite(String habitat, Slot slot, Block block, int meta, Block below, int belowMeta) {
        if (finalBatch(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, pondProfile(habitat, slot));
        if (slot.kind() == Kind.MARINE) return BopMarineFeatures.encode(slot.name(), slot.generator(), block, meta);
        if (slot.kind() == Kind.WASTELAND_FORMATION || slot.kind() == Kind.VOLCANO_SPOUT)
            return BopFinalSurfaceFeatures.encode(slot.name(), slot.generator(), block, meta);
        if (LAND_OF_LAKES_MARSH.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.LAND_OF_LAKES_MARSH_WATER);
        if (SILKGLADES.equals(habitat) && (slot.kind() == Kind.WEB_NEST || slot.name()
            .equals("cobwebsPerChunk"))) {
            if (block != Blocks.web || meta != 0)
                throw new IllegalStateException("Native Silkglades web state changed");
            return "minecraft:cobweb";
        }
        if (SLUDGEPIT.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.SLUDGEPIT_POISON);
        if (SLUDGEPIT.equals(habitat) && slot.kind() == Kind.TREE && block == Blocks.vine)
            return BopTreeAttachments.vineState(meta);
        if (BAYOU.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.BAYOU_WATER);
        if (LUSH_SWAMP.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.LUSH_SWAMP_POISON);
        if (BOG.equals(habitat) && slot.kind() == Kind.TREE && block == Blocks.leaves && meta == 4)
            return "minecraft:oak_leaves[persistent=true]";
        if (BOG.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, bogPondProfile(slot));
        if (MARSH.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.MARSH_WATER);
        if (nativeMudBanks(habitat) && slot.kind() == Kind.SHORE_PATCH
            && slot.name()
                .equals("mudPerChunk")) {
            if (meta != 0 || !MUD.equals(NativeBlockCatalogue.encode(block, meta)))
                throw new IllegalStateException("Native Marsh bank changed material");
            return MUD;
        }
        if (LUSH_DESERT.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.LUSH_DESERT_WATER);
        if (FUNGI_FOREST.equals(habitat) && slot.kind() == Kind.GIANT_MUSHROOM)
            return BopGiantMushroom.encode(block, meta);
        if (OMINOUS_WOODS.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.OMINOUS_POISON);
        if (MYSTIC_GROVE.equals(habitat) && slot.kind() == Kind.POND)
            return BopNativePond.encode(block, meta, BopNativePond.Profile.MYSTIC_POISON);
        if (TROPICAL_RAINFOREST.equals(habitat) && slot.kind() == Kind.TREE && block == Blocks.air && meta == 0)
            return "minecraft:air"; // Native branched tree's deliberate hollow; not a generic terrain carve.
        if (TROPICS.equals(habitat) && slot.kind() == Kind.TREE && block == Blocks.cocoa)
            return BopTreeAttachments.cocoaState(meta);
        if (humidForest(habitat) && slot.kind() == Kind.TREE) {
            if (block == Blocks.vine) return BopTreeAttachments.vineState(meta);
            if (block == Blocks.cocoa) return BopTreeAttachments.cocoaState(meta);
        }
        return encodeWrite(slot, block, meta, below, belowMeta);
    }

    public static String encodeWrite(Slot slot, Block block, int meta, Block below, int belowMeta) {
        if (slot.kind() == Kind.OASIS_BANK) {
            if (block != Blocks.grass || meta != 0)
                throw new IllegalStateException("Native oasis bank changed block/metadata");
            return "minecraft:grass_block[snowy=false]";
        }
        if (slot.kind() == Kind.SPLATTER)
            return BopAridFeatures.encode(slot.name(), slot.generator(), block, meta, below, belowMeta);
        if (slot.kind() == Kind.CACTUS) {
            if (block != Blocks.cactus || meta != 0)
                throw new IllegalStateException("Native cactus changed block/metadata");
            return "minecraft:cactus[age=0]";
        }
        if (slot.kind() == Kind.MELON) {
            if (block != Blocks.melon_block || meta != 0 || below != Blocks.grass)
                throw new IllegalStateException("Native melon lost its source block/grass footing");
            return "minecraft:melon";
        }
        if (slot.kind() == Kind.POND) return BopNativePond.encode(block, meta);
        if (slot.kind() == Kind.REEDS || slot.kind() == Kind.RIVER_CANE)
            return BopNativeCane.encode(block, meta, slot.kind() == Kind.RIVER_CANE);
        if (slot.kind() == Kind.SPLOTCH) return BopSurfaceSplotch.encode(slot.name(), slot.generator(), block, meta);
        if (slot.kind() == Kind.VANILLA_SHORE) return BopShorePatch.encode(slot.generator(), block, meta);
        if (slot.kind() == Kind.ROCKPILE) {
            if (meta != 0 || block != Blocks.cobblestone)
                throw new IllegalStateException("Unaudited native rock material");
            return "minecraft:cobblestone";
        }
        if (slot.kind() == Kind.WATERLILY) {
            if (block != Blocks.waterlily || meta != 0)
                throw new IllegalStateException("Unaudited vanilla waterlily state");
            return "minecraft:lily_pad";
        }
        if (slot.kind() == Kind.MOSS) {
            String state = NativeBlockCatalogue.encode(block, meta);
            if (!("biomesoplenty:moss[legacy_meta=" + meta + "]").equals(state) || Integer.bitCount(meta) != 1
                || meta > 8
                || meta < 1) throw new IllegalStateException("Unaudited native moss attachment state");
            return state;
        }
        if (slot.kind() == Kind.SHORE_PATCH) {
            if (block != Blocks.gravel || meta != 0) throw new IllegalStateException("Unaudited BOP shore material");
            return "minecraft:gravel";
        }
        String nativeState = NativeBlockCatalogue.encode(block, meta);
        if (slot.kind() == Kind.TREE && LOAMY_DIRT.equals(nativeState)) return nativeState;
        if (slot.kind() == Kind.SUBMERGED_PLANT) {
            if (!SEAWEED.equals(nativeState)) throw new IllegalStateException("Unaudited native submerged plant");
            return nativeState;
        }
        return encodeWrite(block, meta, below, belowMeta);
    }
}
