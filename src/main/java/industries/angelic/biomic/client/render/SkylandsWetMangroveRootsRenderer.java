package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import ganymedes01.etfuturum.client.renderer.block.BlockMangroveRootsRenderer;

/** Exact native EFR root mesh in cutout pass; standard source-water mesh/shader in fluid pass. */
public final class SkylandsWetMangroveRootsRenderer extends BlockMangroveRootsRenderer {

    public SkylandsWetMangroveRootsRenderer(int renderId) {
        super(renderId);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        return ForgeHooksClient.getWorldRenderPass() == 1 ? AquaticPlantRenderer.renderWater(world, x, y, z, renderer)
            : super.renderWorldBlock(world, x, y, z, block, modelId, renderer);
    }
}
