package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.modern.ModernDecorationGraph;

public class SkyIslandHabitatPoolsTest {

    /** Islands that follow the ground host its habitat with the modern decoration graph; every pick must be in it. */
    @Test
    public void everyGroundPickIsAHostedHabitat() {
        Set<String> hosted = new ModernDecorationGraph().biomeIds();
        for (OrigenBiomeProfile profile : OrigenBiomeProfile.values()) {
            int family = RegionalHabitatSampler.family(profile);
            if (family == 0) {
                String landform = RegionalHabitatSampler.landform(profile);
                assertTrue(profile + " -> " + landform, landform == null || hosted.contains(landform));
                continue;
            }
            if (family >= 33) continue; // Water: islands choose their own.
            String[] pool = family >= 10 ? RegionalHabitatSampler.landmarkOptions(family)
                : RegionalHabitatSampler.firstWaves(family);
            for (String habitat : pool)
                assertTrue(profile + " -> " + habitat, habitat.isEmpty() || hosted.contains(habitat));
        }
    }

    @Test
    public void cavesAndWaterHaveNoLandform() {
        for (OrigenBiomeProfile profile : new OrigenBiomeProfile[] { OrigenBiomeProfile.LUSH_CAVES,
            OrigenBiomeProfile.DEEP_DARK, OrigenBiomeProfile.FROZEN_OCEAN, OrigenBiomeProfile.VERDANT_RIVER })
            assertNull(RegionalHabitatSampler.landform(profile));
    }
}
