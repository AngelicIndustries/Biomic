package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.block.BlockRuFloweringLeaves;

/** Native oak foliage plus RU's original untinted cutout flowers; no precolored background. */
public final class RuFloweringLeavesRenderer implements ISimpleBlockRenderingHandler {

    // Separate cutout planes, rather than changing depth/alpha state while a world batch is tessellating.
    static final double FLOWER_OFFSET = 1D / 1024D;
    private final int id;

    public RuFloweringLeavesRenderer(int id) {
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
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        var leaves = (BlockRuFloweringLeaves) block;
        var state = new RenderState(renderer);
        try {
            renderer.lockBlockBounds = false;
            renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
            boolean rendered = renderer.renderStandardBlock(block, x, y, z);
            // A caller's override is a crack/selection texture, not another flower layer.
            if (state.texture != null) return rendered;
            renderer.setOverrideBlockTexture(leaves.flowerIcon());
            renderer.setRenderBounds(
                -FLOWER_OFFSET,
                -FLOWER_OFFSET,
                -FLOWER_OFFSET,
                1 + FLOWER_OFFSET,
                1 + FLOWER_OFFSET,
                1 + FLOWER_OFFSET);
            // Same native face culling/lighting as foliage, but never its biome color multiplier.
            return (Minecraft.isAmbientOcclusionEnabled() && block.getLightValue() == 0
                ? renderer.renderStandardBlockWithAmbientOcclusion(block, x, y, z, 1, 1, 1)
                : renderer.renderStandardBlockWithColorMultiplier(block, x, y, z, 1, 1, 1)) | rendered;
        } finally {
            state.restore(renderer);
            // World vertices have explicit light/color; leave the customary neutral color for the next handler.
            Tessellator.instance.setColorOpaque_F(1, 1, 1);
        }
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        var leaves = (BlockRuFloweringLeaves) block;
        var state = new RenderState(renderer);
        GL11.glPushAttrib(GL11.GL_CURRENT_BIT | GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(-.5F, -.5F, -.5F);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, .1F);
            renderer.enableAO = false;
            renderer.lockBlockBounds = false;
            renderer.setRenderBounds(0, 0, 0, 1, 1, 1);
            int color = layerColor(block.getRenderColor(metadata), false);
            for (int side = 0; side < 6; side++)
                inventoryFace(renderer, block, side, leaves.getIcon(side, metadata), color);
            if (state.texture == null) {
                renderer.setRenderBounds(
                    -FLOWER_OFFSET,
                    -FLOWER_OFFSET,
                    -FLOWER_OFFSET,
                    1 + FLOWER_OFFSET,
                    1 + FLOWER_OFFSET,
                    1 + FLOWER_OFFSET);
                for (int side = 0; side < 6; side++)
                    inventoryFace(renderer, block, side, leaves.flowerIcon(), layerColor(color, true));
            }
        } finally {
            state.restore(renderer);
            Tessellator.instance.setColorOpaque_F(1, 1, 1);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    static int layerColor(int foliage, boolean flowers) {
        return flowers ? 0xFFFFFF : foliage;
    }

    private static void inventoryFace(RenderBlocks renderer, Block block, int side, IIcon icon, int color) {
        var tess = Tessellator.instance;
        tess.startDrawingQuads();
        float r = ((color >> 16) & 255) / 255F, g = ((color >> 8) & 255) / 255F, b = (color & 255) / 255F;
        if (EntityRenderer.anaglyphEnable) {
            float ar = (r * 30 + g * 59 + b * 11) / 100, ag = (r * 30 + g * 70) / 100, ab = (r * 30 + b * 70) / 100;
            r = ar;
            g = ag;
            b = ab;
        }
        tess.setColorOpaque_F(r, g, b);
        switch (side) {
            case 0 -> {
                tess.setNormal(0, -1, 0);
                renderer.renderFaceYNeg(block, 0, 0, 0, icon);
            }
            case 1 -> {
                tess.setNormal(0, 1, 0);
                renderer.renderFaceYPos(block, 0, 0, 0, icon);
            }
            case 2 -> {
                tess.setNormal(0, 0, -1);
                renderer.renderFaceZNeg(block, 0, 0, 0, icon);
            }
            case 3 -> {
                tess.setNormal(0, 0, 1);
                renderer.renderFaceZPos(block, 0, 0, 0, icon);
            }
            case 4 -> {
                tess.setNormal(-1, 0, 0);
                renderer.renderFaceXNeg(block, 0, 0, 0, icon);
            }
            case 5 -> {
                tess.setNormal(1, 0, 0);
                renderer.renderFaceXPos(block, 0, 0, 0, icon);
            }
            default -> throw new IllegalArgumentException("Invalid leaf side");
        }
        tess.draw();
    }

    /** Preserve caller bounds/override/AO even if the second layer throws. No world GL state is mutated. */
    static final class RenderState {

        final IIcon texture;
        private final double x0, y0, z0, x1, y1, z1;
        private final boolean ao, partial, locked;

        RenderState(RenderBlocks r) {
            texture = r.overrideBlockTexture;
            x0 = r.renderMinX;
            y0 = r.renderMinY;
            z0 = r.renderMinZ;
            x1 = r.renderMaxX;
            y1 = r.renderMaxY;
            z1 = r.renderMaxZ;
            ao = r.enableAO;
            partial = r.partialRenderBounds;
            locked = r.lockBlockBounds;
        }

        void restore(RenderBlocks r) {
            r.renderMinX = x0;
            r.renderMinY = y0;
            r.renderMinZ = z0;
            r.renderMaxX = x1;
            r.renderMaxY = y1;
            r.renderMaxZ = z1;
            r.overrideBlockTexture = texture;
            r.enableAO = ao;
            r.partialRenderBounds = partial;
            r.lockBlockBounds = locked;
        }
    }
}
