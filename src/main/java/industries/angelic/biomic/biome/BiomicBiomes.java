package industries.angelic.biomic.biome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.api.BiomeKeys;
import industries.angelic.biomic.api.BiomeTags;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.origen.OrigenTerrainFamily;
import industries.angelic.biomic.biome.ru.RuBiomeCatalogue;
import industries.angelic.biomic.biome.ru.RuBiomeDefinition;
import industries.angelic.biomic.config.WorldgenConfigSnapshot;

/** Owns Biomic's numeric 1.7 registrations and stable-key metadata. */
public final class BiomicBiomes {

    /**
     * Keys Biomic registers that no new world stores: scaffold identities nothing selects any more, and Alex's Caves'
     * retired cave biomes. The biome catalogue leaves them out; the biome browser lists the registered biomes among
     * them as never generated.
     */
    public static final Set<String> NOT_GENERATED = Set.of(
        BiomeKeys.TEMPERATE_LOWLANDS,
        BiomeKeys.TEMPERATE_ARCHIPELAGO,
        BiomeKeys.WINDSWEPT_HIGHLANDS,
        BiomeKeys.ESTUARY,
        BiomeKeys.LUSH_CAVES,
        "alexscaves:forlorn_hollows",
        "alexscaves:primordial_caves");

    private static RegistryState state;

    private BiomicBiomes() {}

