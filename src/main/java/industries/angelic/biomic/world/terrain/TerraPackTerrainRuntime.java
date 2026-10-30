package industries.angelic.biomic.world.terrain;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

/**
 * Isolated execution of imported Terra terrain equations. Not a registered
 * world type: provider, blend, palette and decoration integration are separate gates.
 * Coordinates here are source-space, not silently squeezed legacy coordinates.
 */
public class TerraPackTerrainRuntime {

    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private final String project;
    private final Settings settings;

    static JsonObject readManifest(String resource) {
        try (InputStream input = TerraPackTerrainRuntime.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Missing Terra manifest: " + resource);
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return new JsonParser().parse(reader)
                    .getAsJsonObject();
            }
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read Terra manifest " + resource, error);
        }
    }

    TerraPackTerrainRuntime(JsonObject manifest, String project, String commit) {
        this.project = project;
        if (manifest.get("format")
            .getAsInt() != 1
            || !project.equals(
                manifest.getAsJsonObject("source")
                    .get("project")
                    .getAsString())
            || !commit.equals(
                manifest.getAsJsonObject("source")
                    .get("commit")
                    .getAsString())) {
            throw new IllegalArgumentException("Unsupported " + project + " manifest/version");
        }
        // Reuse Terra's generic interpreter. Origen-specific native optimizations
        // must not override a similarly named but differently authored pack function.
        OrigenTerrainRuntime.Compiler compiler = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"),
            false);
        JsonObject options = manifest.getAsJsonObject("generator_settings");
        settings = new Settings(
            (int) compiler.numeric(options.get("elevation_blend")),
            (int) compiler.numeric(options.get("carver_horizontal_resolution")),
            (int) compiler.numeric(options.get("carver_vertical_resolution")),
            options.get("slant_method")
                .getAsString());
        Map<String, NoiseSampler> graphs = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : manifest.getAsJsonObject("graphs")
            .entrySet()) {
            try {
                graphs.put(
                    entry.getKey(),
                    compiler.compileSampler(
                        entry.getValue()
                            .getAsJsonObject()));
            } catch (RuntimeException error) {
                throw new IllegalStateException(project + " density graph " + entry.getKey(), error);
            }
        }
        for (Map.Entry<String, JsonElement> entry : manifest.getAsJsonObject("profiles")
            .entrySet()) {
            JsonObject data = entry.getValue()
                .getAsJsonObject();
            JsonObject blend = data.getAsJsonObject("blend");
            profiles.put(
                entry.getKey(),
                new Profile(
                    graph(graphs, data, "base"),
                    graph(graphs, data, "elevation"),
                    graph(graphs, data, "carver"),
                    new Blend(
                        (int) compiler.numeric(blend.get("distance")),
                        (int) compiler.numeric(blend.get("step")),
                        compiler.numeric(blend.get("weight")),
                        compiler.numeric(blend.get("weight-2d")))));
        }
    }

    Settings settings() {
        return settings;
    }

    Blend blend(String profile) {
        return require(profile).blend;
    }

    double base(String profile, long seed, double x, double y, double z) {
        return require(profile).base.noise(seed, x, y, z);
    }

    double elevation(String profile, long seed, double x, double z) {
        NoiseSampler sampler = require(profile).elevation;
        return sampler == null ? 0 : sampler.noise(seed, x, z);
    }

    public double carving(String profile, long seed, double x, double y, double z) {
        return require(profile).carver.noise(seed, x, y, z);
    }

    private Profile require(String id) {
        Profile result = profiles.get(id);
        if (result == null) throw new IllegalArgumentException("Unknown or quarantined " + project + " profile: " + id);
        return result;
    }

    private static NoiseSampler graph(Map<String, NoiseSampler> graphs, JsonObject data, String key) {
        if (data.get(key)
            .isJsonNull() && key.equals("elevation")) return null;
        NoiseSampler graph = graphs.get(
            data.get(key)
                .getAsString());
        if (graph == null) throw new IllegalArgumentException("Missing imported Terra graph " + key);
        return graph;
    }

    record Settings(int elevationBlend, int carverHorizontal, int carverVertical, String slantMethod) {

        Settings {
            if (elevationBlend < 0 || elevationBlend > 16
                || carverHorizontal < 1
                || carverHorizontal > 16
                || carverVertical < 1
                || carverVertical > 16
                || 16 % carverHorizontal != 0
                || (!slantMethod.equals("Derivative") && !slantMethod.equals("DotProduct")))
                throw new IllegalArgumentException("Unsupported Terra generator settings");
        }
    }

    record Blend(int distance, int step, double weight, double elevationWeight) {

        Blend {
            if (distance < 0 || distance > 8
                || step < 1
                || step > 16
                || distance * step > 64
                || !Double.isFinite(weight)
                || weight <= 0
                || !Double.isFinite(elevationWeight)
                || elevationWeight <= 0) throw new IllegalArgumentException("Unsupported Terra density blend");
        }
    }

    private record Profile(NoiseSampler base, NoiseSampler elevation, NoiseSampler carver, Blend blend) {}
}
