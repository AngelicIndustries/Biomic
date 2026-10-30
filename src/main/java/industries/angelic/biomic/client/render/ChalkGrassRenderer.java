package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.LayeredGrassBlock;

/** Legacy layered-cube renderer for RU's chalk grass JSON model. */
public final class ChalkGrassRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public ChalkGrassRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        LayeredGrassBlock chalkGrass = (LayeredGrassBlock) block;
        int color = block.getRenderColor(metadata);
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        renderer.setRenderBoundsFromBlock(block);
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        inventoryFace(renderer, block, Face.DOWN, chalkGrass.getBottomIcon(), null, 0.5F, 0.5F, 0.5F);
        inventoryFace(renderer, block, Face.UP, chalkGrass.getGrassTopIcon(), null, red, green, blue);
        inventoryFace(
            renderer,
            block,
            Face.NORTH,
            chalkGrass.getGrassSideIcon(),
            chalkGrass.getGrassSideOverlayIcon(),
            red * 0.8F,
            green * 0.8F,
            blue * 0.8F);
        inventoryFace(
            renderer,
            block,
            Face.SOUTH,
            chalkGrass.getGrassSideIcon(),
            chalkGrass.getGrassSideOverlayIcon(),
            red * 0.8F,
            green * 0.8F,
            blue * 0.8F);
        inventoryFace(
            renderer,
            block,
            Face.WEST,
            chalkGrass.getGrassSideIcon(),
            chalkGrass.getGrassSideOverlayIcon(),
            red * 0.6F,
            green * 0.6F,
            blue * 0.6F);
        inventoryFace(
            renderer,
            block,
            Face.EAST,
            chalkGrass.getGrassSideIcon(),
            chalkGrass.getGrassSideOverlayIcon(),
            red * 0.6F,
            green * 0.6F,
            blue * 0.6F);
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        LayeredGrassBlock chalkGrass = (LayeredGrassBlock) block;
        Tessellator tessellator = Tessellator.instance;
        int color = block.colorMultiplier(world, x, y, z);
        float red = red(color);
        float green = green(color);
        float blue = blue(color);
        boolean snowy = LayeredGrassBlock.isSnowy(world, x, y, z);
        renderer.setRenderBoundsFromBlock(block);
        boolean rendered = false;
        rendered |= worldFace(
            renderer,
            tessellator,
            world,
            block,
            x,
            y,
            z,
            Face.DOWN,
            chalkGrass.getBottomIcon(),
            null,
            0.5F,
            0.5F,
            0.5F);
        rendered |= worldFace(
            renderer,
            tessellator,
            world,
            block,
            x,
            y,
            z,
            Face.UP,
            chalkGrass.getGrassTopIcon(),
            null,
            red,
            green,
            blue);
        IIcon side = snowy ? chalkGrass.getSnowySideIcon() : chalkGrass.getGrassSideIcon();
        IIcon overlay = snowy ? null : chalkGrass.getGrassSideOverlayIcon();
        rendered |= worldFace(
            renderer,
            tessellator,
            world,
            block,
            x,
            y,
            z,
            Face.NORTH,
            side,
            overlay,
            red * 0.8F,
            green * 0.8F,
            blue * 0.8F);
        rendered |= worldFace(
            renderer,
            tessellator,
            world,
            block,
            x,
            y,
            z,
            Face.SOUTH,
            side,
            overlay,
            red * 0.8F,
            green * 0.8F,
            blue * 0.8F);
        rendered |= worldFace(
            renderer,
            tessellator,
            world,
            block,
            x,
            y,
            z,
            Face.WEST,
            side,
            overlay,
            red * 0.6F,
            green * 0.6F,
            blue * 0.6F);
        rendered |= worldFace(
            renderer,
            tessellator,
            world,
            block,
            x,
            y,
            z,
            Face.EAST,
            side,
            overlay,
            red * 0.6F,
            green * 0.6F,
            blue * 0.6F);
        return rendered;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    private static boolean worldFace(RenderBlocks renderer, Tessellator tessellator, IBlockAccess world, Block block,
        int x, int y, int z, Face face, IIcon base, IIcon overlay, float overlayRed, float overlayGreen,
        float overlayBlue) {
        int neighborX = x + face.offsetX;
        int neighborY = y + face.offsetY;
        int neighborZ = z + face.offsetZ;
        if (!renderer.renderAllFaces
            && !block.shouldSideBeRendered(world, neighborX, neighborY, neighborZ, face.side)) {
            return false;
        }
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, neighborX, neighborY, neighborZ));
        if (face == Face.UP) {
            tessellator.setColorOpaque_F(overlayRed, overlayGreen, overlayBlue);
            renderFace(renderer, block, face, x, y, z, base);
            return true;
        }
        tessellator.setColorOpaque_F(face.shade, face.shade, face.shade);
        renderFace(renderer, block, face, x, y, z, base);
        if (overlay != null) {
            tessellator.setColorOpaque_F(overlayRed, overlayGreen, overlayBlue);
            renderFace(renderer, block, face, x, y, z, overlay);
        }
        return true;
    }

    private static void inventoryFace(RenderBlocks renderer, Block block, Face face, IIcon base, IIcon overlay,
        float overlayRed, float overlayGreen, float overlayBlue) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setNormal(face.offsetX, face.offsetY, face.offsetZ);
        if (face == Face.UP) {
            tessellator.setColorOpaque_F(overlayRed, overlayGreen, overlayBlue);
            renderFace(renderer, block, face, 0, 0, 0, base);
            tessellator.draw();
            return;
        }
        tessellator.setColorOpaque_F(face.shade, face.shade, face.shade);
        renderFace(renderer, block, face, 0, 0, 0, base);
        if (overlay != null) {
            tessellator.setColorOpaque_F(overlayRed, overlayGreen, overlayBlue);
            renderFace(renderer, block, face, 0, 0, 0, overlay);
        }
        tessellator.draw();
    }

    private static void renderFace(RenderBlocks renderer, Block block, Face face, double x, double y, double z,
        IIcon icon) {
        switch (face) {
            case DOWN:
                renderer.renderFaceYNeg(block, x, y, z, icon);
                break;
            case UP:
                renderer.renderFaceYPos(block, x, y, z, icon);
                break;
            case NORTH:
                renderer.renderFaceZNeg(block, x, y, z, icon);
                break;
            case SOUTH:
                renderer.renderFaceZPos(block, x, y, z, icon);
                break;
            case WEST:
                renderer.renderFaceXNeg(block, x, y, z, icon);
                break;
            case EAST:
                renderer.renderFaceXPos(block, x, y, z, icon);
                break;
            default:
                throw new AssertionError(face);
        }
    }

    private static float red(int color) {
        return (color >> 16 & 0xFF) / 255.0F;
    }

    private static float green(int color) {
        return (color >> 8 & 0xFF) / 255.0F;
    }

    private static float blue(int color) {
        return (color & 0xFF) / 255.0F;
    }

    private enum Face {

        DOWN(0, -1, 0, 0, 0.5F),
        UP(0, 1, 0, 1, 1.0F),
        NORTH(0, 0, -1, 2, 0.8F),
        SOUTH(0, 0, 1, 3, 0.8F),
        WEST(-1, 0, 0, 4, 0.6F),
        EAST(1, 0, 0, 5, 0.6F);

        private final int offsetX;
        private final int offsetY;
        private final int offsetZ;
        private final int side;
        private final float shade;

        Face(int offsetX, int offsetY, int offsetZ, int side, float shade) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.side = side;
            this.shade = shade;
        }
    }
}