    public static synchronized void register() {
        WorldgenConfigSnapshot snapshot = WorldgenConfigSnapshot.current();
        OrigenBiomeProfile[] origenProfiles = OrigenBiomeProfile.values();
        List<RuBiomeDefinition> ruDefinitions = RuBiomeCatalogue.all();
        int gardensOffset = 6 + ruDefinitions.size() + origenProfiles.length;
        int tartarusOffset = gardensOffset + 3;
        int[] ids = new int[tartarusOffset + TartarusBiomes.values().length];
        ids[0] = snapshot.getTemperateLowlandsId();
        ids[1] = snapshot.getTemperateArchipelagoId();
        ids[2] = snapshot.getWindsweptHighlandsId();
        ids[3] = snapshot.getLushCavesId();
        ids[4] = snapshot.getSkyDonutId();
        ids[5] = snapshot.getAeropelagoId();
        for (int index = 0; index < ruDefinitions.size(); index++) {
            ids[6 + index] = ruDefinitions.get(index)
                .resolveId(snapshot);
        }
        for (OrigenBiomeProfile profile : origenProfiles) {
            ids[6 + ruDefinitions.size() + profile.ordinal()] = snapshot.getOrigenBiomeBaseId() + profile.ordinal();
        }
        ids[gardensOffset] = snapshot.getSoulblightForestId();
        ids[gardensOffset + 1] = snapshot.getWhistlingWoodsId();
        ids[gardensOffset + 2] = industries.angelic.biomic.config.BiomeConfig.estuaryId;
        for (TartarusBiomes biome : TartarusBiomes.values())
            ids[tartarusOffset + biome.ordinal()] = industries.angelic.biomic.config.BiomeConfig.tartarusBiomeBaseId
                + biome.ordinal();
        if (state != null) {
            if (!Arrays.equals(ids, state.ids)) {
                throw new IllegalStateException("Biomic biome IDs changed after registration");
            }
            return;
        }

        // Plain unit-test JVMs do not run the EndlessIDs coremod. Production reaches this point only after the
        // bootstrap validator has proved the extended registry, so tests retain identity metadata without mutating a
        // nonexistent high-ID slot.
        boolean attachToNumericRegistry = validateAvailableIds(ids);
        Definition lowlands = definition(
            BiomeKeys.TEMPERATE_LOWLANDS,
            new BiomeGenBiomic(
                ids[0],
                attachToNumericRegistry,
                "Biomic Temperate Lowlands",
                0x6FAF55,
                0.70F,
                0.70F,
                0.125F,
                0.12F,
                SpawnProfile.TEMPERATE_SURFACE),
            BiomeTags.SURFACE,
            BiomeTags.TEMPERATE,
            BiomeTags.HUMID);
        Definition archipelago = definition(
            BiomeKeys.TEMPERATE_ARCHIPELAGO,
            new BiomeGenBiomic(
                ids[1],
                attachToNumericRegistry,
                "Biomic Temperate Archipelago",
                0x397FA3,
                0.80F,
                0.80F,
                -0.75F,
                0.18F,
                SpawnProfile.TEMPERATE_SURFACE),
            BiomeTags.SURFACE,
            BiomeTags.OCEAN,
            BiomeTags.ARCHIPELAGO,
            BiomeTags.TEMPERATE,
            BiomeTags.HUMID);
        Definition highlands = definition(
            BiomeKeys.WINDSWEPT_HIGHLANDS,
            new BiomeGenBiomic(
                ids[2],
                attachToNumericRegistry,
                "Biomic Windswept Highlands",
                0x66835C,
                0.35F,
                0.65F,
                1.0F,
                0.5F,
                SpawnProfile.TEMPERATE_SURFACE),
            BiomeTags.SURFACE,
            BiomeTags.MOUNTAIN,
            BiomeTags.TEMPERATE,
            BiomeTags.HUMID);
        Definition lushCaves = definition(
            BiomeKeys.LUSH_CAVES,
            new BiomeGenBiomic(
                ids[3],
                attachToNumericRegistry,
                "Biomic Lush Caves",
                0x3F7652,
                0.75F,
                0.95F,
                0.1F,
                0.2F,
                SpawnProfile.LUSH_CAVE),
            BiomeTags.CAVE,
            BiomeTags.TEMPERATE,
            BiomeTags.HUMID);
        Definition skyDonut = definition(
            BiomeKeys.SKY_DONUT,
            new BiomeGenBiomic(
                ids[4],
                attachToNumericRegistry,
                "Sky Donut",
                0x6EA95B,
                0.62F,
                0.78F,
                0.1F,
                0.2F,
                SpawnProfile.TEMPERATE_SURFACE,
                0x72AD57,
                0x5F9D4B,
                0x44AFF5,
                -1),
            BiomeTags.FLOATING_ISLAND,
            BiomeTags.PLAINS,
            BiomeTags.TEMPERATE,
            BiomeTags.HUMID);
        Definition aeropelago = definition(
            BiomeKeys.AEROPELAGO,
            new BiomeGenBiomic(
                ids[5],
                attachToNumericRegistry,
                "Aeropelago",
                0x8DB360,
                0.80F,
                0.40F,
                0.125F,
                0.05F,
                SpawnProfile.TEMPERATE_SURFACE),
            BiomeTags.FLOATING_ISLAND,
            BiomeTags.ARCHIPELAGO,
            BiomeTags.PLAINS,
            BiomeTags.TEMPERATE);
        Map<String, Definition> ru = new LinkedHashMap<>();
        for (int index = 0; index < ruDefinitions.size(); index++) {
            RuBiomeDefinition ruDefinition = ruDefinitions.get(index);
            ru.put(ruDefinition.getBiomeKey(), ruDefinition(ids[6 + index], attachToNumericRegistry, ruDefinition));
        }

        Map<OrigenBiomeProfile, Definition> origen = new LinkedHashMap<>();
        for (OrigenBiomeProfile profile : origenProfiles) {
            int index = 6 + ruDefinitions.size() + profile.ordinal();
            Definition definition = origenDefinition(ids[index], attachToNumericRegistry, profile);
            origen.put(profile, definition);
        }

        Definition soulblight = definition(
            BiomeKeys.SOULBLIGHT_FOREST,
            new BiomeGenBiomic(
                ids[gardensOffset],
                attachToNumericRegistry,
                "Soulblight Forest",
                0x593A21,
                2F,
                0F,
                .1F,
                .2F,
                SpawnProfile.NETHER),
            BiomeTags.NETHER,
            BiomeTags.FOREST,
            BiomeTags.WARM,
            BiomeTags.DRY);
        Definition whistling = definition(
            BiomeKeys.WHISTLING_WOODS,
            new BiomeGenBiomic(
                ids[gardensOffset + 1],
                attachToNumericRegistry,
                "Whistling Woods",
                0x5E1118,
                2F,
                0F,
                .1F,
                .2F,
                SpawnProfile.NETHER),
            BiomeTags.NETHER,
            BiomeTags.FOREST,
            BiomeTags.WARM,
            BiomeTags.DRY);

        var river = RuBiomeCatalogue.forBiomeKey(BiomeKeys.MUDDY_RIVER);
        Definition estuary = definition(
            BiomeKeys.ESTUARY,
            new BiomeGenBiomic(
                ids[gardensOffset + 2],
                attachToNumericRegistry,
                "Estuary",
                river.getMapColor(),
                river.getTemperature(),
                river.getRainfall(),
                river.getRootHeight(),
                river.getVariation(),
                SpawnProfile.UNASSIGNED,
                river.getGrassColor(),
                river.getFoliageColor(),
                river.getWaterColor(),
                river.getSkyColor()),
            BiomeTags.SURFACE,
            BiomeTags.RIVER,
            BiomeTags.COAST,
            BiomeTags.HUMID);
        Map<TartarusBiomes, Definition> tartarus = new java.util.EnumMap<>(TartarusBiomes.class);
        for (TartarusBiomes biome : TartarusBiomes.values()) tartarus.put(
            biome,
            definition(
                biome.key(),
                new BiomeGenBiomic(
                    ids[tartarusOffset + biome.ordinal()],
                    attachToNumericRegistry,
                    biome.displayName,
                    biome.color,
                    2F,
                    0F,
                    .1F,
                    .2F,
                    SpawnProfile.HELL),
                BiomeTags.NETHER,
                BiomeTags.WARM,
                BiomeTags.DRY));
        state = new RegistryState(
            ids,
            lowlands,
            archipelago,
            highlands,
            lushCaves,
            skyDonut,
            aeropelago,
            ru,
            origen,
            soulblight,
            whistling,
            estuary,
            tartarus);
        if (attachToNumericRegistry) {
            registerDictionaryTypes(lowlands.biome, BiomeDictionary.Type.PLAINS, BiomeDictionary.Type.LUSH);
            registerDictionaryTypes(archipelago.biome, BiomeDictionary.Type.OCEAN, BiomeDictionary.Type.WET);
            registerDictionaryTypes(highlands.biome, BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.HILLS);
            registerDictionaryTypes(lushCaves.biome, BiomeDictionary.Type.LUSH, BiomeDictionary.Type.WET);
            registerDictionaryTypes(skyDonut.biome, BiomeDictionary.Type.PLAINS, BiomeDictionary.Type.LUSH);
            registerDictionaryTypes(aeropelago.biome, BiomeDictionary.Type.PLAINS);
            for (RuBiomeDefinition ruDefinition : ruDefinitions) {
                registerRuDictionaryTypes(ruDefinition, ru.get(ruDefinition.getBiomeKey()).biome);
                registerClimateTypes(ru.get(ruDefinition.getBiomeKey()).biome);
            }
            for (Map.Entry<OrigenBiomeProfile, Definition> entry : origen.entrySet()) {
                registerOrigenDictionaryTypes(entry.getKey(), entry.getValue().biome);
                registerClimateTypes(entry.getValue().biome);
            }
            registerDictionaryTypes(
                soulblight.biome,
                BiomeDictionary.Type.NETHER,
                BiomeDictionary.Type.FOREST,
                BiomeDictionary.Type.HOT,
                BiomeDictionary.Type.DRY);
            registerDictionaryTypes(
                whistling.biome,
                BiomeDictionary.Type.NETHER,
                BiomeDictionary.Type.FOREST,
                BiomeDictionary.Type.HOT,
                BiomeDictionary.Type.DRY);
            registerDictionaryTypes(estuary.biome, BiomeDictionary.Type.RIVER, BiomeDictionary.Type.WET);
            for (Definition definition : tartarus.values()) registerDictionaryTypes(
                definition.biome,
                BiomeDictionary.Type.NETHER,
                BiomeDictionary.Type.HOT,
                BiomeDictionary.Type.DRY);
        }

        Biomic.LOG.info(
            "Registered Biomic biomes: {}={}, {}={}, {}={}, {}={}, {}={}",
            lowlands.key,
            lowlands.biome.biomeID,
            archipelago.key,
            archipelago.biome.biomeID,
            highlands.key,
            highlands.biome.biomeID,
            lushCaves.key,
            lushCaves.biome.biomeID,
            RuBiomeCatalogue.CLOVER_PLAINS.getBiomeKey(),
            ru.get(RuBiomeCatalogue.CLOVER_PLAINS.getBiomeKey()).biome.biomeID);
        Biomic.LOG.info("Registered {} completed RU biome slices", ru.size());
        Biomic.LOG.info(
            "Registered {} attributed Origen biome profiles at EndlessIDs block {}..{}",
            origen.size(),
            snapshot.getOrigenBiomeBaseId(),
            snapshot.getOrigenBiomeBaseId() + origen.size() - 1);
    }

