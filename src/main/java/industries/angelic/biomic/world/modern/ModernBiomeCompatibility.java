package industries.angelic.biomic.world.modern;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.ru.RuRuntimeManifest;
import industries.angelic.biomic.biome.ru.RuRuntimeManifest.BiomeEntry;

/** Maps exact modern biome keys onto the available 1.7 identities without changing selection. */
public final class ModernBiomeCompatibility {

    private static final Map<String, String> RU_KEYS = ruKeys();

    private ModernBiomeCompatibility() {}

    public static String storedKey(String modernBiome) {
        String ruKey = RU_KEYS.get(modernBiome);
        return ruKey == null ? modernBiome : ruKey;
    }

    public static BiomeGenBase biome(String modernBiome) {
        if (industries.angelic.biomic.api.BiomeKeys.ESTUARY.equals(modernBiome)) {
            BiomeGenBase estuary = BiomicBiomes.getBiome(modernBiome);
            if (estuary == null) throw new IllegalStateException("Missing registered estuary identity");
            return estuary;
        }
        BiomeGenBase nativeBiome = industries.angelic.biomic.compat.biome.NativeBiomeCatalogue.biome(modernBiome);
        if (nativeBiome != null) return nativeBiome;
        String ruKey = RU_KEYS.get(modernBiome);
        if (ruKey != null) {
            BiomeGenBase biome = BiomicBiomes.getBiome(ruKey);
            if (biome == null) throw new IllegalStateException("Missing registered RU biome " + ruKey);
            return biome;
        }
        return vanilla(modernBiome);
    }

    public static BiomeGenBase biomeForStoredKey(String storedKey) {
        return storedKey != null && storedKey.startsWith("minecraft:") ? vanilla(storedKey) : null;
    }

    private static BiomeGenBase vanilla(String id) {
        return switch (id) {
            case "minecraft:ocean", "minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:cold_ocean" -> BiomeGenBase.ocean;
            case "minecraft:deep_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:deep_cold_ocean" -> BiomeGenBase.deepOcean;
            case "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean" -> BiomeGenBase.frozenOcean;
            case "minecraft:river" -> BiomeGenBase.river;
            case "minecraft:frozen_river" -> BiomeGenBase.frozenRiver;
            case "minecraft:beach" -> BiomeGenBase.beach;
            case "minecraft:snowy_beach" -> BiomeGenBase.coldBeach;
            case "minecraft:stony_shore" -> BiomeGenBase.stoneBeach;
            case "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow" -> BiomeGenBase.plains;
            case "minecraft:desert" -> BiomeGenBase.desert;
            case "minecraft:savanna" -> BiomeGenBase.savanna;
            case "minecraft:savanna_plateau" -> BiomeGenBase.savannaPlateau;
            case "minecraft:windswept_savanna" -> BiomeGenBase.savanna;
            case "minecraft:badlands", "minecraft:eroded_badlands" -> BiomeGenBase.mesa;
            case "minecraft:wooded_badlands" -> BiomeGenBase.mesaPlateau_F;
            case "minecraft:forest", "minecraft:flower_forest" -> BiomeGenBase.forest;
            case "minecraft:birch_forest", "minecraft:old_growth_birch_forest" -> BiomeGenBase.birchForest;
            case "minecraft:dark_forest" -> BiomeGenBase.roofedForest;
            case "minecraft:taiga" -> BiomeGenBase.taiga;
            case "minecraft:snowy_taiga" -> BiomeGenBase.coldTaiga;
            case "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga" -> BiomeGenBase.megaTaiga;
            case "minecraft:jungle", "minecraft:bamboo_jungle" -> BiomeGenBase.jungle;
            case "minecraft:sparse_jungle" -> BiomeGenBase.jungleEdge;
            case "minecraft:swamp", "minecraft:mangrove_swamp" -> BiomeGenBase.swampland;
            case "minecraft:mushroom_fields" -> BiomeGenBase.mushroomIsland;
            case "minecraft:snowy_plains", "minecraft:ice_spikes" -> BiomeGenBase.icePlains;
            case "minecraft:grove" -> BiomeGenBase.coldTaiga;
            case "minecraft:snowy_slopes", "minecraft:frozen_peaks", "minecraft:jagged_peaks" -> BiomeGenBase.iceMountains;
            case "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:stony_peaks" -> BiomeGenBase.extremeHills;
            case "minecraft:windswept_forest" -> BiomeGenBase.extremeHillsPlus;
            case "minecraft:cherry_grove" -> BiomeGenBase.birchForest;
            case "minecraft:lush_caves" -> BiomicBiomes.getLushCaves();
            case "minecraft:dripstone_caves", "minecraft:deep_dark" -> BiomeGenBase.extremeHills;
            default -> throw new IllegalArgumentException("No 1.7 biome identity for modern biome " + id);
        };
    }

    private static Map<String, String> ruKeys() {
        Map<String, String> result = new LinkedHashMap<>();
        for (BiomeEntry entry : RuRuntimeManifest.biomes()) {
            result.put("regions_unexplored:" + entry.getName(), entry.getKey());
        }
        return Collections.unmodifiableMap(result);
    }
}
