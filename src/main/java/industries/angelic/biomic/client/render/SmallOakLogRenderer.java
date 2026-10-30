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

/** Pinned RU small_oak_log -> bamboo_log_noleaf model, including packed bark/end UVs and axis rotations. */
public final class SmallOakLogRenderer implements ISimpleBlockRenderingHandler {

    private final int id;
    private static final Face[] FACES = Face.values();

    public SmallOakLogRenderer(int id) {
        this.id = id;
    }

    @Override
    public int getRenderId() {
        return id;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        Tessellator tess = Tessellator.instance;
        for (int face = 0; face < 6; face++) {
            tess.startDrawingQuads();
            double[][] vertices = vertices(0, face);
            double[] normal = normal(vertices);
            tess.setNormal((float) normal[0], (float) normal[1], (float) normal[2]);
            tess.setColorOpaque_F(1, 1, 1);
            emit(tess, block.getIcon(0, 0), vertices, face, 0, 0, 0);
            tess.draw();
        }
        IIcon leaves = ((industries.angelic.biomic.content.block.BlockSmallOakLog) block).leavesIcon(metadata);
        if (leaves != null) {
            tess.startDrawingQuads();
            tess.setColorOpaque_F(1, 1, 1);
            renderLeaves(tess, leaves, metadata, 0, 0, 0);
            tess.draw();
        }
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        for (int face = 0; face < 6; face++) {
            double[][] vertices = vertices(world.getBlockMetadata(x, y, z), face);
            double[] normal = normal(vertices);
            float shade = normal[1] > .5 ? 1 : normal[1] < -.5 ? .5F : Math.abs(normal[0]) > .5 ? .6F : .8F;
            tess.setColorOpaque_F(shade, shade, shade);
            emit(
                tess,
                renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : block.getIcon(0, 0),
                vertices,
                face,
                x,
                y,
                z);
        }
        IIcon leaves = ((industries.angelic.biomic.content.block.BlockSmallOakLog) block)
            .leavesIcon(world.getBlockMetadata(x, y, z));
        if (leaves != null) {
            tess.setColorOpaque_F(1, 1, 1); // Source leaf elements have shade=false; their texture is already colored.
            renderLeaves(
                tess,
                renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : leaves,
                world.getBlockMetadata(x, y, z),
                x,
                y,
                z);
        }
        return true;
    }

    private static void renderLeaves(Tessellator tess, IIcon icon, int metadata, int x, int y, int z) {
        double[][] uv = LegacyBlockModelRenderer.uvs(0, 0, 16, 16, 0);
        for (int plane = 0; plane < 2; plane++) for (boolean back : new boolean[] { false, true }) {
            double[][] vertices = bambooLeafVertices(metadata, plane, back);
            for (int i = 0; i < 4; i++) tess.addVertexWithUV(
                x + vertices[i][0] / 16,
                y + vertices[i][1] / 16,
                z + vertices[i][2] / 16,
                icon.getInterpolatedU(uv[i][0]),
                icon.getInterpolatedV(uv[i][1]));
        }
    }

    /** Exact two unshaded, double-sided elements from RU bamboo_log_withleaf.json. */
    static double[][] bambooLeafVertices(int metadata, int plane, boolean back) {
        double[][] vertices = plane == 0
            ? LegacyBlockModelRenderer
                .vertices(11.35, 0, -9, 11.35, 16, 9, back ? Face.WEST : Face.EAST, Axis.Y, -45, 0, 0, 0, false, 0)
            : LegacyBlockModelRenderer
                .vertices(0, 0, 2.35, 0, 16, 20.35, back ? Face.WEST : Face.EAST, Axis.Y, 45, 0, 0, 0, false, 0);
        int axis = metadata & 12;
        for (double[] v : vertices) {
            if (axis == 4 || axis == 8) {
                double oldY = v[1];
                v[1] = v[2];
                v[2] = 16 - oldY;
            }
            if (axis == 4) {
                double oldX = v[0];
                v[0] = 16 - v[2];
                v[2] = oldX;
            }
        }
        return vertices;
    }

    static double[][] vertices(int metadata, int face) {
        int axis = metadata & 12;
        return LegacyBlockModelRenderer.vertices(
            4,
            0,
            4,
            12,
            16,
            12,
            FACES[face],
            axis == 4 || axis == 8 ? Axis.X : Axis.NONE,
            -90,
            8,
            8,
            8,
            false,
            axis == 4 ? 90 : 0);
    }

    static double[][] uvs(int face) {
        return face < 2 ? LegacyBlockModelRenderer.uvs(8, 0, 16, 8, 0) : LegacyBlockModelRenderer.uvs(0, 0, 8, 16, 0);
    }

    static double[] normal(double[][] vertices) {
        double[] a = vertices[0], b = vertices[1], c = vertices[2];
        double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        double vx = c[0] - b[0], vy = c[1] - b[1], vz = c[2] - b[2];
        double nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        return new double[] { nx / length, ny / length, nz / length };
    }

    private static void emit(Tessellator tess, IIcon icon, double[][] vertices, int face, int x, int y, int z) {
        double[][] uv = uvs(face);
        for (int i = 0; i < 4; i++) tess.addVertexWithUV(
            x + vertices[i][0] / 16,
            y + vertices[i][1] / 16,
            z + vertices[i][2] / 16,
            icon.getInterpolatedU(uv[i][0]),
            icon.getInterpolatedV(uv[i][1]));
    }
}
