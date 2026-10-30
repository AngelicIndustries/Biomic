package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import org.junit.Test;

public class AbyssalHostPollutionPolicyTest {

    @Test
    public void rejectsEveryObservedOrigenIntrusionFamily() {
        assertTrue(AbyssalHostPollutionPolicy.displaced("global-preprocessors", "LAVA_FLOOR"));
        assertTrue(AbyssalHostPollutionPolicy.displaced("global-preprocessors", "UNDERGROUND_LAVA_COLUMNS"));
        assertTrue(AbyssalHostPollutionPolicy.displaced("landforms", "FOSSIL_OCEAN"));
        assertTrue(AbyssalHostPollutionPolicy.displaced("trees", "DENDRIFORM_REEF"));
        assertTrue(AbyssalHostPollutionPolicy.displaced("flora", "CORAL_PLANTS"));
        assertTrue(AbyssalHostPollutionPolicy.displaced("flora", "CORAL_FANS"));
    }

    @Test
    public void preservesSourceOwnedOreAndMagmaStages() {
        assertFalse(AbyssalHostPollutionPolicy.displaced("ores", "DIAMOND_ORE"));
        assertFalse(AbyssalHostPollutionPolicy.displaced("deposits", "UNDERGROUND_MAGMA_BLOCK_DEPOSITS"));
        assertFalse(AbyssalHostPollutionPolicy.displaced("postprocessors", "CONTAIN_FLOATING_WATER_DEEP"));
    }
}
