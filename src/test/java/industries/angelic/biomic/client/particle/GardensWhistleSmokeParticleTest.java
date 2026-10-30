package industries.angelic.biomic.client.particle;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GardensWhistleSmokeParticleTest {

    @Test
    public void sourceLifetimeFormulaIsBounded() {
        assertEquals(40, GardensWhistleSmokeParticle.lifetime(0));
        assertEquals(8, GardensWhistleSmokeParticle.lifetime(1));
    }

    @Test
    public void exactSourceTwelveFrameSheetAnimatesForward() {
        assertEquals(0, GardensWhistleSmokeParticle.frame(0, 40));
        assertEquals(11, GardensWhistleSmokeParticle.frame(39, 40));
    }
}
