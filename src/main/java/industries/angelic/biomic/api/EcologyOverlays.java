package industries.angelic.biomic.api;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Data other mods add to Biomic's ecology files (assets/biomic/ecology/, "palettes.json"): an overlay is a JSON file of
 * the same shape, a classpath resource of its own mod (by convention assets/&lt;modid&gt;/biomic/ecology/), registered
 * at
 * preInit. Overlays apply in registration order, key by key:
 * <ul>
 * <li>{@code "key": {...}} merges into the object there (a habitat, a section);</li>
 * <li>{@code "key": [...]} adds the entries the list there does not have yet, at its end;</li>
 * <li>{@code "key": value} sets a number, string or boolean;</li>
 * <li>{@code "=key": anything} replaces what is there;</li>
 * <li>{@code "-key": [...]} takes those entries out of the list there, {@code "-key": true} removes the key.</li>
 * </ul>
 * A plant whose mod is not installed is skipped where it would grow, as in Biomic's own files.
 */
public final class EcologyOverlays {

    private record Overlay(String file, String resource) {}

    private static final List<Overlay> OVERLAYS = new ArrayList<>();
    private static final java.util.Set<String> LOADED = new java.util.HashSet<>();

    private EcologyOverlays() {}

    /** Adds {@code resource} (an absolute classpath path) to Biomic's ecology file {@code file}. */
    public static synchronized void register(String file, String resource) {
        if (file == null || resource == null || !resource.startsWith("/"))
            throw new IllegalArgumentException("An ecology file and an absolute resource path are required");
        if (LOADED.contains(file))
            throw new IllegalStateException(file + " is loaded already; register overlays at preInit");
        OVERLAYS.add(new Overlay(file, resource));
    }

    /** Biomic's own file with every overlay registered for it applied (Biomic loads its files through this). */
    public static synchronized JsonObject load(String file) {
        LOADED.add(file);
        List<String> resources = new ArrayList<>();
        for (Overlay overlay : OVERLAYS) if (overlay.file.equals(file)) resources.add(overlay.resource);
        return compose(file, resources.toArray(new String[0]));
    }

    /** Biomic's own file with the given overlays applied in order, the registered ones aside (tests, tools). */
    public static JsonObject compose(String file, String... overlays) {
        JsonObject root = read("/assets/biomic/ecology/" + file);
        for (String resource : overlays) merge(root, read(resource), resource);
        return root;
    }

    private static JsonObject read(String resource) {
        try (InputStream in = EcologyOverlays.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Missing ecology data " + resource);
            return new JsonParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8))
                .getAsJsonObject();
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Unreadable ecology data " + resource, error);
        }
    }

    /** Applies {@code overlay} onto {@code base} in place (see the class doc). */
    public static void merge(JsonObject base, JsonObject overlay, String source) {
        for (Map.Entry<String, JsonElement> entry : overlay.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (key.startsWith("=")) {
                base.add(key.substring(1), value);
            } else if (key.startsWith("-")) {
                String target = key.substring(1);
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive()
                    .isBoolean()) {
                    if (value.getAsBoolean()) base.remove(target);
                } else if (value.isJsonArray() && base.has(target)
                    && base.get(target)
                        .isJsonArray()) {
                            JsonArray kept = new JsonArray();
                            for (JsonElement item : base.getAsJsonArray(target))
                                if (!contains(value.getAsJsonArray(), item)) kept.add(item);
                            base.add(target, kept);
                        } else
                    if (!value.isJsonArray()) {
                        throw new IllegalStateException(source + ": \"" + key + "\" takes a list or true");
                    }
            } else if (value.isJsonObject()) {
                // A new object is merged too, so the keys inside it work as anywhere else.
                if (!base.has(key) || !base.get(key)
                    .isJsonObject()) base.add(key, new JsonObject());
                merge(base.getAsJsonObject(key), value.getAsJsonObject(), source);
            } else if (value.isJsonArray() && base.has(key)
                && base.get(key)
                    .isJsonArray()) {
                        JsonArray list = base.getAsJsonArray(key);
                        for (JsonElement item : value.getAsJsonArray()) if (!contains(list, item)) list.add(item);
                    } else {
                        base.add(key, value);
                    }
        }
    }

    private static boolean contains(JsonArray list, JsonElement item) {
        for (JsonElement present : list) if (present.equals(item)) return true;
        return false;
    }
}
