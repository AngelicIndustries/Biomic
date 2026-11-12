package industries.angelic.biomic.client.render;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.Biomic;

/**
 * The shells and beach litter as small 3D things lying on the ground (ported, see THIRD_PARTY_NOTICES.md): TFC's own
 * groundcover models (clam, cockle, mussel, urchin, driftwood, seaweed, guano) read from their JSON element boxes, and
 * Tropicraft's shells as extruded sprites, the way a dropped item lies. Each is turned by its position.
 */
public final class ShellModels {

    private record Face(String dir, float[] uv, int rotation, String texture) {}

    private record Element(float[] from, float[] to, List<Face> faces) {}

    private record Model(List<Element> elements, Map<String, String> textures) {}

    private static final Map<String, Model> MODELS = new HashMap<>();
    private static final Map<String, IIcon> ICONS = new HashMap<>();
    /** Each sprite's opaque pixels, [column][row], read from its texture; chunk meshes may be built off-thread. */
    private static final Map<String, boolean[][]> MASKS = new java.util.concurrent.ConcurrentHashMap<>();

    private ShellModels() {}

    /** Registers every texture the shell kinds draw with (call from the shell block's icon registration). */
    public static void registerIcons(IIconRegister register, String[] kinds) {
        // A resource reload may bring other textures: their outlines are read again.
        MASKS.clear();
        for (String kind : kinds) {
            icon(register, "biomic:ports/" + kind);
            Model model = model(kind);
            if (model != null) for (String texture : model.textures.values()) icon(register, texture);
        }
    }

    private static void icon(IIconRegister register, String name) {
        ICONS.put(name, register.registerIcon(name));
    }

    /** One shell of {@code kind} at the block (x, y, z) of a chunk mesh. */
    public static void render(Tessellator tess, String kind, double x, double y, double z, int turn) {
        Model model = model(kind);
        if (model != null) {
            if (kind.startsWith("tfc_driftwood")) model = model("tfc_driftwood_" + turn % 6);
            renderModel(tess, model, x, y, z, turn);
            return;
        }
        IIcon icon = ICONS.get("biomic:ports/" + kind);
        if (icon != null) renderSprite(tess, icon, x, y, z, turn);
    }

    private static Model model(String kind) {
        String name = kind.equals("tfc_driftwood") ? "tfc_driftwood_0" : kind;
        if (!name.startsWith("tfc_") && !name.equals("mari_oyster") || kind.equals("tfc_sea_urchin_spikes"))
            return null;
        return MODELS.computeIfAbsent(name, ShellModels::load);
    }

