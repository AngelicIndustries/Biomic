package industries.angelic.biomic.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GardensWhistlecaneEffectsTest {

    @Test
    public void preservesPinnedPrimaryAndTrailingSmokeCadence() {
        int primary = 0, trailing = 0;
        for (int remaining = GardensWhistlecaneEffects.WHISTLING_TIME; remaining
            >= -GardensWhistlecaneEffects.SMOKE_TIME; remaining--) {
            primary += GardensWhistlecaneEffects.primaryParticles(remaining);
            if (GardensWhistlecaneEffects.hasTrailingParticle(remaining)) trailing++;
        }
        assertEquals(102, primary);
        assertEquals(16, trailing);
    }
}
