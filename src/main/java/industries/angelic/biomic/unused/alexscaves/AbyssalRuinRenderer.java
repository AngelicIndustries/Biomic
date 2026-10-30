package industries.angelic.biomic.unused.alexscaves;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.client.render.AbyssalBlockRenderer;
import industries.angelic.biomic.client.render.AquaticPlantRenderer;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer;
import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry.Model;

/**
 * Rendering of the unused ruin and bone blocks ({@link AbyssalRuinBlocks}), taken out of {@link AbyssalBlockRenderer}.
 * To bring it back, ask {@link #renderInventory} and {@link #renderWorld} first in that renderer, and let
 * {@code AquaticRenderSemantics} and {@code AquaticWaterAccess} treat wet furnishings and waterlogged scaffolding,
 * lightning rods and iron trapdoors as water in the translucent pass again.
 */
@SideOnly(Side.CLIENT)
public final class AbyssalRuinRenderer {

    private final ganymedes01.etfuturum.client.renderer.block.BlockChainRenderer chainRenderer;

    public AbyssalRuinRenderer(int renderId) {
        chainRenderer = new ganymedes01.etfuturum.client.renderer.block.BlockChainRenderer(renderId);
    }

    /** Draws a ruin or bone block's item; false for any other block. */
    public boolean renderInventory(Block block, int metadata, int modelId, RenderBlocks renderer) {
        if (block instanceof BlockAbyssalBone bone) renderBoneInventory(bone, metadata);
        else if (block instanceof BlockAbyssalRuinShell shell) renderRuinInventory(shell, metadata);
        else if (block instanceof BlockAbyssalWetFurnishing wet) renderWetInventory(wet, metadata, modelId, renderer);
        else return false;
        return true;
    }

    /** Draws a ruin or bone block in the world; null for any other block. */
    public Boolean renderWorld(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        if (block instanceof BlockAbyssalBone bone) {
            if (bone.kind() == BlockAbyssalBone.Kind.WORMS
                && net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1)
                return (world.getBlockMetadata(x, y, z) & 8) == 0
                    && AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
            if (bone.kind() == BlockAbyssalBone.Kind.WORMS) return renderBoneWorms(world, x, y, z, bone);
            boolean rendered = false;
            for (var box : bone.cuboids(world.getBlockMetadata(x, y, z))) {
                renderer.setRenderBounds(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1());
                rendered |= renderer.renderStandardBlock(block, x, y, z);
            }
            renderer.setRenderBoundsFromBlock(block);
            return rendered;
        }
        if (block instanceof BlockAbyssalRuinShell shell) return renderRuin(world, x, y, z, shell, renderer);
        if (block instanceof BlockAbyssalWetFurnishing wet) return renderWet(world, x, y, z, wet, modelId, renderer);
        return null;
    }

    private static void renderBoneInventory(BlockAbyssalBone bone, int metadata) {
        Tessellator tess = Tessellator.instance;
        IIcon icon = bone.getIcon(0, metadata);
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        tess.startDrawingQuads();
        tess.setColorOpaque_F(1, 1, 1);
        if (bone.kind() == BlockAbyssalBone.Kind.WORMS) AbyssalBlockRenderer.renderGeneratedItem(tess, icon);
        else for (var box : bone.cuboids(metadata))
            for (var face : LegacyBlockModelRenderer.Face.values()) LegacyBlockModelRenderer.face(
                tess,
                icon,
                0,
                0,
                0,
                box.x0() * 16,
                box.y0() * 16,
                box.z0() * 16,
                box.x1() * 16,
                box.y1() * 16,
                box.z1() * 16,
                face,
                0,
                0,
                16,
                16,
                0);
        tess.draw();
        GL11.glPopMatrix();
    }

