package industries.angelic.biomic.biome.origen;

import industries.angelic.biomic.Biomic;

/**
 * Stable Biomic identities for every production profile under Origen's {@code biomes/rearth/variants} catalogue.
 *
 * <p>
 * Heights retain the upstream equation's authored base and principal relief before the shared 1.7 scaling pass.
 * Multi-part profiles remain distinct so province nesting can reproduce the authored border/middle/interior
 * transitions.
 * </p>
 */
public enum OrigenBiomeProfile {

    ABYSSAL_ALLEYS("Abyssal Alleys", Kind.OCEAN, Climate.TEMPERATE, OrigenTerrainFamily.ABYSSAL_OCEAN, 28, 32),
    STONEGATE_SEAS("Stonegate Seas", Kind.OCEAN, Climate.TEMPERATE, OrigenTerrainFamily.OCEAN_ARCHES, 52, 24),
    ARCTIC_ARCHES("Arctic Arches", Kind.OCEAN, Climate.COLD, OrigenTerrainFamily.OCEAN_ARCHES, 52, 24),
    ARID_ARBORETUM("Arid Arboretum", Kind.LAND, Climate.WARM, OrigenTerrainFamily.ARID_PILLARS, 73, 38),
    BADLANDS_BALCONIES("Badlands Balconies", Kind.LAND, Climate.WARM, OrigenTerrainFamily.TERRACE_MOUNTAINS, 75, 58),
    BAMBOO_BASIN("Bamboo Basin", Kind.LAND, Climate.WARM, OrigenTerrainFamily.BASIN_MOUNTAIN, 72, 58),
    BARE_BOULDERFIELDS("Bare Boulderfields", Kind.LAND, Climate.COLD, OrigenTerrainFamily.TILTED_PLATEAU, 85, 44),
    BLACK_FOREST("Black Forest", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.MOUNTAIN_CLEARINGS, 82, 55),
    SANDY_SPLITS("Sandy Splits", Kind.SPECIAL, Climate.WARM, OrigenTerrainFamily.CANYON, 73, 48),
    ICY_INCISIONS("Icy Incisions", Kind.SPECIAL, Climate.COLD, OrigenTerrainFamily.CANYON, 73, 48),
    CARVING_CREAKS("Carving Creaks", Kind.LAND, Climate.WARM, OrigenTerrainFamily.CANYON, 79, 62),
    VINE_VAULT("Vine Vault", Kind.CAVE, Climate.WARM, OrigenTerrainFamily.CAVE_VAULT, 31, 44),
    FOSSILIZED_FENLANDS("Fossilized Fenlands", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.SEGMENTED_LOWLAND, 67,
        22),
    FROSTY_FINGERS("Frosty Fingers", Kind.LAND, Climate.COLD, OrigenTerrainFamily.FROZEN_DUNES, 66, 31),
    FROZEN_FUNGI("Frozen Fungi", Kind.LAND, Climate.COLD, OrigenTerrainFamily.TILTED_PLATEAU, 71, 55),
    MOUNTAIN_MIRRORS("Mountain Mirrors", Kind.LAND, Climate.COLD, OrigenTerrainFamily.TERRACE_MOUNTAINS, 75, 58),
    GLOOMY_GORGE("Gloomy Gorge", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.ERODED_VALLEY_MOUNTAINS, 73, 75),
    ALPINE_ASCENDANCY("Alpine Ascendancy", Kind.LAND, Climate.COLD, OrigenTerrainFamily.ALPINE, 90, 190),
    INFERNO_ISLES("Inferno Isles", Kind.CAVE, Climate.WARM, OrigenTerrainFamily.CAVE_ISLANDS, 22, 50),
    CANOPY_CASCADES("Canopy Cascades", Kind.LAND, Climate.WARM, OrigenTerrainFamily.TERRACE_MOUNTAINS, 75, 58),
    LUSH_LOOPS("Lush Loops", Kind.LAND, Climate.WARM, OrigenTerrainFamily.LOOP_MOUNTAINS, 76, 54),
    MARINE_MONOLITHS("Marine Monoliths", Kind.COAST, Climate.TEMPERATE, OrigenTerrainFamily.COAST_MONOLITHS, 72, 58),
    MESA_MONUMENTS("Mesa Monuments", Kind.LAND, Climate.WARM, OrigenTerrainFamily.ARID_PILLARS, 75, 63),
    MURKY_MARSHLANDS("Murky Marshlands", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.MARSH_TERRACES, 63, 18),
    OASIS("Oasis", Kind.LAND, Climate.WARM, OrigenTerrainFamily.OASIS, 63, 14),
    PILLOW_PLAINS_INNER("Pillow Plains Inner", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.PILLOW_INNER, 65, 13),
    PILLOW_PLAINS_MIDDLE("Pillow Plains Middle", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.PILLOW_MIDDLE, 72,
        24),
    PILLOW_PLAINS_OUTER("Pillow Plains Outer", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.PILLOW_OUTER, 80, 32),
    FOLIAGE_FORTRESS_INNER("Foliage Fortress Inner", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.FORTRESS_INNER,
        126, 12),
    FOLIAGE_FORTRESS_MIDDLE("Foliage Fortress Middle", Kind.LAND, Climate.TEMPERATE,
        OrigenTerrainFamily.FORTRESS_MIDDLE, 103, 34),
    FOLIAGE_FORTRESS_OUTER("Foliage Fortress Outer", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.FORTRESS_OUTER,
        75, 61),
    TALL_TIMBERLAND("Tall Timberland", Kind.LAND, Climate.COLD, OrigenTerrainFamily.MOUNTAIN_CLEARINGS, 82, 55),
    ROCKY_REFUGE("Rocky Refuge", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.ROCKY_PILLARS, 77, 72),
    SAKURA_STREAMS("Sakura Streams", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.SUMMIT_STREAMS, 72, 76),
    SCARLET_SANCTUARY("Scarlet Sanctuary", Kind.LAND, Climate.WARM, OrigenTerrainFamily.TILTED_PLATEAU, 71, 55),
    SECLUDED_VALLEY("Secluded Valley", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.SECLUDED_INNER, 66, 17),
    SECLUDED_VALLEY_OUTER("Secluded Valley Outer", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.SECLUDED_OUTER, 83,
        44),
    SINKHOLE_FOREST("Forest Sinkhole", Kind.SPECIAL, Climate.TEMPERATE, OrigenTerrainFamily.SINKHOLE_INNER, -40, 9),
    SINKHOLE_FROZEN("Frozen Sinkhole", Kind.SPECIAL, Climate.COLD, OrigenTerrainFamily.SINKHOLE_INNER, -40, 9),
    SINKHOLE_JUNGLE("Jungle Sinkhole", Kind.SPECIAL, Climate.WARM, OrigenTerrainFamily.SINKHOLE_INNER, -40, 9),
    SINKHOLE_OUTER("Sinkhole Rim", Kind.SPECIAL, Climate.TEMPERATE, OrigenTerrainFamily.SINKHOLE_OUTER, 69, 36),
    SNOWY_SPIRES("Snowy Spires", Kind.LAND, Climate.COLD, OrigenTerrainFamily.ARID_PILLARS, 75, 63),
    WATERY_WILDS("Watery Wilds", Kind.LAND, Climate.WARM, OrigenTerrainFamily.LOWLAND_HILLS, 63, 32),
    TERRACOTTA_TOMBS("Terracotta Tombs", Kind.CAVE, Climate.WARM, OrigenTerrainFamily.CAVE_VAULT, 24, 46),
    TUNDRA_TRACKS("Tundra Tracks", Kind.LAND, Climate.COLD, OrigenTerrainFamily.LOWLAND_HILLS, 63, 32),
    VERDANT_VALLEYS("Verdant Valleys", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.VERDANT_VALLEY, 67, 38),
    VERTICAL_VISTAS("Vertical Vistas", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.TERRACE_MOUNTAINS, 75, 78),
    WHITE_WALLOWS("White Wallows", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.ERODED_VALLEY_MOUNTAINS, 73, 75),

