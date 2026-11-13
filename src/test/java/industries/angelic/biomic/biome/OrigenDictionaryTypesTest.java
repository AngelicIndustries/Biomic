package industries.angelic.biomic.biome;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import net.minecraftforge.common.BiomeDictionary.Type;

import org.junit.Test;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.origen.OrigenTerrainFamily;

/**
 * Other mods read an Origen biome's Forge types (Waystones names a waystone by the first of its words a biome has:
 * ocean, beach, river, mushroom, swamp, jungle, mesa, savanna, sandy, coniferous, snowy, mountain, forest, hills,
 * plains). Every land biome has a landscape, and the biomes once all called plains or hills read as what they are.
 */
public class OrigenDictionaryTypesTest {

    @Test
    public void everyLandBiomeHasALandscape() {
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
            if (profile.getKind() == OrigenBiomeProfile.Kind.CAVE || profile.getKind() == OrigenBiomeProfile.Kind.OCEAN
                || profile.getTerrainFamily() == OrigenTerrainFamily.RIVER_CHANNEL) continue;
            assertTrue(profile + " has a landscape", BiomicBiomes.landscape(profile).length > 0);
        }
    }

    @Test
    public void biomesReadAsWhatTheyAre() {
        assertTrue(has(OrigenBiomeProfile.BLACK_FOREST, Type.FOREST));
        assertFalse("a forest first, though on mountains", has(OrigenBiomeProfile.BLACK_FOREST, Type.MOUNTAIN));
        assertTrue(has(OrigenBiomeProfile.FLOWERING_FOREST, Type.FOREST));
        assertTrue(has(OrigenBiomeProfile.DESERT, Type.SANDY));
        assertTrue(has(OrigenBiomeProfile.MANGROVE_SWAMP, Type.SWAMP));
        assertTrue(has(OrigenBiomeProfile.BEACH, Type.BEACH));
        assertTrue(has(OrigenBiomeProfile.PALM_BEACH, Type.BEACH));
        assertTrue(has(OrigenBiomeProfile.TALL_TIMBERLAND, Type.CONIFEROUS));
        assertTrue(has(OrigenBiomeProfile.MESA_MONUMENTS, Type.MESA));
        assertTrue(has(OrigenBiomeProfile.ALPINE_ASCENDANCY, Type.MOUNTAIN));
    }

    private static boolean has(OrigenBiomeProfile profile, Type type) {
        List<Type> types = Arrays.asList(BiomicBiomes.landscape(profile));
        return types.contains(type);
    }
}
