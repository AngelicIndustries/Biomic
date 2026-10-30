package industries.angelic.biomic.content.block;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.client.BiomicRenderIds;
import sun.misc.Unsafe;

public class RuBlockRenderContractTest {

    @Test
    public void chalkGrassUsesItsLayeredCubeRenderer() throws ReflectiveOperationException {
        int previous = BiomicRenderIds.CHALK_GRASS;
        try {
            BiomicRenderIds.CHALK_GRASS = 1357;
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            BlockChalkGrass block = (BlockChalkGrass) ((Unsafe) field.get(null))
                .allocateInstance(BlockChalkGrass.class);

            Assert.assertEquals(1357, block.getRenderType());
        } finally {
            BiomicRenderIds.CHALK_GRASS = previous;
        }
    }

    @Test
    public void doubleShrubsNeverUseVanillasSunflowerRenderer() throws ReflectiveOperationException {
        int previous = BiomicRenderIds.RU_DOUBLE_PLANT;
        try {
            BiomicRenderIds.RU_DOUBLE_PLANT = 2468;
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            BlockRuDoublePlant block = (BlockRuDoublePlant) ((Unsafe) field.get(null))
                .allocateInstance(BlockRuDoublePlant.class);

            Assert.assertEquals(2468, block.getRenderType());
            Assert.assertNotEquals("render type 40 reads vanilla sunflowerIcons", 40, block.getRenderType());
        } finally {
            BiomicRenderIds.RU_DOUBLE_PLANT = previous;
        }
    }

    @Test
    public void joshuaCrownAndBeardUseTheirExactModelRenderers() throws ReflectiveOperationException {
        int previousCrown = BiomicRenderIds.JOSHUA_CROWN;
        int previousBeard = BiomicRenderIds.JOSHUA_BEARD;
        try {
            BiomicRenderIds.JOSHUA_CROWN = 9753;
            BiomicRenderIds.JOSHUA_BEARD = 9754;
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            BlockJoshuaLeaves leaves = (BlockJoshuaLeaves) unsafe.allocateInstance(BlockJoshuaLeaves.class);
            BlockJoshuaBeard beard = (BlockJoshuaBeard) unsafe.allocateInstance(BlockJoshuaBeard.class);

            Assert.assertEquals(9753, leaves.getRenderType());
            Assert.assertEquals(9754, beard.getRenderType());
            Assert.assertTrue(leaves.isNaturalLower(0));
            Assert.assertTrue(leaves.isUpper(1));
            Assert.assertFalse(leaves.isNaturalLower(2));
        } finally {
            BiomicRenderIds.JOSHUA_CROWN = previousCrown;
            BiomicRenderIds.JOSHUA_BEARD = previousBeard;
        }
    }

    @Test
    public void eucalyptusUsesDedicatedLayeredLogAndExactBranchRenderers() throws ReflectiveOperationException {
        int previousLog = BiomicRenderIds.EUCALYPTUS_LOG;
        int previousBranch = BiomicRenderIds.EUCALYPTUS_BRANCH;
        try {
            BiomicRenderIds.EUCALYPTUS_LOG = 8642;
            BiomicRenderIds.EUCALYPTUS_BRANCH = 8643;
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            BlockEucalyptusLog log = (BlockEucalyptusLog) unsafe.allocateInstance(BlockEucalyptusLog.class);
            BlockEucalyptusBranch branch = (BlockEucalyptusBranch) unsafe.allocateInstance(BlockEucalyptusBranch.class);

            Assert.assertEquals(8642, log.getRenderType());
            Assert.assertEquals(8643, branch.getRenderType());
        } finally {
            BiomicRenderIds.EUCALYPTUS_LOG = previousLog;
            BiomicRenderIds.EUCALYPTUS_BRANCH = previousBranch;
        }
    }
}