    public static BiomeGenBase getTemperateLowlands() {
        return requireState().lowlands.biome;
    }

    public static BiomeGenBase getTemperateArchipelago() {
        return requireState().archipelago.biome;
    }

    public static BiomeGenBase getWindsweptHighlands() {
        return requireState().highlands.biome;
    }

    public static BiomeGenBase getLushCaves() {
        return requireState().lushCaves.biome;
    }

    public static BiomeGenBase getTartarusBiome(TartarusBiomes biome) {
        return requireState().tartarus.get(biome).biome;
    }

    public static BiomeGenBase getOrigenBiome(OrigenBiomeProfile profile) {
        Definition definition = requireState().origen.get(profile);
        return definition == null ? null : definition.biome;
    }

    /** Reachable, land-based identities suitable for vanilla's initial player-spawn search. */
    public static List<BiomeGenBase> getNaturalSpawnBiomes() {
        RegistryState registry = requireState();
        ArrayList<BiomeGenBase> biomes = new ArrayList<>();
        for (RuBiomeDefinition ruDefinition : RuBiomeCatalogue.all()) {
            if (ruDefinition.isNaturalSpawn()) {
                biomes.add(registry.ru.get(ruDefinition.getBiomeKey()).biome);
            }
        }
        for (Map.Entry<OrigenBiomeProfile, Definition> entry : registry.origen.entrySet()) {
            if (entry.getKey()
                .getKind() == OrigenBiomeProfile.Kind.LAND) {
                biomes.add(entry.getValue().biome);
            }
        }
        return Collections.unmodifiableList(biomes);
    }

