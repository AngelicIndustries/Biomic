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
import industries.angelic.biomic.content.block.BlockJoshuaBeard;

/** Forge 1.7 rendering of RU's directional Joshua-beard block model. */
public final class JoshuaBeardRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public JoshuaBeardRenderer(int renderId) {
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
        render(tessellator, ((BlockJoshuaBeard) block).getBeardIcon(), metadata, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        render(tessellator, ((BlockJoshuaBeard) block).getBeardIcon(), world.getBlockMetadata(x, y, z), x, y, z);
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

    private static void render(Tessellator tessellator, IIcon icon, int metadata, double x, double y, double z) {
        int rotation = (metadata & 3) * 90;
        element(tessellator, icon, x, y, z, 8.18923D, 16.18923D, 14.47867D, 22.5D, 12.18923D, 0.0D, 8.0D, rotation);
        element(tessellator, icon, x, y, z, 0.57403D, 8.57403D, 14.38582D, 22.5D, 4.57403D, 0.0D, 8.0D, rotation);
        element(tessellator, icon, x, y, z, -3.11985D, 4.88015D, 14.11392D, 45.0D, 0.88015D, 8.0D, 16.0D, rotation);
        element(tessellator, icon, x, y, z, 3.95122D, 11.95122D, 14.11392D, 45.0D, 7.95122D, 8.0D, 16.0D, rotation);
    }

    private static void element(Tessellator tessellator, IIcon icon, double x, double y, double z, double fromY,
        double toY, double planeZ, double angle, double originY, double minimumV, double maximumV, int modelRotation) {
        for (Face face : new Face[] { Face.NORTH, Face.SOUTH }) {
            LegacyBlockModelRenderer.face(
                tessellator,
                icon,
                x,
                y,
                z,
                -1.0D,
                fromY,
                planeZ,
                17.0D,
                toY,
                planeZ,
                face,
                0.0D,
                minimumV,
                16.0D,
                maximumV,
                0,
                Axis.X,
                angle,
                8.0D,
                originY,
                planeZ,
                false,
                modelRotation);
        }
    }
}
