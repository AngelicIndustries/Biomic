package industries.angelic.biomic.compat.flora;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import com.google.gson.JsonObject;

import industries.angelic.biomic.compat.food.PamFood;

/** Pinned, soft adapter. No HarvestCraft class is referenced in a common-side type signature. */
public final class HarvestCraftOrchards {

    public static final String HABITAT = "regions_unexplored:orchard";
    private static WorldGenerator appleTree;
    private static Block apple;
    private static boolean discovered;
    private static String status = "not inspected";

    private HarvestCraftOrchards() {}

    public static boolean available() {
        discover();
        return appleTree != null && NativeFeatureCapture.installed();
    }

    private static synchronized void discover() {
        if (discovered) return;
        discovered = true;
        if (!PamFood.installed()) {
            status = "HarvestCraft not installed";
            return;
        }
        apple = PamFood.block(PamFood.MOD, "pamApple");
        appleTree = apple == null ? null : PamFood.fruitTree("apple");
        status = appleTree == null ? "HarvestCraft has no apple tree"
            : "HarvestCraft " + PamFood.SUPPORTED
                + ", native apple generator; captureHooks="
                + NativeFeatureCapture.installed();
    }

    /** Match the authored tree intent, not every oak/plant in an orchard. */
    public static boolean replaces(String habitat, String type, JsonObject config) {
        if (!HABITAT.equals(habitat) || !"minecraft:tree".equals(type)) return false;
        if (!config.has("foliage_provider")) return false;
        return containsAppleState(config.get("foliage_provider"));
    }

    private static boolean containsAppleState(com.google.gson.JsonElement element) {
        if (element.isJsonArray()) {
            for (var child : element.getAsJsonArray()) if (containsAppleState(child)) return true;
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("Name") && object.get("Name")
                .isJsonPrimitive()
                && "regions_unexplored:apple_oak_leaves".equals(
                    object.get("Name")
                        .getAsString()))
                return true;
            for (var entry : object.entrySet()) if (containsAppleState(entry.getValue())) return true;
        }
        return false;
    }

    /** Normal oak without decorative fruit; keep the authored trunk/foliage SHAPE, not the fake apple palette. */
    public static JsonObject oakFallback(JsonObject config) {
        JsonObject copy = new com.google.gson.JsonParser().parse(config.toString())
            .getAsJsonObject();
        copy.add(
            "foliage_provider",
            new com.google.gson.JsonParser().parse(
                "{\"type\":\"minecraft:simple_state_provider\","
                    + "\"state\":{\"Name\":\"minecraft:oak_leaves\",\"Properties\":{\"persistent\":\"false\",\"distance\":\"7\"}}}"));
        return copy;
    }

    public static boolean place(World world, NativeFeatureCapture.Access access, Random random, int x, int y, int z) {
        if (!available()) throw new IllegalStateException("Frozen HarvestCraft orchard unavailable: " + status);
        return NativeFloraHeightFrame
            .run(world, access, y, sourceY -> appleTree.generate(world, random, x, sourceY, z));
    }

    public static String encodeWrite(Block block, int metadata) {
        if (block == apple && apple != null)
            return industries.angelic.biomic.compat.biome.NativeBlockCatalogue.encode(block, metadata);
        // Audited native apple generator writes precisely dirt, vanilla oak logs/leaves, and fruit.
        if (block == Blocks.dirt && metadata == 0) return "minecraft:dirt";
        if (block == Blocks.log && metadata == 0) return "minecraft:oak_log[axis=y]";
        if (block == Blocks.leaves && metadata == 0) return "minecraft:oak_leaves[persistent=false,distance=1]";
        throw new IllegalStateException(
            "Unexpected native apple-tree write: " + Block.blockRegistry.getNameForObject(block) + ':' + metadata);
    }
}
