package industries.angelic.biomic.biome.ru;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import industries.angelic.biomic.api.BiomeKeys;
import industries.angelic.biomic.api.BiomeTags;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition.Dimension;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition.IdSlot;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition.SurfaceStyle;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition.TreeStyle;

/** Completed RU vertical slices, including their one-to-one Origen terrain hosts. */
public final class RuBiomeCatalogue {

    public static final RuBiomeDefinition CLOVER_PLAINS = RuBiomeDefinition
        .builder(BiomeKeys.CLOVER_PLAINS, "Clover Plains", IdSlot.CLOVER_PLAINS)
        .terrain(OrigenBiomeProfile.FLOWERING_FOREST, 0.10F, 0.20F)
        .climate(1.0F, 0.30F)
        .colors(0xA3C563, 0xA3C563, 0xA3C563, 0x3F76E4, 0x79A6FF)
        .tags(BiomeTags.SURFACE, BiomeTags.PLAINS, BiomeTags.FLORAL, BiomeTags.WARM)
        .naturalSpawn()
        .villageLowland()
        .build();

    public static final RuBiomeDefinition WISTERIA_GROVE = RuBiomeDefinition
        .builder(BiomeKeys.WISTERIA_GROVE, "Wisteria Grove", IdSlot.WISTERIA_GROVE)
        .terrain(OrigenBiomeProfile.CANOPY_CASCADES, 0.18F, 0.45F)
        .climate(1.10F, 0.85F)
        .colors(0x72B788, 0x72B788, 0xADAAFF, 0x54A179, -1)
        .tags(BiomeTags.SURFACE, BiomeTags.FOREST, BiomeTags.FLORAL, BiomeTags.WARM, BiomeTags.HUMID)
        .decoration(TreeStyle.WISTERIA, 3, 12, 5)
        .naturalSpawn()
        .build();

    public static final RuBiomeDefinition PINE_SLOPES = RuBiomeDefinition
        .builder(BiomeKeys.PINE_SLOPES, "Pine Slopes", IdSlot.PINE_SLOPES)
        .terrain(OrigenBiomeProfile.SNOWY_MOUNTAINS, 0.35F, 0.55F)
        .climate(0.40F, 0.40F)
        .colors(0x84A75A, 0x84A75A, 0x758646, 0x3F76E4, 0x7CA4FF)
        .tags(BiomeTags.SURFACE, BiomeTags.MOUNTAIN, BiomeTags.FOREST, BiomeTags.COLD)
        .decoration(TreeStyle.PINE, 11, 0, 4)
        .surface(SurfaceStyle.COARSE_DIRT)
        .build();

    public static final RuBiomeDefinition GRASSY_BEACH = RuBiomeDefinition
        .builder(BiomeKeys.GRASSY_BEACH, "Grassy Beach", IdSlot.GRASSY_BEACH)
        .terrain(OrigenBiomeProfile.PALM_BEACH, -0.10F, 0.10F)
        .climate(0.85F, 0.75F)
        .colors(0xD6D579, 0xD6D579, 0x77B14D, 0x3F76E4, 0x78A7FF)
        .tags(BiomeTags.SURFACE, BiomeTags.COAST, BiomeTags.WARM, BiomeTags.HUMID)
        .decoration(TreeStyle.PALM, 1, 0, 0)
        .surface(SurfaceStyle.SAND)
        .naturalSpawn()
        .villageLowland()
        .build();

    public static final RuBiomeDefinition CHALK_CLIFFS = RuBiomeDefinition
        .builder(BiomeKeys.CHALK_CLIFFS, "Chalk Cliffs", IdSlot.CHALK_CLIFFS)
        .terrain(OrigenBiomeProfile.MARINE_MONOLITHS, 0.65F, 0.75F)
        .climate(0.775F, 0.70F)
        .colors(0x7E9A63, 0xA3C563, 0x7F998E, 0x3F76E4, -1)
        .tags(BiomeTags.SURFACE, BiomeTags.COAST, BiomeTags.MOUNTAIN, BiomeTags.TEMPERATE, BiomeTags.HUMID)
        .decoration(TreeStyle.CHALK_BUSH, 1, 5, 3)
        .surface(SurfaceStyle.CHALK)
        .build();

    public static final RuBiomeDefinition TOWERING_CLIFFS = RuBiomeDefinition
        .builder(BiomeKeys.TOWERING_CLIFFS, "Towering Cliffs", IdSlot.TOWERING_CLIFFS)
        .terrain(OrigenBiomeProfile.VERTICAL_VISTAS, 1.0F, 1.20F)
        .climate(0.75F, 0.80F)
        .colors(0x7E9052, 0x7E9052, 0x8FA960, 0x3F76E4, -1)
        .tags(BiomeTags.SURFACE, BiomeTags.MOUNTAIN, BiomeTags.FOREST, BiomeTags.TEMPERATE, BiomeTags.HUMID)
        .decoration(TreeStyle.TOWERING_PINE, 2, 2, 6)
        .surface(SurfaceStyle.TOWERING_CLIFFS)
        .naturalSpawn()
        .build();