    // Appended in upstream registry order so every previously assigned numeric ID remains stable.
    ALPINE_ASCENDANCY_RIVER("Alpine Ascendancy River", Kind.LAND, Climate.COLD, OrigenTerrainFamily.RIVER_CHANNEL, 90,
        190),
    BAD_BALCOONIES_RIVER("Badlands Balconies River", Kind.LAND, Climate.WARM, OrigenTerrainFamily.RIVER_CHANNEL, 75,
        58),
    BEACH("Beach", Kind.COAST, Climate.TEMPERATE, OrigenTerrainFamily.FLAT_ERODED, 63, 8),
    COLD_STEPPE("Cold Steppe", Kind.LAND, Climate.COLD, OrigenTerrainFamily.GENERAL, 65, 18),
    CORAL_OCEAN("Coral Ocean", Kind.OCEAN, Climate.WARM, OrigenTerrainFamily.ABYSSAL_OCEAN, 38, 22),
    DEEP_DARK("Deep Dark", Kind.CAVE, Climate.TEMPERATE, OrigenTerrainFamily.CAVE_VAULT, -42, 12),
    DEEP_OCEAN("Deep Ocean", Kind.OCEAN, Climate.TEMPERATE, OrigenTerrainFamily.ABYSSAL_OCEAN, 30, 32),
    DESERT("Desert", Kind.LAND, Climate.WARM, OrigenTerrainFamily.DUNES, 65, 18),
    DESERT_SPIKES_GOLD("Golden Desert Spikes", Kind.LAND, Climate.WARM, OrigenTerrainFamily.ARID_PILLARS, 75, 63),
    DRIPSTONE_CAVES("Dripstone Caves", Kind.CAVE, Climate.TEMPERATE, OrigenTerrainFamily.CAVE_VAULT, 24, 46),
    FLOWERING_FOREST("Flowering Forest", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.GENERAL, 67, 22),
    FROZEN_FUNGI_RIVER("Frozen Fungi River", Kind.LAND, Climate.COLD, OrigenTerrainFamily.RIVER_CHANNEL, 71, 55),
    FROZEN_OCEAN("Frozen Ocean", Kind.OCEAN, Climate.COLD, OrigenTerrainFamily.ABYSSAL_OCEAN, 42, 22),
    FROZEN_RIVER("Frozen River", Kind.LAND, Climate.COLD, OrigenTerrainFamily.RIVER_CHANNEL, 63, 16),
    GLOOMY_GORGE_RIVER("Gloomy Gorge River", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.RIVER_CHANNEL, 73, 75),
    ICEBERG_OCEAN("Iceberg Ocean", Kind.OCEAN, Climate.COLD, OrigenTerrainFamily.OCEAN_ARCHES, 44, 30),
    LOOPS_RIVER("Lush Loops River", Kind.LAND, Climate.WARM, OrigenTerrainFamily.RIVER_CHANNEL, 76, 54),
    LUSH_CAVES("Lush Caves", Kind.CAVE, Climate.WARM, OrigenTerrainFamily.CAVE_VAULT, 31, 44),
    MANGROVE_SWAMP("Mangrove Swamp", Kind.COAST, Climate.WARM, OrigenTerrainFamily.MARSH_TERRACES, 63, 14),
    MOUNTAIN_MIRRORS_RIVER("Mountain Mirrors River", Kind.LAND, Climate.COLD, OrigenTerrainFamily.RIVER_CHANNEL, 75,
        58),
    OCEAN("Ocean", Kind.OCEAN, Climate.TEMPERATE, OrigenTerrainFamily.ABYSSAL_OCEAN, 42, 22),
    OVERGROWN_CLIFFS("Overgrown Cliffs", Kind.LAND, Climate.WARM, OrigenTerrainFamily.ERODED_PILLARS, 75, 63),
    PALM_BEACH("Palm Beach", Kind.COAST, Climate.WARM, OrigenTerrainFamily.FLAT_ERODED, 63, 10),
    PLATEAO_RIVER_INNER("Foliage Fortress Inner River", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.RIVER_CHANNEL,
        126, 12),
    PLATEAO_RIVER_MIDDLE("Foliage Fortress Middle River", Kind.LAND, Climate.TEMPERATE,
        OrigenTerrainFamily.RIVER_CHANNEL, 103, 34),
    RIVER("River", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.RIVER_CHANNEL, 63, 16),
    SALT_FLATS("Salt Flats", Kind.LAND, Climate.WARM, OrigenTerrainFamily.CRACKED_FLATS, 63, 8),
    SCARLET_SANCTUARY_RIVER("Scarlet Sanctuary River", Kind.LAND, Climate.WARM, OrigenTerrainFamily.RIVER_CHANNEL, 71,
        55),
    SHALE_BEACH("Shale Beach", Kind.COAST, Climate.TEMPERATE, OrigenTerrainFamily.FLAT_ERODED, 63, 10),
    SNOWY_MEADOW("Snowy Meadow", Kind.LAND, Climate.COLD, OrigenTerrainFamily.GENERAL, 65, 18),
    SNOWY_MOUNTAINS("Snowy Mountains", Kind.LAND, Climate.COLD, OrigenTerrainFamily.TERRACE_MOUNTAINS, 75, 58),
    TEMPERATE_SEA_ARCHES("Temperate Sea Arches", Kind.COAST, Climate.TEMPERATE, OrigenTerrainFamily.OCEAN_ARCHES, 80,
        50),
    TROPICAL_DEEP_OCEAN("Tropical Deep Ocean", Kind.OCEAN, Climate.WARM, OrigenTerrainFamily.ABYSSAL_OCEAN, 28, 34),
    TROPICAL_OCEAN("Tropical Ocean", Kind.OCEAN, Climate.WARM, OrigenTerrainFamily.ABYSSAL_OCEAN, 42, 22),
    VERDANT_RIVER("Verdant River", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.RIVER_CHANNEL, 67, 38),
    VERTICAL_JUNGLE_RIVER("Vertical Jungle River", Kind.LAND, Climate.WARM, OrigenTerrainFamily.RIVER_CHANNEL, 75, 58),
    VISTAS_RIVER("Vertical Vistas River", Kind.LAND, Climate.TEMPERATE, OrigenTerrainFamily.RIVER_CHANNEL, 75, 78),

