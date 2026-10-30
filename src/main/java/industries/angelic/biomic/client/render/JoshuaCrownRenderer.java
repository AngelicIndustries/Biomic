package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Axis;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Face;
import industries.angelic.biomic.content.block.BlockJoshuaLeaves;

/** Exact Forge 1.7 transcription of RU's oversized, four-plane Joshua crown models. */
public final class JoshuaCrownRenderer implements ISimpleBlockRenderingHandler {

    private static final double CENTER = 8.0D;

    private final int renderId;

    public JoshuaCrownRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        BlockJoshuaLeaves leaves = (BlockJoshuaLeaves) block;
        Tessellator tessellator = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        render(tessellator, leaves, metadata, leaves.getRenderColor(metadata), 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        BlockJoshuaLeaves leaves = (BlockJoshuaLeaves) block;
        Tessellator tessellator = Tessellator.instance;
        int metadata = world.getBlockMetadata(x, y, z);
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        render(tessellator, leaves, metadata, leaves.colorMultiplier(world, x, y, z), x, y, z);
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

    private static void render(Tessellator tessellator, BlockJoshuaLeaves leaves, int metadata, int foliageColor,
        double x, double y, double z) {
        if (leaves.isNaturalLower(metadata)) {
            setColor(tessellator, 0xFFFFFF);
            renderNaturalBeard(tessellator, leaves.getBeardIcon(), x, y, z);
        }

        setColor(tessellator, foliageColor);
        IIcon left = leaves.getLeftIcon(metadata);
        IIcon right = leaves.getRightIcon(metadata);
        if (leaves.isUpper(metadata)) {
            renderCross(tessellator, left, right, x, y, z, 0.0D, 20.0D, 3.0D);
        } else {
            double originY = leaves.isNaturalLower(metadata) ? 1.0D : -1.0D;
            renderCross(tessellator, left, right, x, y, z, -4.0D, 16.0D, originY);
        }
    }

    private static void renderCross(Tessellator tessellator, IIcon left, IIcon right, double x, double y, double z,
        double fromY, double toY, double originY) {
        wideXPlane(tessellator, left, right, x, y, z, fromY, toY, -11.0D, 27.0D, CENTER, -22.5D, originY);
        wideZPlane(tessellator, left, right, x, y, z, fromY, toY, -11.0D, 27.0D, CENTER, 22.5D, originY);
        wideZPlane(tessellator, left, right, x, y, z, fromY, toY, -11.0D, 27.0D, CENTER, -22.5D, originY);
        wideXPlane(tessellator, left, right, x, y, z, fromY, toY, -11.0D, 27.0D, CENTER, 22.5D, originY);
    }

    /**
     * RU's crown sprites are 32x16. Forge 1.7 rejects non-square atlas sprites, so each exact source sprite is
     * losslessly split into two 16x16 atlas entries and reunited across the model plane here.
     */
    private static void wideXPlane(Tessellator tessellator, IIcon left, IIcon right, double x, double y, double z,
        double fromY, double toY, double fromX, double toX, double planeZ, double angle, double originY) {
        splitFace(
            tessellator,
            right,
            left,
            x,
            y,
            z,
            fromX,
            fromY,
            planeZ,
            toX,
            toY,
            planeZ,
            Face.NORTH,
            Axis.Y,
            angle,
            CENTER,
            originY,
            CENTER,
            true);
        splitFace(
            tessellator,
            left,
            right,
            x,
            y,
            z,
            fromX,
            fromY,
            planeZ,
            toX,
            toY,
            planeZ,
            Face.SOUTH,
            Axis.Y,
            angle,
            CENTER,
            originY,
            CENTER,
            true);
    }

    private static void wideZPlane(Tessellator tessellator, IIcon left, IIcon right, double x, double y, double z,
        double fromY, double toY, double fromZ, double toZ, double planeX, double angle, double originY) {
        splitFace(
            tessellator,
            right,
            left,
            x,
            y,
            z,
            planeX,
            fromY,
            fromZ,
            planeX,
            toY,
            toZ,
            Face.EAST,
            Axis.Y,
            angle,
            CENTER,
            originY,
            CENTER,
            false);
        splitFace(
            tessellator,
            left,
            right,
            x,
            y,
            z,
            planeX,
            fromY,
            fromZ,
            planeX,
            toY,
            toZ,
            Face.WEST,
            Axis.Y,
            angle,
            CENTER,
            originY,
            CENTER,
            false);
    }

    private static void splitFace(Tessellator tessellator, IIcon first, IIcon second, double x, double y, double z,
        double fromX, double fromY, double fromZ, double toX, double toY, double toZ, Face face, Axis axis,
        double angle, double originX, double originY, double originZ, boolean splitX) {
        if (splitX) {
            modelFace(
                tessellator,
                first,
                x,
                y,
                z,
                fromX,
                fromY,
                fromZ,
                CENTER,
                toY,
                toZ,
                face,
                axis,
                angle,
                originX,
                originY,
                originZ,
                true,
                0,
                16);
            modelFace(
                tessellator,
                second,
                x,
                y,
                z,
                CENTER,
                fromY,
                fromZ,
                toX,
                toY,
                toZ,
                face,
                axis,
                angle,
                originX,
                originY,
                originZ,
                true,
                0,
                16);
        } else {
            modelFace(
                tessellator,
                first,
                x,
                y,
                z,
                fromX,
                fromY,
                fromZ,
                toX,
                toY,
                CENTER,
                face,
                axis,
                angle,
                originX,
                originY,
                originZ,
                true,
                0,
                16);
            modelFace(
                tessellator,
                second,
                x,
                y,
                z,
                fromX,
                fromY,
                CENTER,
                toX,
                toY,
                toZ,
                face,
                axis,
                angle,
                originX,
                originY,
                originZ,
                true,
                0,
                16);
        }
    }

    private static void renderNaturalBeard(Tessellator tessellator, IIcon icon, double x, double y, double z) {
        for (Face face : new Face[] { Face.NORTH, Face.SOUTH }) {
            modelFace(tessellator, icon, x, y, z, 0, -3, 15, 16, 5, 15, face, Axis.NONE, 0, 0, 0, 0, false, 180, 8);
            modelFace(tessellator, icon, x, y, z, 0, -3, 1, 16, 5, 1, face, Axis.NONE, 0, 0, 0, 0, false, 180, 8);
        }
        for (Face face : new Face[] { Face.EAST, Face.WEST }) {
            modelFace(tessellator, icon, x, y, z, 1, -3, 0, 1, 5, 16, face, Axis.NONE, 0, 0, 0, 0, false, 180, 8);
            modelFace(tessellator, icon, x, y, z, 15, -3, 0, 15, 5, 16, face, Axis.NONE, 0, 0, 0, 0, false, 180, 8);
        }
    }

    private static void modelFace(Tessellator tessellator, IIcon icon, double x, double y, double z, double fromX,
        double fromY, double fromZ, double toX, double toY, double toZ, Face face, Axis rotationAxis,
        double rotationAngle, double originX, double originY, double originZ, boolean rescale, int uvRotation,
        double maximumV) {
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
            0,
            0,
            16,
            maximumV,
            uvRotation,
            rotationAxis,
            rotationAngle,
            originX,
            originY,
            originZ,
            rescale,
            0);
    }

    private static void setColor(Tessellator tessellator, int color) {
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float convertedRed = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
            float convertedGreen = (red * 30.0F + green * 70.0F) / 100.0F;
            float convertedBlue = (red * 30.0F + blue * 70.0F) / 100.0F;
            red = convertedRed;
            green = convertedGreen;
            blue = convertedBlue;
        }
        tessellator.setColorOpaque_F(red, green, blue);
    }
}