    /** All registered surface identities eligible for vanilla stronghold relocation. */
    public static List<BiomeGenBase> getStrongholdSurfaceBiomes() {
        RegistryState registry = requireState();
        ArrayList<BiomeGenBase> biomes = new ArrayList<>();
        biomes.add(registry.lowlands.biome);
        biomes.add(registry.archipelago.biome);
        biomes.add(registry.highlands.biome);
        for (RuBiomeDefinition ruDefinition : RuBiomeCatalogue.surface()) {
            biomes.add(registry.ru.get(ruDefinition.getBiomeKey()).biome);
        }
        for (Map.Entry<OrigenBiomeProfile, Definition> entry : registry.origen.entrySet()) {
            if (entry.getKey()
                .isSurface()) {
                biomes.add(entry.getValue().biome);
            }
        }
        return Collections.unmodifiableList(biomes);
    }

    /** Every biome Biomic defines, in registration order (whether or not the numeric registry holds it). */
    public static List<BiomeGenBase> all() {
        List<BiomeGenBase> biomes = new ArrayList<>();
        for (Definition definition : requireState().byKey.values()) biomes.add(definition.biome);
        return biomes;
    }

    public static BiomeGenBase getBiome(String key) {
        Definition definition = requireState().byKey.get(key);
        return definition == null ? industries.angelic.biomic.compat.biome.NativeBiomeCatalogue.biome(key)
            : definition.biome;
    }

    public static String getKey(BiomeGenBase biome) {
        Definition definition = requireState().byBiome.get(biome);
        return definition == null ? industries.angelic.biomic.compat.biome.NativeBiomeCatalogue.key(biome)
            : definition.key;
    }

    public static Set<String> getTags(String key) {
        Definition definition = requireState().byKey.get(key);
        return definition == null ? industries.angelic.biomic.compat.biome.NativeBiomeCatalogue.tags(key)
            : definition.tags;
    }

    public static Set<String> getTags(BiomeGenBase biome) {
        Definition definition = requireState().byBiome.get(biome);
        return definition == null ? industries.angelic.biomic.compat.biome.NativeBiomeCatalogue.tags(biome)
            : definition.tags;
    }

