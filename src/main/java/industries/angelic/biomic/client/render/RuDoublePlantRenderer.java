package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockRuDoublePlant;

/** Renders RU's ordinary two-high shrubs and its bespoke two-block elephant-ear model. */
public final class RuDoublePlantRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public RuDoublePlantRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        BlockRuDoublePlant plant = (BlockRuDoublePlant) block;
        Tessellator tessellator = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1.0F, 0.0F);
        setColor(tessellator, plant.getRenderColor(metadata));
        if (plant.isElephantEar(metadata)) {
            renderElephantEar(tessellator, plant, metadata, 0.0D, 0.0D, 0.0D);
        } else {
            renderer.drawCrossedSquares(block.getIcon(0, metadata & 7), 0.0D, 0.0D, 0.0D, 1.0F);
            renderFlowerOverlay(tessellator, renderer, plant, metadata & 7, 0, 0, 0);
        }
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        BlockRuDoublePlant plant = (BlockRuDoublePlant) block;
        int metadata = world.getBlockMetadata(x, y, z);
        String sourceModel = plant.sourceModelName(metadata);
        if (sourceModel != null) {
            RuCaveModelRenderer.renderSource(
                block,
                sourceModel,
                metadata,
                x,
                y,
                z,
                plant.colorMultiplier(world, x, y, z),
                world,
                plant::sourceModelIcon);
            return true;
        }
        if (plant.isElephantEar(metadata)) {
            // RU's upper state deliberately uses an empty model; the lower state renders the complete two-block plant.
            if (plant.isUpperHalf(metadata)) return true;
            Tessellator tessellator = Tessellator.instance;
            tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
            setColor(tessellator, plant.colorMultiplier(world, x, y, z));
            double[] offset = plantOffset(x, z, 0.5D);
            renderElephantEar(tessellator, plant, metadata, x + offset[0], y, z + offset[1]);
            return true;
        }

        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        setColor(tessellator, plant.colorMultiplier(world, x, y, z));
        double[] offset = plantOffset(x, z, 0.3D);
        renderer.drawCrossedSquares(plant.getIcon(0, metadata), x + offset[0], y, z + offset[1], 1.0F);
        renderFlowerOverlay(tessellator, renderer, plant, metadata, x + offset[0], y, z + offset[1]);
        return true;
    }

    private static void renderFlowerOverlay(Tessellator tessellator, RenderBlocks renderer, BlockRuDoublePlant plant,
        int metadata, double x, double y, double z) {
        IIcon flowers = plant.getFlowerOverlay(metadata);
        if (flowers == null) return;
        // Pinned two_layer_cross: exactly the same two planes, but the flower faces have no tintindex.
        setColor(tessellator, 0xFFFFFF);
        renderer.drawCrossedSquares(flowers, x, y, z, 1.0F);
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    private static void renderElephantEar(Tessellator tessellator, BlockRuDoublePlant block, int metadata, double x,
        double y, double z) {
        IIcon leaf = block.getUpperIcon(metadata);
        IIcon stem = block.getLowerIcon(metadata);
        IIcon grass = Blocks.tallgrass.getIcon(0, 1);

        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            0,
            17,
            2,
            16,
            17,
            18,
            LegacyBlockModelRenderer.Face.UP,
            0,
            0,
            16,
            16,
            0,
            LegacyBlockModelRenderer.Axis.X,
            -45);
        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            0,
            17,
            2,
            16,
            17,
            18,
            LegacyBlockModelRenderer.Face.DOWN,
            0,
            16,
            16,
            0,
            0,
            LegacyBlockModelRenderer.Axis.X,
            -45);
        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            0,
            29,
            -6,
            16,
            29,
            10,
            LegacyBlockModelRenderer.Face.UP,
            0,
            0,
            16,
            16,
            180,
            LegacyBlockModelRenderer.Axis.X,
            45);
        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            0,
            29,
            -6,
            16,
            29,
            10,
            LegacyBlockModelRenderer.Face.DOWN,
            0,
            16,
            16,
            0,
            180,
            LegacyBlockModelRenderer.Axis.X,
            45);
        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            -6,
            29,
            0,
            10,
            29,
            16,
            LegacyBlockModelRenderer.Face.UP,
            0,
            0,
            16,
            16,
            90,
            LegacyBlockModelRenderer.Axis.Z,
            -45);
        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            -6,
            29,
            0,
            10,
            29,
            16,
            LegacyBlockModelRenderer.Face.DOWN,
            0,
            16,
            16,
            0,
            270,
            LegacyBlockModelRenderer.Axis.Z,
            -45);
        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            2,
            17,
            0,
            18,
            17,
            16,
            LegacyBlockModelRenderer.Face.UP,
            0,
            0,
            16,
            16,
            270,
            LegacyBlockModelRenderer.Axis.Z,
            45);
        modelFace(
            tessellator,
            leaf,
            x,
            y,
            z,
            2,
            17,
            0,
            18,
            17,
            16,
            LegacyBlockModelRenderer.Face.DOWN,
            0,
            16,
            16,
            0,
            90,
            LegacyBlockModelRenderer.Axis.Z,
            45);

        modelFace(
            tessellator,
            stem,
            x,
            y,
            z,
            8,
            0,
            -8,
            8,
            32,
            24,
            LegacyBlockModelRenderer.Face.EAST,
            0,
            0,
            16,
            16,
            0,
            LegacyBlockModelRenderer.Axis.NONE,
            0);
        modelFace(
            tessellator,
            stem,
            x,
            y,
            z,
            8,
            0,
            -8,
            8,
            32,
            24,
            LegacyBlockModelRenderer.Face.WEST,
            16,
            0,
            0,
            16,
            0,
            LegacyBlockModelRenderer.Axis.NONE,
            0);
        modelFace(
            tessellator,
            stem,
            x,
            y,
            z,
            -8,
            0,
            8,
            24,
            32,
            8,
            LegacyBlockModelRenderer.Face.NORTH,
            0,
            0,
            16,
            16,
            0,
            LegacyBlockModelRenderer.Axis.NONE,
            0);
        modelFace(
            tessellator,
            stem,
            x,
            y,
            z,
            -8,
            0,
            8,
            24,
            32,
            8,
            LegacyBlockModelRenderer.Face.SOUTH,
            16,
            0,
            0,
            16,
            0,
            LegacyBlockModelRenderer.Axis.NONE,
            0);

        modelFace(
            tessellator,
            grass,
            x,
            y,
            z,
            4,
            0,
            0,
            18.4D,
            13,
            0,
            LegacyBlockModelRenderer.Face.NORTH,
            0,
            0,
            16,
            16,
            0,
            LegacyBlockModelRenderer.Axis.Y,
            -45);
        modelFace(
            tessellator,
            grass,
            x,
            y,
            z,
            4,
            0,
            0,
            18.4D,
            13,
            0,
            LegacyBlockModelRenderer.Face.SOUTH,
            16,
            0,
            0,
            16,
            0,
            LegacyBlockModelRenderer.Axis.Y,
            -45);
        modelFace(
            tessellator,
            grass,
            x,
            y,
            z,
            -7,
            0,
            11,
            7.4D,
            13,
            11,
            LegacyBlockModelRenderer.Face.NORTH,
            0,
            0,
            16,
            16,
            0,
            LegacyBlockModelRenderer.Axis.Y,
            45);
        modelFace(
            tessellator,
            grass,
            x,
            y,
            z,
            -7,
            0,
            11,
            7.4D,
            13,
            11,
            LegacyBlockModelRenderer.Face.SOUTH,
            16,
            0,
            0,
            16,
            0,
            LegacyBlockModelRenderer.Axis.Y,
            45);
    }

    private static void modelFace(Tessellator tessellator, IIcon icon, double x, double y, double z, double fromX,
        double fromY, double fromZ, double toX, double toY, double toZ, LegacyBlockModelRenderer.Face face,
        double minimumU, double minimumV, double maximumU, double maximumV, int uvRotation,
        LegacyBlockModelRenderer.Axis axis, double angle) {
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
            axis,
            angle,
            0.0D,
            0.0D,
            0.0D,
            false,
            0);
    }

    private static double[] plantOffset(int x, int z, double spread) {
        long hash = (long) (x * 3129871) ^ (long) z * 116129781L;
        hash = hash * hash * 42317861L + hash * 11L;
        double offsetX = ((double) ((float) (hash >> 16 & 15L) / 15.0F) - 0.5D) * spread;
        double offsetZ = ((double) ((float) (hash >> 24 & 15L) / 15.0F) - 0.5D) * spread;
        return new double[] { offsetX, offsetZ };
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
