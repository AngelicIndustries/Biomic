package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/**
 * An Origen biome's climate decides its temperature (snow falling and lying), its village style and whether an igloo
 * stands there; ClimateClasses decides which habitats it hosts. They must agree: Cold Steppe, which Origen gives
 * vanilla's taiga, once read as snowy and grew snow villages on bare ground.
 */
public class OrigenClimateTest {

    @Test
    public void snowyGroundIsSnowyAndBareColdIsCold() {
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
            int climate = ClimateClasses.of(profile.name());
            if (climate == 0)
                assertEquals(profile + " has snowy ground", OrigenBiomeProfile.Climate.SNOWY, profile.getClimate());
            if (climate == 1)
                assertEquals(profile + " is cold and bare", OrigenBiomeProfile.Climate.COLD, profile.getClimate());
        }
        assertEquals(OrigenBiomeProfile.Climate.COLD, OrigenBiomeProfile.COLD_STEPPE.getClimate());
    }
}
