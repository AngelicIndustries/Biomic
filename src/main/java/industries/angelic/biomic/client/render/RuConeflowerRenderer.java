package industries.angelic.biomic.client.render;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Axis;
import industries.angelic.biomic.client.render.LegacyBlockModelRenderer.Face;
import industries.angelic.biomic.content.block.BlockRuConeflower;

/** Exact pinned RU coneflower_1..4 models, precompiled once for all sixteen multipart variants. */
public final class RuConeflowerRenderer implements ISimpleBlockRenderingHandler {

    private static final Quad[][] MODELS = loadModels("coneflower");
    private static final Quad[][] CLOVER_MODELS = loadModels("clover");
    private final int renderId;

    public RuConeflowerRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setNormal(0, 1, 0);
        render(tess, block, metadata, block.getRenderColor(metadata), 0, 0, 0);
        tess.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        render(tess, block, world.getBlockMetadata(x, y, z), block.colorMultiplier(world, x, y, z), x, y, z);
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

    private static void render(Tessellator tess, Block block, int metadata, int tint, double x, double y, double z) {
        boolean clover = block instanceof industries.angelic.biomic.content.block.BlockClover;
        for (Quad quad : (clover ? CLOVER_MODELS : MODELS)[metadata & 15]) {
            IIcon icon = !quad.stem ? block.getIcon(1, metadata)
                : clover ? ((industries.angelic.biomic.content.block.BlockClover) block).getStemIcon()
                    : ((BlockRuConeflower) block).getStemIcon();
            int color = quad.tinted ? tint : 0xFFFFFF;
            tess.setColorOpaque_F(
                ((color >> 16) & 255) / 255.0F * quad.shade,
                ((color >> 8) & 255) / 255.0F * quad.shade,
                (color & 255) / 255.0F * quad.shade);
            for (int v = 0; v < 4; v++) {
                tess.addVertexWithUV(
                    x + quad.vertices[v][0] / 16,
                    y + quad.vertices[v][1] / 16,
                    z + quad.vertices[v][2] / 16,
                    icon.getInterpolatedU(quad.uv[v][0]),
                    icon.getInterpolatedV(quad.uv[v][1]));
            }
        }
    }

    private static Quad[][] loadModels(String prefix) {
        JsonObject[] source = new JsonObject[4];
        for (int part = 0; part < 4; part++) {
            String path = "/assets/biomic/ru_reference/models/" + prefix + "_" + (part + 1) + ".json";
            try (InputStream stream = RuConeflowerRenderer.class.getResourceAsStream(path)) {
                if (stream == null) throw new IllegalStateException("Missing RU model " + path);
                source[part] = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            } catch (Exception failure) {
                throw new IllegalStateException("Could not load RU model " + path, failure);
            }
        }
        Quad[][] models = new Quad[16][];
        for (int metadata = 0; metadata < 16; metadata++) {
            List<Quad> quads = new ArrayList<>();
            int amount = (metadata & 3) + 1, rotation = (metadata >> 2) * 90;
            for (int part = 0; part < amount; part++) {
                for (JsonElement elementValue : source[part].getAsJsonArray("elements")) {
                    JsonObject element = elementValue.getAsJsonObject();
                    double[] from = vector(element.getAsJsonArray("from")), to = vector(element.getAsJsonArray("to"));
                    JsonObject turn = element.getAsJsonObject("rotation");
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
                    for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces")
                        .entrySet()) {
                        JsonObject face = entry.getValue()
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
                            rotation);
                        double[] uv = vector(face.getAsJsonArray("uv"));
                        String texture = face.get("texture")
                            .getAsString();
                        if (!texture.equals("#stem") && !texture.equals("#flowerbed")) {
                            throw new IllegalStateException("Unexpected coneflower texture " + texture);
                        }
                        quads.add(
                            new Quad(
                                vertices,
                                LegacyBlockModelRenderer.uvs(
                                    uv[0],
                                    uv[1],
                                    uv[2],
                                    uv[3],
                                    face.has("rotation") ? face.get("rotation")
                                        .getAsInt() : 0),
                                texture.equals("#stem"),
                                face.has("tintindex") && face.get("tintindex")
                                    .getAsInt() >= 0,
                                element.has("shade") && !element.get("shade")
                                    .getAsBoolean() ? 1 : shade(vertices)));
                    }
                }
            }
            models[metadata] = quads.toArray(new Quad[0]);
        }
        return models;
    }

    private static double[] vector(JsonArray array) {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) values[i] = array.get(i)
            .getAsDouble();
        return values;
    }

    private static float shade(double[][] v) {
        double ax = v[1][0] - v[0][0], ay = v[1][1] - v[0][1], az = v[1][2] - v[0][2];
        double bx = v[2][0] - v[0][0], by = v[2][1] - v[0][1], bz = v[2][2] - v[0][2];
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        if (Math.abs(ny) >= Math.max(Math.abs(nx), Math.abs(nz))) return ny >= 0 ? 1 : 0.5F;
        return Math.abs(nx) > Math.abs(nz) ? 0.6F : 0.8F;
    }

    record Quad(double[][] vertices, double[][] uv, boolean stem, boolean tinted, float shade) {}
}
