package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import ganymedes01.etfuturum.client.renderer.block.BlockGlowLichenRenderer;

/** Original EFR six-face mesh in cutout pass, existing native source-water mesh in fluid pass. */
public final class SkylandsWetGlowLichenRenderer extends BlockGlowLichenRenderer {

    public SkylandsWetGlowLichenRenderer(int renderId) {
        super(renderId);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        return ForgeHooksClient.getWorldRenderPass() == 1 ? AquaticPlantRenderer.renderWater(world, x, y, z, renderer)
            : super.renderWorldBlock(world, x, y, z, block, modelId, renderer);
    }
}