    public static final RuBiomeDefinition REDWOODS = RuBiomeDefinition
        .builder(BiomeKeys.REDWOODS, "Redwoods", IdSlot.REDWOODS)
        .terrain(OrigenBiomeProfile.TALL_TIMBERLAND, 0.35F, 0.55F)
        .climate(1.0F, 0.80F)
        .colors(0x7B9F39, 0x7B9F39, 0x7E9539, 0x3F76E4, -1)
        .tags(BiomeTags.SURFACE, BiomeTags.FOREST, BiomeTags.TEMPERATE, BiomeTags.HUMID)
        .decoration(TreeStyle.REDWOOD, 4, 2, 20)
        .surface(SurfaceStyle.PODZOL)
        .naturalSpawn()
        .build();

    public static final RuBiomeDefinition BAYOU = RuBiomeDefinition.builder(BiomeKeys.BAYOU, "Bayou", IdSlot.BAYOU)
        .terrain(OrigenBiomeProfile.MURKY_MARSHLANDS, -0.10F, 0.10F)
        .climate(1.0F, 1.0F)
        .colors(0x7CA254, 0x7CA254, 0x71934C, 0x437C4A, 0xA2C1B5)
        .tags(BiomeTags.SURFACE, BiomeTags.SWAMP, BiomeTags.WARM, BiomeTags.HUMID)
        .decoration(TreeStyle.CYPRESS, 4, 3, 8)
        .surface(SurfaceStyle.BAYOU)
        .villageLowland()
        .build();

    public static final RuBiomeDefinition OLD_GROWTH_BAYOU = RuBiomeDefinition
        .builder(BiomeKeys.OLD_GROWTH_BAYOU, "Old Growth Bayou", IdSlot.OLD_GROWTH_BAYOU)
        .terrain(OrigenBiomeProfile.WATERY_WILDS, -0.05F, 0.18F)
        .climate(1.2F, 1.0F)
        .colors(0x71934C, 0x7CA254, 0x71934C, 0x437C4A, 0x87AD82)
        .tags(BiomeTags.SURFACE, BiomeTags.SWAMP, BiomeTags.FOREST, BiomeTags.WARM, BiomeTags.HUMID)
        .decoration(TreeStyle.GIANT_CYPRESS, 4, 4, 12)
        .surface(SurfaceStyle.OLD_GROWTH_BAYOU)
        .naturalSpawn()
        .build();

    public static final RuBiomeDefinition JOSHUA_DESERT = RuBiomeDefinition
        .builder(BiomeKeys.JOSHUA_DESERT, "Joshua Desert", IdSlot.JOSHUA_DESERT)
        .terrain(OrigenBiomeProfile.ARID_ARBORETUM, 0.15F, 0.30F)
        .climate(2.0F, 0.0F)
        .colors(0xFFEBA3, 0xFFEBA3, 0x7FAE4D, 0x3F76E4, -1)
        .tags(BiomeTags.SURFACE, BiomeTags.DESERT, BiomeTags.WARM, BiomeTags.DRY)
        .decoration(TreeStyle.JOSHUA, 4, 2, 5)
        .surface(SurfaceStyle.JOSHUA_DESERT)
        .naturalSpawn()
        .villageLowland()
        .build();

    public static final RuBiomeDefinition EUCALYPTUS_FOREST = RuBiomeDefinition
        .builder(BiomeKeys.EUCALYPTUS_FOREST, "Eucalyptus Forest", IdSlot.EUCALYPTUS_FOREST)
        .terrain(OrigenBiomeProfile.BAMBOO_BASIN, 0.20F, 0.40F)
        .climate(1.30F, 0.85F)
        .colors(0x93B536, 0x93B536, 0x86B52B, 0x3F76E4, 0x79A6FF)
        .tags(BiomeTags.SURFACE, BiomeTags.FOREST, BiomeTags.JUNGLE, BiomeTags.WARM, BiomeTags.HUMID)
        .decoration(TreeStyle.EUCALYPTUS, 24, 1, 25)
        .naturalSpawn()
        .build();

    private static final List<RuBiomeDefinition> BUILT_INS = Collections.unmodifiableList(
        Arrays.asList(
            CLOVER_PLAINS,
            WISTERIA_GROVE,
            PINE_SLOPES,
            GRASSY_BEACH,
            CHALK_CLIFFS,
            TOWERING_CLIFFS,
            REDWOODS,
            BAYOU,
            OLD_GROWTH_BAYOU,
            JOSHUA_DESERT,
            EUCALYPTUS_FOREST));
    private static final List<RuBiomeDefinition> DEFINITIONS;
    private static final List<RuBiomeDefinition> SURFACE_DEFINITIONS;
    private static final List<RuBiomeDefinition> CAVE_DEFINITIONS;
    private static final Map<String, RuBiomeDefinition> BY_KEY;

