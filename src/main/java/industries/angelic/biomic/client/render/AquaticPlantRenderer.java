package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockAquaticPlant;

/** Recreates the exact vanilla cross, seagrass, and floor-coral-fan model families on Forge 1.7. */
public final class AquaticPlantRenderer implements ISimpleBlockRenderingHandler {

    private static final double PIXEL = 1.0D / 16.0D;
    private static final boolean ANGELICA_PRESENT = cpw.mods.fml.common.Loader.isModLoaded("angelica");

    private final int renderId;
    private final boolean inventory3d;

    public AquaticPlantRenderer(int renderId) {
        this(renderId, true);
    }

    public AquaticPlantRenderer(int renderId, boolean inventory3d) {
        this.renderId = renderId;
        this.inventory3d = inventory3d;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        render(tessellator, (BlockAquaticPlant) block, metadata, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        // These blocks contain source water. Vanilla culls neighboring water against Material.water,
        // so we must also draw the fluid in the plant cell (especially its exposed top surface).
        if (net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1) {
            return renderWater(world, x, y, z, renderer);
        }
        // Foliage is cutout geometry, not translucent fluid. Mixing both in pass 1
        // leaves dark transparent texels above the waterline in modern chunk renderers.
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        render(tessellator, (BlockAquaticPlant) block, world.getBlockMetadata(x, y, z), x, y, z);
        return true;
    }

    public static boolean renderWater(IBlockAccess world, int x, int y, int z, RenderBlocks renderer) {
        IBlockAccess previous = renderer.blockAccess;
        try {
            renderer.blockAccess = new AquaticWaterAccess(world);
            // The mesher still sees the enclosing plant's registry ID. Merely asking
            // RenderBlocks for a water texture does not select the shader's water material.
            if (ANGELICA_PRESENT) AngelicaAquaticMaterial.beginWater();
            return renderer.renderBlockLiquid(net.minecraft.init.Blocks.water, x, y, z);
        } finally {
            renderer.blockAccess = previous;
            if (ANGELICA_PRESENT) AngelicaAquaticMaterial.endWater();
        }
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return inventory3d;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    private static void render(Tessellator tessellator, BlockAquaticPlant block, int metadata, double originX,
        double originY, double originZ) {
        IIcon icon = block.getPlantIcon(metadata);
        switch (block.getKind()) {
            case KELP, CORAL_PLANT, PLANT -> renderCross(tessellator, icon, originX, originY, originZ);
            case WALL -> renderWall(tessellator, icon, metadata, originX, originY, originZ);
            case FLAT -> ShellModels.render(
                tessellator,
                industries.angelic.biomic.content.ports.PortBlocks.SHELLS[Math
                    .min(metadata & 15, industries.angelic.biomic.content.ports.PortBlocks.SHELLS.length - 1)],
                originX,
                originY,
                originZ,
                ShellModels.turn((int) originX, (int) originY, (int) originZ));
            case SEAGRASS -> renderSeagrass(tessellator, icon, originX, originY, originZ);
            case SEA_PICKLE -> renderSeaPickles(tessellator, icon, metadata, originX, originY, originZ);
            case CORAL_FAN -> renderCoralFan(tessellator, icon, originX, originY, originZ);
        }
    }

    /**
     * Vanilla's one to four sea pickles: each a 4-wide body with its UV regions of the sea_pickle texture, a cap,
     * and two crossed tentacle planes on top. Bodies: x, z of the corner and height, in pixels.
     */
    private static final int[][][] PICKLES = { { { 6, 6, 6 } }, { { 3, 3, 6 }, { 8, 8, 4 } },
        { { 6, 9, 6 }, { 2, 2, 4 }, { 8, 4, 6 } }, { { 2, 2, 6 }, { 9, 10, 4 }, { 9, 2, 6 }, { 2, 8, 7 } } };

    /** A growth flat on its rock face, just off the face. */
    private static void renderWall(Tessellator tessellator, IIcon icon, int metadata, double originX, double originY,
        double originZ) {
        double o = 0.02D;
        Point[] p = switch (metadata & 3) {
            case 0 -> new Point[] { new Point(1, 1, o), new Point(1, 0, o), new Point(0, 0, o), new Point(0, 1, o) };
            case 1 -> new Point[] { new Point(0, 1, 1 - o), new Point(0, 0, 1 - o), new Point(1, 0, 1 - o),
                new Point(1, 1, 1 - o) };
            case 2 -> new Point[] { new Point(o, 1, 0), new Point(o, 0, 0), new Point(o, 0, 1), new Point(o, 1, 1) };
            default -> new Point[] { new Point(1 - o, 1, 1), new Point(1 - o, 0, 1), new Point(1 - o, 0, 0),
                new Point(1 - o, 1, 0) };
        };
        uvQuad(tessellator, icon, originX, originY, originZ, p[0], p[1], p[2], p[3], 0, 0, 16, 16);
    }

    private static void renderSeaPickles(Tessellator tessellator, IIcon icon, int metadata, double originX,
        double originY, double originZ) {
        for (int[] pickle : PICKLES[Math.min(3, metadata & 15)]) {
            double x0 = pickle[0], z0 = pickle[1], h = pickle[2], x1 = x0 + 4, z1 = z0 + 4;
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(x1, h, z0),
                point(x1, 0, z0),
                point(x0, 0, z0),
                point(x0, h, z0),
                4,
                5,
                8,
                5 + h); // north
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(x0, h, z1),
                point(x0, 0, z1),
                point(x1, 0, z1),
                point(x1, h, z1),
                0,
                5,
                4,
                5 + h); // south
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(x0, h, z0),
                point(x0, 0, z0),
                point(x0, 0, z1),
                point(x0, h, z1),
                8,
                5,
                12,
                5 + h); // west
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(x1, h, z1),
                point(x1, 0, z1),
                point(x1, 0, z0),
                point(x1, h, z0),
                12,
                5,
                16,
                5 + h); // east
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(x0, h, z0),
                point(x0, h, z1),
                point(x1, h, z1),
                point(x1, h, z0),
                4,
                1,
                8,
                5); // up
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(x0, 0, z1),
                point(x0, 0, z0),
                point(x1, 0, z0),
                point(x1, 0, z1),
                8,
                1,
                12,
                5); // down
            // Tentacles: two crossed planes, 45 degrees about the body's own axis, a little wider (rescaled).
            // A 1-pixel plane rescaled by the square root of two, on the diagonal: half a pixel along x and z.
            double cx = x0 + 2, cz = z0 + 2, top = h + 2.7, bottom = h - 0.8, d = 0.5D;
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(cx - d, top, cz - d),
                point(cx - d, bottom, cz - d),
                point(cx + d, bottom, cz + d),
                point(cx + d, top, cz + d),
                1,
                0,
                3,
                5);
            uvQuad(
                tessellator,
                icon,
                originX,
                originY,
                originZ,
                point(cx - d, top, cz + d),
                point(cx - d, bottom, cz + d),
                point(cx + d, bottom, cz - d),
                point(cx + d, top, cz - d),
                13,
                0,
                15,
                5);
        }
    }

    /** A double-sided quad (top-left, bottom-left, bottom-right, top-right) with an explicit UV rectangle in pixels. */
    private static void uvQuad(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point a, Point b, Point c, Point d, double u0, double v0, double u1, double v1) {
        Point[] points = { a, b, c, d };
        double[][] texture = { { u0, v0 }, { u0, v1 }, { u1, v1 }, { u1, v0 } };
        for (int index = 0; index < 4; index++)
            addVertex(tessellator, icon, originX, originY, originZ, points[index], texture[index]);
        for (int index = 3; index >= 0; index--)
            addVertex(tessellator, icon, originX, originY, originZ, points[index], texture[index]);
    }

    private static void renderCross(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ) {
        // The 0.8..15.2 inset and diagonal endpoints are vanilla block/cross after its rescaled 45-degree rotation.
        verticalQuad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(0.8D, 0, 0.8D),
            point(15.2D, 0, 15.2D),
            point(15.2D, 16, 15.2D),
            point(0.8D, 16, 0.8D));
        verticalQuad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(0.8D, 0, 15.2D),
            point(15.2D, 0, 0.8D),
            point(15.2D, 16, 0.8D),
            point(0.8D, 16, 15.2D));
    }

    private static void renderSeagrass(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ) {
        verticalQuad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(0, 0, 4),
            point(16, 0, 4),
            point(16, 16, 4),
            point(0, 16, 4));
        verticalQuad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(12, 0, 0),
            point(12, 0, 16),
            point(12, 16, 16),
            point(12, 16, 0));
        verticalQuad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(4, 0, 16),
            point(4, 0, 0),
            point(4, 16, 0),
            point(4, 16, 16));
        verticalQuad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(16, 0, 12),
            point(0, 0, 12),
            point(0, 16, 12),
            point(16, 16, 12));
    }

    private static void renderCoralFan(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ) {
        horizontalRotated(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(8, 0, 0),
            point(24, 0, 0),
            point(24, 0, 16),
            point(8, 0, 16),
            point(8, 0, 0),
            Axis.Z,
            22.5D,
            90);
        horizontalRotated(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(-8, 0, 0),
            point(8, 0, 0),
            point(8, 0, 16),
            point(-8, 0, 16),
            point(8, 0, 0),
            Axis.Z,
            -22.5D,
            270);
        horizontalRotated(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(0, 0, 8),
            point(16, 0, 8),
            point(16, 0, 24),
            point(0, 0, 24),
            point(0, 0, 8),
            Axis.X,
            -22.5D,
            180);
        horizontalRotated(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            point(0, 0, -8),
            point(16, 0, -8),
            point(16, 0, 8),
            point(0, 0, 8),
            point(0, 0, 8),
            Axis.X,
            22.5D,
            0);
    }

    private static void verticalQuad(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ, Point a, Point b, Point c, Point d) {
        quad(tessellator, icon, originX, originY, originZ, a, b, c, d, 0);
    }

    private static void horizontalRotated(Tessellator tessellator, IIcon icon, double originX, double originY,
        double originZ, Point a, Point b, Point c, Point d, Point pivot, Axis axis, double angle, int textureRotation) {
        quad(
            tessellator,
            icon,
            originX,
            originY,
            originZ,
            rotate(a, pivot, axis, angle),
            rotate(b, pivot, axis, angle),
            rotate(c, pivot, axis, angle),
            rotate(d, pivot, axis, angle),
            textureRotation);
    }

    private static void quad(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point a, Point b, Point c, Point d, int textureRotation) {
        Point[] points = { a, b, c, d };
        double[][] texture = { { 0, 16 }, { 16, 16 }, { 16, 0 }, { 0, 0 } };
        int offset = Math.floorMod(textureRotation / 90, 4);
        for (int index = 0; index < 4; index++) {
            addVertex(tessellator, icon, originX, originY, originZ, points[index], texture[(index + offset) & 3]);
        }
        for (int index = 3; index >= 0; index--) {
            addVertex(tessellator, icon, originX, originY, originZ, points[index], texture[(index + offset) & 3]);
        }
    }

    private static void addVertex(Tessellator tessellator, IIcon icon, double originX, double originY, double originZ,
        Point point, double[] texture) {
        tessellator.addVertexWithUV(
            originX + point.x,
            originY + point.y,
            originZ + point.z,
            icon.getInterpolatedU(texture[0]),
            icon.getInterpolatedV(texture[1]));
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

    private record Point(double x, double y, double z) {}
}
