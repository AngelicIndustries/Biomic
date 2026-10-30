// Literal frozen ROOT11 implementation; renamed only for differential proof.
package industries.angelic.biomic.world.terrain;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonParser;

/**
 * Real 1.21.10 registry defaults, used with pinned Terra Bukkit block-type semantics.
 * BukkitBlockTypeAndItem.isSolid delegates to Material.isOccluding, which CraftBlockType
 * evaluates as defaultState.isRedstoneConductor(EmptyBlockGetter, ZERO), not collision/full-cube.
 * State properties intentionally do not participate in Terra's Bukkit matches/BlockType predicates.
 * This is an explicitly selected platform contract, not a claim that every Terra platform is identical.
 */
public final class TerraBlockTraitsRoot11Literal {

    public record Trait(boolean air, boolean solid, boolean opaqueFullCube, int luminance) {}

    private static final Map<String, Trait> TRAITS = load();

    private TerraBlockTraitsRoot11Literal() {}

    public static Trait get(String state) {
        String id = baseId(state);
        Trait trait = TRAITS.get(id);
        if (trait == null) throw new IllegalArgumentException("No captured modern block traits for " + id);
        return trait;
    }

    public static boolean air(String state) {
        return get(state).air();
    }

    public static boolean solid(String state) {
        return get(state).solid();
    }

    public static boolean matches(String expected, String actual) {
        return baseId(expected).equals(baseId(actual));
    }

    public static String baseId(String state) {
        int bracket = state.indexOf('[');
        String id = bracket < 0 ? state : state.substring(0, bracket);
        // SkyLands retains the pre-1.20.3 short-grass name. Alias is explicit, not a missing-ID fallback.
        return id.equals("minecraft:grass") ? "minecraft:short_grass" : id;
    }

    private static Map<String, Trait> load() {
        try (var stream = TerraBlockTraitsRoot11Literal.class
            .getResourceAsStream("/assets/biomic/terra_reference/vanilla-block-traits-1.21.10.json")) {
            if (stream == null) throw new IllegalStateException("Missing captured vanilla traits");
            var root = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
            if (!root.get("minecraft_version")
                .getAsString()
                .equals("1.21.10")) throw new IllegalStateException("Registry version drift");
            Map<String, Trait> result = new HashMap<>();
            for (var entry : root.getAsJsonObject("blocks")
                .entrySet()) {
                var t = entry.getValue()
                    .getAsJsonObject();
                result.put(
                    entry.getKey(),
                    new Trait(
                        t.get("air")
                            .getAsBoolean(),
                        t.get("redstone_conductor")
                            .getAsBoolean(),
                        t.get("opaque_full_cube")
                            .getAsBoolean(),
                        t.get("luminance")
                            .getAsInt()));
            }
            return Map.copyOf(result);
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot load captured vanilla traits", failure);
        }
    }
}
