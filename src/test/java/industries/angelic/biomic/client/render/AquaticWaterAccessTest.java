package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.content.BiomicAquaticBlocks;

public class AquaticWaterAccessTest {

    private static final class NativeReed extends Block
        implements industries.angelic.biomic.content.block.WaterloggedRenderPlant {

        NativeReed() {
            super(net.minecraft.block.material.Material.water);
        }
    }

    @Test
    public void optionalNativeLowerReedsShareFluidSemanticsWithoutLinkingTheirMod() {
        Block reed = new NativeReed();
        IBlockAccess water = new AquaticWaterAccess(new Cell(reed, 7));
        Assert.assertSame(Blocks.water, water.getBlock(0, 63, 0));
        Assert.assertEquals(0, water.getBlockMetadata(0, 63, 0));
        Assert.assertSame(
            net.minecraft.block.material.Material.plants,
            AquaticRenderSemantics.material(reed, reed.getMaterial(), 0));
        Assert.assertSame(
            net.minecraft.block.material.Material.water,
            AquaticRenderSemantics.material(reed, reed.getMaterial(), 1));
        Block dryTop = new Block(net.minecraft.block.material.Material.plants) {};
        Assert.assertSame(
            net.minecraft.block.material.Material.plants,
            AquaticRenderSemantics.material(dryTop, dryTop.getMaterial(), 0));
    }

    @Test
    public void speciesAndPlantHalfMetadataNeverBecomeFluidLevels() {
        for (Block plant : new Block[] { BiomicAquaticBlocks.SEAGRASS, BiomicAquaticBlocks.KELP,
            BiomicAquaticBlocks.SEA_PICKLE, BiomicAquaticBlocks.CORAL_PLANT, BiomicAquaticBlocks.CORAL_FAN }) {
            for (int metadata = 0; metadata < 16; metadata++) {
                IBlockAccess water = new AquaticWaterAccess(new Cell(plant, metadata));
                Assert.assertEquals(0, water.getBlockMetadata(0, 64, 0));
                Assert.assertSame(Blocks.water, water.getBlock(0, 64, 0));
                Assert.assertFalse(water.isSideSolid(0, 64, 0, ForgeDirection.UP, true));
                Assert.assertEquals(0xF000F0, water.getLightBrightnessForSkyBlocks(0, 64, 0, 0));
            }
        }
    }

    @Test
    public void ordinaryCellsRetainTheirBlockAndMetadata() {
        IBlockAccess water = new AquaticWaterAccess(new Cell(Blocks.water, 5));
        Assert.assertEquals(5, water.getBlockMetadata(0, 64, 0));
        Assert.assertSame(Blocks.water, water.getBlock(0, 64, 0));
    }

    @Test
    public void nativeAlgaeMetadataEightIsNotAFlowingWaterLevel() {
        Block algae = new NativeReed();
        IBlockAccess water = new AquaticWaterAccess(new Cell(algae, 8));
        Assert.assertEquals(0, water.getBlockMetadata(0, 63, 0));
        Assert.assertSame(Blocks.water, water.getBlock(0, 63, 0));
        Assert.assertSame(
            net.minecraft.block.material.Material.plants,
            AquaticRenderSemantics.material(algae, algae.getMaterial(), 0));
        Assert.assertFalse(new NativeAquaticPlantRenderer(1).shouldRender3DInInventory(1));
    }

    private static final class Cell implements IBlockAccess {

        private final Block block;
        private final int metadata;

        Cell(Block block, int metadata) {
            this.block = block;
            this.metadata = metadata;
        }

        public Block getBlock(int x, int y, int z) {
            return block;
        }

        public int getBlockMetadata(int x, int y, int z) {
            return metadata;
        }

        public TileEntity getTileEntity(int x, int y, int z) {
            return null;
        }

        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) {
            return 0xF000F0;
        }

        public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
            return 0;
        }

        public boolean isAirBlock(int x, int y, int z) {
            return false;
        }

        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return null;
        }

        public int getHeight() {
            return 256;
        }

        public boolean extendedLevelsInChunkCache() {
            return false;
        }

        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
            return true;
        }
    }
}
