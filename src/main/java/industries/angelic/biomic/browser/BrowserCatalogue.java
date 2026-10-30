package industries.angelic.biomic.browser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.util.Constants;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.api.BiomeKeys;
import industries.angelic.biomic.api.BiomeTags;
import industries.angelic.biomic.api.EcologyOverlays;
import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.ru.RuBiomeCatalogue;
import industries.angelic.biomic.compat.biome.BopNativeBiomes;
import industries.angelic.biomic.compat.biome.NativeHabitatSettings;
import industries.angelic.biomic.compat.biome.SaltMarshCompat;
import industries.angelic.biomic.world.terrain.HostedIslandBiomes;
import industries.angelic.biomic.world.terrain.HostedVegetationPolicy;

/**
 * Every biome the biome browser lists: Biomic's own, Regions Unexplored's, Origen's, Tartarus' and Gardens of the
 * Dead's, and the biomes Biomic hosts from other mods (vanilla, BOP, SaltyMod, Netherlicious), with where each lives in
 * this world, its climate and the plants its palette names. Built on the server, which knows the world's setup (its
 * pinned Nether neighbours above all), and sent to the browser.
 */
public final class BrowserCatalogue {

    /** Where a biome lives. */
    public enum Place {
        SURFACE,
        SKY,
        CAVES,
        NETHER
    }

    /** One biome as the browser shows it. */
    public record Entry(int id, String key, String name, String source, Set<Place> places, float temperature,
        float rainfall, List<String> types, String habitat, List<String> plants) {

        NBTTagCompound write() {
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setInteger("id", id);
            nbt.setString("key", key);
            nbt.setString("name", name);
            nbt.setString("source", source);
            int mask = 0;
            for (Place place : places) mask |= 1 << place.ordinal();
            nbt.setInteger("places", mask);
            nbt.setFloat("temperature", temperature);
            nbt.setFloat("rainfall", rainfall);
            nbt.setTag("types", strings(types));
            nbt.setString("habitat", habitat);
            nbt.setTag("plants", strings(plants));
            return nbt;
        }

        static Entry read(NBTTagCompound nbt) {
            Set<Place> places = new LinkedHashSet<>();
            int mask = nbt.getInteger("places");
            for (Place place : Place.values()) if ((mask & 1 << place.ordinal()) != 0) places.add(place);
            return new Entry(
                nbt.getInteger("id"),
                nbt.getString("key"),
                nbt.getString("name"),
                nbt.getString("source"),
                places,
                nbt.getFloat("temperature"),
                nbt.getFloat("rainfall"),
                strings(nbt.getTagList("types", Constants.NBT.TAG_STRING)),
                nbt.getString("habitat"),
                strings(nbt.getTagList("plants", Constants.NBT.TAG_STRING)));
        }
    }

    private static final int PLANTS = 8;

    /**
     * Biomic's registered biomes no generator places (docs/biome-atlas-seed42.md, "Not found or not generated"), with
     * where they would live: the surface sampler's fallbacks, which its Origen profiles never reach, an estuary nothing
     * selects, and a lush caves key no volume writes (this world's lush caves are Origen's). The browser probe still
     * looks for each, so a revision that brings one back shows up there.
     */
    static final java.util.Map<String, Place> NEVER_GENERATED = java.util.Map.of(
        BiomeKeys.TEMPERATE_LOWLANDS,
        Place.SURFACE,
        BiomeKeys.TEMPERATE_ARCHIPELAGO,
        Place.SURFACE,
        BiomeKeys.WINDSWEPT_HIGHLANDS,
        Place.SURFACE,
        BiomeKeys.ESTUARY,
        Place.SURFACE,
        BiomeKeys.LUSH_CAVES,
        Place.CAVES);

    private BrowserCatalogue() {}

