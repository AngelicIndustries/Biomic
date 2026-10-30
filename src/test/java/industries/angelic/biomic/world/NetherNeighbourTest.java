package industries.angelic.biomic.world;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.config.ExpansionConfig;

/** The Nether's config switches decide which neighbour biomes a new world pins. */
public class NetherNeighbourTest {

    @BeforeClass
    public static void registerBiomes() {
        BiomicBiomes.register();
    }

    @Test
    public void gardensSwitchKeepsItsBiomesOutOfNewNethers() {
        boolean before = ExpansionConfig.gardensNetherHabitats;
        try {
            ExpansionConfig.gardensNetherHabitats = true;
            assertTrue(
                NetherNeighbour.available()
                    .contains(NetherNeighbour.SOULBLIGHT_FOREST.name()));
            assertTrue(
                NetherNeighbour.available()
                    .contains(NetherNeighbour.WHISTLING_WOODS.name()));
            ExpansionConfig.gardensNetherHabitats = false;
            assertFalse(
                NetherNeighbour.available()
                    .contains(NetherNeighbour.SOULBLIGHT_FOREST.name()));
            assertFalse(
                NetherNeighbour.available()
                    .contains(NetherNeighbour.WHISTLING_WOODS.name()));
            // Regions Unexplored's Nether biomes have no switch of their own.
            assertTrue(
                NetherNeighbour.available()
                    .contains(NetherNeighbour.GLISTERING_MEADOW.name()));
        } finally {
            ExpansionConfig.gardensNetherHabitats = before;
        }
    }
}
