package industries.angelic.biomic.client.particle;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PrimordialFlyParticleTest {

    @Test
    public void alternatesPinnedTwoFrameWingCadence() {
        assertEquals(0, PrimordialFlyParticle.frame(0));
        assertEquals(0, PrimordialFlyParticle.frame(1));
        assertEquals(1, PrimordialFlyParticle.frame(2));
        assertEquals(1, PrimordialFlyParticle.frame(3));
        assertEquals(0, PrimordialFlyParticle.frame(4));
    }

    @Test
    public void rotatesSourceOrbitOffsetInEitherDirection() {
        // Vec3.yRot uses x'=x cos+z sin, z'=z cos-x sin.
        assertArrayEquals(new double[] { 0, -1 }, PrimordialFlyParticle.orbitPosition(30, false, 3, 1, 0), 1.0E-9);
        assertArrayEquals(new double[] { 0, 1 }, PrimordialFlyParticle.orbitPosition(30, true, 3, 1, 0), 1.0E-9);
    }
}
