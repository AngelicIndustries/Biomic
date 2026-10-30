package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.world.World;

import com.google.gson.JsonObject;

/**
 * Tartarus' feature stages (landforms, trees, flora, ores) in Terra's order: per stage, per resolution cell, each
 * column's scheduled features through their distributor, locator and weighted structures. Scripts, schematics and
 * ores run straight against the caller's {@link Region}; nothing here touches a Minecraft world.
 */
public final class TartarusFeatures {

    /** The blocks features read and write, at the pack's authored height (0..255). */
    public interface Region {

        /** Block state at a position; "minecraft:air" outside 0..255. Properties may be omitted. */
        String block(int x, int y, int z);

        /** Places a state; y is inside 0..255. */
        void set(int x, int y, int z, String state);

        /** Whether the state at a position is a full opaque block (Terra's {@code isSolid}). */
        boolean solid(int x, int y, int z);

        /** Terra's interpolated terrain density before carving; positive is land. */
        double density(double x, double y, double z);

        /** The pack biome id of a column. */
        String biome(int x, int z);
    }

    private static final int MIN_Y = TartarusTerrainRuntime.SOURCE_MIN_Y, MAX_Y = TartarusTerrainRuntime.SOURCE_MAX_Y;
    private static final int MAX_RECURSIONS = 64;

    private record Stage(String id, int resolution) {}

    private final long seed;
    private final OrigenTerrainRuntime.Compiler compiler;
    private final TerraPackFeatureSelectors selectors;
    private final List<Stage> stages = new ArrayList<>();
    private final Map<String, Map<String, List<String>>> schedules = new HashMap<>();
    private final Map<String, OrigenTerraScript> scripts = new HashMap<>();
    private final Map<String, TerraPackSchematic> schematics = new HashMap<>();
    private final Map<String, TerraPackOre> ores = new HashMap<>();

    public static TartarusFeatures load(JsonObject manifest, long seed) {
        return new TartarusFeatures(manifest, seed);
    }

