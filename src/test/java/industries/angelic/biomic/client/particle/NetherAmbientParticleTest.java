package industries.angelic.biomic.client.particle;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NetherAmbientParticleTest {

    @Test
    public void vanillaLifetimesStretchUpToFiveTimes() {
        assertEquals(80, NetherAmbientParticle.lifetime(0, 16));
        assertEquals(16, NetherAmbientParticle.lifetime(1, 16));
        assertEquals(100, NetherAmbientParticle.lifetime(0, 20));
        assertEquals(20, NetherAmbientParticle.lifetime(1, 20));
    }

    @Test
    public void ashShrinksThroughTheSmokePuffs() {
        assertEquals(7, NetherAmbientParticle.frame(0, 40));
        assertEquals(4, NetherAmbientParticle.frame(20, 40));
        assertEquals(0, NetherAmbientParticle.frame(40, 40));
        assertEquals(0, NetherAmbientParticle.frame(41, 40));
    }
}