    private void renderWetInventory(BlockAbyssalWetFurnishing block, int metadata, int modelId, RenderBlocks renderer) {
        if (block.kind() == BlockAbyssalWetFurnishing.Kind.CHAIN) {
            chainRenderer.renderInventoryBlock(block, metadata, modelId, renderer);
            return;
        }
        Tessellator tess = Tessellator.instance;
        IIcon icon = block.getIcon(0, metadata);
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        tess.startDrawingQuads();
        tess.setColorOpaque_F(1, 1, 1);
        for (var box : block.cuboids(metadata))
            for (var face : LegacyBlockModelRenderer.Face.values()) LegacyBlockModelRenderer.face(
                tess,
                icon,
                0,
                0,
                0,
                box.x0() * 16,
                box.y0() * 16,
                box.z0() * 16,
                box.x1() * 16,
                box.y1() * 16,
                box.z1() * 16,
                face,
                0,
                0,
                16,
                16,
                0);
        tess.draw();
        GL11.glPopMatrix();
    }

    private static void renderRuinInventory(BlockAbyssalRuinShell block, int metadata) {
        Tessellator tess = Tessellator.instance;
        Model model = switch (block.kind()) {
            case SCRAP -> AbyssalRuinModelGeometry.scrap(0);
            case DEPTH_GLASS -> AbyssalRuinModelGeometry.depthGlassItem();
            case SCAFFOLDING -> AbyssalRuinModelGeometry.scaffolding(false);
            case DRAIN -> AbyssalRuinModelGeometry.drain(false);
            case MAGNETIC_ACTIVATOR -> AbyssalRuinModelGeometry.magneticActivator(1);
            case LIGHTNING_ROD -> RustyVanillaModelGeometry.lightningRod(metadata >= 1 && metadata <= 5 ? metadata : 1);
            case WET_IRON_TRAPDOOR -> RustyVanillaModelGeometry
                .wetIronTrapdoor(metadata >= 2 && metadata <= 5 ? metadata : 2);
            case CEILING_BUTTON -> RustyVanillaModelGeometry
                .ceilingButton(metadata >= 2 && metadata <= 5 ? metadata : 2);
        };
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        AbyssalBlockRenderer.renderGeometry(tess, model, block::icon, 0, 0, 0);
        tess.draw();
        GL11.glPopMatrix();
    }

    private static boolean renderRuin(IBlockAccess world, int x, int y, int z, BlockAbyssalRuinShell block,
        RenderBlocks renderer) {
        int metadata = world.getBlockMetadata(x, y, z),
            pass = net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass();
        if (pass == 1 && (block.kind() == BlockAbyssalRuinShell.Kind.SCAFFOLDING
            || block.kind() == BlockAbyssalRuinShell.Kind.LIGHTNING_ROD
            || block.kind() == BlockAbyssalRuinShell.Kind.WET_IRON_TRAPDOOR))
            return block.waterlogged(metadata) && AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
        if (block.kind() == BlockAbyssalRuinShell.Kind.DRAIN && pass != ((metadata & 1) != 0 ? 1 : 0)) return false;
        Model model = switch (block.kind()) {
            case SCRAP -> AbyssalRuinModelGeometry.scrap(BlockAbyssalRuinShell.scrapVariant(x, y, z));
            case DEPTH_GLASS -> AbyssalRuinModelGeometry.depthGlass(block.depthConnections(world, x, y, z));
            case SCAFFOLDING -> AbyssalRuinModelGeometry.scaffolding((metadata & 1) != 0);
            case DRAIN -> AbyssalRuinModelGeometry.drain((metadata & 1) != 0);
            case MAGNETIC_ACTIVATOR -> AbyssalRuinModelGeometry.magneticActivator(metadata & 7);
            case LIGHTNING_ROD -> RustyVanillaModelGeometry.lightningRod(metadata);
            case WET_IRON_TRAPDOOR -> RustyVanillaModelGeometry.wetIronTrapdoor(metadata);
            case CEILING_BUTTON -> RustyVanillaModelGeometry.ceilingButton(metadata);
        };
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        AbyssalBlockRenderer.renderGeometry(Tessellator.instance, model, block::icon, x, y, z, world, block, x, y, z);
        return true;
    }

