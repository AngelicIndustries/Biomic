package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.internal.terra.noise.samplers.noise.random.PositiveWhiteNoiseSampler;

/** Regression points emitted by Terra 6.4.2 with Origen c01cd797 at seed 42. */
public class OrigenFeatureDistributorParityTest {

    @Test
    public void graniteDepositDistributorMatchesPinnedTerraWindow() {
        PositiveWhiteNoiseSampler sampler = new PositiveWhiteNoiseSampler();
        sampler.setSalt(9141L);
        List<String> matches = new ArrayList<String>();
        for (int z = 2981; z < 3029; z++) {
            for (int x = -3141; x < -3093; x++) {
                if (sampler.noise(42L, x, z) < 1.0D / 128.0D) matches.add(x + "," + z);
            }
        }
        Assert.assertEquals(
            Arrays.asList(
                "-3132,2981",
                "-3105,2983",
                "-3096,2999",
                "-3131,3000",
                "-3139,3001",
                "-3109,3011",
                "-3119,3015",
                "-3100,3015",
                "-3120,3016",
                "-3104,3017",
                "-3128,3020",
                "-3102,3022",
                "-3128,3023",
                "-3094,3023",
                "-3100,3024",
                "-3137,3026",
                "-3135,3028"),
            matches);
    }
}