    /**
     * The catalogue for a world whose Nether hosts {@code netherBiomes} (Tartarus' and its pinned neighbours') and
     * whose
     * overworld hosts the native habitats its save names. A registered biome this world never generates (a BOP biome
     * outside the save's selection, the salt marsh without SaltyMod's) lives nowhere: it has no places.
     */
    public static List<Entry> build(Collection<BiomeGenBase> netherBiomes, NativeHabitatSettings natives) {
        Set<Integer> nether = new HashSet<>();
        for (BiomeGenBase biome : netherBiomes) if (biome != null) nether.add(biome.biomeID);
        Set<String> islands = new HashSet<>(HostedIslandBiomes.habitats());
        JsonObject palettes = EcologyOverlays.load("palettes.json")
            .getAsJsonObject("habitats");
        List<Entry> entries = new ArrayList<>();
        for (BiomeGenBase biome : biomes()) {
            String key = BiomicBiomes.getKey(biome);
            if (key == null) continue;
            String[] origin = origin(biome, key);
            String source = origin[0], habitat = origin[1];
            Set<String> tags = BiomicBiomes.getTags(biome);
            Set<Place> places = new LinkedHashSet<>();
            boolean inNether = nether.contains(biome.biomeID) || TartarusBiomes.fromBiomeId(biome.biomeID) != null
                || tags.contains(BiomeTags.NETHER);
            if (inNether) places.add(Place.NETHER);
            else if (key.startsWith("biomesoplenty:") && !natives.bopHabitats()
                .contains(key) || key.equals(SaltMarshCompat.BIOME) && !natives.saltMarsh()
                || netherType(biome)
                || NEVER_GENERATED.containsKey(key)) {
                    // Not in this world.
                } else {
                    boolean cave = tags.contains(BiomeTags.CAVE), floating = tags.contains(BiomeTags.FLOATING_ISLAND);
                    if (cave) places.add(Place.CAVES);
                    if (!cave && !floating || tags.contains(BiomeTags.SURFACE)) places.add(Place.SURFACE);
                    if (islands.contains(habitat) && !marine(habitat) || floating) places.add(Place.SKY);
                }
            List<String> types = new ArrayList<>();
            try {
                for (BiomeDictionary.Type type : BiomeDictionary.getTypesForBiome(biome)) types.add(
                    type.name()
                        .toLowerCase(Locale.ROOT));
            } catch (ArrayIndexOutOfBoundsException noEndlessIds) {
                // Without EndlessIDs (unit tests) the dictionary holds ids below 256 only.
            }
            entries.add(
                new Entry(
                    biome.biomeID,
                    key,
                    biome.biomeName,
                    source,
                    places,
                    biome.temperature,
                    biome.rainfall,
                    types,
                    habitat,
                    plants(palettes, habitat)));
        }
        return entries;
    }

    /** A sea habitat, which islands never take: they follow dry ground. */
    private static boolean marine(String habitat) {
        return HostedVegetationPolicy.minimumWaterDepth(habitat) > 0 || BopNativeBiomes.marine(habitat);
    }

    /** A Nether biome by its mod's word (a Netherlicious biome Tartarus leaves out, say). */
    private static boolean netherType(BiomeGenBase biome) {
        try {
            return BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.NETHER);
        } catch (ArrayIndexOutOfBoundsException noEndlessIds) {
            return false;
        }
    }

    /** Biomic's own first (the numeric registry holds them only with EndlessIDs), then what else it hosts. */
    private static Set<BiomeGenBase> biomes() {
        Set<BiomeGenBase> biomes = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        biomes.addAll(BiomicBiomes.all());
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) if (biome != null) biomes.add(biome);
        return biomes;
    }

    /** The biome of a catalogue key, or null. */
    static BiomeGenBase biome(String key) {
        for (BiomeGenBase biome : biomes()) if (key.equals(BiomicBiomes.getKey(biome))) return biome;
        return null;
    }

    /** {source, modern habitat key} of a biome Biomic knows. */
    private static String[] origin(BiomeGenBase biome, String key) {
        if (TartarusBiomes.fromBiomeId(biome.biomeID) != null) return new String[] { "Tartarus", key };
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values())
            if (BiomicBiomes.getOrigenBiome(profile) == biome)
                return new String[] { "Origen", "origen:" + profile.name() };
        if (RuBiomeCatalogue.forBiomeKey(key) != null)
            return new String[] { "Regions Unexplored", "regions_unexplored:" + key.substring(key.indexOf(':') + 1) };
        if (key.equals("biomic:soulblight_forest") || key.equals("biomic:whistling_woods"))
            return new String[] { "Gardens of the Dead", key };
        String namespace = key.substring(0, key.indexOf(':'));
        String source = switch (namespace) {
            case "biomic" -> "Biomic";
            case "minecraft" -> "Vanilla";
            case "biomesoplenty" -> "Biomes O' Plenty";
            case "saltymod" -> "SaltyMod";
            case "netherlicious" -> "Netherlicious";
            default -> namespace;
        };
        return new String[] { source, key };
    }

    /** The first plants the habitat's palette names, signature first. */
    private static List<String> plants(JsonObject palettes, String habitat) {
        List<String> plants = new ArrayList<>();
        JsonElement entry = palettes.get(habitat);
        if (entry == null || !entry.isJsonObject()) return plants;
        for (String tier : new String[] { "signature", "common", "uncommon", "rare" }) {
            JsonArray list = entry.getAsJsonObject()
                .getAsJsonArray(tier);
            if (list == null) continue;
            for (JsonElement plant : list) {
                if (plants.size() >= PLANTS) return plants;
                if (!plants.contains(plant.getAsString())) plants.add(plant.getAsString());
            }
        }
        return plants;
    }

    static NBTTagList write(List<Entry> entries) {
        NBTTagList list = new NBTTagList();
        for (Entry entry : entries) list.appendTag(entry.write());
        return list;
    }

    static List<Entry> read(NBTTagList list) {
        List<Entry> entries = new ArrayList<>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) entries.add(Entry.read(list.getCompoundTagAt(i)));
        return entries;
    }

    private static NBTTagList strings(List<String> values) {
        NBTTagList list = new NBTTagList();
        for (String value : values) list.appendTag(new NBTTagString(value));
        return list;
    }

    private static List<String> strings(NBTTagList list) {
        List<String> values = new ArrayList<>(list.tagCount());
        for (int i = 0; i < list.tagCount(); i++) values.add(list.getStringTagAt(i));
        return values;
    }
}