    private static Model load(String name) {
        ResourceLocation location = new ResourceLocation(Biomic.MODID, "models/ports/" + name + ".json");
        try (InputStream in = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(location)
            .getInputStream()) {
            JsonObject root = new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8))
                .getAsJsonObject();
            Map<String, String> textures = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("textures")
                .entrySet())
                textures.put(
                    "#" + entry.getKey(),
                    entry.getValue()
                        .getAsString());
            List<Element> elements = new ArrayList<>();
            float minY = Float.MAX_VALUE;
            for (JsonElement e : root.getAsJsonArray("elements"))
                minY = Math.min(minY, floats(e.getAsJsonObject(), "from")[1]);
            for (JsonElement e : root.getAsJsonArray("elements")) {
                JsonObject element = e.getAsJsonObject();
                float[] from = floats(element, "from"), to = floats(element, "to");
                // Lying on the ground: some models float a few pixels up.
                from[1] -= minY;
                to[1] -= minY;
                List<Face> faces = new ArrayList<>();
                for (Map.Entry<String, JsonElement> f : element.getAsJsonObject("faces")
                    .entrySet()) {
                    JsonObject face = f.getValue()
                        .getAsJsonObject();
                    faces.add(
                        new Face(
                            f.getKey(),
                            floats(face, "uv"),
                            face.has("rotation") ? face.get("rotation")
                                .getAsInt() : 0,
                            face.get("texture")
                                .getAsString()));
                }
                elements.add(new Element(from, to, faces));
            }
            return new Model(elements, textures);
        } catch (Exception failure) {
            Biomic.LOG.warn("Shell model {} unavailable: {}", name, failure.toString());
            return new Model(List.of(), Map.of());
        }
    }

    private static float[] floats(JsonObject object, String key) {
        JsonArray array = object.getAsJsonArray(key);
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) values[i] = array.get(i)
            .getAsFloat();
        return values;
    }

    private static void renderModel(Tessellator tess, Model model, double x, double y, double z, int turn) {
        for (Element element : model.elements) {
            // A hair above the ground, so a face at the model's foot never shares the ground's plane.
            float x0 = element.from[0] / 16, y0 = element.from[1] / 16 + LIFT, z0 = element.from[2] / 16;
            float x1 = element.to[0] / 16, y1 = element.to[1] / 16 + LIFT, z1 = element.to[2] / 16;
            for (Face face : element.faces) {
                IIcon icon = ICONS.get(model.textures.get(face.texture));
                if (icon == null) continue;
                // Corners as seen from outside: top-left, bottom-left, bottom-right, top-right.
                float[][] c = switch (face.dir) {
                    case "north" -> new float[][] { { x1, y1, z0 }, { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 } };
                    case "south" -> new float[][] { { x0, y1, z1 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 } };
                    case "west" -> new float[][] { { x0, y1, z0 }, { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 } };
                    case "east" -> new float[][] { { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 } };
                    case "up" -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
                    default -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
                };
                float[] uv = face.uv;
                double[][] corners = { { uv[0], uv[1] }, { uv[0], uv[3] }, { uv[2], uv[3] }, { uv[2], uv[1] } };
                int shift = Math.floorMod(face.rotation / 90, 4);
                double[][] u = new double[4][];
                for (int i = 0; i < 4; i++) u[i] = corners[(i + shift) & 3];
                // A box's faces are seen from outside only; a flat element (no depth across its face) from both sides.
                float[] out = switch (face.dir) {
                    case "north" -> z1 > z0 ? NORTH : null;
                    case "south" -> z1 > z0 ? SOUTH : null;
                    case "west" -> x1 > x0 ? WEST : null;
                    case "east" -> x1 > x0 ? EAST : null;
                    case "up" -> y1 > y0 ? UP : null;
                    default -> y1 > y0 ? DOWN : null;
                };
                quad(tess, icon, c, u, x, y, z, turn, out);
            }
        }
    }

    /**
     * A sprite lying flat as an extruded item, a little smaller than the block: its top and bottom, and a side face
     * along each edge of its outline. Vanilla extrudes an item with a strip the sprite's length at every pixel row and
     * column, its transparent texels cut by the alpha test; on the mipmapped block atlas those strips, seen edge-on,
     * sample blurred levels where the transparent texels are no longer transparent, and drew as long lines across the
     * block. Here each side face covers one pixel's edge and samples that texel's centre alone, so it is never blurred.
     */
    private static void renderSprite(Tessellator tess, IIcon icon, double x, double y, double z, int turn) {
        float s = 0.75F, o = (1 - s) / 2, bottom = LIFT, top = bottom + 1 / 16F;
        float[][] upFace = { { o, top, o }, { o, top, o + s }, { o + s, top, o + s }, { o + s, top, o } };
        double[][] full = { { 0, 0 }, { 0, 16 }, { 16, 16 }, { 16, 0 } };
        quad(tess, icon, upFace, full, x, y, z, turn, UP);
        float[][] downFace = { { o, bottom, o + s }, { o, bottom, o }, { o + s, bottom, o }, { o + s, bottom, o + s } };
        double[][] flipped = { { 0, 16 }, { 0, 0 }, { 16, 0 }, { 16, 16 } };
        quad(tess, icon, downFace, flipped, x, y, z, turn, DOWN);
        boolean[][] mask = MASKS.computeIfAbsent(icon.getIconName(), ShellModels::mask);
        int n = mask.length;
        float p = s / n;
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            if (!mask[i][j]) continue;
            float x0 = o + p * i, x1 = x0 + p, z0 = o + p * j, z1 = z0 + p;
            double u = (i + 0.5) * 16 / n, v = (j + 0.5) * 16 / n;
            double[][] texel = { { u, v }, { u, v }, { u, v }, { u, v } };
            if (i == 0 || !mask[i - 1][j]) quad(
                tess,
                icon,
                new float[][] { { x0, top, z0 }, { x0, bottom, z0 }, { x0, bottom, z1 }, { x0, top, z1 } },
                texel,
                x,
                y,
                z,
                turn,
                WEST);
            if (i == n - 1 || !mask[i + 1][j]) quad(
                tess,
                icon,
                new float[][] { { x1, top, z0 }, { x1, bottom, z0 }, { x1, bottom, z1 }, { x1, top, z1 } },
                texel,
                x,
                y,
                z,
                turn,
                EAST);
            if (j == 0 || !mask[i][j - 1]) quad(
                tess,
                icon,
                new float[][] { { x0, top, z0 }, { x0, bottom, z0 }, { x1, bottom, z0 }, { x1, top, z0 } },
                texel,
                x,
                y,
                z,
                turn,
                NORTH);
            if (j == n - 1 || !mask[i][j + 1]) quad(
                tess,
                icon,
                new float[][] { { x0, top, z1 }, { x0, bottom, z1 }, { x1, bottom, z1 }, { x1, top, z1 } },
                texel,
                x,
                y,
                z,
                turn,
                SOUTH);
        }
    }

    /**
     * The opaque pixels of an icon's texture ({@code domain:path} under textures/blocks), as the alpha test keeps them;
     * the first frame of an animated one. A texture that cannot be read gets no side faces.
     */
    private static boolean[][] mask(String iconName) {
        int colon = iconName.indexOf(':');
        ResourceLocation location = new ResourceLocation(
            colon < 0 ? "minecraft" : iconName.substring(0, colon),
            "textures/blocks/" + iconName.substring(colon + 1) + ".png");
        try (InputStream in = Minecraft.getMinecraft()
            .getResourceManager()
            .getResource(location)
            .getInputStream()) {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(in);
            int n = image.getWidth();
            boolean[][] mask = new boolean[n][n];
            for (int i = 0; i < n; i++)
                for (int j = 0; j < n && j < image.getHeight(); j++) mask[i][j] = (image.getRGB(i, j) >>> 24) > 25;
            return mask;
        } catch (Exception failure) {
            Biomic.LOG.warn("Shell sprite {} has no readable outline: {}", iconName, failure.toString());
            return new boolean[0][0];
        }
    }

    private static final float LIFT = 0.005F;
    private static final float[] UP = { 0, 1, 0 }, DOWN = { 0, -1, 0 }, NORTH = { 0, 0, -1 }, SOUTH = { 0, 0, 1 },
        WEST = { -1, 0, 0 }, EAST = { 1, 0, 0 };

    /**
     * A quad, its corners turned {@code turn} quarter turns about the block's vertical axis: seen from the side
     * {@code outward} points to, or from both sides when it is null.
     */
    private static void quad(Tessellator tess, IIcon icon, float[][] corners, double[][] uv, double x, double y,
        double z, int turn, float[] outward) {
        double[][] p = new double[4][];
        for (int i = 0; i < 4; i++) {
            double cx = corners[i][0] - 0.5, cz = corners[i][2] - 0.5;
            for (int t = 0; t < (turn & 3); t++) {
                double nx = -cz;
                cz = cx;
                cx = nx;
            }
            p[i] = new double[] { x + cx + 0.5, y + corners[i][1], z + cz + 0.5 };
        }
        boolean forward = true, backward = outward == null;
        if (outward != null) {
            // The front of a quad is where (p1 - p0) x (p2 - p0) points; a turn about the vertical keeps that side.
            float[] a = corners[0], b = corners[1], c = corners[2];
            float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1],
                vz = c[2] - a[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            forward = nx * outward[0] + ny * outward[1] + nz * outward[2] >= 0;
            backward = !forward;
        }
        if (forward) for (int i = 0; i < 4; i++) tess.addVertexWithUV(
            p[i][0],
            p[i][1],
            p[i][2],
            icon.getInterpolatedU(uv[i][0]),
            icon.getInterpolatedV(uv[i][1]));
        if (backward) for (int i = 3; i >= 0; i--) tess.addVertexWithUV(
            p[i][0],
            p[i][1],
            p[i][2],
            icon.getInterpolatedU(uv[i][0]),
            icon.getInterpolatedV(uv[i][1]));
    }

    /** A quarter-turn count from the position, so neighbours lie differently. */
    public static int turn(int x, int y, int z) {
        return ((x * 3129871) ^ (z * 116129781) ^ y) >>> 4 & 0xFFFF;
    }
}
