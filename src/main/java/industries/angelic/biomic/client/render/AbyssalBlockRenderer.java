package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry;
import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry.Element;
import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry.Model;
import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry.Surface;
import industries.angelic.biomic.content.alexscaves.BlockAbyssalAquatic;

/** Source-shaped Abyssal models plus the same real-water/shader pass used by existing aquatic flora. */
public final class AbyssalBlockRenderer implements ISimpleBlockRenderingHandler {

    private final int renderId;
    private final boolean inventory3d;

    public AbyssalBlockRenderer(int renderId) {
        this(renderId, true);
    }

    public AbyssalBlockRenderer(int renderId, boolean inventory3d) {
        this.renderId = renderId;
        this.inventory3d = inventory3d;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return inventory3d;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        BlockAbyssalAquatic aquatic = (BlockAbyssalAquatic) block;
        Tessellator tess = Tessellator.instance;
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        IIcon generated = aquatic.generatedItemIcon();
        if (generated == null) renderModel(tess, aquatic, metadata, musselCount(aquatic), 0, 0, 0, 0);
        else renderGeneratedItem(tess, generated);
        tess.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        BlockAbyssalAquatic aquatic = (BlockAbyssalAquatic) block;
        int metadata = world.getBlockMetadata(x, y, z);
        Model model = AbyssalModelGeometry.model(aquatic.kind(), metadata, musselCount(aquatic), alternate(x, y, z));
        if (net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1)
            return model.waterlogged() && aquatic.containsWater(metadata)
                && AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        double[] offset = aquatic.sourceOffset(x, z);
        renderModel(
            Tessellator.instance,
            aquatic,
            metadata,
            musselCount(aquatic),
            alternate(x, y, z),
            x + (offset == null ? 0 : offset[0]),
            y,
            z + (offset == null ? 0 : offset[1]));
        return true;
    }

    private static void renderModel(Tessellator tess, BlockAbyssalAquatic block, int metadata, int musselCount,
        int alternate, double x, double y, double z) {
        Model model = AbyssalModelGeometry.model(block.kind(), metadata, musselCount, alternate);
        if (model.crossed()) {
            renderCross(tess, block.icon(model.crossTexture()), x, y, z);
            return;
        }
        renderGeometry(tess, model, block::icon, x, y, z);
    }

    public static void renderGeometry(Tessellator tess, Model model, java.util.function.IntFunction<IIcon> iconAt,
        double x, double y, double z) {
        renderGeometry(tess, model, iconAt, x, y, z, null, null, 0, 0, 0);
    }

    public static void renderGeometry(Tessellator tess, Model model, java.util.function.IntFunction<IIcon> iconAt,
        double x, double y, double z, IBlockAccess world, Block block, int blockX, int blockY, int blockZ) {
        for (Element element : model.elements()) for (Surface surface : element.surfaces()) {
            LegacyBlockModelRenderer.Face face = face(surface.face());
            LegacyBlockModelRenderer.Axis axis = axis(element.axis());
            if (world != null && surface.cullFace() != null
                && !visible(world, block, blockX, blockY, blockZ, rotatedCull(element, surface.cullFace()))) continue;
            double[][] vertices = LegacyBlockModelRenderer.vertices(
                element.fromX(),
                element.fromY(),
                element.fromZ(),
                element.toX(),
                element.toY(),
                element.toZ(),
                face,
                axis,
                element.angle(),
                element.originX(),
                element.originY(),
                element.originZ(),
                false,
                element.modelX(),
                element.modelY());
            float shade = surface.shaded() ? shade(LegacyBlockModelRenderer.facing(vertices)) : 1F;
            tess.setColorOpaque_F(shade, shade, shade);
            double[][] uvs = LegacyBlockModelRenderer.uvs(
                surface.minimumU(),
                surface.minimumV(),
                surface.maximumU(),
                surface.maximumV(),
                surface.uvRotation());
            IIcon icon = iconAt.apply(surface.texture());
            for (int i = 0; i < 4; i++) tess.addVertexWithUV(
                x + vertices[i][0] / 16D,
                y + vertices[i][1] / 16D,
                z + vertices[i][2] / 16D,
                icon.getInterpolatedU(uvs[i][0]),
                icon.getInterpolatedV(uvs[i][1]));
        }
    }

