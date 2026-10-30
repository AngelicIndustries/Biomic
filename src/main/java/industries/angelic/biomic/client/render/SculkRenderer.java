package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockSculkFlora;
import industries.angelic.biomic.content.block.BlockSculkVein;

/** Faithful legacy renderer for the vanilla sensor, shrieker, and six-face sculk-vein models. */
public final class SculkRenderer implements ISimpleBlockRenderingHandler {

    private static final double PIXEL = 1.0D / 16.0D;
    private static final double VEIN_INSET = 0.1D * PIXEL;

    private final int renderId;

    public SculkRenderer(int renderId) {
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
        if (block instanceof BlockSculkVein vein) {
            renderVein(tessellator, vein.getVeinIcon(), 15, true, true, 0.0D, 0.0D, 0.0D);
        } else {
            renderFlora(tessellator, (BlockSculkFlora) block, 0.0D, 0.0D, 0.0D);
        }
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        if (block instanceof industries.angelic.biomic.content.block.BlockWetSculkVein
            && net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1) {
            return AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
        }
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        if (block instanceof BlockSculkVein vein) {
            renderVein(
                tessellator,
                vein.getVeinIcon(),
                world.getBlockMetadata(x, y, z),
                vein.hasUp(),
                vein.hasDown(),
                x,
                y,
                z);
        } else {
            renderFlora(tessellator, (BlockSculkFlora) block, x, y, z);
        }
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

    private static void renderFlora(Tessellator tessellator, BlockSculkFlora block, double originX, double originY,
        double originZ) {
        if (block.getKind() == BlockSculkFlora.Kind.SENSOR) {
            cuboid(
                tessellator,
                originX,
                originY,
                originZ,
                0,
                0,
                0,
                16,
                8,
                16,
                block.getBottomIcon(),
                block.getSideIcon(),
                block.getTopIcon(),
                0,
                8,
                16,
                16,
                0,
                0,
                16,
                16);
            renderSensorTendrils(tessellator, block.getDetailIcon(), originX, originY, originZ);
            return;
        }

        cuboid(
            tessellator,
            originX,
            originY,
            originZ,
            0,
            0,
            0,
            16,
            8,
            16,
            block.getBottomIcon(),
            block.getSideIcon(),
            block.getDetailIcon(),
            0,
            8,
            16,
            16,
            0,
            0,
            16,
            16);
        cuboid(
            tessellator,
            originX,
            originY,
            originZ,
            1,
            8,
            1,
            15,
            15,
            15,
            null,
            block.getSideIcon(),
            block.getTopIcon(),
            1,
            1,
            15,
            8,
            1,
            1,
            15,
            15);
    }

    private static void renderSensorTendrils(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ) {
        tendril(tessellator, icon, originX, originY, originZ, -1, 3, 7, 3, 3, 12, 3, 45.0D, false);
        tendril(tessellator, icon, originX, originY, originZ, 9, 3, 17, 3, 13, 12, 3, -45.0D, true);
        tendril(tessellator, icon, originX, originY, originZ, 9, 13, 17, 13, 13, 12, 13, 45.0D, true);
        tendril(tessellator, icon, originX, originY, originZ, -1, 13, 7, 13, 3, 12, 13, -45.0D, false);
    }

    private static void tendril(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        double fromX, double z, double toX, double ignoredZ, double pivotX, double pivotY, double pivotZ, double angle,
        boolean reverseTexture) {
        Point pivot = point(pivotX, pivotY, pivotZ);
        Point lowerLeft = rotateY(point(fromX, 8, z), pivot, angle);
        Point lowerRight = rotateY(point(toX, 8, ignoredZ), pivot, angle);
        Point upperRight = rotateY(point(toX, 16, ignoredZ), pivot, angle);
        Point upperLeft = rotateY(point(fromX, 16, z), pivot, angle);
        if (reverseTexture) {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                lowerLeft,
                lowerRight,
                upperRight,
                upperLeft,
                12,
                8,
                4,
                16);
        } else {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                lowerLeft,
                lowerRight,
                upperRight,
                upperLeft,
                4,
                8,
                12,
                16);
        }
    }

    private static void renderVein(Tessellator tessellator, IIcon icon, int metadata, boolean up, boolean down,
        double originX, double originY, double originZ) {
        if ((metadata & BlockSculkVein.NORTH) != 0) {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                new Point(1, 0, VEIN_INSET),
                new Point(0, 0, VEIN_INSET),
                new Point(0, 1, VEIN_INSET),
                new Point(1, 1, VEIN_INSET),
                0,
                0,
                16,
                16);
        }
        if ((metadata & BlockSculkVein.EAST) != 0) {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                new Point(1 - VEIN_INSET, 0, 0),
                new Point(1 - VEIN_INSET, 0, 1),
                new Point(1 - VEIN_INSET, 1, 1),
                new Point(1 - VEIN_INSET, 1, 0),
                0,
                0,
                16,
                16);
        }
        if ((metadata & BlockSculkVein.SOUTH) != 0) {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                new Point(0, 0, 1 - VEIN_INSET),
                new Point(1, 0, 1 - VEIN_INSET),
                new Point(1, 1, 1 - VEIN_INSET),
                new Point(0, 1, 1 - VEIN_INSET),
                0,
                0,
                16,
                16);
        }
        if ((metadata & BlockSculkVein.WEST) != 0) {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                new Point(VEIN_INSET, 0, 1),
                new Point(VEIN_INSET, 0, 0),
                new Point(VEIN_INSET, 1, 0),
                new Point(VEIN_INSET, 1, 1),
                0,
                0,
                16,
                16);
        }
        if (up) {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                new Point(0, 1 - VEIN_INSET, 0),
                new Point(1, 1 - VEIN_INSET, 0),
                new Point(1, 1 - VEIN_INSET, 1),
                new Point(0, 1 - VEIN_INSET, 1),
                0,
                0,
                16,
                16);
        }
        if (down) {
            quadDouble(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                new Point(0, VEIN_INSET, 1),
                new Point(1, VEIN_INSET, 1),
                new Point(1, VEIN_INSET, 0),
                new Point(0, VEIN_INSET, 0),
                0,
                0,
                16,
                16);
        }
    }

    private static void cuboid(Tessellator tessellator, double originX, double originY, double originZ, double minX,
        double minY, double minZ, double maxX, double maxY, double maxZ, IIcon bottom, IIcon side, IIcon top,
        double sideMinU, double sideMinV, double sideMaxU, double sideMaxV, double topMinU, double topMinV,
        double topMaxU, double topMaxV) {
        double x0 = minX * PIXEL;
        double y0 = minY * PIXEL;
        double z0 = minZ * PIXEL;
        double x1 = maxX * PIXEL;
        double y1 = maxY * PIXEL;
        double z1 = maxZ * PIXEL;
        if (bottom != null) {
            quadDouble(
                tessellator,
                bottom,
                originX,
                originY,
                originZ,
                new Point(x0, y0, z1),
                new Point(x1, y0, z1),
                new Point(x1, y0, z0),
                new Point(x0, y0, z0),
                topMinU,
                topMinV,
                topMaxU,
                topMaxV);
        }
        if (top != null) {
            quadDouble(
                tessellator,
                top,
                originX,
                originY,
                originZ,
                new Point(x0, y1, z0),
                new Point(x1, y1, z0),
                new Point(x1, y1, z1),
                new Point(x0, y1, z1),
                topMinU,
                topMinV,
                topMaxU,
                topMaxV);
        }
        quadDouble(
            tessellator,
            side,
            originX,
            originY,
            originZ,
            new Point(x1, y0, z0),
            new Point(x0, y0, z0),
            new Point(x0, y1, z0),
            new Point(x1, y1, z0),
            sideMinU,
            sideMinV,
            sideMaxU,
            sideMaxV);
        quadDouble(
            tessellator,
            side,
            originX,
            originY,
            originZ,
            new Point(x0, y0, z1),
            new Point(x1, y0, z1),
            new Point(x1, y1, z1),
            new Point(x0, y1, z1),
            sideMinU,
            sideMinV,
            sideMaxU,
            sideMaxV);
        quadDouble(
            tessellator,
            side,
            originX,
            originY,
            originZ,
            new Point(x0, y0, z0),
            new Point(x0, y0, z1),
            new Point(x0, y1, z1),
            new Point(x0, y1, z0),
            sideMinU,
            sideMinV,
            sideMaxU,
            sideMaxV);
        quadDouble(
            tessellator,
            side,
            originX,
            originY,
            originZ,
            new Point(x1, y0, z1),
            new Point(x1, y0, z0),
            new Point(x1, y1, z0),
            new Point(x1, y1, z1),
            sideMinU,
            sideMinV,
            sideMaxU,
            sideMaxV);
    }

    private static void quadDouble(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point a, Point b, Point c, Point d, double minU, double minV, double maxU, double maxV) {
        quad(tessellator, icon, originX, originY, originZ, a, b, c, d, minU, minV, maxU, maxV);
        quad(tessellator, icon, originX, originY, originZ, d, c, b, a, minU, minV, maxU, maxV);
    }

    private static void quad(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point a, Point b, Point c, Point d, double minU, double minV, double maxU, double maxV) {
        vertex(tessellator, icon, originX, originY, originZ, a, minU, maxV);
        vertex(tessellator, icon, originX, originY, originZ, b, maxU, maxV);
        vertex(tessellator, icon, originX, originY, originZ, c, maxU, minV);
        vertex(tessellator, icon, originX, originY, originZ, d, minU, minV);
    }

    private static void vertex(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point point, double u, double v) {
        tessellator.addVertexWithUV(
            originX + point.x,
            originY + point.y,
            originZ + point.z,
            icon.getInterpolatedU(u),
            icon.getInterpolatedV(v));
    }

    private static Point rotateY(Point point, Point pivot, double angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double x = point.x - pivot.x;
        double z = point.z - pivot.z;
        return new Point(pivot.x + x * cos + z * sin, point.y, pivot.z - x * sin + z * cos);
    }

    private static Point point(double x, double y, double z) {
        return new Point(x * PIXEL, y * PIXEL, z * PIXEL);
    }

    private record Point(double x, double y, double z) {}
}
