package industries.angelic.biomic.world;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.compat.biome.NativeBiomeCatalogue;
import industries.angelic.biomic.config.ExpansionConfig;
import industries.angelic.biomic.world.terrain.TartarusManifest;

/**
 * Other mods' Nether biomes that join the Tartarus Nether (user's choice, 2026-10-20): the distinct ones, not the
 * near-copies of Tartarus' own. Each takes the terrain of a Tartarus biome with a fitting equation and its own
 * ground, and is dressed by its own mod.
 */
enum NetherNeighbour {

    // BOP: biome.decorate, whose Nether features mostly already span 0..255.
    UNDERGARDEN("biomesoplenty:undergarden", "CRIMSON_FOREST", Dressing.BOP),
    POLAR_CHASM("biomesoplenty:polar_chasm", "TORRID_CRAGS", Dressing.BOP),
    BONEYARD("biomesoplenty:boneyard", "CORRUPTED_WASTES", Dressing.BOP),
    // Netherlicious: its own 256-tall decoration path where the biome has one, otherwise a second pass 128 up.
    FOXFIRE_SWAMP("netherlicious:foxfire_swamp", "SOUL_VALLEY", Dressing.NETHERLICIOUS),
    ABYSSAL_SHADOWLAND("netherlicious:abyssal_shadowland", "LIFELESS_SPIRES", Dressing.NETHERLICIOUS),
    RUPTURED_CHASM("netherlicious:ruptured_chasm", "SERRATED_MOUNTAINS", Dressing.NETHERLICIOUS),
    GLOW_JUNGLE("netherlicious:glow_jungle", "WARPED_FOREST", Dressing.NETHERLICIOUS),
    BURNING_DESERT("netherlicious:burning_desert", "FLAMING_HELLSCAPES", Dressing.NETHERLICIOUS),
    TAINTED_SHROOM_CAVE("netherlicious:tainted_shroom_cave", "BLOODY_REMNANTS", Dressing.NETHERLICIOUS),
    // Gardens of the Dead, Biomic's own port: its features run with the terrain.
    SOULBLIGHT_FOREST("biomic:soulblight_forest", "BLOODLESS_UNDERGROWTH", Dressing.GARDENS,
        List.of("minecraft:soul_soil", "minecraft:soul_soil", "minecraft:soul_sand"), "minecraft:soul_soil"),
    WHISTLING_WOODS("biomic:whistling_woods", "CRIMSON_FOREST", Dressing.GARDENS,
        List.of(
            "minecraft:crimson_nylium",
            "minecraft:crimson_nylium",
            "minecraft:crimson_nylium",
            "minecraft:nether_wart_block"),
        "minecraft:netherrack"),
    // Regions Unexplored, Biomic's own port (user's choice, 2026-10-25): RU's own placed features through the modern
    // engine (RuNetherDressing), on the ground of RU's surface rules (Biomic's blocks by registry name: the Nether's
    // state palette resolves only vanilla's and Et Futurum's modern names).
    BLACKSTONE_BASIN("biomic:blackstone_basin", "BASALTIC_DELTAS", Dressing.RU, List
        .of("native:biomic:ru_decor_cobalt_nylium@0", "native:biomic:ru_decor_cobalt_nylium@0", "minecraft:blackstone"),
        "minecraft:blackstone"),
    GLISTERING_MEADOW("biomic:glistering_meadow", "CRYSTALLINE_VELDTS", Dressing.RU,
        List.of(
            "native:biomic:ru_decor_glistering_nylium@0",
            "native:biomic:ru_decor_glistering_nylium@0",
            "native:biomic:ru_decor_glistering_nylium@0",
            "native:biomic:ru_decor_glistering_wart@0"),
        "minecraft:netherrack"),
    INFERNAL_HOLT("biomic:infernal_holt", "CRIMSON_FOREST", Dressing.RU,
        List.of(
            "native:biomic:ru_decor_brimsprout_nylium@0",
            "native:biomic:ru_decor_brimsprout_nylium@0",
            "minecraft:blackstone"),
        "minecraft:netherrack"),
    MYCOTOXIC_UNDERGROWTH("biomic:mycotoxic_undergrowth", "BLOODLESS_UNDERGROWTH", Dressing.RU,
        List.of(
            "native:biomic:ru_decor_mycotoxic_moss@0",
            "native:biomic:ru_decor_mycotoxic_moss@0",
            "native:biomic:ru_decor_mycotoxic_moss@0",
            "minecraft:netherrack"),
        "minecraft:netherrack");

    enum Dressing {
        BOP,
        NETHERLICIOUS,
        GARDENS,
        RU
    }

    /** Each joins with weight 2 against Tartarus' 49: together they make up about a third of the Nether. */
    private static final int WEIGHT = 2;

    final String key;
    final String terrainOf;
    final Dressing dressing;
    private final List<String> top;
    private final String filler;

    NetherNeighbour(String key, String terrainOf, Dressing dressing) {
        this(key, terrainOf, dressing, null, null);
    }

    NetherNeighbour(String key, String terrainOf, Dressing dressing, List<String> top, String filler) {
        this.key = key;
        this.terrainOf = terrainOf;
        this.dressing = dressing;
        this.top = top;
        this.filler = filler;
    }

    /** The mod's biome, or null when its mod is absent or the biome disabled. */
    BiomeGenBase biome() {
        return dressing == Dressing.GARDENS || dressing == Dressing.RU ? BiomicBiomes.getBiome(key)
            : NativeBiomeCatalogue.biome(key);
    }

    /** The pack id it joins Tartarus' pool under. */
    String packId() {
        return name();
    }

    TartarusManifest.Neighbour spec() {
        BiomeGenBase biome = biome();
        if (biome == null) throw new IllegalStateException("The Nether biome " + key + " is not installed");
        List<String> ground = top != null ? top
            : List.of(NetherStates.nativeState(biome.topBlock, biome.field_150604_aj));
        // 1.7.10 biomes carry a top-block metadata but none for the filler.
        String under = filler != null ? filler : NetherStates.nativeState(biome.fillerBlock, 0);
        // Polar Chasm is ice: no lava falls through it.
        List<String> landforms = this == POLAR_CHASM ? List.of("GLOWSTONE_SPIKE")
            : List.of("LAVA_COLUMN", "GLOWSTONE_SPIKE");
        return new TartarusManifest.Neighbour(packId(), terrainOf, ground, under, WEIGHT, landforms);
    }

    /** The neighbours whose mods are installed now and whose config switch is on, for a new world to pin. */
    static List<String> available() {
        List<String> names = new ArrayList<>();
        for (NetherNeighbour neighbour : values())
            if (neighbour.biome() != null && neighbour.enabled()) names.add(neighbour.name());
        return names;
    }

    private boolean enabled() {
        return switch (dressing) {
            case BOP -> ExpansionConfig.bopNetherHabitats;
            case NETHERLICIOUS -> ExpansionConfig.netherliciousNetherHabitats;
            case GARDENS -> ExpansionConfig.gardensNetherHabitats;
            case RU -> true;
        };
    }

    static List<NetherNeighbour> of(List<String> names) {
        List<NetherNeighbour> neighbours = new ArrayList<>();
        for (String name : names) neighbours.add(valueOf(name));
        return neighbours;
    }
}