    private static LegacyBlockModelRenderer.Face rotatedCull(Element element, AbyssalModelGeometry.Face cull) {
        double[][] vertices = LegacyBlockModelRenderer.vertices(
            0,
            0,
            0,
            16,
            16,
            16,
            face(cull),
            LegacyBlockModelRenderer.Axis.NONE,
            0,
            0,
            0,
            0,
            false,
            element.modelX(),
            element.modelY());
        return LegacyBlockModelRenderer.facing(vertices);
    }

    private static boolean visible(IBlockAccess world, Block block, int x, int y, int z,
        LegacyBlockModelRenderer.Face face) {
        int nx = x, ny = y, nz = z;
        switch (face) {
            case DOWN -> ny--;
            case UP -> ny++;
            case NORTH -> nz--;
            case SOUTH -> nz++;
            case WEST -> nx--;
            case EAST -> nx++;
        }
        return block.shouldSideBeRendered(world, nx, ny, nz, face.ordinal());
    }

    public static void renderGeneratedItem(Tessellator tess, IIcon icon) {
        tess.setColorOpaque_F(1, 1, 1);
        double z = .5;
        tess.addVertexWithUV(0, 1, z, icon.getMinU(), icon.getMinV());
        tess.addVertexWithUV(0, 0, z, icon.getMinU(), icon.getMaxV());
        tess.addVertexWithUV(1, 0, z, icon.getMaxU(), icon.getMaxV());
        tess.addVertexWithUV(1, 1, z, icon.getMaxU(), icon.getMinV());
    }

    private static void renderCross(Tessellator tess, IIcon icon, double x, double y, double z) {
        double[][] first = { { .8, 0, .8 }, { 15.2, 0, 15.2 }, { 15.2, 16, 15.2 }, { .8, 16, .8 } };
        double[][] second = { { .8, 0, 15.2 }, { 15.2, 0, .8 }, { 15.2, 16, .8 }, { .8, 16, 15.2 } };
        tess.setColorOpaque_F(1, 1, 1);
        quadBoth(tess, icon, x, y, z, first);
        quadBoth(tess, icon, x, y, z, second);
    }

    private static void quadBoth(Tessellator tess, IIcon icon, double x, double y, double z, double[][] points) {
        double[][] uv = { { 0, 16 }, { 16, 16 }, { 16, 0 }, { 0, 0 } };
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < 4; i++) {
            int at = pass == 0 ? i : 3 - i;
            tess.addVertexWithUV(
                x + points[at][0] / 16D,
                y + points[at][1] / 16D,
                z + points[at][2] / 16D,
                icon.getInterpolatedU(uv[at][0]),
                icon.getInterpolatedV(uv[at][1]));
        }
    }

    private static int alternate(int x, int y, int z) {
        long seed = (long) (x * 3129871) ^ ((long) z * 116129781L) ^ y;
        seed = (seed * seed * 42317861L + seed * 11L) >> 16;
        return (int) seed & 1;
    }

    private static int musselCount(BlockAbyssalAquatic block) {
        int count = industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks.musselCount(block);
        if (count > 0) return count;
        return 1;
    }

    private static float shade(LegacyBlockModelRenderer.Face face) {
        return switch (face) {
            case UP -> 1F;
            case DOWN -> .5F;
            case EAST, WEST -> .6F;
            default -> .8F;
        };
    }

    private static LegacyBlockModelRenderer.Axis axis(AbyssalModelGeometry.Axis axis) {
        return switch (axis) {
            case NONE -> LegacyBlockModelRenderer.Axis.NONE;
            case X -> LegacyBlockModelRenderer.Axis.X;
            case Y -> LegacyBlockModelRenderer.Axis.Y;
            case Z -> LegacyBlockModelRenderer.Axis.Z;
        };
    }

    private static LegacyBlockModelRenderer.Face face(AbyssalModelGeometry.Face face) {
        return switch (face) {
            case DOWN -> LegacyBlockModelRenderer.Face.DOWN;
            case UP -> LegacyBlockModelRenderer.Face.UP;
            case NORTH -> LegacyBlockModelRenderer.Face.NORTH;
            case SOUTH -> LegacyBlockModelRenderer.Face.SOUTH;
            case WEST -> LegacyBlockModelRenderer.Face.WEST;
            case EAST -> LegacyBlockModelRenderer.Face.EAST;
        };
    }
}
