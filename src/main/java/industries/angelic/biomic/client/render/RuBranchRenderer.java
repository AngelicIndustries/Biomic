package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockRuBranch;

/** Long crossed planes matching the visual envelope of RU's 32-by-16 branch model. */
public final class RuBranchRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public RuBranchRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        renderBranch(tessellator, (BlockRuBranch) block, metadata, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        if (((BlockRuBranch) block).isSilverBirch()
            && !industries.angelic.biomic.config.VisualConfig.silverBirch3dBranches) return true;
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        renderBranch(tessellator, (BlockRuBranch) block, world.getBlockMetadata(x, y, z), x, y, z);
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    private static void renderBranch(Tessellator tessellator, BlockRuBranch block, int metadata, double x, double y,
        double z) {
        if (block.getLogIcon() != null) {
            EucalyptusBranchRenderer.render(tessellator, block.getLogIcon(), block.getBranchIcon(), metadata, x, y, z);
        } else {
            render(tessellator, block.getBranchIcon(), metadata, x, y, z);
        }
    }

    private static void render(Tessellator tessellator, IIcon icon, int metadata, double originX, double originY,
        double originZ) {
        int rotation = metadata & 3;
        Point[] vertical = { point(-1, 4, -15), point(1, 4, 20), point(1, 17, 20), point(-1, 17, -15) };
        Point[] sloped = { point(-8, 7, -15), point(8, 7, -15), point(8, 13, 20), point(-8, 13, 20) };
        quad(tessellator, icon, originX, originY, originZ, rotate(vertical, rotation));
        quad(tessellator, icon, originX, originY, originZ, rotate(sloped, rotation));
    }

    private static Point[] rotate(Point[] points, int quarterTurns) {
        Point[] result = new Point[points.length];
        for (int index = 0; index < points.length; index++) {
            Point point = points[index];
            double x = point.x;
            double z = point.z;
            for (int turn = 0; turn < quarterTurns; turn++) {
                double previousX = x;
                x = -z;
                z = previousX;
            }
            result[index] = new Point(x, point.y, z);
        }
        return result;
    }

    private static void quad(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point[] points) {
        double[][] uv = { { icon.getMinU(), icon.getMaxV() }, { icon.getMaxU(), icon.getMaxV() },
            { icon.getMaxU(), icon.getMinV() }, { icon.getMinU(), icon.getMinV() } };
        for (int index = 0; index < 4; index++)
            vertex(tessellator, originX, originY, originZ, points[index], uv[index]);
        for (int index = 3; index >= 0; index--)
            vertex(tessellator, originX, originY, originZ, points[index], uv[index]);
    }

    private static void vertex(Tessellator tessellator, double originX, double originY, double originZ, Point point,
        double[] uv) {
        tessellator.addVertexWithUV(
            originX + 0.5D + point.x / 16.0D,
            originY + point.y / 16.0D,
            originZ + 0.5D + point.z / 16.0D,
            uv[0],
            uv[1]);
    }

    private static Point point(double x, double y, double z) {
        return new Point(x, y, z);
    }

    private record Point(double x, double y, double z) {}
}
