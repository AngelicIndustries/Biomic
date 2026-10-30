package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.gardens.BlockSoulSpore;

/** Cross renderer retaining the pinned model's vertical flip for ceiling-grown normal and glowing spores. */
public final class GardensSoulSporeRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public GardensSoulSporeRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        tess.setColorOpaque_F(1, 1, 1);
        crossed(tess, block.getIcon(0, metadata), 0, 0, 0, false);
        tess.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        int metadata = world.getBlockMetadata(x, y, z);
        IIcon icon = renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : block.getIcon(0, metadata);
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        int color = block.colorMultiplier(world, x, y, z);
        tess.setColorOpaque_F((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F, (color & 255) / 255F);
        crossed(tess, icon, x, y, z, ((BlockSoulSpore) block).flipModel(metadata));
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    static double[] verticalUvs(IIcon icon, boolean ceiling) {
        return verticalUvs(icon.getMinV(), icon.getMaxV(), ceiling);
    }

    static double[] verticalUvs(double minV, double maxV, boolean ceiling) {
        return ceiling ? new double[] { maxV, minV } : new double[] { minV, maxV };
    }

    private static void crossed(Tessellator tess, IIcon icon, double x, double y, double z, boolean ceiling) {
        double min = x + .05, max = x + .95, minZ = z + .05, maxZ = z + .95, u0 = icon.getMinU(), u1 = icon.getMaxU();
        double[] v = verticalUvs(icon, ceiling);
        plane(tess, min, y, minZ, max, y + 1, maxZ, u0, u1, v[0], v[1]);
        plane(tess, min, y, maxZ, max, y + 1, minZ, u0, u1, v[0], v[1]);
    }

    private static void plane(Tessellator tess, double x0, double y0, double z0, double x1, double y1, double z1,
        double u0, double u1, double vTop, double vBottom) {
        tess.addVertexWithUV(x0, y1, z0, u0, vTop);
        tess.addVertexWithUV(x0, y0, z0, u0, vBottom);
        tess.addVertexWithUV(x1, y0, z1, u1, vBottom);
        tess.addVertexWithUV(x1, y1, z1, u1, vTop);
        tess.addVertexWithUV(x1, y1, z1, u0, vTop);
        tess.addVertexWithUV(x1, y0, z1, u0, vBottom);
        tess.addVertexWithUV(x0, y0, z0, u1, vBottom);
        tess.addVertexWithUV(x0, y1, z0, u1, vTop);
    }
}
