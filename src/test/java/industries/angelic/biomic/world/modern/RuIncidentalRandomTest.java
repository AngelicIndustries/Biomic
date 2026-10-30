package industries.angelic.biomic.world.modern;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.world.replay.RuIncidentalRandom;

public class RuIncidentalRandomTest {

    @Test
    public void sourceCosmeticEntropyIsRepeatableAndDoesNotUseATraversalCounter() {
        for (int x = -64; x < 64; x++) {
            java.util.Random first = RuIncidentalRandom.random(42, x, 180, -1600, RuIncidentalRandom.LEAF_BLOB);
            java.util.Random second = RuIncidentalRandom.random(42, x, 180, -1600, RuIncidentalRandom.LEAF_BLOB);
            for (int i = 0; i < 12; i++) Assert.assertEquals(first.nextInt(16), second.nextInt(16));
        }
        Assert.assertNotEquals(
            RuIncidentalRandom.random(42, 1, 2, 3, RuIncidentalRandom.TOP)
                .nextLong(),
            RuIncidentalRandom.random(42, 1, 2, 3, RuIncidentalRandom.ROOT)
                .nextLong());
        Assert.assertNotEquals(
            RuIncidentalRandom.random(42, 1, 2, 3, RuIncidentalRandom.TOP)
                .nextLong(),
            RuIncidentalRandom.random(43, 1, 2, 3, RuIncidentalRandom.TOP)
                .nextLong());
    }
}
