package industries.angelic.biomic.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Axis;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Face;
import industries.angelic.biomic.content.RuCaveModelData;
import industries.angelic.biomic.content.block.BlockRuCaveModel;

/** Prebaked source quads: crystal silhouettes, both shine/base layers, and per-face prismoss tint. */
public final class RuCaveModelRenderer implements ISimpleBlockRenderingHandler {

    private static final Map<String, Quad[][]> MODELS = new java.util.concurrent.ConcurrentHashMap<>();
    private final int id;
    private final boolean inventory3d;

    public RuCaveModelRenderer(int id) {
        this(id, true);
    }

    public RuCaveModelRenderer(int id, boolean inventory3d) {
        this.id = id;
        this.inventory3d = inventory3d;
    }

    @Override
    public int getRenderId() {
        return id;
    }

    @Override
    public boolean shouldRender3DInInventory(int id) {
        return inventory3d;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        var cave = (BlockRuCaveModel) block;
        var tess = Tessellator.instance;
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslatef(-.5F, -.5F, -.5F);
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        render(cave, metadata, 0, 0, 0, cave.getRenderColor(metadata), null);
        tess.draw();
        org.lwjgl.opengl.GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        var cave = (BlockRuCaveModel) block;
        if (cave.isWaterlogged() && net.minecraftforge.client.ForgeHooksClient.getWorldRenderPass() == 1)
            return AquaticPlantRenderer.renderWater(world, x, y, z, renderer);
        int metadata = cave.modelMetadata(world.getBlockMetadata(x, y, z));
        if (cave.modelName()
            .equals("tall_hyacinth_stock")) {
            // Older chunks lost shape metadata. Infer the same six shapes from
            // immediate neighbours for rendering without rewriting the save.
            metadata = RuCaveModelData.hyacinthShape(
                world.getBlock(x, y - 1, z) == block,
                world.getBlock(x, y + 1, z) == block,
                world.getBlock(x, y + 2, z) == block);
        }
        if (RuCaveModelData.multiface(cave.modelName()) && metadata == 0) {
            metadata = cave.supportedAttachments(world, x, y, z, 63);
            if (metadata == 0) return false;
        }
        render(cave, metadata, x, y, z, cave.colorMultiplier(world, x, y, z), world);
        return true;
    }

    private static void render(BlockRuCaveModel block, int metadata, int x, int y, int z, int tint,
        IBlockAccess world) {
        renderSource(block, block.modelName(), metadata, x, y, z, tint, world, block::modelIcon);
    }

    /** Share the source baker with existing double plants without changing their block/item contract. */
    static void renderSource(Block block, String name, int metadata, int x, int y, int z, int tint, IBlockAccess world,
        java.util.function.Function<String, net.minecraft.util.IIcon> icons) {
        Quad[][] choices = models(name, metadata);
        long seed = (long) (x * 3129871) ^ ((long) z * 116129781L) ^ y;
        seed = (seed * seed * 42317861L + seed * 11L) >> 16;
        var random = new industries.angelic.biomic.world.modern.noise.ModernLegacyRandom(seed);
        int choice = choices.length == 1 ? 0 : (int) Math.abs((long) (int) random.nextLong()) % choices.length;
        boolean offset = name.equals("hyacinth_bloom") || name.startsWith("dropleaf")
            || name.contains("prismarite")
            || name.equals("prismoss_sprout")
            || name.equals("brimsprout")
            || name.equals("glistering_fern")
            || RuCaveModelData.legacyDoublePlant(name);
        long offsetSeed = (long) (x * 3129871) ^ (long) z * 116129781L;
        offsetSeed = (offsetSeed * offsetSeed * 42317861L + offsetSeed * 11L) >> 16;
        double ox = offset ? ((double) ((offsetSeed >> 16) & 15) / 15D - .5D) * .5D : 0;
        double oz = offset ? ((double) ((offsetSeed >> 24) & 15) / 15D - .5D) * .5D : 0;
        double oy = name.equals("large_prismarite_cluster") || name.equals("prismoss_sprout")
            || name.equals("brimsprout") ? ((double) ((offsetSeed >> 20) & 15) / 15D - 1D) * .2D : 0;
        var tess = Tessellator.instance;
        for (Quad quad : choices[choice]) {
            if (world != null && quad.cull != null) {
                int[] d = delta(quad.cull);
                if (world.getBlock(x + d[0], y + d[1], z + d[2])
                    .isOpaqueCube()) continue;
            }
            if (world != null) tess.setBrightness(
                name.contains("prismarite") || RuCaveModelData.hangingHead(name)
                    || name.equals("mycotoxic_mushrooms")
                    || name.equals("hyacinth_flowers")
                    || RuCaveModelData.alwaysWaterFilled(name)
                    || RuCaveModelData.legacyDoublePlant(name) ? 0xF000F0
                        : block.getMixedBrightnessForBlock(world, x, y, z));
            int color = quad.tinted ? tint : 0xFFFFFF;
            tess.setColorOpaque_F(
                ((color >> 16) & 255) / 255F * quad.shade,
                ((color >> 8) & 255) / 255F * quad.shade,
                (color & 255) / 255F * quad.shade);
            var icon = icons.apply(quad.texture);
            for (int v = 0; v < 4; v++) tess.addVertexWithUV(
                x + ox + quad.vertices[v][0] / 16,
                y + oy + quad.vertices[v][1] / 16,
                z + oz + quad.vertices[v][2] / 16,
                icon.getInterpolatedU(quad.uv[v][0]),
                icon.getInterpolatedV(quad.uv[v][1]));
        }
    }

