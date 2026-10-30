package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import industries.angelic.biomic.content.block.BlockAquaticPlant;

/** Fluid-only rendering view: plant species/count metadata must never become flowing-water levels. */
final class AquaticWaterAccess implements IBlockAccess {

    private final IBlockAccess delegate;

    AquaticWaterAccess(IBlockAccess delegate) {
        this.delegate = delegate;
    }

    private boolean aquatic(int x, int y, int z) {
        Block block = delegate.getBlock(x, y, z);
        return block instanceof BlockAquaticPlant
            || block instanceof industries.angelic.biomic.content.block.WaterloggedRenderPlant wet
                && wet.containsSourceWater()
            || block instanceof industries.angelic.biomic.content.block.BlockRuCaveModel cave && cave.isWaterlogged();
    }

    public Block getBlock(int x, int y, int z) {
        return aquatic(x, y, z) ? Blocks.water : delegate.getBlock(x, y, z);
    }

    public int getBlockMetadata(int x, int y, int z) {
        return aquatic(x, y, z) ? 0 : delegate.getBlockMetadata(x, y, z);
    }

    public TileEntity getTileEntity(int x, int y, int z) {
        return delegate.getTileEntity(x, y, z);
    }

    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int light) {
        return delegate.getLightBrightnessForSkyBlocks(x, y, z, light);
    }

    public int isBlockProvidingPowerTo(int x, int y, int z, int direction) {
        return delegate.isBlockProvidingPowerTo(x, y, z, direction);
    }

    public boolean isAirBlock(int x, int y, int z) {
        return delegate.isAirBlock(x, y, z);
    }

    public BiomeGenBase getBiomeGenForCoords(int x, int z) {
        return delegate.getBiomeGenForCoords(x, z);
    }

    public int getHeight() {
        return delegate.getHeight();
    }

    public boolean extendedLevelsInChunkCache() {
        return delegate.extendedLevelsInChunkCache();
    }

    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean fallback) {
        return !aquatic(x, y, z) && delegate.isSideSolid(x, y, z, side, fallback);
    }
}
