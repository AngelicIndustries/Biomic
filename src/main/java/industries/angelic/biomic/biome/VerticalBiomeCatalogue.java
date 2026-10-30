package industries.angelic.biomic.biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

import industries.angelic.biomic.api.BiomeKeys;
import industries.angelic.biomic.biome.VerticalBiomeDefinition.CaveShape;
import industries.angelic.biomic.biome.VerticalBiomeDefinition.CaveSurface;
import industries.angelic.biomic.biome.VerticalBiomeDefinition.FloatingSurface;
import industries.angelic.biomic.biome.VerticalBiomeDefinition.Trait;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.ru.RuBiomeCatalogue;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition;
import industries.angelic.biomic.storage.BiomeVolume;

/** Catalogue of vertical identities and the bounded behavior terrain may derive from each stable key. */
public final class VerticalBiomeCatalogue {

    public static final VerticalBiomeDefinition SURFACE = definition(
        BiomeVolume.SURFACE_KEY,
        CaveShape.NONE,
        CaveSurface.NONE);
    public static final VerticalBiomeDefinition LUSH_CAVES = definition(
        BiomeKeys.LUSH_CAVES,
        CaveShape.NONE,
        CaveSurface.VEGETATED,
        Trait.CAVE,
        Trait.VEGETATED);
    public static final VerticalBiomeDefinition ORIGEN_LUSH_CAVES = definition(
        OrigenBiomeProfile.LUSH_CAVES.getKey(),
        CaveShape.NONE,
        CaveSurface.VEGETATED,
        Trait.CAVE,
        Trait.VEGETATED);
    public static final VerticalBiomeDefinition DRIPSTONE_CAVES = definition(
        OrigenBiomeProfile.DRIPSTONE_CAVES.getKey(),
        CaveShape.NONE,
        CaveSurface.NONE,
        Trait.CAVE);
    public static final VerticalBiomeDefinition DEEP_DARK = definition(
        OrigenBiomeProfile.DEEP_DARK.getKey(),
        CaveShape.NONE,
        CaveSurface.NONE,
        Trait.CAVE);
    public static final VerticalBiomeDefinition VINE_VAULT = definition(
        OrigenBiomeProfile.VINE_VAULT.getKey(),
        CaveShape.VINE_VAULT,
        CaveSurface.VEGETATED,
        Trait.CAVE,
        Trait.VEGETATED,
        Trait.LARGE_CAVE);
    public static final VerticalBiomeDefinition INFERNO_ISLES = definition(
        OrigenBiomeProfile.INFERNO_ISLES.getKey(),
        CaveShape.INFERNO_ISLANDS,
        CaveSurface.INFERNO,
        Trait.CAVE,
        Trait.LARGE_CAVE);
    public static final VerticalBiomeDefinition TERRACOTTA_TOMBS = definition(
        OrigenBiomeProfile.TERRACOTTA_TOMBS.getKey(),
        CaveShape.TERRACOTTA_TOMBS,
        CaveSurface.TERRACOTTA,
        Trait.CAVE,
        Trait.LARGE_CAVE);
    public static final VerticalBiomeDefinition FORLORN_HOLLOWS = definition(
        "alexscaves:forlorn_hollows",
        CaveShape.NONE,
        CaveSurface.NONE,
        Trait.CAVE,
        Trait.LARGE_CAVE,
        Trait.VEGETATED);
    public static final VerticalBiomeDefinition PRIMORDIAL_CAVES = definition(
        "alexscaves:primordial_caves",
        CaveShape.NONE,
        CaveSurface.VEGETATED,
        Trait.CAVE,
        Trait.LARGE_CAVE,
        Trait.VEGETATED);
    public static final VerticalBiomeDefinition ABYSSAL_CHASM = definition(
        industries.angelic.biomic.world.terrain.HostedAbyssalRuntime.BIOME_KEY,
        CaveShape.NONE,
        CaveSurface.NONE,
        Trait.CAVE,
        Trait.LARGE_CAVE,
        Trait.VEGETATED);
    public static final VerticalBiomeDefinition SKY_DONUT = new VerticalBiomeDefinition(
        BiomeKeys.SKY_DONUT,
        CaveShape.NONE,
        CaveSurface.NONE,
        FloatingSurface.TEMPERATE_GRASS,
        Trait.FLOATING_ISLAND,
        Trait.VEGETATED);
    public static final VerticalBiomeDefinition AEROPELAGO = new VerticalBiomeDefinition(
        BiomeKeys.AEROPELAGO,
        CaveShape.NONE,
        CaveSurface.NONE,
        FloatingSurface.TEMPERATE_GRASS,
        Trait.FLOATING_ISLAND,
        Trait.VEGETATED);

    private static final Map<String, VerticalBiomeDefinition> BY_KEY;
    private static final Map<OrigenBiomeProfile, VerticalBiomeDefinition> ORIGEN_CAVES;
    private static final Map<OrigenBiomeProfile, VerticalBiomeDefinition> FLOATING_ISLANDS;
    private static final Map<String, OrigenBiomeProfile> FLOATING_ISLAND_PROFILES;
    private static final Map<OrigenBiomeProfile, VerticalBiomeDefinition> AEROPELAGO_HOSTS;
    private static final Map<String, OrigenBiomeProfile> AEROPELAGO_HOST_PROFILES;

