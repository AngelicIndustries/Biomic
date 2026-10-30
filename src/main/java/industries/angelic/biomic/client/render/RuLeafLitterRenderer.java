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

/** RU leaf_litter_1..4 multipart union. Emit each quadrant once to avoid coplanar overlap. */
public final class RuLeafLitterRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public RuLeafLitterRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        render(tess, block.getIcon(1, metadata), metadata, block.getRenderColor(metadata), 0, 0, 0);
        tess.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        int metadata = world.getBlockMetadata(x, y, z);
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        render(tess, block.getIcon(1, metadata), metadata, block.colorMultiplier(world, x, y, z), x, y, z);
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

    static int quadrantMask(int amount) {
        if (amount < 1 || amount > 4) throw new IllegalArgumentException("Leaf litter amount " + amount);
        return (1 << amount) - 1;
    }

    private static void render(Tessellator tess, IIcon icon, int metadata, int color, double x, double y, double z) {
        tess.setColorOpaque_I(color);
        int mask = quadrantMask((metadata & 3) + 1), rotation = ((metadata >> 2) & 3) * 90;
        for (int q = 0; q < 4; q++) {
            if ((mask & (1 << q)) == 0) continue;
            int x0 = q < 2 ? 0 : 8, z0 = q == 0 || q == 3 ? 0 : 8;
            face(tess, icon, x, y, z, x0, z0, Face.UP, z0, z0 + 8, rotation);
            face(tess, icon, x, y, z, x0, z0, Face.DOWN, z0 + 8, z0, rotation);
        }
    }

    private static void face(Tessellator tess, IIcon icon, double x, double y, double z, int x0, int z0, Face face,
        int v0, int v1, int rotation) {
        LegacyBlockModelRenderer.face(
            tess,
            icon,
            x,
            y,
            z,
            x0,
            0.25,
            z0,
            x0 + 8,
            0.25,
            z0 + 8,
            face,
            x0,
            v0,
            x0 + 8,
            v1,
            0,
            Axis.NONE,
            0,
            0,
            0,
            0,
            false,
            rotation);
    }
}
