package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

public class MarineLayerTest {

    private final MarineLayer marine = new MarineLayer(42L, 191);

    private MarineLayer.Look look(OrigenBiomeProfile profile, String habitat, int floor, int depth, float temperature) {
        return marine.look(new MarineLayer.Water(100, 100, floor, depth, habitat, profile, temperature));
    }

    @Test
    public void profilesTakeTheirDecidedLooks() {
        assertEquals(MarineLayer.Look.POLAR, look(OrigenBiomeProfile.FROZEN_OCEAN, null, 160, 30, 0.0F));
        assertEquals(MarineLayer.Look.ESTUARY, look(OrigenBiomeProfile.MANGROVE_SWAMP, null, 188, 3, 0.8F));
        assertEquals(MarineLayer.Look.LAGOON, look(OrigenBiomeProfile.PALM_BEACH, null, 188, 3, 1.0F));
        assertEquals(MarineLayer.Look.ROCKY, look(OrigenBiomeProfile.SHALE_BEACH, null, 188, 3, 0.5F));
        assertEquals(MarineLayer.Look.DEEP, look(OrigenBiomeProfile.ABYSSAL_ALLEYS, null, 100, 90, 0.5F));
        assertEquals(MarineLayer.Look.LAGOON, look(OrigenBiomeProfile.CORAL_OCEAN, null, 186, 5, 1.0F));
        assertEquals(MarineLayer.Look.REEF, look(OrigenBiomeProfile.CORAL_OCEAN, null, 175, 16, 1.0F));
    }

    @Test
    public void inlandWaterIsFresh() {
        assertEquals(MarineLayer.Look.FRESH, look(null, "minecraft:river", 185, 5, 0.5F));
        assertEquals(MarineLayer.Look.FRESH, look(OrigenBiomeProfile.LUSH_LOOPS, null, 230, 4, 0.9F));
        assertEquals(MarineLayer.Look.FRESH, look(null, "minecraft:forest", 240, 2, 0.7F));
    }

    @Test
    public void temperateOpenSeaMixesFlatsMeadowsAndKelp() {
        MarineLayer.Look look = look(OrigenBiomeProfile.OCEAN, null, 170, 20, 0.5F);
        assertTrue(look == MarineLayer.Look.FLATS || look == MarineLayer.Look.KELP);
        assertEquals(MarineLayer.Look.DEEP, look(OrigenBiomeProfile.OCEAN, null, 140, 50, 0.5F));
    }

    @Test
    public void animatedKelpAndSeagrassAreReplaced() {
        assertTrue(MarineLayer.replaces("KELP"));
        assertTrue(MarineLayer.replaces("MANGROVE_SEAGRASS"));
        assertTrue(MarineLayer.replaces("minecraft:kelp_cold"));
        assertTrue(MarineLayer.replaces("minecraft:seagrass_warm"));
        assertFalse(MarineLayer.replaces("CORAL_PLANTS"));
        assertFalse(MarineLayer.replaces("minecraft:sea_pickle"));
    }
}
