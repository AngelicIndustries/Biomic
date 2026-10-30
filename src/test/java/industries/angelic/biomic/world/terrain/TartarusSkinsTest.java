package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The Nether's skins (terrain-study rules): no per-block randomness or dithered cells on a visible layer, at most three
 * materials a layer, one base rock under every ground, and every listed material actually shows.
 */
public class TartarusSkinsTest {

    private static final Set<String> GRAINY = Set.of("WHITE_NOISE", "POSITIVE_WHITE_NOISE", "GAUSSIAN", "CELLULAR");
    /** Layers this deep or shallower are seen on floors, ceilings and cut edges. */
    private static final int VISIBLE = 4;

    private final JsonObject manifest = TartarusManifest.load(List.of());
    private final TerraPackPaletteRuntime runtime = TerraPackPaletteRuntime.tartarus(manifest);

    private Set<String> skins() {
        Set<String> skins = new HashSet<>();
        for (var surface : manifest.getAsJsonObject("surfaces")
            .entrySet()) {
            JsonObject raw = surface.getValue()
                .getAsJsonObject();
            for (JsonElement band : raw.getAsJsonArray("palette")) {
                String palette = band.getAsJsonObject()
                    .get("palette")
                    .getAsString();
                if (palette.endsWith("_GROUND")) skins.add(palette);
            }
        }
        return skins;
    }

    /** Soul Valley's dried ghasts are gone: no structure, no block id, and the rib cages no longer call it. */
    @Test
    public void noDriedGhasts() {
        JsonObject structures = manifest.getAsJsonObject("decoration")
            .getAsJsonObject("structures");
        assertFalse(structures.has("dried_ghast"));
        for (JsonElement id : manifest.getAsJsonArray("authored_block_ids")) assertFalse(
            id.getAsString(),
            id.getAsString()
                .contains("dried_ghast"));
        String ribCage = structures.getAsJsonObject("fossil_rib_cage_procedural")
            .get("script")
            .getAsString();
        assertFalse(ribCage.contains("\"dried_ghast\""));
        assertTrue(ribCage.contains("bone_block"));
    }

    @Test
    public void visibleLayersUseSmoothPatchesAndFewMaterials() {
        for (String name : skins()) {
            JsonObject palette = manifest.getAsJsonObject("palettes")
                .getAsJsonObject(name);
            int depth = 0;
            for (JsonElement value : palette.getAsJsonArray("layers")) {
                JsonObject layer = value.getAsJsonObject();
                if (depth < VISIBLE) {
                    JsonElement sampler = layer.has("sampler") ? layer.get("sampler") : palette.get("sampler");
                    assertFalse(name + " layer at depth " + depth + " is grainy", grainy(sampler));
                    Set<String> materials = new HashSet<>();
                    for (JsonElement m : layer.getAsJsonArray("materials")) materials.add(
                        m.getAsJsonObject()
                            .get("material")
                            .getAsString());
                    assertTrue(name + " layer at depth " + depth + " mixes " + materials, materials.size() <= 3);
                }
                depth += layer.get("layers")
                    .getAsInt();
            }
        }
    }

    @Test
    public void everyGroundEndsInOneBaseRock() {
        for (String name : skins()) {
            if (!name.endsWith("_GROUND") || name.equals("SOUL_GROUND")) continue;
            var layers = manifest.getAsJsonObject("palettes")
                .getAsJsonObject(name)
                .getAsJsonArray("layers");
            var last = layers.get(layers.size() - 1)
                .getAsJsonObject()
                .getAsJsonArray("materials");
            assertEquals(name + " has a mixed body", 1, last.size());
        }
    }

    @Test
    public void noWartLattices() {
        JsonObject schedules = manifest.getAsJsonObject("decoration")
            .getAsJsonObject("feature_schedules");
        for (var biome : schedules.entrySet()) for (var stage : biome.getValue()
            .getAsJsonObject()
            .entrySet())
            for (JsonElement feature : stage.getValue()
                .getAsJsonArray())
                assertFalse(
                    biome.getKey() + " still grows " + feature,
                    feature.getAsString()
                        .equals("TENTACLES")
                        || feature.getAsString()
                            .equals("TENDONS"));
    }

    @Test
    public void hugeFungiLeaveRoomToWalk() {
        JsonObject structures = manifest.getAsJsonObject("decoration")
            .getAsJsonObject("structures");
        for (String fungus : List.of("huge_crimson_fungus", "huge_warped_fungus")) {
            String script = structures.getAsJsonObject(fungus)
                .get("script")
                .getAsString();
            assertTrue(fungus, script.contains("num sideBranchMinHeight = 7+randomInt(3);"));
            assertTrue(fungus, script.contains("num sideBranchLeafDensity = 0.75;"));
        }
    }

    private static boolean grainy(JsonElement node) {
        if (node == null) return false;
        if (node.isJsonArray()) {
            for (JsonElement child : node.getAsJsonArray()) if (grainy(child)) return true;
            return false;
        }
        if (!node.isJsonObject()) return false;
        JsonObject object = node.getAsJsonObject();
        if (object.has("type") && object.get("type")
            .isJsonPrimitive()
            && GRAINY.contains(
                object.get("type")
                    .getAsString()))
            return true;
        for (var entry : object.entrySet()) if (grainy(entry.getValue())) return true;
        return false;
    }
}
