package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockRuLilyPad;

/** Forge 1.7 rendering of RU's flowering pad and four-piece giant lily model families. */
public final class RuAquaticRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;

    public RuAquaticRenderer(int renderId) {
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
        render(tessellator, (BlockRuLilyPad) block, metadata, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        BlockRuLilyPad lily = (BlockRuLilyPad) block;
        boolean closed = lily.getKind() == BlockRuLilyPad.Kind.FLOWERING
            && Math.floorMod(positionHash(x, y, z), 3) == 2;
        render(tessellator, lily, world.getBlockMetadata(x, y, z), x, y, z, closed);
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

    private static void render(Tessellator tessellator, BlockRuLilyPad block, int metadata, double x, double y,
        double z) {
        render(tessellator, block, metadata, x, y, z, false);
    }

    private static void render(Tessellator tessellator, BlockRuLilyPad block, int metadata, double x, double y,
        double z, boolean closed) {
        if (block.getKind() == BlockRuLilyPad.Kind.FLOWERING) {
            renderFlowering(tessellator, block, x, y, z, closed);
            return;
        }
        renderGiant(tessellator, block, x, y, z, (metadata & 3) * 90);
    }

    private static void renderFlowering(Tessellator tessellator, BlockRuLilyPad block, double x, double y, double z,
        boolean closed) {
        IIcon top = block.getTopIcon();
        IIcon bottom = block.getBottomIcon();
        IIcon flower = block.getFlowerIcon(closed);

        face(tessellator, top, x, y, z, 2, 0.3D, 2, 14, 0.3D, 14, LegacyBlockModelRenderer.Face.UP, 2, 2, 14, 14, 0, 0);
        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            2,
            0.3D,
            2,
            14,
            0.3D,
            14,
            LegacyBlockModelRenderer.Face.DOWN,
            2,
            14,
            14,
            2,
            0,
            0);

        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            2,
            0.3D,
            2,
            14,
            2.3D,
            2,
            LegacyBlockModelRenderer.Face.NORTH,
            14,
            2,
            16,
            14,
            270,
            0);
        face(
            tessellator,
            top,
            x,
            y,
            z,
            2,
            0.3D,
            2,
            14,
            2.3D,
            2,
            LegacyBlockModelRenderer.Face.SOUTH,
            14,
            14,
            16,
            2,
            270,
            0);

        flowerFace(tessellator, flower, x, y, z, LegacyBlockModelRenderer.Face.NORTH, -45);
        flowerFace(tessellator, flower, x, y, z, LegacyBlockModelRenderer.Face.SOUTH, -45);
        flowerFace(tessellator, flower, x, y, z, LegacyBlockModelRenderer.Face.NORTH, 45);
        flowerFace(tessellator, flower, x, y, z, LegacyBlockModelRenderer.Face.SOUTH, 45);

        face(
            tessellator,
            top,
            x,
            y,
            z,
            2,
            0.3D,
            14,
            14,
            2.3D,
            14,
            LegacyBlockModelRenderer.Face.NORTH,
            2,
            14,
            0,
            2,
            270,
            0);
        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            2,
            0.3D,
            14,
            14,
            2.3D,
            14,
            LegacyBlockModelRenderer.Face.SOUTH,
            2,
            2,
            0,
            14,
            270,
            0);

        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            14,
            0.3D,
            2,
            14,
            2.3D,
            14,
            LegacyBlockModelRenderer.Face.EAST,
            2,
            0,
            14,
            2,
            0,
            0);
        face(
            tessellator,
            top,
            x,
            y,
            z,
            14,
            0.3D,
            2,
            14,
            2.3D,
            14,
            LegacyBlockModelRenderer.Face.WEST,
            2,
            2,
            14,
            0,
            180,
            0);

        face(
            tessellator,
            top,
            x,
            y,
            z,
            2,
            0.3D,
            2,
            2,
            2.3D,
            14,
            LegacyBlockModelRenderer.Face.EAST,
            2,
            14,
            14,
            16,
            180,
            0);
        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            2,
            0.3D,
            2,
            2,
            2.3D,
            14,
            LegacyBlockModelRenderer.Face.WEST,
            2,
            16,
            14,
            14,
            0,
            0);
    }

    private static void renderGiant(Tessellator tessellator, BlockRuLilyPad block, double x, double y, double z,
        int modelYRotation) {
        IIcon top = block.getTopIcon();
        IIcon bottom = block.getBottomIcon();
        face(
            tessellator,
            top,
            x,
            y,
            z,
            0,
            0.3D,
            2,
            14,
            0.3D,
            16,
            LegacyBlockModelRenderer.Face.UP,
            0,
            2,
            14,
            16,
            0,
            modelYRotation);
        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            0,
            0.3D,
            2,
            14,
            0.3D,
            16,
            LegacyBlockModelRenderer.Face.DOWN,
            0,
            16,
            14,
            2,
            0,
            modelYRotation);
        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            0,
            0.3D,
            2,
            14,
            2.3D,
            2,
            LegacyBlockModelRenderer.Face.NORTH,
            14,
            2,
            16,
            16,
            270,
            modelYRotation);
        face(
            tessellator,
            top,
            x,
            y,
            z,
            0,
            0.3D,
            2,
            14,
            2.3D,
            2,
            LegacyBlockModelRenderer.Face.SOUTH,
            14,
            16,
            16,
            2,
            270,
            modelYRotation);
        face(
            tessellator,
            bottom,
            x,
            y,
            z,
            14,
            0.3D,
            2,
            14,
            2.3D,
            16,
            LegacyBlockModelRenderer.Face.EAST,
            0,
            0,
            14,
            2,
            0,
            modelYRotation);
        face(
            tessellator,
            top,
            x,
            y,
            z,
            14,
            0.3D,
            2,
            14,
            2.3D,
            16,
            LegacyBlockModelRenderer.Face.WEST,
            0,
            2,
            14,
            0,
            180,
            modelYRotation);
    }

    private static void flowerFace(Tessellator tessellator, IIcon icon, double x, double y, double z,
        LegacyBlockModelRenderer.Face face, double angle) {
        LegacyBlockModelRenderer.face(
            tessellator,
            icon,
            x,
            y,
            z,
            5,
            0.3D,
            8,
            11,
            5.3D,
            8,
            face,
            3,
            11,
            13,
            16,
            0,
            LegacyBlockModelRenderer.Axis.Y,
            angle,
            8,
            1.3D,
            8,
            true,
            0);
    }

    private static void face(Tessellator tessellator, IIcon icon, double x, double y, double z, double fromX,
        double fromY, double fromZ, double toX, double toY, double toZ, LegacyBlockModelRenderer.Face direction,
        double minimumU, double minimumV, double maximumU, double maximumV, int uvRotation, int modelYRotation) {
        LegacyBlockModelRenderer.face(
            tessellator,
            icon,
            x,
            y,
            z,
            fromX,
            fromY,
            fromZ,
            toX,
            toY,
            toZ,
            direction,
            minimumU,
            minimumV,
            maximumU,
            maximumV,
            uvRotation,
            LegacyBlockModelRenderer.Axis.NONE,
            0,
            0,
            0,
            0,
            false,
            modelYRotation);
    }

    private static int positionHash(int x, int y, int z) {
        long hash = (long) (x * 3129871) ^ (long) z * 116129781L ^ y;
        hash = hash * hash * 42317861L + hash * 11L;
        return (int) (hash ^ hash >>> 32);
    }

}
