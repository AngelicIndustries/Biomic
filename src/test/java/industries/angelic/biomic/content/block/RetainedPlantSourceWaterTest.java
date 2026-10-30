package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Test;

import industries.angelic.biomic.content.BiomicDripleafBlocks;
import industries.angelic.biomic.content.BiomicWetSculkBlocks;

public class RetainedPlantSourceWaterTest {

    private static class RenderOnly extends Block implements WaterloggedRenderPlant {

        RenderOnly() {
            super(Material.water);
        }
    }

    @Test
    public void onlyExactWetCompanionsHaveFluidAuthority() {
        assertFalse(RetainedPlantSourceWater.contains(new RenderOnly(), 0));
        for (int meta = 0; meta < 16; meta++) {
            assertFalse(RetainedPlantSourceWater.contains(BiomicDripleafBlocks.BIG_DRIPLEAF, meta));
            assertFalse(RetainedPlantSourceWater.contains(BiomicDripleafBlocks.SMALL_DRIPLEAF, meta));
            assertEquals(meta < 4, RetainedPlantSourceWater.contains(BiomicDripleafBlocks.WET_BIG_DRIPLEAF, meta));
            assertEquals(meta < 8, RetainedPlantSourceWater.contains(BiomicDripleafBlocks.WET_SMALL_DRIPLEAF, meta));
            assertEquals(meta != 0, RetainedPlantSourceWater.contains(BiomicWetSculkBlocks.VEIN, meta));
            assertTrue(RetainedPlantSourceWater.contains(BiomicWetSculkBlocks.VEIN_VERTICAL, meta));
        }
        assertFalse(RetainedPlantSourceWater.contains(new BlockBigDripleaf(false, true), 0));
        assertFalse(RetainedPlantSourceWater.contains(BiomicDripleafBlocks.WET_SMALL_DRIPLEAF, -1));
        assertFalse(RetainedPlantSourceWater.contains(BiomicWetSculkBlocks.VEIN_VERTICAL, 16));
    }

    @Test
    public void drySharedRenderModelsDoNotBecomeWater() {
        assertFalse(BiomicDripleafBlocks.BIG_DRIPLEAF.containsSourceWater());
        assertFalse(BiomicDripleafBlocks.SMALL_DRIPLEAF.containsSourceWater());
        assertTrue(BiomicDripleafBlocks.WET_BIG_DRIPLEAF.containsSourceWater());
        assertTrue(BiomicDripleafBlocks.WET_SMALL_DRIPLEAF.containsSourceWater());
    }
}
