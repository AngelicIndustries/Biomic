package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import net.minecraft.block.material.Material;

import org.junit.Test;

import industries.angelic.biomic.content.BiomicDripleafBlocks;

public class DripleafWaterSemanticsTest {

    @Test
    public void wetIsRealSourceWaterWithUnchangedFacingHalfModelsAndDryCounterparts() {
        for (BlockDripleafBase block : new BlockDripleafBase[] { BiomicDripleafBlocks.WET_SMALL_DRIPLEAF,
            BiomicDripleafBlocks.WET_BIG_DRIPLEAF, BiomicDripleafBlocks.WET_BIG_DRIPLEAF_STEM }) {
            assertTrue(block.isWaterlogged());
            assertTrue(block.containsSourceWater());
            assertEquals(Material.water, block.getMaterial());
            assertTrue(block.canRenderInPass(0));
            assertTrue(block.canRenderInPass(1));
            assertFalse(block.canRenderInPass(2));
            assertEquals(1, block.getRenderBlockPass());
            assertEquals(block, DripleafBucket.replacement(DripleafBucket.replacement(block, false), true));
        }
        for (BlockDripleafBase block : new BlockDripleafBase[] { BiomicDripleafBlocks.SMALL_DRIPLEAF,
            BiomicDripleafBlocks.BIG_DRIPLEAF, BiomicDripleafBlocks.BIG_DRIPLEAF_STEM }) {
            assertFalse(block.isWaterlogged());
            assertFalse(block.containsSourceWater());
            assertEquals(Material.plants, block.getMaterial());
            assertTrue(block.canRenderInPass(0));
            assertFalse(block.canRenderInPass(1));
            assertEquals(0, block.getRenderBlockPass());
        }
        for (int metadata = 0; metadata < 8; metadata++)
            assertEquals(metadata >= 4, BlockSmallDripleaf.isUpper(metadata));
    }

    @Test
    public void bucketDrainFillKeepsAllFacingHalfBitsAndRejectsMalformedStates() {
        for (int m = 0; m < 8; m++) {
            assertEquals(DripleafBucket.Action.DRAIN, DripleafBucket.action(true, false, true, true, m, true));
            assertEquals(DripleafBucket.Action.FILL, DripleafBucket.action(false, true, false, true, m, true));
        }
        for (int m = 0; m < 4; m++)
            assertEquals(DripleafBucket.Action.DRAIN, DripleafBucket.action(true, false, true, true, m, false));
        assertEquals(DripleafBucket.Action.NONE, DripleafBucket.action(true, false, true, true, 4, false));
        assertEquals(DripleafBucket.Action.NONE, DripleafBucket.action(false, true, true, true, 0, false));
        assertEquals(DripleafBucket.Action.NONE, DripleafBucket.action(true, false, false, true, 0, true));
        assertEquals(DripleafBucket.Action.NONE, DripleafBucket.action(true, false, true, false, 0, true));
    }
}
