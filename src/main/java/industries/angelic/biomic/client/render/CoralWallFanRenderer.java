package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockCoralWallFan;

/** Official1.20.4 coral_wall_fan model: two rescaled X±22.5 planes around(8,8,14), no shade. */
public final class CoralWallFanRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public CoralWallFanRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int id) {
        return false;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int id,
        RenderBlocks renderer) {/* Flat item uses the original fan sprite. */}

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int id,
        RenderBlocks renderer) {
        var fan = (BlockCoralWallFan) block;
        int meta = world.getBlockMetadata(x, y, z);
        if (!BlockCoralWallFan.validMetadata(meta)) return false;
        if (net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1)
            return fan.isWaterlogged() && AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
        var tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1, 1, 1);
        IIcon icon = fan.getIcon(0, meta);
        for (int plane = 0; plane < 2; plane++) {
            for (int vertex = 0; vertex < 4; vertex++) emit(tess, icon, x, y, z, meta & 3, plane, vertex, false);
            for (int vertex = 3; vertex >= 0; vertex--) emit(tess, icon, x, y, z, meta & 3, plane, vertex, true);
        }
        return true;
    }

    private static void emit(Tessellator tess, IIcon icon, int x, int y, int z, int facing, int plane, int vertex,
        boolean back) {
        double[] point = CoralWallFanModel.vertex(facing, plane, vertex);
        double u = (vertex == 0 || vertex == 3) ? 0 : 16, v = vertex < 2 ? 0 : 16;
        // Official down face reverses UV axes, rather than mirroring the living sprite arbitrarily.
        if (back) {
            u = 16 - u;
            v = 16 - v;
        }
        tess.addVertexWithUV(
            x + point[0],
            y + point[1],
            z + point[2],
            icon.getInterpolatedU(u),
            icon.getInterpolatedV(v));
    }
}
