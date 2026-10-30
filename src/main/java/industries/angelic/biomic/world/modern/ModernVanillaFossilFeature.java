package industries.angelic.biomic.world.modern;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact modern fossil placement using the pinned 1.21.1 structure templates. */
final class ModernVanillaFossilFeature {

    private static final String RESOURCE = "/assets/biomic/ru_modern_parity/fossil-templates.json";
    private static final Map<String, Template> TEMPLATES = loadTemplates();

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        int rotation = random.nextInt(4);
        JsonArray fossilNames = config.getAsJsonArray("fossil_structures");
        JsonArray overlayNames = config.getAsJsonArray("overlay_structures");
        int index = random.nextInt(fossilNames.size());
        Template fossil = template(
            fossilNames.get(index)
                .getAsString());
        Template overlay = template(
            overlayNames.get(index)
                .getAsString());

        int rotatedSizeX = swapsAxes(rotation) ? fossil.sizeZ : fossil.sizeX;
        int rotatedSizeZ = swapsAxes(rotation) ? fossil.sizeX : fossil.sizeZ;
        Pos corner = origin.offset(-rotatedSizeX / 2, 0, -rotatedSizeZ / 2);
        int minimumSurface = origin.y;
        for (int x = 0; x < rotatedSizeX; x++) {
            for (int z = 0; z < rotatedSizeZ; z++) {
                minimumSurface = Math.min(minimumSurface, world.height("OCEAN_FLOOR_WG", corner.x + x, corner.z + z));
            }
        }
        int y = Math.max(minimumSurface - 15 - random.nextInt(10), world.minBuildHeight() + 10);
        Pos placement = zeroPosition(corner.atY(y), fossil, rotation);
        if (emptyCorners(world, fossil, placement, rotation) > config.get("max_empty_corners_allowed")
            .getAsInt()) return false;

        placeTemplate(engine, world, random, fossil, placement, rotation, 0.9F, false);
        boolean diamonds = "minecraft:fossil_diamonds".equals(
            config.get("overlay_processors")
                .getAsString());
        placeTemplate(engine, world, random, overlay, placement, rotation, 0.1F, diamonds);
        return true;
    }

    private static void placeTemplate(ModernDecorationEngine engine, World world, ModernRandom random,
        Template template, Pos placement, int rotation, float integrity, boolean diamonds) {
        // StructurePlaceSettings.getRandomPalette consumes this even for the single palette.
        random.nextInt(1);
        for (Block block : template.blocks) {
            Pos transformed = transform(block.position, rotation).add(placement);
            if (random.nextFloat() > integrity) continue;
            if (engine.isBlockInTag(
                "#minecraft:features_cannot_replace",
                world.blockState(transformed.x, transformed.y, transformed.z))) continue;
            String state = rotateState(template.palette.get(block.palette), rotation);
            if (diamonds && "minecraft:coal_ore".equals(ModernDecorationEngine.blockName(state))) {
                state = "minecraft:deepslate_diamond_ore";
            }
            world.setBlock(transformed.x, transformed.y, transformed.z, state);
        }
    }

    private static int emptyCorners(World world, Template template, Pos placement, int rotation) {
        Pos first = placement.add(transform(new Pos(0, 0, 0), rotation));
        Pos second = placement
            .add(transform(new Pos(template.sizeX - 1, template.sizeY - 1, template.sizeZ - 1), rotation));
        int minX = Math.min(first.x, second.x);
        int minY = Math.min(first.y, second.y);
        int minZ = Math.min(first.z, second.z);
        int maxX = Math.max(first.x, second.x);
        int maxY = Math.max(first.y, second.y);
        int maxZ = Math.max(first.z, second.z);
        int empty = 0;
        for (int x : new int[] { minX, maxX }) {
            for (int y : new int[] { minY, maxY }) {
                for (int z : new int[] { minZ, maxZ }) {
                    String current = ModernDecorationEngine.blockName(world.blockState(x, y, z));
                    if ("minecraft:air".equals(current) || "minecraft:cave_air".equals(current)
                        || "minecraft:void_air".equals(current)
                        || "minecraft:water".equals(current)
                        || "minecraft:lava".equals(current)) empty++;
                }
            }
        }
        return empty;
    }

    private static Pos zeroPosition(Pos corner, Template template, int rotation) {
        if (rotation == 1) return corner.offset(template.sizeZ - 1, 0, 0);
        if (rotation == 2) return corner.offset(template.sizeX - 1, 0, template.sizeZ - 1);
        if (rotation == 3) return corner.offset(0, 0, template.sizeX - 1);
        return corner;
    }

    private static Pos transform(Pos position, int rotation) {
        if (rotation == 1) return new Pos(-position.z, position.y, position.x);
        if (rotation == 2) return new Pos(-position.x, position.y, -position.z);
        if (rotation == 3) return new Pos(position.z, position.y, -position.x);
        return position;
    }

    private static boolean swapsAxes(int rotation) {
        return rotation == 1 || rotation == 3;
    }

    private static String rotateState(String state, int rotation) {
        if (!swapsAxes(rotation)) return state;
        if (state.contains("axis=x")) return ModernFeatureEngine.property(state, "axis", "z");
        if (state.contains("axis=z")) return ModernFeatureEngine.property(state, "axis", "x");
        return state;
    }

    private static Template template(String name) {
        Template result = TEMPLATES.get(name);
        if (result == null) throw new IllegalArgumentException("Missing pinned fossil template " + name);
        return result;
    }

    private static Map<String, Template> loadTemplates() {
        InputStream input = ModernVanillaFossilFeature.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing fossil templates " + RESOURCE);
        try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader)
                .getAsJsonObject();
            if (!"biomic-modern-fossil-templates-v1".equals(
                root.get("format")
                    .getAsString())) {
                throw new IllegalStateException("Fossil template format drift");
            }
            Map<String, Template> result = new HashMap<String, Template>();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("templates")
                .entrySet()) {
                JsonObject encoded = entry.getValue()
                    .getAsJsonObject();
                JsonArray size = encoded.getAsJsonArray("size");
                List<String> palette = new ArrayList<String>();
                for (JsonElement state : encoded.getAsJsonArray("palette")) {
                    palette.add(state.getAsString());
                }
                List<Block> blocks = new ArrayList<Block>();
                for (JsonElement element : encoded.getAsJsonArray("blocks")) {
                    JsonArray block = element.getAsJsonArray();
                    blocks.add(
                        new Block(
                            new Pos(
                                block.get(0)
                                    .getAsInt(),
                                block.get(1)
                                    .getAsInt(),
                                block.get(2)
                                    .getAsInt()),
                            block.get(3)
                                .getAsInt()));
                }
                result.put(
                    entry.getKey(),
                    new Template(
                        size.get(0)
                            .getAsInt(),
                        size.get(1)
                            .getAsInt(),
                        size.get(2)
                            .getAsInt(),
                        palette,
                        blocks));
            }
            return result;
        } catch (Exception failure) {
            throw new IllegalStateException("Invalid fossil templates " + RESOURCE, failure);
        }
    }

    private static final class Template {

        final int sizeX;
        final int sizeY;
        final int sizeZ;
        final List<String> palette;
        final List<Block> blocks;

        Template(int sizeX, int sizeY, int sizeZ, List<String> palette, List<Block> blocks) {
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.palette = palette;
            this.blocks = blocks;
        }
    }

    private static final class Block {

        final Pos position;
        final int palette;

        Block(Pos position, int palette) {
            this.position = position;
            this.palette = palette;
        }
    }
}
