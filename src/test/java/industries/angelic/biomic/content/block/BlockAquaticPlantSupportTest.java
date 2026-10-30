package industries.angelic.biomic.content.block;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class BlockAquaticPlantSupportTest {

    @Test
    public void randomTickChecksKelpAndSeagrassSupportExactlyOnce() {
        for (BlockAquaticPlant.Kind kind : new BlockAquaticPlant.Kind[] { BlockAquaticPlant.Kind.KELP,
            BlockAquaticPlant.Kind.SEAGRASS }) {
            AtomicInteger checks = new AtomicInteger();
            Assert.assertFalse(BlockAquaticPlant.shouldRepairSupport(kind, () -> {
                checks.incrementAndGet();
                return true;
            }));
            Assert.assertEquals(1, checks.get());
            checks.set(0);
            Assert.assertTrue(BlockAquaticPlant.shouldRepairSupport(kind, () -> {
                checks.incrementAndGet();
                return false;
            }));
            Assert.assertEquals(1, checks.get());
        }
    }

    @Test
    public void decorativeAquaticsDoNotRunSupportRepair() {
        for (BlockAquaticPlant.Kind kind : new BlockAquaticPlant.Kind[] { BlockAquaticPlant.Kind.SEA_PICKLE,
            BlockAquaticPlant.Kind.CORAL_PLANT, BlockAquaticPlant.Kind.CORAL_FAN }) {
            AtomicInteger checks = new AtomicInteger();
            Assert.assertFalse(BlockAquaticPlant.shouldRepairSupport(kind, () -> {
                checks.incrementAndGet();
                return false;
            }));
            Assert.assertEquals(0, checks.get());
        }
    }
}
