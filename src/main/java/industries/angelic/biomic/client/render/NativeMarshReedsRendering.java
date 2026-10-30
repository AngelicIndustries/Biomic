package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import industries.angelic.biomic.content.block.WaterloggedRenderPlant;

/** Native Salty textures/cross model, separate cutout and correctly tagged fluid geometry. */
public final class NativeMarshReedsRendering {

    private NativeMarshReedsRendering() {}

    public static boolean render(IBlockAccess world, int x, int y, int z, Block block, RenderBlocks renderer) {
        int pass = ForgeHooksClient.getWorldRenderPass();
        if (pass == 1) return block instanceof WaterloggedRenderPlant
            && AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
        if (pass != 0) return false;
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1, 1, 1);
        renderer.drawCrossedSquares(block.getIcon(0, world.getBlockMetadata(x, y, z)), x, y, z, 1);
        return true;
    }
}
