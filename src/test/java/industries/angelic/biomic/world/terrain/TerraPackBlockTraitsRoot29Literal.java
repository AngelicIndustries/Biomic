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
public final class TerraPackBlockTraitsRoot29Literal {

    public record Trait(boolean air, boolean solid, boolean opaqueFullCube, int luminance) {}

    private static final Map<String, Trait> TRAITS = load();
    private static final Map<String, Integer> MATCH_TOKENS = matchingTokens();

    /** Immutable registry token; zero means use the public literal matcher, including unknown/null behavior. */
    public static int matchingToken(String state) {
        if (state == null) return 0;
        return MATCH_TOKENS.getOrDefault(baseId(state), 0);
    }

    private static Map<String, Integer> matchingTokens() {
        var names = new java.util.ArrayList<>(TRAITS.keySet());
        java.util.Collections.sort(names);
        if (names.size() > 65535)
            throw new IllegalStateException("Captured matching registry exceeds compact token range");
        Map<String, Integer> result = new HashMap<>();
        int token = 1;
        for (String name : names) result.put(name, token++);
        return Map.copyOf(result);
    }

    private TerraPackBlockTraitsRoot29Literal() {}

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
        if (expected == actual && expected != null) return true;
        // Preserve the public baseId/null contract, but compare property prefixes without making substrings.
        if (expected == null || actual == null) return baseId(expected).equals(baseId(actual));
        int expectedEnd = expected.indexOf('['), actualEnd = actual.indexOf('[');
        if (expectedEnd < 0) expectedEnd = expected.length();
        if (actualEnd < 0) actualEnd = actual.length();
        if (expectedEnd == "minecraft:grass".length() && expected.startsWith("minecraft:grass")) {
            expected = "minecraft:short_grass";
            expectedEnd = expected.length();
        }
        if (actualEnd == "minecraft:grass".length() && actual.startsWith("minecraft:grass")) {
            actual = "minecraft:short_grass";
            actualEnd = actual.length();
        }
        if (expectedEnd != actualEnd) return false;
        if (expectedEnd == expected.length() && actualEnd == actual.length()) return expected.equals(actual);
        return expected.regionMatches(0, actual, 0, expectedEnd);
    }

    public static String baseId(String state) {
        int bracket = state.indexOf('[');
        String id = bracket < 0 ? state : state.substring(0, bracket);
        // SkyLands retains the pre-1.20.3 short-grass name. Alias is explicit, not a missing-ID fallback.
        return id.equals("minecraft:grass") ? "minecraft:short_grass" : id;
    }

    private static Map<String, Trait> load() {
        try (var stream = TerraPackBlockTraitsRoot29Literal.class
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
