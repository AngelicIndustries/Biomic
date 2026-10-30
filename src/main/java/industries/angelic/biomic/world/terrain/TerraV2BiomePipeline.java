/*
 * Pipeline array/stage algorithms adapted from Terra
 * 3745c3e947c8b1f955e9c934f12ea402ef29907f, biome-provider-pipeline-v2.
 * Copyright (c) 2020-2023 Polyhedral Development. MIT; see THIRD_PARTY_NOTICES.md.
 */
package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/**
 * Bounded, source-coordinate execution of Terra v2 biome selection. No World access,
 * height projection, fallback biomes or generation registration. Legacy pipelines
 * are deliberately rejected, not interpreted using subtly different v2 rules.
 */
public final class TerraV2BiomePipeline {

    private final long seed;
    private final int resolution, expansions, size, origin, chunkSize;
    private final double amplitude;
    private final NoiseSampler warp, sourceNoise;
    private final List<String> source;
    private final List<Stage> stages = new ArrayList<>();
    private final List<Extrusion> extrusions = new ArrayList<>();
    private final Map<String, Set<String>> tags = new HashMap<>();
    private final Map<String, Integer> extrusionBiomeIds;
    private final String[] extrusionBiomeNames;
    private final CompiledExtrusion[] compiledExtrusions;
    private final Set<String> renderable = new HashSet<>();
    private final OrigenTerrainRuntime.Compiler compiler;
    // Primitive hashing mixes both coordinate halves; Long.hashCode made x^z collision lines expensive.
    // Keep the exact sixteen-entry access-order LRU and the existing synchronization boundary.
    private final Long2ObjectLinkedOpenHashMap<BiomeGrid> cache = new Long2ObjectLinkedOpenHashMap<>(16);

    public static TerraV2BiomePipeline tartarus(JsonObject manifest, long seed) {
        return new TerraV2BiomePipeline(manifest, seed);
    }