    static {
        ArrayList<RuBiomeDefinition> definitions = new ArrayList<>(BUILT_INS);
        for (RuRuntimeManifest.BiomeEntry entry : RuRuntimeManifest.biomes()) {
            if (entry.getExpansionOffset() >= 0) definitions.add(fromManifest(entry));
        }
        DEFINITIONS = Collections.unmodifiableList(definitions);
        Map<String, RuBiomeDefinition> keys = new LinkedHashMap<>();
        Map<OrigenBiomeProfile, RuBiomeDefinition> terrain = new EnumMap<>(OrigenBiomeProfile.class);
        Map<OrigenBiomeProfile, RuBiomeDefinition> caves = new EnumMap<>(OrigenBiomeProfile.class);
        ArrayList<RuBiomeDefinition> surfaceDefinitions = new ArrayList<>();
        ArrayList<RuBiomeDefinition> caveDefinitions = new ArrayList<>();
        ArrayList<RuBiomeDefinition> netherDefinitions = new ArrayList<>();
        for (RuBiomeDefinition definition : DEFINITIONS) {
            if (keys.put(definition.getBiomeKey(), definition) != null) {
                throw new IllegalStateException("Duplicate RU biome key " + definition.getBiomeKey());
            }
            OrigenBiomeProfile profile = definition.getTerrainProfile();
            if (definition.getDimension() == Dimension.SURFACE) {
                surfaceDefinitions.add(definition);
                if (profile != null && terrain.put(profile, definition) != null) {
                    throw new IllegalStateException("Multiple RU surface biomes own Origen profile " + profile);
                }
            } else if (definition.getDimension() == Dimension.CAVE) {
                caveDefinitions.add(definition);
                if (profile != null && caves.put(profile, definition) != null) {
                    throw new IllegalStateException("Multiple RU cave biomes own Origen profile " + profile);
                }
            } else {
                netherDefinitions.add(definition);
            }
        }
        if (surfaceDefinitions.size() != 55 || caveDefinitions.size() != 4 || netherDefinitions.size() != 4) {
            throw new IllegalStateException(
                "Incomplete active RU catalogue: surface=" + surfaceDefinitions
                    .size() + ", cave=" + caveDefinitions.size() + ", nether=" + netherDefinitions.size());
        }
        SURFACE_DEFINITIONS = Collections.unmodifiableList(surfaceDefinitions);
        CAVE_DEFINITIONS = Collections.unmodifiableList(caveDefinitions);
        BY_KEY = Collections.unmodifiableMap(keys);
    }

    private RuBiomeCatalogue() {}

    public static List<RuBiomeDefinition> all() {
        return DEFINITIONS;
    }

    public static List<RuBiomeDefinition> surface() {
        return SURFACE_DEFINITIONS;
    }

    public static List<RuBiomeDefinition> caves() {
        return CAVE_DEFINITIONS;
    }

    public static RuBiomeDefinition forBiomeKey(String biomeKey) {
        return biomeKey == null ? null : BY_KEY.get(biomeKey);
    }

    /**
     * A bayou is a swamp: vanilla's swamp olive for its grass and leaves, not RU's meadow green (review: "why is the
     * grass so green?").
     */
    private static final int SWAMP_OLIVE = 0x6A7039;

    private static boolean swamp(RuRuntimeManifest.BiomeEntry entry) {
        return entry.getKey()
            .contains("bayou");
    }

    private static RuBiomeDefinition fromManifest(RuRuntimeManifest.BiomeEntry entry) {
        OrigenBiomeProfile profile = entry.getTerrainProfile() == null ? null
            : OrigenBiomeProfile.valueOf(entry.getTerrainProfile());
        float rootHeight = profile == null ? 0.1F : (float) ((profile.getAuthoredBaseY() - 63.0D) / 64.0D);
        float variation = profile == null ? 0.2F : (float) Math.min(1.6D, profile.getAuthoredRelief() / 72.0D);
        RuBiomeDefinition.Builder builder = RuBiomeDefinition
            .expansionBuilder(entry.getKey(), entry.getDisplayName(), entry.getExpansionOffset())
            .climate(entry.getTemperature(), entry.getDownfall())
            .colors(
                entry.getMapColor(),
                swamp(entry) ? SWAMP_OLIVE : entry.getGrassColor(),
                swamp(entry) ? SWAMP_OLIVE : entry.getFoliageColor(),
                entry.getWaterColor(),
                entry.getSkyColor())
            .tags(
                entry.getTags()
                    .toArray(
                        new String[entry.getTags()
                            .size()]))
            .surface(SurfaceStyle.valueOf(entry.getSurfaceStyle()))
            .dimension(
                Dimension.valueOf(
                    entry.getCategory()
                        .toUpperCase(java.util.Locale.ROOT)));
        if (profile != null) builder.terrain(profile, rootHeight, variation);
        if (entry.isNaturalSpawn()) builder.naturalSpawn();
        if (entry.isVillageLowland()) builder.villageLowland();
        return builder.build();
    }
}