    private boolean renderWet(IBlockAccess world, int x, int y, int z, BlockAbyssalWetFurnishing block, int modelId,
        RenderBlocks renderer) {
        if (net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1)
            return AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
        if (block.kind() == BlockAbyssalWetFurnishing.Kind.CHAIN)
            return chainRenderer.renderWorldBlock(world, x, y, z, block, modelId, renderer);
        boolean rendered = false;
        for (var box : block.cuboids(world.getBlockMetadata(x, y, z))) {
            renderer.setRenderBounds(box.x0(), box.y0(), box.z0(), box.x1(), box.y1(), box.z1());
            rendered |= renderer.renderStandardBlock(block, x, y, z);
        }
        block.setBlockBoundsBasedOnState(world, x, y, z);
        renderer.setRenderBoundsFromBlock(block);
        return rendered;
    }

    /** Exact four double-sided planes from the pinned bone_worms.json, rotated by its blockstate variant. */
    private static boolean renderBoneWorms(IBlockAccess world, int x, int y, int z, BlockAbyssalBone block) {
        Tessellator tess = Tessellator.instance;
        IIcon icon = block.getIcon(0, world.getBlockMetadata(x, y, z));
        int[] rotation = boneWormRotation(world.getBlockMetadata(x, y, z));
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        tess.setColorOpaque_F(1, 1, 1);
        for (WormPlane p : boneWormPlanes())
            wormFace(tess, icon, x, y, z, p.x0, p.y0, p.z0, p.x1, p.y1, p.z1, p.face, p.mirror, rotation);
        return true;
    }

    record WormPlane(double x0, double y0, double z0, double x1, double y1, double z1,
        LegacyBlockModelRenderer.Face face, boolean mirror) {}

    static WormPlane[] boneWormPlanes() {
        return new WormPlane[] { new WormPlane(4, 0, 0, 4, 16, 16, LegacyBlockModelRenderer.Face.WEST, false),
            new WormPlane(4, 0, 0, 4, 16, 16, LegacyBlockModelRenderer.Face.EAST, true),
            new WormPlane(12, 0, 0, 12, 16, 16, LegacyBlockModelRenderer.Face.WEST, true),
            new WormPlane(12, 0, 0, 12, 16, 16, LegacyBlockModelRenderer.Face.EAST, false),
            new WormPlane(0, 0, 4, 16, 16, 4, LegacyBlockModelRenderer.Face.NORTH, false),
            new WormPlane(0, 0, 4, 16, 16, 4, LegacyBlockModelRenderer.Face.SOUTH, true),
            new WormPlane(0, 0, 12, 16, 16, 12, LegacyBlockModelRenderer.Face.NORTH, true),
            new WormPlane(0, 0, 12, 16, 16, 12, LegacyBlockModelRenderer.Face.SOUTH, false) };
    }

    static int[] boneWormRotation(int metadata) {
        return switch (metadata & 7) {
            case 0 -> new int[] { 180, 0 };
            case 2 -> new int[] { 90, 0 };
            case 3 -> new int[] { 90, 180 };
            case 4 -> new int[] { 90, 270 };
            case 5 -> new int[] { 90, 90 };
            default -> new int[] { 0, 0 };
        };
    }

    private static void wormFace(Tessellator tess, IIcon icon, double x, double y, double z, double x0, double y0,
        double z0, double x1, double y1, double z1, LegacyBlockModelRenderer.Face face, boolean mirror,
        int[] rotation) {
        double[][] vertices = LegacyBlockModelRenderer.vertices(
            x0,
            y0,
            z0,
            x1,
            y1,
            z1,
            face,
            LegacyBlockModelRenderer.Axis.NONE,
            0,
            0,
            0,
            0,
            false,
            rotation[0],
            rotation[1]);
        double[][] uvs = LegacyBlockModelRenderer.uvs(mirror ? 16 : 0, 0, mirror ? 0 : 16, 16, 0);
        for (int i = 0; i < 4; i++) tess.addVertexWithUV(
            x + vertices[i][0] / 16D,
            y + vertices[i][1] / 16D,
            z + vertices[i][2] / 16D,
            icon.getInterpolatedU(uvs[i][0]),
            icon.getInterpolatedV(uvs[i][1]));
    }
}
