package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

public class FloraPalettesTest {

    /** Biomic's own palettes, as a world without overlays loads them. */
    private final FloraPalettes palettes = FloraPalettes
        .parse(industries.angelic.biomic.api.EcologyOverlays.compose("palettes.json"));

    @Test
    public void origenProfilesFoldIntoTheirNamedHabitat() {
        assertEquals(
            "origen:FOLIAGE_FORTRESS",
            palettes.origen(OrigenBiomeProfile.FOLIAGE_FORTRESS_OUTER)
                .key());
        assertEquals(
            "origen:LUSH_LOOPS",
            palettes.origen(OrigenBiomeProfile.LOOPS_RIVER)
                .key());
        assertNotNull(palettes.origen(OrigenBiomeProfile.FLOWERING_FOREST));
    }

    @Test
    public void biomicsOwnPalettesNameOnlyItsOwnBlocks() {
        FloraPalettes own = FloraPalettes.parse(industries.angelic.biomic.api.EcologyOverlays.compose("palettes.json"));
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
            FloraPalettes.Habitat habitat = own.origen(profile);
            if (habitat != null) for (FloraPalettes.Plant plant : habitat.plants()) assertTrue(
                plant.id(),
                plant.id()
                    .matches("(minecraft|etfuturum|biomic):.*"));
        }
    }

    @Test
    public void untouchedHabitatsAddNothing() {
        assertFalse(
            palettes.habitat("regions_unexplored:alpha_grove")
                .adds());
        assertFalse(
            palettes.habitat("regions_unexplored:flower_fields")
                .adds());
    }

    @Test
    public void theBambooForestKeepsItsOwnBamboo() {
        for (FloraPalettes.Plant plant : palettes.habitat("regions_unexplored:bamboo_forest")
            .plants())
            assertFalse(
                plant.id(),
                plant.id()
                    .startsWith("plantmegapack:bamboo"));
    }
}
