package industries.angelic.biomic.client.render;

import java.awt.Color;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockEucalyptusLog;

/** Exact two-layer rendering contract for RU's coordinate-rainbow eucalyptus bark. */
public final class EucalyptusLogRenderer implements ISimpleBlockRenderingHandler {

    static final float TRANSITION_SIZE = 25.0F;
    static final float SATURATION = 0.5F;
    static final float BRIGHTNESS = 0.8F;

    private final int renderId;

    public EucalyptusLogRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        BlockEucalyptusLog log = (BlockEucalyptusLog) block;
        renderer.setRenderBoundsFromBlock(block);
        configureLogUv(renderer, metadata);
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        try {
            int color = rainbowColor(0, 0, 0);
            for (Face face : Face.values()) {
                inventoryFace(renderer, block, face, log.getIcon(face.side, metadata), 0xFFFFFF);
                if (!isEnd(metadata, face.side)) {
                    inventoryFace(renderer, block, face, log.getOverlayIcon(), color);
                }
            }
        } finally {
            resetLogUv(renderer);
            GL11.glPopMatrix();
        }
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        BlockEucalyptusLog log = (BlockEucalyptusLog) block;
        boolean rendered = renderer.renderBlockLog(block, x, y, z);
        renderer.setRenderBoundsFromBlock(block);
        configureLogUv(renderer, world.getBlockMetadata(x, y, z));
        try {
            int metadata = world.getBlockMetadata(x, y, z);
            int color = rainbowColor(x, y, z);
            for (Face face : Face.values()) {
                if (!isEnd(metadata, face.side)) {
                    rendered |= worldFace(renderer, world, block, x, y, z, face, log.getOverlayIcon(), color);
                }
            }
        } finally {
            resetLogUv(renderer);
        }
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

    static int rainbowColor(int x, int y, int z) {
        return Color.HSBtoRGB((x + y + z) / TRANSITION_SIZE, SATURATION, BRIGHTNESS) & 0xFFFFFF;
    }

    private static boolean isEnd(int metadata, int side) {
        int axis = metadata & 12;
        return axis == 0 && (side == 0 || side == 1) || axis == 4 && (side == 4 || side == 5)
            || axis == 8 && (side == 2 || side == 3);
    }

    private static boolean worldFace(RenderBlocks renderer, IBlockAccess world, Block block, int x, int y, int z,
        Face face, IIcon icon, int color) {
        int neighborX = x + face.offsetX;
        int neighborY = y + face.offsetY;
        int neighborZ = z + face.offsetZ;
        if (!renderer.renderAllFaces
            && !block.shouldSideBeRendered(world, neighborX, neighborY, neighborZ, face.side)) {
            return false;
        }
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, neighborX, neighborY, neighborZ));
        setColor(tessellator, color, face.shade);
        renderFace(renderer, block, face, x, y, z, icon);
        return true;
    }

    private static void inventoryFace(RenderBlocks renderer, Block block, Face face, IIcon icon, int color) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.setNormal(face.offsetX, face.offsetY, face.offsetZ);
        setColor(tessellator, color, face.shade);
        renderFace(renderer, block, face, 0, 0, 0, icon);
        tessellator.draw();
    }

    private static void setColor(Tessellator tessellator, int color, float shade) {
        float red = (color >> 16 & 0xFF) / 255.0F;
        float green = (color >> 8 & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        if (EntityRenderer.anaglyphEnable) {
            float grayRed = (red * 30.0F + green * 59.0F + blue * 11.0F) / 100.0F;
            float grayGreen = (red * 30.0F + green * 70.0F) / 100.0F;
            float grayBlue = (red * 30.0F + blue * 70.0F) / 100.0F;
            red = grayRed;
            green = grayGreen;
            blue = grayBlue;
        }
        tessellator.setColorOpaque_F(red * shade, green * shade, blue * shade);
    }

    private static void configureLogUv(RenderBlocks renderer, int metadata) {
        if ((metadata & 12) == 4) {
            renderer.uvRotateEast = 1;
            renderer.uvRotateWest = 1;
            renderer.uvRotateTop = 1;
            renderer.uvRotateBottom = 1;
        } else if ((metadata & 12) == 8) {
            renderer.uvRotateSouth = 1;
            renderer.uvRotateNorth = 1;
        }
    }

    private static void resetLogUv(RenderBlocks renderer) {
        renderer.uvRotateSouth = 0;
        renderer.uvRotateEast = 0;
        renderer.uvRotateWest = 0;
        renderer.uvRotateNorth = 0;
        renderer.uvRotateTop = 0;
        renderer.uvRotateBottom = 0;
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