    // Biomic's own profiles (tools/origen_overrides.py), appended so every earlier numeric ID stays.
    FERN_FISSURES("Fern Fissures", Kind.SPECIAL, Climate.TEMPERATE, OrigenTerrainFamily.CANYON, 73, 48);

    private final String key;
    private final String displayName;
    private final Kind kind;
    private final Climate climate;
    private final OrigenTerrainFamily terrainFamily;
    private final double authoredBaseY;
    private final double authoredRelief;

    OrigenBiomeProfile(String displayName, Kind kind, Climate climate, OrigenTerrainFamily terrainFamily,
        double authoredBaseY, double authoredRelief) {
        this.key = Biomic.MODID + ":origen/" + name().toLowerCase(java.util.Locale.ROOT);
        this.displayName = displayName;
        this.kind = kind;
        this.climate = climate;
        this.terrainFamily = terrainFamily;
        this.authoredBaseY = authoredBaseY;
        this.authoredRelief = authoredRelief;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Kind getKind() {
        return kind;
    }

    public Climate getClimate() {
        return climate;
    }

    public OrigenTerrainFamily getTerrainFamily() {
        return terrainFamily;
    }

    public double getAuthoredBaseY() {
        return authoredBaseY;
    }

    public double getAuthoredRelief() {
        return authoredRelief;
    }

    public boolean isSurface() {
        return kind != Kind.CAVE;
    }

    public enum Kind {
        LAND,
        COAST,
        OCEAN,
        CAVE,
        SPECIAL
    }

    public enum Climate {
        COLD,
        TEMPERATE,
        WARM
    }
}
