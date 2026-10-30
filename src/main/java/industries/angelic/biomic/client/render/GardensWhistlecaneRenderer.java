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
import industries.angelic.biomic.content.gardens.BlockWhistlecane;

/** Exact legacy bake of the three pinned Gardens whistlecane multipart models and their four equal rotations. */
public final class GardensWhistlecaneRenderer implements ISimpleBlockRenderingHandler {

    private static final Quad[][] MODELS = loadModels();
    private final int renderId;

    public GardensWhistlecaneRenderer(int renderId) {
        this.renderId = renderId;
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-.5F, -.5F, -.5F);
        Tessellator tess = Tessellator.instance;
        IIcon icon = ((BlockWhistlecane) block).gardensModelIcon(4);
        tess.startDrawingQuads();
        tess.setNormal(0, 0, 1);
        tess.addVertexWithUV(0, 1, .5, icon.getMinU(), icon.getMinV());
        tess.addVertexWithUV(0, 0, .5, icon.getMinU(), icon.getMaxV());
        tess.addVertexWithUV(1, 0, .5, icon.getMaxU(), icon.getMaxV());
        tess.addVertexWithUV(1, 1, .5, icon.getMaxU(), icon.getMinV());
        tess.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        render(tess, (BlockWhistlecane) block, MODELS[choice(x, y, z)], world, renderer, x, y, z);
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return renderId;
    }

    private static int choice(int x, int y, int z) {
        long seed = (long) x * 3129871L ^ (long) z * 116129781L ^ y;
        seed = seed * seed * 42317861L + seed * 11L;
        return Math.floorMod((int) (seed >> 16), 12);
    }

    private static void render(Tessellator tess, BlockWhistlecane block, Quad[] model, IBlockAccess world,
        RenderBlocks renderer, int x, int y, int z) {
        for (Quad quad : model) {
            if (world != null && quad.cull != null && !renderer.renderAllFaces && opaque(world, x, y, z, quad.cull))
                continue;
            IIcon icon = renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture
                : block.gardensModelIcon(quad.texture);
            tess.setColorOpaque_F(quad.shade, quad.shade, quad.shade);
            for (int v = 0; v < 4; v++) tess.addVertexWithUV(
                x + quad.vertices[v][0] / 16,
                y + quad.vertices[v][1] / 16,
                z + quad.vertices[v][2] / 16,
                icon.getInterpolatedU(quad.uv[v][0]),
                icon.getInterpolatedV(quad.uv[v][1]));
        }
    }

    private static boolean opaque(IBlockAccess world, int x, int y, int z, Face face) {
        switch (face) {
            case DOWN:
                y--;
                break;
            case UP:
                y++;
                break;
            case NORTH:
                z--;
                break;
            case SOUTH:
                z++;
                break;
            case WEST:
                x--;
                break;
            case EAST:
                x++;
                break;
            default:
        }
        return world.getBlock(x, y, z)
            .isOpaqueCube();
    }

    private static Quad[][] loadModels() {
        Quad[][] models = new Quad[12][];
        for (int variant = 0; variant < 3; variant++) {
            String path = "/assets/biomic/gardens_reference/models/whistlecane" + variant + ".json";
            JsonObject source;
            try (InputStream stream = GardensWhistlecaneRenderer.class.getResourceAsStream(path)) {
                if (stream == null) throw new IllegalStateException("Missing Gardens model " + path);
                source = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            } catch (Exception failure) {
                throw new IllegalStateException("Could not load Gardens model " + path, failure);
            }
            JsonObject textures = source.getAsJsonObject("textures");
            for (int quarter = 0; quarter < 4; quarter++) {
                List<Quad> quads = new ArrayList<>();
                int rotation = quarter * 90;
                for (JsonElement elementValue : source.getAsJsonArray("elements")) {
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
                            false,
                            rotation);
                        double[] uv = vector(face.getAsJsonArray("uv"));
                        String key = face.get("texture")
                            .getAsString()
                            .substring(1);
                        String texture = textures.get(key)
                            .getAsString();
                        int textureIndex = texture.endsWith("whistlecane0") ? 0
                            : texture.endsWith("whistlecane1") ? 1 : texture.endsWith("leaves0") ? 2 : 3;
                        Face cull = face.has("cullface") ? LegacyBlockModelRenderer.facing(vertices) : null;
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
                                textureIndex,
                                cull,
                                shade(vertices)));
                    }
                }
                models[variant * 4 + quarter] = quads.toArray(new Quad[0]);
            }
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
        double ax = v[1][0] - v[0][0], ay = v[1][1] - v[0][1], az = v[1][2] - v[0][2], bx = v[2][0] - v[0][0],
            by = v[2][1] - v[0][1], bz = v[2][2] - v[0][2];
        double nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        if (Math.abs(ny) >= Math.max(Math.abs(nx), Math.abs(nz))) return ny >= 0 ? 1 : .5F;
        return Math.abs(nx) > Math.abs(nz) ? .6F : .8F;
    }

    record Quad(double[][] vertices, double[][] uv, int texture, Face cull, float shade) {}
}
