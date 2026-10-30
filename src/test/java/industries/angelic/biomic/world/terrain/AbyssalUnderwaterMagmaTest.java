package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AbyssalUnderwaterMagmaTest {

    @Test
    public void pinnedVanillaConfigurationIsUsedByPhysicalPopulation() {
        var config = HostedAbyssalRuntime.underwaterMagmaConfig();
        assertEquals(44, config.minimumAttempts());
        assertEquals(9, config.attemptSpread());
        assertEquals(5, config.floorSearchRange());
        assertEquals(1, config.radius());
        assertEquals(.5F, config.probability(), 0F);
    }
}
