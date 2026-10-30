package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/** Native BOP's un-tinted crossed-square icon; no replacement block, texture or inventory cube. */
public final class NativeAquaticPlantRenderer implements ISimpleBlockRenderingHandler {

    private final int id;

    public NativeAquaticPlantRenderer(int id) {
        this.id = id;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        return NativeMarshReedsRendering.render(world, x, y, z, block, renderer);
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {}

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return id;
    }
}