    private static boolean validateAvailableIds(int[] ids) {
        Set<Integer> unique = new LinkedHashSet<>();
        BiomeGenBase[] registry = BiomeGenBase.getBiomeGenArray();
        boolean allIdsFitRegistry = true;
        for (int id : ids) {
            if (id < 0 || id >= 0xFFFF) {
                throw new IllegalStateException("Biomic biome ID is outside the usable EndlessIDs range: " + id);
            }
            if (!unique.add(id)) {
                throw new IllegalStateException("Duplicate configured Biomic biome ID: " + id);
            }
            if (id >= registry.length) {
                allIdsFitRegistry = false;
            } else if (registry[id] != null) {
                throw new IllegalStateException(
                    "Configured Biomic biome ID " + id + " is already occupied by " + registry[id].biomeName);
            }
        }
        return allIdsFitRegistry;
    }

    private static Definition definition(String key, BiomeGenBase biome, String... tags) {
        return new Definition(key, biome, Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(tags))));
    }

    private static Definition ruDefinition(int id, boolean register, RuBiomeDefinition definition) {
        return new Definition(
            definition.getBiomeKey(),
            new BiomeGenBiomic(
                id,
                register,
                definition.getDisplayName(),
                definition.getMapColor(),
                definition.getTemperature(),
                definition.getRainfall(),
                definition.getRootHeight(),
                definition.getVariation(),
                definition.getDimension() == RuBiomeDefinition.Dimension.CAVE ? SpawnProfile.LUSH_CAVE
                    : SpawnProfile.TEMPERATE_SURFACE,
                runtimeRuGrassColor(definition),
                definition.getFoliageColor(),
                definition.getWaterColor(),
                definition.getSkyColor()),
            definition.getTags());
    }

    private static int runtimeRuGrassColor(RuBiomeDefinition definition) {
        if ("biomic:alpha_grove".equals(definition.getBiomeKey())) {
            // Alpha Grove's surface uses a pre-colored sprite. Angelica's smooth biome cache bypasses
            // BlockGrass.colorMultiplier entirely, so the biome itself must resolve to white to keep that sprite
            // untinted. The authored feature set has no grass-colored plants; its trees and flowers use independent
            // colors, while the original 0x87D257 remains recorded in the pinned RU manifest/catalogue.
            return 0xFFFFFF;
        }
        return definition.getGrassColor();
    }

    private static Definition origenDefinition(int id, boolean register, OrigenBiomeProfile profile) {
        float temperature = profile.getClimate() == OrigenBiomeProfile.Climate.COLD ? 0.05F
            : profile.getClimate() == OrigenBiomeProfile.Climate.WARM ? 1.1F : 0.70F;
        float rainfall = profile.getClimate() == OrigenBiomeProfile.Climate.WARM ? 0.45F : 0.72F;
        float rootHeight = (float) ((profile.getAuthoredBaseY() - 63.0D) / 64.0D);
        float variation = (float) Math.min(1.6D, profile.getAuthoredRelief() / 72.0D);
        int color = profileColor(profile);
        int grassColor = origenGrassColor(profile, color);
        int foliageColor = origenFoliageColor(profile, color);
        int waterColor = origenWaterColor(profile);
        int skyColor = profile == OrigenBiomeProfile.ROCKY_REFUGE ? 0xE2D3B3 : -1;
        ArrayList<String> tags = new ArrayList<>();
        tags.add(profile.getKind() == OrigenBiomeProfile.Kind.CAVE ? BiomeTags.CAVE : BiomeTags.SURFACE);
        if (profile.getKind() == OrigenBiomeProfile.Kind.OCEAN) {
            tags.add(BiomeTags.OCEAN);
        }
        if (isMountain(profile.getTerrainFamily())) {
            tags.add(BiomeTags.MOUNTAIN);
        }
        if (profile.getClimate() == OrigenBiomeProfile.Climate.COLD) {
            tags.add(BiomeTags.COLD);
        } else if (profile.getClimate() == OrigenBiomeProfile.Climate.WARM) {
            tags.add(BiomeTags.WARM);
        } else {
            tags.add(BiomeTags.TEMPERATE);
        }
        return definition(
            profile.getKey(),
            new BiomeGenBiomic(
                id,
                register,
                profile.getDisplayName(),
                color,
                temperature,
                rainfall,
                rootHeight,
                variation,
                profile.getKind() == OrigenBiomeProfile.Kind.CAVE ? SpawnProfile.LUSH_CAVE
                    : SpawnProfile.TEMPERATE_SURFACE,
                grassColor,
                foliageColor,
                waterColor,
                skyColor),
            tags.toArray(new String[tags.size()]));
    }

    private static int profileColor(OrigenBiomeProfile profile) {
        int ordinalVariation = (profile.ordinal() * 0x001109) & 0x1F1F1F;
        int base = profile.getClimate() == OrigenBiomeProfile.Climate.COLD ? 0x89A6A5
            : profile.getClimate() == OrigenBiomeProfile.Climate.WARM ? 0x8EAD62 : 0x69A763;
        return (base ^ ordinalVariation) & 0xFFFFFF;
    }

    private static int origenGrassColor(OrigenBiomeProfile profile, int fallback) {
        return switch (profile) {
            case ROCKY_REFUGE -> 0x70E66D;
            case SCARLET_SANCTUARY, SCARLET_SANCTUARY_RIVER -> 0x599C2C;
            case FROZEN_FUNGI, FROZEN_FUNGI_RIVER -> 0x2AB2C7;
            default -> fallback;
        };
    }

    private static int origenFoliageColor(OrigenBiomeProfile profile, int fallback) {
        return switch (profile) {
            // Pinned Origen murky_marshlands.yml explicitly colors its dark-swamp tree foliage.
            case MURKY_MARSHLANDS -> 0x1C4000;
            case ROCKY_REFUGE -> 0x70E66D;
            case SCARLET_SANCTUARY, SCARLET_SANCTUARY_RIVER -> 0xEC5448;
            case FROZEN_FUNGI, FROZEN_FUNGI_RIVER -> 0xA6E2ED;
            default -> fallback;
        };
    }

    private static int origenWaterColor(OrigenBiomeProfile profile) {
        return switch (profile) {
            case SCARLET_SANCTUARY, SCARLET_SANCTUARY_RIVER -> 0x5DB7EF;
            case FROZEN_FUNGI, FROZEN_FUNGI_RIVER -> 0xA298E4;
            default -> profile.getClimate() == OrigenBiomeProfile.Climate.COLD ? 0x3F76E4 : 0x44AFF5;
        };
    }

    private static void registerOrigenDictionaryTypes(OrigenBiomeProfile profile, BiomeGenBase biome) {
        if (profile.getKind() == OrigenBiomeProfile.Kind.OCEAN) {
            registerDictionaryTypes(biome, BiomeDictionary.Type.OCEAN);
        } else if (isMountain(profile.getTerrainFamily())) {
            registerDictionaryTypes(biome, BiomeDictionary.Type.MOUNTAIN, BiomeDictionary.Type.HILLS);
        } else if (profile.getKind() != OrigenBiomeProfile.Kind.CAVE) {
            registerDictionaryTypes(biome, BiomeDictionary.Type.PLAINS);
        }
    }

    private static void registerRuDictionaryTypes(RuBiomeDefinition definition, BiomeGenBase biome) {
        Set<String> tags = definition.getTags();
        ArrayList<BiomeDictionary.Type> types = new ArrayList<>();
        if (tags.contains(BiomeTags.OCEAN)) {
            types.add(BiomeDictionary.Type.OCEAN);
        }
        if (tags.contains(BiomeTags.COAST)) {
            types.add(BiomeDictionary.Type.BEACH);
        }
        if (tags.contains(BiomeTags.MOUNTAIN)) {
            types.add(BiomeDictionary.Type.MOUNTAIN);
            types.add(BiomeDictionary.Type.HILLS);
        }
        if (tags.contains(BiomeTags.FOREST)) {
            types.add(BiomeDictionary.Type.FOREST);
        }
        if (tags.contains(BiomeTags.JUNGLE)) {
            types.add(BiomeDictionary.Type.JUNGLE);
        }
        if (tags.contains(BiomeTags.SWAMP)) {
            types.add(BiomeDictionary.Type.SWAMP);
            types.add(BiomeDictionary.Type.WET);
        }
        if (tags.contains(BiomeTags.DESERT)) {
            types.add(BiomeDictionary.Type.SANDY);
            types.add(BiomeDictionary.Type.DRY);
            types.add(BiomeDictionary.Type.HOT);
        }
        if (tags.contains(BiomeTags.PLAINS)) {
            types.add(BiomeDictionary.Type.PLAINS);
        }
        if (tags.contains(BiomeTags.HUMID)) {
            types.add(BiomeDictionary.Type.LUSH);
        }
        if (!types.isEmpty()) {
            registerDictionaryTypes(biome, types.toArray(new BiomeDictionary.Type[types.size()]));
        }
    }

    /**
     * Climate types from the biome's own temperature and rainfall, by Forge's own guesses for unregistered biomes:
     * snowy and cold below 0.15, cold below 0.5, hot above 0.85; dry under 0.25 rainfall, wet over 0.85. Other mods
     * read them (Village Names' fallback styles, spawn and plant rules).
     */
    private static void registerClimateTypes(BiomeGenBase biome) {
        ArrayList<BiomeDictionary.Type> types = new ArrayList<>();
        if (biome.temperature < 0.15F) types.add(BiomeDictionary.Type.SNOWY);
        if (biome.temperature < 0.5F) types.add(BiomeDictionary.Type.COLD);
        if (biome.temperature > 0.85F) types.add(BiomeDictionary.Type.HOT);
        if (biome.rainfall < 0.25F) types.add(BiomeDictionary.Type.DRY);
        if (biome.rainfall > 0.85F) types.add(BiomeDictionary.Type.WET);
        if (!types.isEmpty()) registerDictionaryTypes(biome, types.toArray(new BiomeDictionary.Type[0]));
    }

    private static boolean isMountain(OrigenTerrainFamily family) {
        switch (family) {
            case TERRACE_MOUNTAINS:
            case ALPINE:
            case ERODED_VALLEY_MOUNTAINS:
            case LOOP_MOUNTAINS:
            case MOUNTAIN_CLEARINGS:
            case BASIN_MOUNTAIN:
            case FORTRESS_OUTER:
            case ROCKY_PILLARS:
            case SUMMIT_STREAMS:
                return true;
            default:
                return false;
        }
    }

    private static void registerDictionaryTypes(BiomeGenBase biome, BiomeDictionary.Type... types) {
        if (!BiomeDictionary.registerBiomeType(biome, types)) {
            throw new IllegalStateException("Could not register Forge biome-dictionary types for " + biome.biomeName);
        }
    }

    private static RegistryState requireState() {
        RegistryState current = state;
        if (current == null) {
            throw new IllegalStateException("Biomic biomes have not been registered");
        }
        return current;
    }

    private static final class BiomeGenBiomic extends BiomeGenBase {

        private BiomeGenBiomic(int id, boolean register, String name, int color, float temperature, float rainfall,
            float rootHeight, float variation, SpawnProfile spawnProfile) {
            this(
                id,
                register,
                name,
                color,
                temperature,
                rainfall,
                rootHeight,
                variation,
                spawnProfile,
                -1,
                -1,
                0xFFFFFF,
                -1);
        }

        private final int grassColor;
        private final int foliageColor;
        private final int skyColor;

        private BiomeGenBiomic(int id, boolean register, String name, int color, float temperature, float rainfall,
            float rootHeight, float variation, SpawnProfile spawnProfile, int grassColor, int foliageColor,
            int waterColor, int skyColor) {
            super(id, register);
            this.grassColor = grassColor;
            this.foliageColor = foliageColor;
            this.skyColor = skyColor;
            setBiomeName(name);
            setColor(color);
            setTemperatureRainfall(temperature, rainfall);
            setHeight(new Height(rootHeight, variation));
            waterColorMultiplier = waterColor;
            if (spawnProfile == SpawnProfile.UNASSIGNED) {
                // New estuary identity assigns no fauna; JAS retains natural-spawn authority.
                spawnableCreatureList.clear();
                spawnableWaterCreatureList.clear();
                spawnableCaveCreatureList.clear();
                spawnableMonsterList.clear();
            }
            if (spawnProfile == SpawnProfile.LUSH_CAVE) {
                spawnableCreatureList.clear();
                spawnableWaterCreatureList.clear();
            } else if (spawnProfile == SpawnProfile.NETHER) {
                spawnableCreatureList.clear();
                spawnableWaterCreatureList.clear();
                spawnableCaveCreatureList.clear();
                spawnableMonsterList.clear();
                setDisableRain();
            } else if (spawnProfile == SpawnProfile.HELL) {
                spawnableCreatureList.clear();
                spawnableWaterCreatureList.clear();
                spawnableCaveCreatureList.clear();
                spawnableMonsterList.clear();
                spawnableMonsterList.add(new SpawnListEntry(net.minecraft.entity.monster.EntityGhast.class, 50, 4, 4));
                spawnableMonsterList
                    .add(new SpawnListEntry(net.minecraft.entity.monster.EntityPigZombie.class, 100, 4, 4));
                spawnableMonsterList
                    .add(new SpawnListEntry(net.minecraft.entity.monster.EntityMagmaCube.class, 1, 4, 4));
                setDisableRain();
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getBiomeGrassColor(int blockX, int blockY, int blockZ) {
            return grassColor < 0 ? super.getBiomeGrassColor(blockX, blockY, blockZ)
                : getModdedBiomeGrassColor(grassColor);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getBiomeFoliageColor(int blockX, int blockY, int blockZ) {
            return foliageColor < 0 ? super.getBiomeFoliageColor(blockX, blockY, blockZ)
                : getModdedBiomeFoliageColor(foliageColor);
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getSkyColorByTemp(float temperature) {
            return skyColor < 0 ? super.getSkyColorByTemp(temperature) : skyColor;
        }
    }

    private enum SpawnProfile {
        UNASSIGNED,
        TEMPERATE_SURFACE,
        LUSH_CAVE,
        NETHER,
        /** The vanilla Hell biome's ghasts, zombie pigmen and magma cubes. */
        HELL
    }

    private static final class Definition {

        private final String key;
        private final BiomeGenBase biome;
        private final Set<String> tags;

        private Definition(String key, BiomeGenBase biome, Set<String> tags) {
            this.key = key;
            this.biome = biome;
            this.tags = tags;
        }
    }

    private static final class RegistryState {

        private final int[] ids;
        private final Definition lowlands;
        private final Definition archipelago;
        private final Definition highlands;
        private final Definition lushCaves;
        private final Map<String, Definition> ru;
        private final Map<OrigenBiomeProfile, Definition> origen;
        private final Map<TartarusBiomes, Definition> tartarus;
        private final Map<String, Definition> byKey;
        private final Map<BiomeGenBase, Definition> byBiome;

        private RegistryState(int[] ids, Definition lowlands, Definition archipelago, Definition highlands,
            Definition lushCaves, Definition skyDonut, Definition aeropelago, Map<String, Definition> ru,
            Map<OrigenBiomeProfile, Definition> origen, Definition soulblight, Definition whistling, Definition estuary,
            Map<TartarusBiomes, Definition> tartarus) {
            this.ids = ids.clone();
            this.lowlands = lowlands;
            this.archipelago = archipelago;
            this.highlands = highlands;
            this.lushCaves = lushCaves;
            this.ru = Collections.unmodifiableMap(new LinkedHashMap<>(ru));
            this.origen = Collections.unmodifiableMap(new LinkedHashMap<>(origen));
            this.tartarus = Collections.unmodifiableMap(new java.util.EnumMap<>(tartarus));

            Map<String, Definition> keys = new LinkedHashMap<>();
            Map<BiomeGenBase, Definition> biomes = new IdentityHashMap<>();
            for (Definition definition : Arrays
                .asList(lowlands, archipelago, highlands, lushCaves, skyDonut, aeropelago)) {
                keys.put(definition.key, definition);
                biomes.put(definition.biome, definition);
            }
            for (Definition definition : ru.values()) {
                keys.put(definition.key, definition);
                biomes.put(definition.biome, definition);
            }
            for (Definition definition : origen.values()) {
                keys.put(definition.key, definition);
                biomes.put(definition.biome, definition);
            }
            for (Definition definition : Arrays.asList(soulblight, whistling, estuary)) {
                keys.put(definition.key, definition);
                biomes.put(definition.biome, definition);
            }
            for (Definition definition : tartarus.values()) {
                keys.put(definition.key, definition);
                biomes.put(definition.biome, definition);
            }
            byKey = Collections.unmodifiableMap(keys);
            byBiome = Collections.unmodifiableMap(biomes);
        }
    }
}
