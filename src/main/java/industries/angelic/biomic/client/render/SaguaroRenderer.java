package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Axis;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Face;
import industries.angelic.biomic.content.block.BlockSaguaro;

/** Cube-model rotation (including UVs), not a log's end-face substitution. */
public final class SaguaroRenderer implements ISimpleBlockRenderingHandler {

    private final int id;
    private static final Face[] FACES = { Face.DOWN, Face.UP, Face.NORTH, Face.SOUTH, Face.WEST, Face.EAST };

    public SaguaroRenderer(int id) {
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
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslatef(-.5F, -.5F, -.5F);
        Tessellator tess = Tessellator.instance;
        for (int face = 0; face < 6; face++) {
            tess.startDrawingQuads();
            int[][] normals = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 }, { 1, 0, 0 } };
            tess.setNormal(normals[face][0], normals[face][1], normals[face][2]);
            tess.setColorOpaque_F(1, 1, 1);
            LegacyBlockModelRenderer.face(
                tess,
                ((BlockSaguaro) block).modelIcon(face, false),
                0,
                0,
                0,
                0,
                0,
                0,
                16,
                16,
                16,
                FACES[face],
                0,
                0,
                16,
                16,
                0);
            tess.draw();
        }
        org.lwjgl.opengl.GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        int metadata = world.getBlockMetadata(x, y, z);
        Tessellator tess = Tessellator.instance;
        for (int face = 0; face < 6; face++) {
            double[][] vertices = vertices(metadata, face);
            int dx = 0, dy = 0, dz = 0;
            double cx = 0, cy = 0, cz = 0;
            for (double[] v : vertices) {
                cx += v[0];
                cy += v[1];
                cz += v[2];
            }
            if (cx < .01) dx = -1;
            else if (cx > 63.99) dx = 1;
            else if (cy < .01) dy = -1;
            else if (cy > 63.99) dy = 1;
            else if (cz < .01) dz = -1;
            else dz = 1;
            if (world.getBlock(x + dx, y + dy, z + dz)
                .isOpaqueCube()) continue;
            tess.setBrightness(block.getMixedBrightnessForBlock(world, x + dx, y + dy, z + dz));
            float shade = dy > 0 ? 1 : dy < 0 ? .5F : dx != 0 ? .6F : .8F;
            tess.setColorOpaque_F(shade, shade, shade);
            IIcon icon = ((BlockSaguaro) block).modelIcon(face, metadata >= 3);
            double[][] uv = LegacyBlockModelRenderer.uvs(0, 0, 16, 16, 0);
            for (int i = 0; i < 4; i++) tess.addVertexWithUV(
                x + vertices[i][0] / 16,
                y + vertices[i][1] / 16,
                z + vertices[i][2] / 16,
                icon.getInterpolatedU(uv[i][0]),
                icon.getInterpolatedV(uv[i][1]));
        }
        return true;
    }

    static double[][] vertices(int metadata, int face) {
        return LegacyBlockModelRenderer.vertices(
            0,
            0,
            0,
            16,
            16,
            16,
            FACES[face],
            metadata == 1 || metadata == 2 ? Axis.X : Axis.NONE,
            metadata == 1 || metadata == 2 ? -90 : 0,
            8,
            8,
            8,
            false,
            metadata >= 3 ? (metadata - 3) * 90 : metadata == 1 ? 90 : 0);
    }
}
