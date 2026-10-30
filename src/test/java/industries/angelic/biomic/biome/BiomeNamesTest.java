package industries.angelic.biomic.biome;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Mods that save biomes by display name (Distant Horizons keeps its LOD data that way, JustAnotherSpawner its biome
 * groups) merge two biomes that share one, so every biome Biomic constructs needs a name no other biome uses: not
 * vanilla's, not Biomic's own, and not those of the biome mods Biomic is played with.
 */
public class BiomeNamesTest {

    /**
     * Biome names in Biomes O' Plenty 2.1.0.2308, Et Futurum Requiem 2.6.64 and Netherlicious 3.2.8, from their jars.
     */
    private static final Set<String> OTHER_MODS = Set.of(
        "Alps",
        "Alps Forest",
        "Arctic",
        "Bamboo Forest",
        "Bayou",
        "Bog",
        "Boneyard",
        "Boreal Forest",
        "Brushland",
        "Canyon",
        "Canyon Ravine",
        "Chaparral",
        "Cherry Blossom Grove",
        "Coniferous Forest",
        "Coral Reef",
        "Corrupted Sands",
        "Crag",
        "Dead Forest",
        "Dead Swamp",
        "Deciduous Forest",
        "Dense Forest",
        "Dry River",
        "Eucalyptus Forest",
        "Fen",
        "Flower Field",
        "Frost Forest",
        "Fungi Forest",
        "Garden",
        "Glacier",
        "Grassland",
        "Grove",
        "Heathland",
        "Highland",
        "Jade Cliffs",
        "Kelp Forest",
        "Land of Lakes",
        "Land of Lakes Marsh",
        "Lavender Fields",
        "Lush Desert",
        "Lush River",
        "Lush Swamp",
        "Mangrove",
        "Maple Woods",
        "Marsh",
        "Meadow",
        "Meadow Forest",
        "Moor",
        "Mountain",
        "Mystic Grove",
        "Oasis",
        "Ominous Woods",
        "Orchard",
        "Origin Valley",
        "Outback",
        "Phantasmagoric Inferno",
        "Polar Chasm",
        "Prairie",
        "Quagmire",
        "Rainforest",
        "Redwood Forest",
        "Sacred Springs",
        "Scrubland",
        "Seasonal Forest",
        "Seasonal Forest Clearing",
        "Shield",
        "Shrubland",
        "Silkglades",
        "Sludgepit",
        "Snowy Coniferous Forest",
        "Spectral Garden",
        "Spruce Woods",
        "Steppe",
        "Temperate Rainforest",
        "Thicket",
        "Tropical Rainforest",
        "Tropics",
        "Tundra",
        "Undergarden",
        "Visceral Heap",
        "Volcano",
        "Wasteland",
        "Wetland",
        "Woodland",
        "Xeric Shrubland",
        "Abyssal Shadowland",
        "Basalt Deltas",
        "Burning Desert",
        "Crimson Forest",
        "Crystalline Crag",
        "Foxfire Swamp",
        "Ruptured Chasm",
        "Soul Sand Valley",
        "Tainted Shroom Cave",
        "Warped Forest");

    @BeforeClass
    public static void registerBiomes() {
        BiomicBiomes.register();
    }

    @Test
    public void biomicBiomeNamesAreUnique() {
        Set<BiomeGenBase> biomic = Collections.newSetFromMap(new IdentityHashMap<>());
        biomic.addAll(BiomicBiomes.all());
        for (String key : List
            .of("alexscaves:forlorn_hollows", "alexscaves:primordial_caves", "alexscaves:abyssal_chasm")) {
            BiomeGenBase visual = AlexCavesBiomeVisuals.colorBiome(key);
            if (visual != null) biomic.add(visual);
        }
        Set<BiomeGenBase> every = Collections.newSetFromMap(new IdentityHashMap<>());
        every.addAll(biomic);
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) if (biome != null) every.add(biome);

        Map<String, List<String>> byName = new TreeMap<>();
        for (BiomeGenBase biome : every)
            byName.computeIfAbsent(biome.biomeName.toLowerCase(), name -> new ArrayList<>())
                .add((biomic.contains(biome) ? BiomicBiomes.getKey(biome) : "vanilla") + " (" + biome.biomeID + ")");
        List<String> collisions = new ArrayList<>();
        for (BiomeGenBase biome : biomic) {
            List<String> owners = byName.get(biome.biomeName.toLowerCase());
            boolean otherMod = OTHER_MODS.stream()
                .anyMatch(biome.biomeName::equalsIgnoreCase);
            if (owners.size() > 1 || otherMod)
                collisions.add(biome.biomeName + ": " + owners + (otherMod ? " and another mod's" : ""));
        }
        Collections.sort(collisions);
        assertTrue("Biome names used twice: " + collisions, collisions.isEmpty());
    }
}
