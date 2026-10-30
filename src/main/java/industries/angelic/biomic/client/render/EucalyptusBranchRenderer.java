package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Axis;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Face;
import industries.angelic.biomic.content.block.BlockEucalyptusBranch;

/** Lossless Forge 1.7 transcription of RU's six-element eucalyptus branch JSON model. */
public final class EucalyptusBranchRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public EucalyptusBranchRenderer(int renderId) {
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
        render(tessellator, (BlockEucalyptusBranch) block, metadata, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        render(tessellator, (BlockEucalyptusBranch) block, world.getBlockMetadata(x, y, z), x, y, z);
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

    private static void render(Tessellator tessellator, BlockEucalyptusBranch branch, int metadata, double x, double y,
        double z) {
        render(tessellator, branch.getLogIcon(), branch.getBranchIcon(), metadata, x, y, z);
    }

    /** RU's shared branch.json, also used by maple, blackwood and silver birch, with padded sprite V coordinates. */
    static void render(Tessellator tessellator, IIcon log, IIcon foliage, int metadata, double x, double y, double z) {
        int rotation = (metadata & 3) * 90;
        renderWood(tessellator, log, x, y, z, rotation);
        renderFoliage(tessellator, foliage, x, y, z, rotation);
    }

    private static void renderWood(Tessellator tessellator, IIcon icon, double x, double y, double z, int rotation) {
        face(tessellator, icon, x, y, z, 6.5, 5, 7, 9.5, 8, 20, Face.NORTH, 5, 0, 8, 3, 270, rotation);
        face(tessellator, icon, x, y, z, 6.5, 5, 7, 9.5, 8, 20, Face.EAST, 8, 3, 11, 16, 90, rotation);
        face(tessellator, icon, x, y, z, 6.5, 5, 7, 9.5, 8, 20, Face.WEST, 5, 16, 2, 3, 90, rotation);
        face(tessellator, icon, x, y, z, 6.5, 5, 7, 9.5, 8, 20, Face.UP, 8, 16, 5, 3, 180, rotation);
        face(tessellator, icon, x, y, z, 6.5, 5, 7, 9.5, 8, 20, Face.DOWN, 11, 3, 14, 16, 180, rotation);

        face(tessellator, icon, x, y, z, 7, 6.5, -4, 9, 8.5, 8, Face.NORTH, 6, 2, 8, 4, 270, rotation);
        face(tessellator, icon, x, y, z, 7, 6.5, -4, 9, 8.5, 8, Face.EAST, 8, 4, 10, 16, 90, rotation);
        face(tessellator, icon, x, y, z, 7, 6.5, -4, 9, 8.5, 8, Face.SOUTH, 4, 2, 6, 4, 90, rotation);
        face(tessellator, icon, x, y, z, 7, 6.5, -4, 9, 8.5, 8, Face.WEST, 6, 16, 4, 4, 90, rotation);
        face(tessellator, icon, x, y, z, 7, 6.5, -4, 9, 8.5, 8, Face.UP, 8, 16, 6, 4, 180, rotation);
        face(tessellator, icon, x, y, z, 7, 6.5, -4, 9, 8.5, 8, Face.DOWN, 10, 4, 12, 16, 180, rotation);
    }

    private static void renderFoliage(Tessellator tessellator, IIcon icon, double x, double y, double z, int rotation) {
        rotatedFace(tessellator, icon, x, y, z, -1, 7.5, -15, 8, 7.5, 17, Face.UP, 0, 4, 16, 8, 90, 22.5, rotation);
        rotatedFace(tessellator, icon, x, y, z, -1, 7.5, -15, 8, 7.5, 17, Face.DOWN, 0, 8, 16, 4, 270, 22.5, rotation);

        rotatedFace(tessellator, icon, x, y, z, 8, 7.5, -12, 8, 16.5, 20, Face.EAST, 0, 4, 16, 8, 180, -22.5, rotation);
        rotatedFace(tessellator, icon, x, y, z, 8, 7.5, -12, 8, 16.5, 20, Face.WEST, 0, 8, 16, 4, 0, -22.5, rotation);

        rotatedFace(tessellator, icon, x, y, z, 8, 7.5, -12, 8, 16.5, 20, Face.EAST, 0, 4, 16, 0, 180, 22.5, rotation);
        rotatedFace(tessellator, icon, x, y, z, 8, 7.5, -12, 8, 16.5, 20, Face.WEST, 0, 0, 16, 4, 0, 22.5, rotation);

        rotatedFace(tessellator, icon, x, y, z, 8, 7.5, -15, 17, 7.5, 17, Face.UP, 0, 0, 16, 4, 90, -22.5, rotation);
        rotatedFace(tessellator, icon, x, y, z, 8, 7.5, -15, 17, 7.5, 17, Face.DOWN, 0, 4, 16, 0, 270, -22.5, rotation);
    }

    private static void face(Tessellator tessellator, IIcon icon, double x, double y, double z, double fromX,
        double fromY, double fromZ, double toX, double toY, double toZ, Face face, double minimumU, double minimumV,
        double maximumU, double maximumV, int uvRotation, int modelRotation) {
        LegacyBlockModelRenderer.face(
            tessellator,
            icon,
            x,
            y,
            z,
            fromX,
            fromY,
            fromZ,
            toX,
            toY,
            toZ,
            face,
            minimumU,
            minimumV,
            maximumU,
            maximumV,
            uvRotation,
            Axis.NONE,
            0,
            0,
            0,
            0,
            false,
            modelRotation);
    }

    private static void rotatedFace(Tessellator tessellator, IIcon icon, double x, double y, double z, double fromX,
        double fromY, double fromZ, double toX, double toY, double toZ, Face face, double minimumU, double minimumV,
        double maximumU, double maximumV, int uvRotation, double angle, int modelRotation) {
        LegacyBlockModelRenderer.face(
            tessellator,
            icon,
            x,
            y,
            z,
            fromX,
            fromY,
            fromZ,
            toX,
            toY,
            toZ,
            face,
            minimumU,
            minimumV,
            maximumU,
            maximumV,
            uvRotation,
            Axis.Z,
            angle,
            8,
            7.5,
            1,
            true,
            modelRotation);
    }
}