    static {
        LinkedHashMap<String, VerticalBiomeDefinition> definitions = new LinkedHashMap<>();
        register(definitions, SURFACE);
        register(definitions, LUSH_CAVES);
        EnumMap<OrigenBiomeProfile, VerticalBiomeDefinition> origenCaves = new EnumMap<>(OrigenBiomeProfile.class);
        registerOrigenCave(definitions, origenCaves, OrigenBiomeProfile.LUSH_CAVES, ORIGEN_LUSH_CAVES);
        registerOrigenCave(definitions, origenCaves, OrigenBiomeProfile.DRIPSTONE_CAVES, DRIPSTONE_CAVES);
        registerOrigenCave(definitions, origenCaves, OrigenBiomeProfile.DEEP_DARK, DEEP_DARK);
        register(definitions, VINE_VAULT);
        register(definitions, INFERNO_ISLES);
        register(definitions, TERRACOTTA_TOMBS);
        register(definitions, FORLORN_HOLLOWS);
        register(definitions, PRIMORDIAL_CAVES);
        register(definitions, ABYSSAL_CHASM);
        register(definitions, SKY_DONUT);
        register(definitions, AEROPELAGO);
        origenCaves.put(OrigenBiomeProfile.VINE_VAULT, VINE_VAULT);
        origenCaves.put(OrigenBiomeProfile.INFERNO_ISLES, INFERNO_ISLES);
        origenCaves.put(OrigenBiomeProfile.TERRACOTTA_TOMBS, TERRACOTTA_TOMBS);

        LinkedHashMap<String, VerticalBiomeDefinition> ruCaves = new LinkedHashMap<>();
        for (RuBiomeDefinition ru : RuBiomeCatalogue.caves()) {
            VerticalBiomeDefinition host = origenCaves.get(ru.getTerrainProfile());
            if (host == null) throw new IllegalStateException("RU cave has no Origen shape host " + ru.getBiomeKey());
            VerticalBiomeDefinition definition = definition(
                ru.getBiomeKey(),
                host.getCaveShape(),
                ruCaveSurface(ru.getBiomeKey()),
                host.getCaveShape() == CaveShape.NONE ? new Trait[] { Trait.CAVE }
                    : new Trait[] { Trait.CAVE, Trait.LARGE_CAVE });
            register(definitions, definition);
            ruCaves.put(ru.getBiomeKey(), definition);
        }

        EnumMap<OrigenBiomeProfile, VerticalBiomeDefinition> floatingIslands = new EnumMap<>(OrigenBiomeProfile.class);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.LUSH_LOOPS);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.SAKURA_STREAMS);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.SNOWY_SPIRES);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.ARID_ARBORETUM);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.ROCKY_REFUGE);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.FROZEN_FUNGI);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.MARINE_MONOLITHS);
        registerFloatingIsland(definitions, floatingIslands, OrigenBiomeProfile.FOLIAGE_FORTRESS_INNER);

        EnumMap<OrigenBiomeProfile, VerticalBiomeDefinition> aeropelagoHosts = new EnumMap<>(OrigenBiomeProfile.class);
        registerAeropelagoHost(definitions, aeropelagoHosts, OrigenBiomeProfile.LUSH_LOOPS);
        registerAeropelagoHost(definitions, aeropelagoHosts, OrigenBiomeProfile.SAKURA_STREAMS);
        registerAeropelagoHost(definitions, aeropelagoHosts, OrigenBiomeProfile.FOLIAGE_FORTRESS_INNER);
        registerAeropelagoHost(definitions, aeropelagoHosts, OrigenBiomeProfile.TALL_TIMBERLAND);

        BY_KEY = Collections.unmodifiableMap(definitions);
        ORIGEN_CAVES = Collections.unmodifiableMap(origenCaves);
        FLOATING_ISLANDS = Collections.unmodifiableMap(floatingIslands);
        LinkedHashMap<String, OrigenBiomeProfile> floatingIslandProfiles = new LinkedHashMap<>();
        for (Map.Entry<OrigenBiomeProfile, VerticalBiomeDefinition> entry : floatingIslands.entrySet()) {
            floatingIslandProfiles.put(
                entry.getValue()
                    .getKey(),
                entry.getKey());
        }
        FLOATING_ISLAND_PROFILES = Collections.unmodifiableMap(floatingIslandProfiles);
        AEROPELAGO_HOSTS = Collections.unmodifiableMap(aeropelagoHosts);
        LinkedHashMap<String, OrigenBiomeProfile> aeropelagoHostProfiles = new LinkedHashMap<>();
        for (Map.Entry<OrigenBiomeProfile, VerticalBiomeDefinition> entry : aeropelagoHosts.entrySet()) {
            aeropelagoHostProfiles.put(
                entry.getValue()
                    .getKey(),
                entry.getKey());
        }
        AEROPELAGO_HOST_PROFILES = Collections.unmodifiableMap(aeropelagoHostProfiles);
        ArrayList<VerticalBiomeDefinition> specialCaves = new ArrayList<>();
        specialCaves.add(VINE_VAULT);
        specialCaves.add(INFERNO_ISLES);
        specialCaves.add(TERRACOTTA_TOMBS);
        for (VerticalBiomeDefinition definition : ruCaves.values()) {
            if (definition.hasTrait(Trait.LARGE_CAVE)) specialCaves.add(definition);
        }
    }

    private VerticalBiomeCatalogue() {}

    /** Every definition, in declaration order: the keys a volume can store, beside the surface marker. */
    public static java.util.Collection<VerticalBiomeDefinition> all() {
        return BY_KEY.values();
    }

    public static VerticalBiomeDefinition get(String key) {
        return BY_KEY.get(key);
    }

    /**
     * Unknown stored keys deliberately receive no terrain behavior while remaining intact in storage and API output.
     */
    public static VerticalBiomeDefinition behaviorFor(String key) {
        VerticalBiomeDefinition definition = get(key);
        return definition == null ? SURFACE : definition;
    }

    public static VerticalBiomeDefinition getOrigenCave(OrigenBiomeProfile profile) {
        return ORIGEN_CAVES.get(profile);
    }

    public static VerticalBiomeDefinition getFloatingIsland(OrigenBiomeProfile profile) {
        return FLOATING_ISLANDS.get(profile);
    }

    public static OrigenBiomeProfile getFloatingIslandProfile(String key) {
        OrigenBiomeProfile profile = FLOATING_ISLAND_PROFILES.get(key);
        return profile == null ? AEROPELAGO_HOST_PROFILES.get(key) : profile;
    }

    public static VerticalBiomeDefinition getAeropelagoHost(OrigenBiomeProfile profile) {
        return AEROPELAGO_HOSTS.get(profile);
    }

    public static boolean isAeropelagoHostedKey(String key) {
        return AEROPELAGO_HOST_PROFILES.containsKey(key);
    }

    private static VerticalBiomeDefinition definition(String key, CaveShape shape, CaveSurface surface,
        Trait... traits) {
        return new VerticalBiomeDefinition(key, shape, surface, traits);
    }

    private static void register(Map<String, VerticalBiomeDefinition> definitions, VerticalBiomeDefinition definition) {
        if (definitions.put(definition.getKey(), definition) != null) {
            throw new IllegalStateException("Duplicate vertical biome key " + definition.getKey());
        }
    }

    /**
     * An Origen profile on a 256-high world's floating islands, stored as {@code biomic:floating/origen/<profile>}: a
     * key
     * of its own, so a cell tells the island from the ground, as Aeropelago's hosts do. It resolves to the profile's
     * Origen biome, its tags with FLOATING_ISLAND.
     */
    private static void registerFloatingIsland(Map<String, VerticalBiomeDefinition> definitions,
        Map<OrigenBiomeProfile, VerticalBiomeDefinition> floatingIslands, OrigenBiomeProfile profile) {
        VerticalBiomeDefinition definition = definition(
            industries.angelic.biomic.Biomic.MODID + ":floating/origen/"
                + profile.name()
                    .toLowerCase(java.util.Locale.ROOT),
            CaveShape.NONE,
            CaveSurface.NONE,
            Trait.FLOATING_ISLAND);
        register(definitions, definition);
        floatingIslands.put(profile, definition);
    }

    private static void registerAeropelagoHost(Map<String, VerticalBiomeDefinition> definitions,
        Map<OrigenBiomeProfile, VerticalBiomeDefinition> hosts, OrigenBiomeProfile profile) {
        String key = BiomeKeys.AEROPELAGO + "/origen/"
            + profile.name()
                .toLowerCase(java.util.Locale.ROOT);
        VerticalBiomeDefinition definition = definition(
            key,
            CaveShape.NONE,
            CaveSurface.NONE,
            Trait.FLOATING_ISLAND,
            Trait.VEGETATED);
        register(definitions, definition);
        hosts.put(profile, definition);
    }

    private static void registerOrigenCave(Map<String, VerticalBiomeDefinition> definitions,
        Map<OrigenBiomeProfile, VerticalBiomeDefinition> caves, OrigenBiomeProfile profile,
        VerticalBiomeDefinition definition) {
        register(definitions, definition);
        caves.put(profile, definition);
    }

    private static CaveSurface ruCaveSurface(String key) {
        if (key.endsWith(":ancient_delta")) return CaveSurface.RU_ANCIENT_DELTA;
        if (key.endsWith(":bioshroom_caves")) return CaveSurface.RU_BIOSHROOM;
        if (key.endsWith(":inferno")) return CaveSurface.INFERNO;
        if (key.endsWith(":prismachasm")) return CaveSurface.RU_PRISMACHASM;
        throw new IllegalArgumentException("Unknown active RU cave " + key);
    }
}
