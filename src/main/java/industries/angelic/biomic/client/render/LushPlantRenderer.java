package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockHangingMangrovePropagule;
import industries.angelic.biomic.content.block.BlockSporeBlossom;

/** Forge rendering of vanilla's ceiling spore blossom and age-aware hanging mangrove propagule models. */
public final class LushPlantRenderer implements ISimpleBlockRenderingHandler {

    private static final double PIXEL = 1.0D / 16.0D;
    private static final double[] PROPAGULE_BOTTOM = { 10.0D, 10.0D, 7.0D, 3.0D, 0.0D };

    private final int renderId;

    public LushPlantRenderer(int renderId) {
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
        render(tessellator, block, metadata, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        render(tessellator, block, world.getBlockMetadata(x, y, z), x, y, z);
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

    private static void render(Tessellator tessellator, Block block, int metadata, double originX, double originY,
        double originZ) {
        if (block instanceof BlockSporeBlossom blossom) {
            renderSporeBlossom(tessellator, blossom, originX, originY, originZ);
        } else if (block instanceof BlockHangingMangrovePropagule propagule) {
            renderPropagule(
                tessellator,
                propagule.getHangingIcon(),
                BlockHangingMangrovePropagule.age(metadata),
                originX,
                originY,
                originZ);
        }
    }

    private static void renderSporeBlossom(Tessellator tessellator, BlockSporeBlossom block, double originX,
        double originY, double originZ) {
        IIcon flower = block.getFlowerIcon();
        quad(
            tessellator,
            block.getBaseIcon(),
            originX,
            originY,
            originZ,
            point(1, 15.9D, 1),
            point(1, 15.9D, 15),
            point(15, 15.9D, 15),
            point(15, 15.9D, 1),
            1,
            1,
            15,
            15);

        double petalY = 15.7D * PIXEL;
        rotatedHorizontal(
            tessellator,
            flower,
            originX,
            originY,
            originZ,
            8,
            0,
            24,
            16,
            petalY,
            Axis.Z,
            -22.5D,
            point(8, 16, 0));
        rotatedHorizontal(
            tessellator,
            flower,
            originX,
            originY,
            originZ,
            -8,
            0,
            8,
            16,
            petalY,
            Axis.Z,
            22.5D,
            point(8, 16, 0));
        rotatedHorizontal(
            tessellator,
            flower,
            originX,
            originY,
            originZ,
            0,
            8,
            16,
            24,
            petalY,
            Axis.X,
            22.5D,
            point(0, 16, 8));
        rotatedHorizontal(
            tessellator,
            flower,
            originX,
            originY,
            originZ,
            0,
            -8,
            16,
            8,
            petalY,
            Axis.X,
            -22.5D,
            point(0, 16, 8));
    }

    private static void renderPropagule(Tessellator tessellator, IIcon icon, int age, double originX, double originY,
        double originZ) {
        // Attachment blades and the four folded cotyledons are present in every vanilla age model.
        rotatedVertical(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(7, 14, 8),
            point(9, 14, 8),
            point(9, 16, 8),
            point(7, 16, 8),
            -45.0D,
            0,
            0,
            2,
            2);
        rotatedVertical(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(7, 14, 8),
            point(9, 14, 8),
            point(9, 16, 8),
            point(7, 16, 8),
            45.0D,
            0,
            0,
            2,
            2);
        cube(tessellator, icon, originX, originY, originZ, 7, 13, 7, 9, 14, 9, 0, 0, 2, 3);
        renderPropaguleLeaves(tessellator, icon, originX, originY, originZ);

        if (age == 0) return;
        cube(tessellator, icon, originX, originY, originZ, 7, 10, 7, 9, 13, 9, 0, 7, 2, 10);
        if (age == 1) return;

        double bottom = PROPAGULE_BOTTOM[age];
        rotatedVertical(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(7, bottom, 8),
            point(9, bottom, 8),
            point(9, 10, 8),
            point(7, 10, 8),
            -45.0D,
            3,
            bottom,
            5,
            10);
        rotatedVertical(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(7, bottom, 8),
            point(9, bottom, 8),
            point(9, 10, 8),
            point(7, 10, 8),
            45.0D,
            3,
            bottom,
            5,
            10);
    }

    private static void renderPropaguleLeaves(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ) {
        double y = 13.61104D * PIXEL;
        Point pivot = point(8, 16, 8);
        rotatedHorizontal(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            7,
            10.07193D,
            9,
            12.07193D,
            y,
            Axis.X,
            22.5D,
            pivot);
        rotatedHorizontal(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            10.07193D,
            7,
            12.07193D,
            9,
            y,
            Axis.Z,
            -22.5D,
            pivot);
        rotatedHorizontal(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            7,
            3.92807D,
            9,
            5.92807D,
            y,
            Axis.X,
            -22.5D,
            pivot);
        rotatedHorizontal(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            3.92807D,
            7,
            5.92807D,
            9,
            y,
            Axis.Z,
            22.5D,
            pivot);
    }

    private static void rotatedHorizontal(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ, double minX, double minZ, double maxX, double maxZ, double y, Axis axis, double angle,
        Point pivot) {
        Point a = rotate(new Point(minX * PIXEL, y, minZ * PIXEL), pivot, axis, angle);
        Point b = rotate(new Point(minX * PIXEL, y, maxZ * PIXEL), pivot, axis, angle);
        Point c = rotate(new Point(maxX * PIXEL, y, maxZ * PIXEL), pivot, axis, angle);
        Point d = rotate(new Point(maxX * PIXEL, y, minZ * PIXEL), pivot, axis, angle);
        quad(tessellator, icon, originX, originY, originZ, a, b, c, d, 0, 0, 16, 16);
    }

    private static void rotatedVertical(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ, Point a, Point b, Point c, Point d, double angle, double minU, double minV, double maxU,
        double maxV) {
        Point pivot = point(8, 16, 8);
        quad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            rotate(a, pivot, Axis.Y, angle),
            rotate(b, pivot, Axis.Y, angle),
            rotate(c, pivot, Axis.Y, angle),
            rotate(d, pivot, Axis.Y, angle),
            minU,
            maxV,
            maxU,
            minV);
    }

    private static void cube(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ, double minU, double minV,
        double maxU, double maxV) {
        Point n0 = point(minX, minY, minZ);
        Point n1 = point(maxX, minY, minZ);
        Point n2 = point(maxX, maxY, minZ);
        Point n3 = point(minX, maxY, minZ);
        Point s0 = point(maxX, minY, maxZ);
        Point s1 = point(minX, minY, maxZ);
        Point s2 = point(minX, maxY, maxZ);
        Point s3 = point(maxX, maxY, maxZ);
        quad(tessellator, icon, originX, originY, originZ, n0, n1, n2, n3, minU, maxV, maxU, minV);
        quad(tessellator, icon, originX, originY, originZ, s0, s1, s2, s3, minU, maxV, maxU, minV);
        quad(tessellator, icon, originX, originY, originZ, s1, n0, n3, s2, minU, maxV, maxU, minV);
        quad(tessellator, icon, originX, originY, originZ, n1, s0, s3, n2, minU, maxV, maxU, minV);
        quad(tessellator, icon, originX, originY, originZ, n3, n2, s3, s2, minU, minV, maxU, maxV);
        quad(tessellator, icon, originX, originY, originZ, s1, s0, n1, n0, minU, minV, maxU, maxV);
    }

    private static void quad(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point a, Point b, Point c, Point d, double minU, double minV, double maxU, double maxV) {
        vertex(tessellator, icon, originX, originY, originZ, a, minU, minV);
        vertex(tessellator, icon, originX, originY, originZ, b, minU, maxV);
        vertex(tessellator, icon, originX, originY, originZ, c, maxU, maxV);
        vertex(tessellator, icon, originX, originY, originZ, d, maxU, minV);
        vertex(tessellator, icon, originX, originY, originZ, d, maxU, minV);
        vertex(tessellator, icon, originX, originY, originZ, c, maxU, maxV);
        vertex(tessellator, icon, originX, originY, originZ, b, minU, maxV);
        vertex(tessellator, icon, originX, originY, originZ, a, minU, minV);
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

    private static Point rotate(Point point, Point pivot, Axis axis, double angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        double x = point.x - pivot.x;
        double y = point.y - pivot.y;
        double z = point.z - pivot.z;
        return switch (axis) {
            case X -> new Point(point.x, pivot.y + y * cos - z * sin, pivot.z + y * sin + z * cos);
            case Y -> new Point(pivot.x + x * cos + z * sin, point.y, pivot.z - x * sin + z * cos);
            case Z -> new Point(pivot.x + x * cos - y * sin, pivot.y + x * sin + y * cos, point.z);
        };
    }

    private static Point point(double x, double y, double z) {
        return new Point(x * PIXEL, y * PIXEL, z * PIXEL);
    }

    private enum Axis {
        X,
        Y,
        Z
    }

    private static final class Point {

        private final double x;
        private final double y;
        private final double z;

        private Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
