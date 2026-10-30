package industries.angelic.biomic.world.modern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.world.modern.noise.ModernPerlinSimplexNoise;

/** The pinned 1.21.1 vanilla + RU surface graph: its resource, and the biome climates it carries. */
public final class ModernSurfaceSystem {

    public static final String RESOURCE = "/assets/biomic/ru_modern_parity/resolved-modern-surface.json";
    private static final String SHA256 = "a47bdcaf55d823c5dabb152019ade5ddb4b73a2e639de2bbc64315cecffc4877";

    private static final String AIR = "minecraft:air";
    private static final ModernPerlinSimplexNoise TEMPERATURE_NOISE = new ModernPerlinSimplexNoise(1234L, 0);
    private static final ModernPerlinSimplexNoise FROZEN_TEMPERATURE_NOISE = new ModernPerlinSimplexNoise(
        3456L,
        -2,
        -1,
        0);
    private static final ModernPerlinSimplexNoise BIOME_INFO_NOISE = new ModernPerlinSimplexNoise(2345L, 0);
    private static final Map<String, BiomeClimate> CLIMATES = climates(load().getAsJsonObject("biome_climate"));

    private ModernSurfaceSystem() {}

    /** Exact pinned Biome.coldEnoughToSnow predicate in uncompressed modern coordinates. */
    public static boolean coldEnoughToSnow(String biome, int x, int y, int z) {
        return temperature(biome, x, y, z) < 0.15F;
    }

    private static float temperature(String biome, int x, int y, int z) {
        BiomeClimate climate = CLIMATES.get(biome);
        if (climate == null) return Float.POSITIVE_INFINITY;
        float temperature = climate.temperature;
        if ("frozen".equals(climate.modifier)) {
            double frozen = FROZEN_TEMPERATURE_NOISE.value(x * 0.05D, z * 0.05D) * 7.0D;
            double detail = BIOME_INFO_NOISE.value(x * 0.2D, z * 0.2D);
            if (frozen + detail < 0.3D && BIOME_INFO_NOISE.value(x * 0.09D, z * 0.09D) < 0.8D) {
                temperature = 0.2F;
            }
        }
        if (y > 80) {
            float variation = (float) (TEMPERATURE_NOISE.value(x / 8.0F, z / 8.0F) * 8.0D);
            temperature -= (variation + y - 80.0F) * 0.05F / 40.0F;
        }
        return temperature;
    }

    public static boolean isAirState(String state) {
        return AIR.equals(state) || "minecraft:cave_air".equals(state) || "minecraft:void_air".equals(state);
    }

    private static Map<String, BiomeClimate> climates(JsonObject source) {
        Map<String, BiomeClimate> result = new HashMap<String, BiomeClimate>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            JsonObject value = entry.getValue()
                .getAsJsonObject();
            result.put(
                entry.getKey(),
                new BiomeClimate(
                    value.get("temperature")
                        .getAsFloat(),
                    string(value, "temperature_modifier")));
        }
        return result;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null ? null : value.getAsString();
    }

    private static JsonObject load() {
        InputStream input = ModernSurfaceSystem.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing resolved modern surface graph " + RESOURCE);
        try {
            byte[] payload = readFully(input);
            if (!SHA256.equals(sha256(payload))) {
                throw new IllegalArgumentException("resolved modern surface graph hash drift");
            }
            JsonObject root = new JsonParser()
                .parse(new InputStreamReader(new ByteArrayInputStream(payload), StandardCharsets.UTF_8))
                .getAsJsonObject();
            if (!"biomic-modern-surface-graph-v1".equals(string(root, "format"))) {
                throw new IllegalArgumentException("resolved modern surface format drift");
            }
            if (!RuModernParitySpec.SOURCE_REVISION.equals(string(root, "revision"))) {
                throw new IllegalArgumentException("resolved modern surface source drift");
            }
            return root;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read resolved modern surface graph", failure);
        }
    }

    private static byte[] readFully(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) >= 0) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(payload);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value & 0xFF));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("Every Java runtime must provide SHA-256", impossible);
        }
    }

    private static final class BiomeClimate {

        private final float temperature;
        private final String modifier;

        private BiomeClimate(float temperature, String modifier) {
            this.temperature = temperature;
            this.modifier = modifier;
        }
    }
}