    TerraV2BiomePipeline(JsonObject manifest, long seed) {
        if (!manifest.has("pipeline_dialect") || !"v2".equals(
            manifest.get("pipeline_dialect")
                .getAsString()))
            throw new IllegalArgumentException("Only the audited Terra v2 pipeline is supported");
        this.seed = seed;
        compiler = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"),
            false);
        for (var entry : manifest.getAsJsonObject("biome_tags")
            .entrySet()) {
            Set<String> values = new HashSet<>();
            for (var value : entry.getValue()
                .getAsJsonArray()) values.add(value.getAsString());
            tags.put(entry.getKey(), Set.copyOf(values));
        }
        for (var entry : manifest.getAsJsonObject("surfaces")
            .entrySet()) renderable.add(entry.getKey());
        JsonObject provider = manifest.getAsJsonObject("source_biome_provider");
        if ("EXTRUSION".equals(type(provider))) {
            for (var value : provider.getAsJsonArray("extrusions")) {
                var data = value.getAsJsonObject();
                if (!"REPLACE".equals(type(data)))
                    throw new IllegalArgumentException("Unsupported Terra extrusion " + type(data));
                var range = data.getAsJsonObject("range");
                int min = number(range, "min", 0), max = number(range, "max", 0);
                if (min >= max) throw new IllegalArgumentException("Invalid Terra extrusion range");
                extrusions.add(
                    new Extrusion(
                        data.get("from")
                            .getAsString(),
                        table(data.get("to")),
                        noise(data),
                        min,
                        max));
            }
            provider = provider.getAsJsonObject("provider");
        }
        if (!"PIPELINE".equals(type(provider)))
            throw new IllegalArgumentException("Unsupported Terra provider " + type(provider));
        resolution = number(provider, "resolution", 1);
        if (resolution < 1 || resolution > 64) throw new IllegalArgumentException("Unbounded Terra resolution");
        JsonObject blend = provider.has("blend") ? provider.getAsJsonObject("blend") : new JsonObject();
        amplitude = blend.has("amplitude") ? compiler.numeric(blend.get("amplitude")) : 0;
        if (!Double.isFinite(amplitude) || Math.abs(amplitude) > 256)
            throw new IllegalArgumentException("Unbounded Terra blend");
        warp = noise(blend);
        JsonObject pipeline = provider.getAsJsonObject("pipeline"), start = pipeline.getAsJsonObject("source");
        if (!"SAMPLER".equals(type(start)))
            throw new IllegalArgumentException("Unsupported Terra source " + type(start));
        source = table(start.get("biomes"));
        sourceNoise = noise(start);
        for (var value : pipeline.getAsJsonArray("stages")) stages.add(stage(value.getAsJsonObject()));
        if (stages.size() > 256) throw new IllegalArgumentException("Unbounded Terra stage count");
        expansions = (int) stages.stream()
            .filter(s -> s.type.equals("FRACTAL_EXPAND"))
            .count();
        if (expansions > 8) throw new IllegalArgumentException("Unbounded Terra expansion count");
        int interval = 1 << expansions, rawOrigin = 0;
        for (Stage stage : stages) {
            if (stage.type.equals("FRACTAL_EXPAND")) interval /= 2;
            if (stage.type.equals("SMOOTH")) rawOrigin += interval;
        }
        int initialInterval = 1 << expansions;
        origin = ((rawOrigin + initialInterval - 1) / initialInterval) * initialInterval;
        int array = 1, chunk = 0;
        // Source BiomePipelineTemplate uses ideal array size 128. Its unbound
        // 'initial-size' YAML property does not override that value.
        for (int initial = 1; initial <= 1024; initial++) {
            array = (initial - 1) * initialInterval + 1;
            chunk = array - 2 * origin - (expansions > 0 ? 1 : 0);
            if (chunk > 1 && array >= 128) break;
        }
        if (chunk <= 1 || array > 1024) throw new IllegalArgumentException("Unbounded Terra working array");
        size = array;
        chunkSize = chunk;
        // Only the finite extrusion lookup is compiled: source pipeline, samplers and winner order stay literal.
        var names = new java.util.TreeSet<String>(tags.keySet());
        for (Extrusion extrusion : extrusions)
            for (String name : extrusion.to) if (!"SELF".equals(name)) names.add(name);
        extrusionBiomeNames = names.toArray(new String[0]);
        var ids = new HashMap<String, Integer>();
        for (int i = 0; i < extrusionBiomeNames.length; i++) ids.put(extrusionBiomeNames[i], i);
        extrusionBiomeIds = Map.copyOf(ids);
        compiledExtrusions = new CompiledExtrusion[extrusions.size()];
        for (int i = 0; i < compiledExtrusions.length; i++) {
            Extrusion extrusion = extrusions.get(i);
            boolean[] matches = new boolean[extrusionBiomeNames.length];
            for (int id = 0; id < matches.length; id++) matches[id] = tagged(extrusionBiomeNames[id], extrusion.from);
            int[] targets = new int[extrusion.to.size()];
            for (int j = 0; j < targets.length; j++) {
                String name = extrusion.to.get(j);
                targets[j] = "SELF".equals(name) ? -1 : ids.get(name);
            }
            compiledExtrusions[i] = new CompiledExtrusion(
                matches,
                targets,
                extrusion.to.toArray(new String[0]),
                extrusion.noise,
                extrusion.min,
                extrusion.max);
        }
    }

    /** Pack only primitive warped/truncated coordinates; query state is never retained or allocated. */
    private long sourceCoordinates(int x, int z) {
        x = (int) (x + warp.noise(seed + 1, x, z) * amplitude);
        z = (int) (z + warp.noise(seed + 2, x, z) * amplitude);
        // Preserve upstream Java truncation towards zero before floor-dividing chunks.
        x /= resolution;
        z /= resolution;
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private BiomeGrid sourceGrid(int x, int z) {
        int cx = Math.floorDiv(x, chunkSize) * chunkSize, cz = Math.floorDiv(z, chunkSize) * chunkSize;
        long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
        BiomeGrid biomes;
        synchronized (cache) {
            biomes = cache.getAndMoveToLast(key);
        }
        if (biomes == null) {
            String[] names = generate(cx, cz);
            int[] ids = new int[names.length];
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                // Unknown cells retain the original deferred failure: only querying that cell throws.
                ids[i] = name == null || !tags.containsKey(name) ? -1 : extrusionBiomeIds.get(name);
            }
            biomes = new BiomeGrid(cx, cz, names, ids);
            synchronized (cache) {
                cache.putAndMoveToLast(key, biomes);
                if (cache.size() > 16) cache.removeFirst();
            }
        }
        return biomes;
    }

    private int sourceIndex(BiomeGrid grid, int x, int z) {
        return (x - grid.originX) * chunkSize + z - grid.originZ;
    }

    private static String sourceName(BiomeGrid grid, int index) {
        String name = grid.names[index];
        if (grid.ids[index] < 0)
            throw new IllegalStateException("Terra pipeline leaked placeholder/missing biome: " + name);
        return name;
    }

    public String surface(int x, int z) {
        long coordinates = sourceCoordinates(x, z);
        int sx = (int) (coordinates >> 32), sz = (int) coordinates;
        BiomeGrid grid = sourceGrid(sx, sz);
        return sourceName(grid, sourceIndex(grid, sx, sz));
    }

    public String biome(int x, int y, int z) {
        long coordinates = sourceCoordinates(x, z);
        int sx = (int) (coordinates >> 32), sz = (int) coordinates;
        BiomeGrid grid = sourceGrid(sx, sz);
        int index = sourceIndex(grid, sx, sz);
        return extruded(sourceName(grid, index), grid.ids[index], x, y, z);
    }

    /**
     * Explicit pack-native, caller-confined cursor. Compiled pack samplers depend only on seed and
     * coordinates, so successive Y queries can share horizontal resolution without retaining a grid.
     * Generic scalar queries keep their original callback path and no cursor is shared by workers.
     */
    final class ColumnSampler {

        private boolean bound;
        private int x, z, sourceId;
        private String sourceName;

        private void bind(int x, int z) {
            if (bound && this.x == x && this.z == z) return;
            long coordinates = sourceCoordinates(x, z);
            int sx = (int) (coordinates >> 32), sz = (int) coordinates;
            BiomeGrid grid = sourceGrid(sx, sz);
            int index = sourceIndex(grid, sx, sz);
            String name = TerraV2BiomePipeline.sourceName(grid, index);
            this.x = x;
            this.z = z;
            sourceName = name;
            sourceId = grid.ids[index];
            bound = true;
        }

        String at(int x, int y, int z) {
            bind(x, z);
            return extruded(sourceName, sourceId, x, y, z);
        }

        String surface(int x, int z) {
            bind(x, z);
            return sourceName;
        }
    }

    ColumnSampler nativeColumnSampler() {
        return new ColumnSampler();
    }

    private String extruded(String surface, int result, int x, int y, int z) {
        if (compiledExtrusions.length == 0) return surface;
        String resultName = surface;
        for (CompiledExtrusion extrusion : compiledExtrusions) {
            if (y >= extrusion.min && y < extrusion.max && extrusion.matches[result]) {
                double sample = extrusion.noise.noise(seed, x, y, z);
                if (!Double.isFinite(sample)) throw new IllegalStateException("Non-finite Terra biome sample");
                int chosenIndex = Math.max(
                    0,
                    Math.min(
                        extrusion.targets.length - 1,
                        (int) Math.floor((sample + 1) / 2 * extrusion.targets.length)));
                int selected = extrusion.targets[chosenIndex];
                if (selected >= 0) {
                    result = selected;
                    resultName = extrusion.names[chosenIndex];
                }
                // SELF retains the current result, including earlier extrusion winners and String identity.
            }
        }
        return resultName;
    }

    private String[] generate(int cx, int cz) {
        String[] current = new String[size * size], lookup = new String[size * size];
        int interval = 1 << expansions, grid = size / interval + (expansions > 0 ? 1 : 0), start = 0;
        for (int x = 0; x < grid; x++) for (int z = 0; z < grid; z++) {
            int ax = x * interval, az = z * interval;
            current[ax * size + az] = pick(
                source,
                sourceNoise.noise(seed, (cx + ax - origin) * resolution, (cz + az - origin) * resolution));
        }
        for (Stage stage : stages) {
            if (stage.type.equals("FRACTAL_EXPAND")) {
                interval /= 2;
                grid = grid * 2 - 1;
            }
            if (stage.type.equals("SMOOTH")) {
                grid -= 2;
                start += interval;
            }
            String[] swap = current;
            current = lookup;
            lookup = swap;
            for (int gz = 0; gz < grid; gz++) for (int gx = 0; gx < grid; gx++) {
                int ax = start + gx * interval, az = start + gz * interval, index = ax * size + az;
                String center = lookup[index];
                int wx = (cx + ax - origin) * resolution, wz = (cz + az - origin) * resolution;
                String result;
                switch (stage.type) {
                    case "REPLACE" -> result = tagged(center, stage.from)
                        ? replace(center, stage.to, stage.noise.noise(seed, wx, wz))
                        : center;
                    case "REPLACE_LIST" -> {
                        List<String> table = stage.replacements.get(center);
                        if (table == null && tagged(center, stage.from)) table = stage.to;
                        result = table == null ? center : replace(center, table, stage.noise.noise(seed, wx, wz));
                    }
                    case "FRACTAL_EXPAND" -> {
                        if (center != null) {
                            result = center;
                            break;
                        }
                        double roll = stage.noise.noise(seed, wx, wz);
                        int dx, dz;
                        if (gx % 2 == 1 && gz % 2 == 0) {
                            dx = roll > 0 ? -1 : 1;
                            dz = 0;
                        } else if (gx % 2 == 0 && gz % 2 == 1) {
                            dx = 0;
                            dz = roll > 0 ? -1 : 1;
                        } else {
                            dx = roll > 0 ? (roll > .25 ? -1 : 1) : (roll > -.25 ? -1 : 1);
                            dz = roll > 0 ? 1 : -1;
                        }
                        result = lookup[(ax + dx * interval) * size + az + dz * interval];
                    }
                    case "SMOOTH" -> {
                        String top = lookup[(ax + interval) * size + az], bottom = lookup[(ax - interval) * size + az];
                        String left = lookup[ax * size + az + interval], right = lookup[ax * size + az - interval];
                        boolean vert = java.util.Objects.equals(top, bottom),
                            horiz = java.util.Objects.equals(left, right);
                        double roll = stage.noise.noise(seed, wx, wz);
                        result = vert && horiz ? (roll > 0 ? (roll > .25 ? left : right) : (roll > -.25 ? top : bottom))
                            : vert ? (roll > 0 ? top : bottom) : horiz ? (roll > 0 ? left : right) : center;
                    }
                    default -> throw new IllegalStateException(stage.type);
                }
                current[index] = result;
            }
        }
        String[] result = new String[chunkSize * chunkSize];
        for (int x = 0; x < chunkSize; x++)
            System.arraycopy(current, (x + origin) * size + origin, result, x * chunkSize, chunkSize);
        return result;
    }

    private boolean tagged(String id, String tag) {
        if (id == null) return false;
        Set<String> defined = tags.get(id);
        return defined == null ? ("ALL".equals(tag) || id.equals(tag)) : defined.contains(tag);
    }

    private static String replace(String center, List<String> values, double roll) {
        String result = pick(values, roll);
        return "SELF".equals(result) ? center : result;
    }

    private static String pick(List<String> values, double sample) {
        if (!Double.isFinite(sample)) throw new IllegalStateException("Non-finite Terra biome sample");
        return values.get(Math.max(0, Math.min(values.size() - 1, (int) Math.floor((sample + 1) / 2 * values.size()))));
    }

    private Stage stage(JsonObject raw) {
        String type = type(raw);
        if (!Set.of("REPLACE", "REPLACE_LIST", "FRACTAL_EXPAND", "SMOOTH")
            .contains(type)) throw new IllegalArgumentException("Unsupported Terra v2 stage " + type);
        String from = null;
        List<String> to = List.of();
        Map<String, List<String>> replacements = new HashMap<>();
        if (type.equals("REPLACE")) {
            from = raw.get("from")
                .getAsString();
            to = table(raw.get("to"));
        }
        if (type.equals("REPLACE_LIST")) {
            from = raw.get("default-from")
                .getAsString();
            to = table(raw.get("default-to"));
            for (var entry : raw.getAsJsonObject("to")
                .entrySet()) replacements.put(entry.getKey(), table(entry.getValue()));
        }
        return new Stage(type, from, to, replacements, noise(raw));
    }

    private NoiseSampler noise(JsonObject parent) {
        return parent.has("sampler") ? compiler.compileSampler(parent.getAsJsonObject("sampler")) : NoiseSampler.zero();
    }

    private List<String> table(JsonElement value) {
        List<String> result = new ArrayList<>();
        if (value.isJsonPrimitive()) return List.of(value.getAsString());
        for (var entry : value.getAsJsonArray()) for (var weighted : entry.getAsJsonObject()
            .entrySet()) {
                int count = (int) compiler.numeric(weighted.getValue());
                if (count < 1 || count > 65536 || result.size() + count > 65536)
                    throw new IllegalArgumentException("Unbounded biome weights");
                for (int i = 0; i < count; i++) result.add(weighted.getKey());
            }
        if (result.isEmpty()) throw new IllegalArgumentException("Empty biome weights");
        return List.copyOf(result);
    }

    private int number(JsonObject object, String key, int fallback) {
        return object.has(key) ? (int) compiler.numeric(object.get(key)) : fallback;
    }

    private static String type(JsonObject object) {
        return object.get("type")
            .getAsString();
    }

    private record Stage(String type, String from, List<String> to, Map<String, List<String>> replacements,
        NoiseSampler noise) {}

    private record Extrusion(String from, List<String> to, NoiseSampler noise, int min, int max) {}

    private record BiomeGrid(int originX, int originZ, String[] names, int[] ids) {}

    private record CompiledExtrusion(boolean[] matches, int[] targets, String[] names, NoiseSampler noise, int min,
        int max) {}
}
