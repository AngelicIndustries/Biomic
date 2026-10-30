package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockBigDripleaf;
import industries.angelic.biomic.content.block.BlockSmallDripleaf;

/** Forge 1.7 rendering of the five vanilla dripleaf model parts referenced by Origen. */
public final class DripleafRenderer implements ISimpleBlockRenderingHandler {

    private static final double STEM_INSET = 3.0D / 16.0D / Math.sqrt(2.0D);
    private static final double SMALL_STEM_INSET = 3.5D / 16.0D / Math.sqrt(2.0D);

    private final int renderId;

    public DripleafRenderer(int renderId) {
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
        if (block instanceof industries.angelic.biomic.content.block.WaterloggedRenderPlant wet
            && wet.containsSourceWater()
            && net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1)
            return AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
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
        int rotation = (metadata + 2) & 3;
        if (block instanceof BlockBigDripleaf dripleaf) {
            if (dripleaf.isStem()) {
                renderBigStem(tessellator, dripleaf.getStemIcon(), rotation, originX, originY, originZ, 1.0D);
            } else {
                renderBigCrown(tessellator, dripleaf, rotation, originX, originY, originZ);
            }
        } else if (block instanceof BlockSmallDripleaf dripleaf) {
            if (BlockSmallDripleaf.isUpper(metadata)) {
                renderSmallCrown(tessellator, dripleaf, rotation, originX, originY, originZ);
            } else {
                renderSmallStem(tessellator, dripleaf.getStemIcon(false), rotation, originX, originY, originZ, 1.0D);
            }
        }
    }

    private static void renderBigCrown(Tessellator tessellator, BlockBigDripleaf block, int rotation, double originX,
        double originY, double originZ) {
        horizontal(
            tessellator,
            block.getTopIcon(),
            rotation,
            originX,
            originY,
            originZ,
            0.0D,
            0.0D,
            1.0D,
            1.0D,
            15.0D / 16.0D,
            0.0D,
            0.0D,
            16.0D,
            16.0D);
        vertical(
            tessellator,
            block.getTipIcon(),
            rotation,
            originX,
            originY,
            originZ,
            0.0D,
            0.0D,
            1.0D,
            0.0D,
            11.0D / 16.0D,
            15.0D / 16.0D,
            0.0D,
            0.0D,
            16.0D,
            4.0D);
        vertical(
            tessellator,
            block.getSideIcon(),
            rotation,
            originX,
            originY,
            originZ,
            0.0D,
            1.0D,
            0.0D,
            0.0D,
            11.0D / 16.0D,
            15.0D / 16.0D,
            0.0D,
            0.0D,
            16.0D,
            4.0D);
        vertical(
            tessellator,
            block.getSideIcon(),
            rotation,
            originX,
            originY,
            originZ,
            1.0D,
            0.0D,
            1.0D,
            1.0D,
            11.0D / 16.0D,
            15.0D / 16.0D,
            0.0D,
            0.0D,
            16.0D,
            4.0D);
        renderBigStem(tessellator, block.getStemIcon(), rotation, originX, originY, originZ, 15.0D / 16.0D);
    }

    private static void renderBigStem(Tessellator tessellator, IIcon icon, int rotation, double originX, double originY,
        double originZ, double height) {
        vertical(
            tessellator,
            icon,
            rotation,
            originX,
            originY,
            originZ,
            0.5D - STEM_INSET,
            0.75D + STEM_INSET,
            0.5D + STEM_INSET,
            0.75D - STEM_INSET,
            0.0D,
            height,
            3.0D,
            0.0D,
            14.0D,
            16.0D);
        vertical(
            tessellator,
            icon,
            rotation,
            originX,
            originY,
            originZ,
            0.5D - STEM_INSET,
            0.75D - STEM_INSET,
            0.5D + STEM_INSET,
            0.75D + STEM_INSET,
            0.0D,
            height,
            3.0D,
            0.0D,
            14.0D,
            16.0D);
    }

    private static void renderSmallCrown(Tessellator tessellator, BlockSmallDripleaf block, int rotation,
        double originX, double originY, double originZ) {
        renderLeafPlate(
            tessellator,
            block.getTopIcon(),
            block.getSideIcon(),
            rotation,
            originX,
            originY,
            originZ,
            8.0D / 16.0D,
            8.0D / 16.0D,
            15.0D / 16.0D,
            15.0D / 16.0D,
            2.0D / 16.0D,
            3.0D / 16.0D,
            8.0D,
            0.0D,
            0.0D,
            8.0D);
        renderLeafPlate(
            tessellator,
            block.getTopIcon(),
            block.getSideIcon(),
            rotation,
            originX,
            originY,
            originZ,
            1.0D / 16.0D,
            1.0D / 16.0D,
            8.0D / 16.0D,
            8.0D / 16.0D,
            7.0D / 16.0D,
            8.0D / 16.0D,
            0.0D,
            8.0D,
            8.0D,
            0.0D);
        renderLeafPlate(
            tessellator,
            block.getTopIcon(),
            block.getSideIcon(),
            rotation,
            originX,
            originY,
            originZ,
            1.0D / 16.0D,
            8.0D / 16.0D,
            8.0D / 16.0D,
            15.0D / 16.0D,
            11.0D / 16.0D,
            12.0D / 16.0D,
            0.0D,
            0.0D,
            8.0D,
            8.0D);
        renderSmallStem(tessellator, block.getStemIcon(true), rotation, originX, originY, originZ, 14.0D / 16.0D);
    }