    TartarusFeatures(JsonObject manifest, long seed) {
        this.seed = seed;
        compiler = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"),
            false);
        selectors = new TerraPackFeatureSelectors(manifest);
        if (!selectors.unavailable()
            .isEmpty()) throw new IllegalStateException("Unsupported Tartarus features: " + selectors.unavailable());
        JsonObject decoration = manifest.getAsJsonObject("decoration");
        for (var raw : decoration.getAsJsonArray("stages")) {
            JsonObject stage = raw.getAsJsonObject();
            if (!"FEATURE".equals(
                stage.get("type")
                    .getAsString()))
                throw new IllegalStateException("Unsupported stage");
            stages.add(
                new Stage(
                    stage.get("id")
                        .getAsString(),
                    stage.has("resolution") ? stage.get("resolution")
                        .getAsInt() : 4));
        }
        for (var biome : decoration.getAsJsonObject("feature_schedules")
            .entrySet()) {
            Map<String, List<String>> byStage = new HashMap<>();
            for (var stage : biome.getValue()
                .getAsJsonObject()
                .entrySet()) {
                List<String> features = new ArrayList<>();
                for (var feature : stage.getValue()
                    .getAsJsonArray()) features.add(feature.getAsString());
                byStage.put(stage.getKey(), List.copyOf(features));
            }
            schedules.put(biome.getKey(), byStage);
        }
        String prefix = manifest.getAsJsonObject("source")
            .get("project")
            .getAsString() + ":";
        for (var entry : decoration.getAsJsonObject("structures")
            .entrySet()) {
            JsonObject source = entry.getValue()
                .getAsJsonObject();
            switch (source.get("format")
                .getAsString()) {
                // Namespaced ids keep Origen's repairs and native rewrites off same-named scripts.
                case "TERRASCRIPT" -> scripts.put(
                    entry.getKey(),
                    new OrigenTerraScript(
                        prefix + entry.getKey(),
                        source.get("script")
                            .getAsString(),
                        false));
                case "SPONGE_SCHEMATIC" -> schematics.put(
                    entry.getKey(),
                    new TerraPackSchematic(
                        source.get("base64")
                            .getAsString()));
                default -> throw new IllegalStateException("Unknown Terra structure format " + entry.getKey());
            }
        }
        for (var entry : decoration.getAsJsonObject("ores")
            .entrySet())
            ores.put(
                entry.getKey(),
                new TerraPackOre(
                    entry.getValue()
                        .getAsJsonObject(),
                    compiler));
    }

    /** Decorates the 16x16 columns from (minX, minZ); both must sit on every stage's resolution grid. */
    public void decorate(Region region, int minX, int minZ) {
        var view = new View(region);
        var environment = new Environment(region);
        for (Stage stage : stages) {
            int resolution = stage.resolution;
            if (Math.floorMod(minX, resolution) != 0 || Math.floorMod(minZ, resolution) != 0)
                throw new IllegalArgumentException("Decoration area off the stage grid");
            for (int tx = 0; tx < 16; tx += resolution) for (int tz = 0; tz < 16; tz += resolution) {
                int x = minX + tx, z = minZ + tz;
                // Tartarus biomes are columns below the roof: one range, MIN_Y..MAX_Y.
                Map<String, List<String>> schedule = schedules.get(region.biome(x, z));
                List<String> features = schedule == null ? null : schedule.get(stage.id);
                if (features == null) continue;
                for (int dx = 0; dx < resolution; dx++)
                    for (int dz = 0; dz < resolution; dz++) for (String feature : features) {
                        for (var anchor : selectors.select(feature, view, x + dx, z + dz, MIN_Y, MAX_Y))
                            environment.generateStructure(
                                anchor.structure(),
                                null,
                                x + dx,
                                anchor.y(),
                                z + dz,
                                anchor.y(),
                                0,
                                0,
                                0,
                                0,
                                new Random(anchor.randomSeed()),
                                0);
                    }
            }
        }
    }

    static String baseId(String state) {
        int bracket = state.indexOf('[');
        return bracket < 0 ? state : state.substring(0, bracket);
    }

    static boolean air(String state) {
        String id = baseId(state);
        return id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air");
    }

    private final class View implements TerraPackFeatureSelectors.View {

        private final Region region;
        private int solidX, solidY = Integer.MIN_VALUE, solidZ;

        View(Region region) {
            this.region = region;
        }

        @Override
        public int minY() {
            return MIN_Y;
        }

        @Override
        public int maxY() {
            return MAX_Y;
        }

        @Override
        public long seed() {
            return seed;
        }

        @Override
        public String block(int x, int y, int z) {
            // Patterns ask solid() right after reading the same cell.
            solidX = x;
            solidY = y;
            solidZ = z;
            return y < MIN_Y || y >= MAX_Y ? "minecraft:air" : region.block(x, y, z);
        }

        @Override
        public boolean air(String state) {
            return TartarusFeatures.air(state);
        }

        @Override
        public boolean solid(String state) {
            if (solidY < MIN_Y || solidY >= MAX_Y) return false;
            return region.solid(solidX, solidY, solidZ);
        }

        @Override
        public boolean matches(String expected, String actual) {
            return baseId(expected).equals(baseId(actual));
        }
    }

    /** The interpreter's view of the region: authored Y is physical Y, and writes outside 0..255 are no-ops. */
    private final class Environment implements OrigenStructureEnvironment {

        private final Region region;

        Environment(Region region) {
            this.region = region;
        }

        @Override
        public int physicalY(int authoredY) {
            return authoredY;
        }

        private String read(int x, int y, int z) {
            return y < MIN_Y || y >= MAX_Y ? "minecraft:air" : region.block(x, y, z);
        }

        @Override
        public boolean placeBlock(World ignored, int x, int physicalY, int z, int y, String state, boolean overwrite,
            boolean physics, boolean waterlog) {
            if (y < MIN_Y || y >= MAX_Y) return false;
            if (!overwrite && !air(read(x, y, z))) return false;
            region.set(x, y, z, state);
            return true;
        }

        @Override
        public String getBlockId(World ignored, int x, int physicalY, int z, int y) {
            return baseId(read(x, y, z));
        }

        @Override
        public boolean generateStructure(String id, World ignored, int ox, int oy, int oz, int authoredY, int dx,
            int dy, int dz, int rotation, Random random, int recursions) {
            if (recursions > MAX_RECURSIONS) throw new IllegalStateException("Terra structure recursion exceeded");
            if (id.startsWith("BLOCK:")) {
                placeBlock(null, ox + dx, 0, oz + dz, authoredY + dy, id.substring(6), true, false, false);
                return true; // Terra's singleton structure reports success even when its write is a no-op.
            }
            var ore = ores.get(id);
            if (ore != null) return ore.generate(new TerraPackOre.Access() {

                @Override
                public int minY() {
                    return MIN_Y;
                }

                @Override
                public int maxY() {
                    return MAX_Y;
                }

                @Override
                public String get(int x, int y, int z) {
                    return read(x, y, z);
                }

                @Override
                public void put(int x, int y, int z, String state, boolean physics) {
                    placeBlock(null, x, y, z, y, state, true, physics, false);
                }
            }, ox + dx, authoredY + dy, oz + dz, random);
            var schematic = schematics.get(id);
            if (schematic != null) {
                schematic.generate(
                    ox + dx,
                    authoredY + dy,
                    oz + dz,
                    rotation,
                    (x, y, z, state) -> placeBlock(null, x, y, z, y, state, true, false, false));
                return true;
            }
            var script = scripts.get(id);
            if (script == null) throw new IllegalArgumentException("Unknown Tartarus structure " + id);
            return script.generateDetached(this, seed, ox, oy, oz, authoredY, dx, dy, dz, rotation, random, recursions);
        }

        @Override
        public double sampleNamed(String id, long sampleSeed, double x, double y, double z, boolean threeDimensional) {
            return compiler.sampleNamed(id, sampleSeed, x, z, threeDimensional);
        }

        @Override
        public String checkDensity(double x, double y, double z) {
            if (y < MIN_Y || y >= MAX_Y) return "AIR";
            return region.density(x, y, z) > 0 ? "LAND" : "AIR";
        }

        @Override
        public void spawnEntity(World ignored, int x, int y, int z, String id) {}
    }
}
