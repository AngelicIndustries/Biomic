package industries.angelic.biomic.client.render;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;

import org.lwjgl.opengl.GL11;

import industries.angelic.biomic.content.block.BlockWoodSign;

/** Draws a wood's sign as vanilla draws its own (TileEntitySignRenderer), with the wood's sign art. */
public final class WoodSignRenderer extends TileEntitySpecialRenderer {

    private static final float SCALE = 0.6666667F;
    private final ModelSign model = new ModelSign();

    @Override
    public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
        TileEntitySign sign = (TileEntitySign) tile;
        if (!(sign.getBlockType() instanceof BlockWoodSign block)) return;
        GL11.glPushMatrix();
        GL11.glTranslatef((float) x + 0.5F, (float) y + 0.75F * SCALE, (float) z + 0.5F);
        if (block.standing()) {
            GL11.glRotatef(-(sign.getBlockMetadata() * 360) / 16.0F, 0.0F, 1.0F, 0.0F);
            model.signStick.showModel = true;
        } else {
            int facing = sign.getBlockMetadata();
            float angle = facing == 2 ? 180.0F : facing == 4 ? 90.0F : facing == 5 ? -90.0F : 0.0F;
            GL11.glRotatef(-angle, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, -0.3125F, -0.4375F);
            model.signStick.showModel = false;
        }
        bindTexture(block.texture());
        GL11.glPushMatrix();
        GL11.glScalef(SCALE, -SCALE, -SCALE);
        model.renderSign();
        GL11.glPopMatrix();
        FontRenderer font = func_147498_b();
        float text = 0.016666668F * SCALE;
        GL11.glTranslatef(0.0F, 0.5F * SCALE, 0.07F * SCALE);
        GL11.glScalef(text, -text, text);
        GL11.glNormal3f(0.0F, 0.0F, -1.0F * text);
        GL11.glDepthMask(false);
        for (int line = 0; line < sign.signText.length; line++) {
            String words = line == sign.lineBeingEdited ? "> " + sign.signText[line] + " <" : sign.signText[line];
            font.drawString(words, -font.getStringWidth(words) / 2, line * 10 - sign.signText.length * 5, 0);
        }
        GL11.glDepthMask(true);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }
}
