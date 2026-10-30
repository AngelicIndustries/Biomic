package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

/**
 * Origen's skins after Biomic's overrides (terrain study): no visible layer of a used palette picks its block by white
 * or gaussian noise, and every material of the hand-made palettes actually shows.
 */
public class OrigenSkinsTest {

    private static final Set<String> GRAINY = Set.of("WHITE_NOISE", "POSITIVE_WHITE_NOISE", "GAUSSIAN");
    private static final Set<String> STRUCTURAL = Set.of("minecraft:air", "minecraft:water", "minecraft:lava");
    private static final int VISIBLE = 4;

    private final JsonObject manifest = OrigenManifest.read();

    private Set<String> used() {
        Set<String> used = new TreeSet<>();
        for (var profile : manifest.getAsJsonObject("profiles")
            .entrySet()) {
            JsonObject surface = profile.getValue()
                .getAsJsonObject()
                .getAsJsonObject("surface");
            for (JsonElement band : surface.getAsJsonArray("palette")) used.add(name(band));
            for (JsonElement slant : surface.getAsJsonArray("slant")) for (JsonElement band : slant.getAsJsonObject()
                .getAsJsonArray("palette")) used.add(name(band));
        }
        used.removeIf(name -> name.startsWith("BLOCK:") || name.equals("BEDROCK"));
        return used;
    }

    private static String name(JsonElement band) {
        return band.getAsJsonObject()
            .get("palette")
            .getAsString();
    }

    @Test
    public void visibleLayersOfUsedPalettesAreNotGrainy() {
        List<String> grainy = new ArrayList<>();
        for (String name : used()) {
            JsonObject palette = manifest.getAsJsonObject("palettes")
                .getAsJsonObject(name);
            // Palettes with air, water or lava carve rivers and pools; their noise shapes the bed, not a skin.
            if (STRUCTURAL.stream()
                .anyMatch(palette.toString()::contains)) continue;
            int depth = 0;
            for (JsonElement value : palette.getAsJsonArray("layers")) {
                JsonObject layer = value.getAsJsonObject();
                Set<String> materials = new TreeSet<>();
                for (JsonElement m : layer.getAsJsonArray("materials")) materials.add(
                    m.getAsJsonObject()
                        .get("material")
                        .getAsString());
                JsonElement sampler = layer.has("sampler") ? layer.get("sampler") : palette.get("sampler");
                if (depth < VISIBLE && materials.size() > 1 && grainy(sampler)) grainy.add(name + " at depth " + depth);
                depth += layer.get("layers")
                    .getAsInt();
            }
        }
        assertEquals(List.of(), grainy);
    }

    @Test
    public void handMadeMaterialsShow() {
        OrigenTerrainRuntime.Compiler compiler = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"));
        JsonObject overrides = TerraPackTerrainRuntime.readManifest(OrigenManifest.OVERRIDES);
        StringBuilder rare = new StringBuilder();
        for (String name : List
            .of("MOSS_CAPS", "STONE_STRATA", "ICE_STONE_MIX_DARK", "ICE_STONE_MIX", "CALCITE_DIORITE_MIX")) {
            JsonObject palette = overrides.getAsJsonObject("palettes")
                .getAsJsonObject(name);
            JsonObject layer = palette.getAsJsonArray("layers")
                .get(0)
                .getAsJsonObject();
            NoiseSampler sampler = compiler.compileSampler(
                layer.has("sampler") ? layer.getAsJsonObject("sampler") : palette.getAsJsonObject("sampler"));
            List<String> slots = new ArrayList<>();
            for (JsonElement m : layer.getAsJsonArray("materials")) for (int i = 0; i < m.getAsJsonObject()
                .get("weight")
                .getAsInt(); i++)
                slots.add(
                    m.getAsJsonObject()
                        .get("material")
                        .getAsString());
            Map<String, Integer> counts = new TreeMap<>();
            int samples = 0;
            for (int x = -1024; x < 1024; x += 4) for (int y = 60; y < 200; y += 20) {
                int z = x * 7 % 2048;
                double noise = sampler.noise(42L, x, y, z);
                int index = Math.max(0, Math.min(slots.size() - 1, (int) Math.floor((noise + 1) / 2 * slots.size())));
                counts.merge(slots.get(index), 1, Integer::sum);
                samples++;
            }
            StringBuilder shares = new StringBuilder(name + ":");
            for (var count : counts.entrySet()) shares.append(
                String.format(
                    " %s %.1f%%",
                    count.getKey()
                        .replace("minecraft:", ""),
                    100.0 * count.getValue() / samples));
            System.out.println(shares);
            for (String material : new TreeSet<>(slots))
                if (counts.getOrDefault(material, 0) < samples / 100) rare.append('\n')
                    .append(shares)
                    .append(": ")
                    .append(material)
                    .append(" is too rare to list");
        }
        assertEquals("", rare.toString());
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