    static Quad[][] models(String name, int metadata) {
        return MODELS.computeIfAbsent(name + ":" + metadata, ignored -> bake(RuCaveModelData.variants(name, metadata)));
    }

    static Quad[][] bake(JsonArray models) {
        List<Quad[]> choices = new ArrayList<>();
        for (JsonElement modelValue : models) {
            JsonObject model = modelValue.getAsJsonObject();
            List<Quad> quads = new ArrayList<>();
            int modelRotation = model.get("y")
                .getAsInt();
            for (JsonElement value : model.getAsJsonArray("elements")) {
                var element = value.getAsJsonObject();
                var modelTurn = element.getAsJsonObject("model_rotation");
                int rotationX = modelTurn == null ? 0
                    : modelTurn.get("x")
                        .getAsInt();
                int rotationY = modelTurn == null ? modelRotation
                    : modelTurn.get("y")
                        .getAsInt();
                boolean uvlock = modelTurn != null && modelTurn.get("uvlock")
                    .getAsBoolean();
                double[] from = vector(element.getAsJsonArray("from")), to = vector(element.getAsJsonArray("to"));
                var turn = element.getAsJsonObject("rotation");
                Axis axis = turn == null ? Axis.NONE
                    : Axis.valueOf(
                        turn.get("axis")
                            .getAsString()
                            .toUpperCase(Locale.ROOT));
                double angle = turn == null ? 0
                    : turn.get("angle")
                        .getAsDouble();
                double[] origin = turn == null ? new double[3] : vector(turn.getAsJsonArray("origin"));
                boolean rescale = turn != null && turn.has("rescale")
                    && turn.get("rescale")
                        .getAsBoolean();
                for (var entry : element.getAsJsonObject("faces")
                    .entrySet()) {
                    var face = entry.getValue()
                        .getAsJsonObject();
                    Face direction = Face.valueOf(
                        entry.getKey()
                            .toUpperCase(Locale.ROOT));
                    double[][] vertices = LegacyBlockModelRenderer.vertices(
                        from[0],
                        from[1],
                        from[2],
                        to[0],
                        to[1],
                        to[2],
                        direction,
                        axis,
                        angle,
                        origin[0],
                        origin[1],
                        origin[2],
                        rescale,
                        rotationX,
                        rotationY);
                    double[] uv = vector(face.getAsJsonArray("uv"));
                    String cull = face.has("cullface") ? face.get("cullface")
                        .getAsString() : null;
                    // Source prismoss only rotates around Y; rotate its occlusion direction with its quads.
                    if (cull != null && !cull.equals("up") && !cull.equals("down")) {
                        List<String> directions = java.util.Arrays.asList("north", "east", "south", "west");
                        cull = directions.get((directions.indexOf(cull) + rotationY / 90) & 3);
                    }
                    Face worldFace = LegacyBlockModelRenderer.facing(vertices);
                    float shade = element.has("shade") && !element.get("shade")
                        .getAsBoolean()
                            ? 1
                            : worldFace == Face.UP ? 1
                                : worldFace == Face.DOWN ? .5F
                                    : worldFace == Face.EAST || worldFace == Face.WEST ? .6F : .8F;
                    int uvRotation = face.has("rotation") ? face.get("rotation")
                        .getAsInt() : 0;
                    quads.add(
                        new Quad(
                            vertices,
                            uvlock ? LegacyBlockModelRenderer.lockedUvs(vertices, worldFace, uv, uvRotation)
                                : LegacyBlockModelRenderer.uvs(uv[0], uv[1], uv[2], uv[3], uvRotation),
                            face.get("texture")
                                .getAsString(),
                            face.has("tintindex") && face.get("tintindex")
                                .getAsInt() >= 0,
                            shade,
                            cull));
                }
            }
            for (int weight = 0; weight < model.get("weight")
                .getAsInt(); weight++) choices.add(quads.toArray(new Quad[0]));
        }
        return choices.toArray(new Quad[0][]);
    }

    private static double[] vector(JsonArray values) {
        if (values == null) throw new IllegalStateException("Model is missing explicit UV/vector data");
        double[] result = new double[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i)
            .getAsDouble();
        return result;
    }

    private static int[] delta(String face) {
        return switch (face) {
            case "up" -> new int[] { 0, 1, 0 };
            case "down" -> new int[] { 0, -1, 0 };
            case "north" -> new int[] { 0, 0, -1 };
            case "south" -> new int[] { 0, 0, 1 };
            case "west" -> new int[] { -1, 0, 0 };
            case "east" -> new int[] { 1, 0, 0 };
            default -> throw new IllegalArgumentException(face);
        };
    }

    record Quad(double[][] vertices, double[][] uv, String texture, boolean tinted, float shade, String cull) {}
}