    private static void renderSmallStem(Tessellator tessellator, IIcon icon, int rotation, double originX,
        double originY, double originZ, double height) {
        vertical(
            tessellator,
            icon,
            rotation,
            originX,
            originY,
            originZ,
            0.5D - SMALL_STEM_INSET,
            0.5D + SMALL_STEM_INSET,
            0.5D + SMALL_STEM_INSET,
            0.5D - SMALL_STEM_INSET,
            0.0D,
            height,
            3.0D,
            0.0D,
            14.0D,
            16.0D);
        vertical(
            tessellator,
            icon,
            rotation,
            originX,
            originY,
            originZ,
            0.5D - SMALL_STEM_INSET,
            0.5D - SMALL_STEM_INSET,
            0.5D + SMALL_STEM_INSET,
            0.5D + SMALL_STEM_INSET,
            0.0D,
            height,
            3.0D,
            0.0D,
            14.0D,
            16.0D);
    }

    private static void renderLeafPlate(Tessellator tessellator, IIcon top, IIcon side, int rotation, double originX,
        double originY, double originZ, double minX, double minZ, double maxX, double maxZ, double bottomY, double topY,
        double minU, double minV, double maxU, double maxV) {
        horizontal(
            tessellator,
            top,
            rotation,
            originX,
            originY,
            originZ,
            minX,
            minZ,
            maxX,
            maxZ,
            topY,
            minU,
            minV,
            maxU,
            maxV);
        vertical(
            tessellator,
            side,
            rotation,
            originX,
            originY,
            originZ,
            minX,
            minZ,
            maxX,
            minZ,
            bottomY,
            topY,
            0.0D,
            0.0D,
            8.0D,
            1.0D);
        vertical(
            tessellator,
            side,
            rotation,
            originX,
            originY,
            originZ,
            maxX,
            minZ,
            maxX,
            maxZ,
            bottomY,
            topY,
            0.0D,
            0.0D,
            8.0D,
            1.0D);
        vertical(
            tessellator,
            side,
            rotation,
            originX,
            originY,
            originZ,
            maxX,
            maxZ,
            minX,
            maxZ,
            bottomY,
            topY,
            0.0D,
            0.0D,
            8.0D,
            1.0D);
        vertical(
            tessellator,
            side,
            rotation,
            originX,
            originY,
            originZ,
            minX,
            maxZ,
            minX,
            minZ,
            bottomY,
            topY,
            0.0D,
            0.0D,
            8.0D,
            1.0D);
    }

    private static void horizontal(Tessellator tessellator, IIcon icon, int rotation, double originX, double originY,
        double originZ, double minX, double minZ, double maxX, double maxZ, double y, double minU, double minV,
        double maxU, double maxV) {
        vertex(tessellator, icon, rotation, originX, originY, originZ, minX, y, minZ, minU, minV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, minX, y, maxZ, minU, maxV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, maxX, y, maxZ, maxU, maxV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, maxX, y, minZ, maxU, minV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, maxX, y, minZ, maxU, minV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, maxX, y, maxZ, maxU, maxV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, minX, y, maxZ, minU, maxV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, minX, y, minZ, minU, minV);
    }

    private static void vertical(Tessellator tessellator, IIcon icon, int rotation, double originX, double originY,
        double originZ, double x1, double z1, double x2, double z2, double minY, double maxY, double minU, double minV,
        double maxU, double maxV) {
        vertex(tessellator, icon, rotation, originX, originY, originZ, x1, minY, z1, minU, maxV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, x2, minY, z2, maxU, maxV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, x2, maxY, z2, maxU, minV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, x1, maxY, z1, minU, minV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, x1, maxY, z1, minU, minV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, x2, maxY, z2, maxU, minV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, x2, minY, z2, maxU, maxV);
        vertex(tessellator, icon, rotation, originX, originY, originZ, x1, minY, z1, minU, maxV);
    }

    private static void vertex(Tessellator tessellator, IIcon icon, int rotation, double originX, double originY,
        double originZ, double x, double y, double z, double u, double v) {
        double translatedX = x - 0.5D;
        double translatedZ = z - 0.5D;
        double rotatedX;
        double rotatedZ;
        switch (rotation & 3) {
            case 1:
                rotatedX = -translatedZ;
                rotatedZ = translatedX;
                break;
            case 2:
                rotatedX = -translatedX;
                rotatedZ = -translatedZ;
                break;
            case 3:
                rotatedX = translatedZ;
                rotatedZ = -translatedX;
                break;
            default:
                rotatedX = translatedX;
                rotatedZ = translatedZ;
        }
        tessellator.addVertexWithUV(
            originX + rotatedX + 0.5D,
            originY + y,
            originZ + rotatedZ + 0.5D,
            icon.getInterpolatedU(u),
            icon.getInterpolatedV(v));
    }
}
