package industries.angelic.biomic.world.palette;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.minecraft.block.Block;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import industries.angelic.biomic.Biomic;

/**
 * Which installed block Biomic places for each block of its vocabulary, the modern ids its terrain and features name
 * ({@code minecraft:deepslate}): Biomic's defaults ({@code assets/biomic/palette/default.json}) under
 * {@code config/biomic-palette.json}. An entry is a block ({@code "mod:block"} or {@code "mod:block:meta"}), a list
 * tried in order (the first installed one is used), or {@code {"block": "mod:block", "meta": 3}}; meta {@code "auto"}
 * keeps the metadata Biomic computes from the state (a log's axis, a stair's facing), for a block laid out like the
 * default one. {@code minecraft:air} places nothing. Blocks of mods that are not installed are skipped; an id none of
 * whose blocks is installed stops the game at startup, naming it. Nothing here touches another mod's classes, so a
 * palette without Et Futurum Requiem's blocks needs no Et Futurum Requiem.
 */
public final class BlockPalette {

    public static final String FILE = "biomic-palette.json";
    public static final String REFERENCE = "biomic-palette-reference.json";
    /** Written beside the user's file: a palette with no Et Futurum Requiem block, to copy from. */
    public static final String VANILLA_EXAMPLE = "biomic-palette-vanilla.json";
    private static final String VANILLA_EXAMPLE_RESOURCE = "/assets/biomic/palette/examples/vanilla-1.7.10.json";
    private static final String DEFAULTS = "/assets/biomic/palette/default.json";
    private static final String AIR = "minecraft:air";

    /** One block an entry may use: its registry name, and a fixed metadata or the state's ("auto"). */
    record Candidate(String name, int metadata, boolean auto) {

        String spelled() {
            return auto ? name + ":auto" : metadata == 0 ? name : name + ":" + metadata;
        }
    }

    /** What an entry places: its block, a fixed metadata or the state's, and whether a later candidate stood in. */
    public record Choice(Block block, int fixedMetadata, boolean auto, boolean substitute, boolean overridden,
        String name) {

        /** The metadata to place, given the one Biomic computes from the state. */
        public int metadata(int computed) {
            return auto ? computed : fixedMetadata;
        }
    }

    private static volatile Map<String, List<Candidate>> defaults;
    private static volatile Map<String, List<Candidate>> overrides = Map.of();
    private static volatile Map<String, Choice> chosen;
    private static File configDirectory;

    private BlockPalette() {}

    /** Reads the defaults and the overrides (pre-init), writing an empty override file when there is none. */
    public static synchronized void load(File directory) {
        configDirectory = directory;
        defaults = readDefaults();
        File file = new File(directory, FILE);
        try {
            if (!file.exists()) write(file, template());
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                root = new JsonParser().parse(reader)
                    .getAsJsonObject();
            }
            overrides = entries(root.has("blocks") ? root.getAsJsonObject("blocks") : new JsonObject(), file.getName());
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot read " + file + ": " + error.getMessage(), error);
        }
        chosen = null;
        if (!overrides.isEmpty()) Biomic.LOG.info("Block palette: {} override(s) from {}", overrides.size(), FILE);
    }

    /**
     * Chooses every entry's block once all mods registered theirs (post-init), and writes the reference file. Stops
     * the game, naming them, if some ids have no installed block.
     */
    public static synchronized void resolveAll() {
        Selection selection = select();
        if (configDirectory != null) {
            try {
                write(new File(configDirectory, REFERENCE), reference(selection.chosen()));
                try (InputStream example = BlockPalette.class.getResourceAsStream(VANILLA_EXAMPLE_RESOURCE)) {
                    if (example != null) Files.copy(
                        example,
                        new File(configDirectory, VANILLA_EXAMPLE).toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException error) {
                Biomic.LOG.warn("Cannot write {} or {}: {}", REFERENCE, VANILLA_EXAMPLE, error.getMessage());
            }
        }
        if (!selection.missing()
            .isEmpty()) throw new PaletteException(selection.missing());
        chosen = selection.chosen();
    }

    /** The entry's choice for an id, or null if the palette has no entry for it (Biomic's own mapping applies). */
    public static Choice choice(String id) {
        Map<String, Choice> map = chosen;
        if (map == null) map = choose();
        return map.get(id);
    }

    /** The palette's block for an id, or {@code fallback} when the palette has no entry for it. */
    public static Block block(String id, Block fallback) {
        Choice choice = choice(id);
        return choice == null ? fallback : choice.block();
    }

    /** The palette's metadata for an id given the state's, or the state's when the palette has no entry for it. */
    public static int metadata(String id, int computed) {
        Choice choice = choice(id);
        return choice == null ? computed : choice.metadata(computed);
    }

    private static volatile Map<String, String> defaultIds;

    /**
     * The id whose default block is this registry name and metadata ({@code etfuturum:moss_carpet}, 0 is
     * {@code minecraft:moss_carpet}), or null: for data that names a default block directly, so the palette still
     * decides what is placed.
     */
    public static String defaultIdFor(String name, int metadata) {
        Map<String, String> map = defaultIds;
        if (map == null) {
            Map<String, String> built = new java.util.HashMap<>();
            for (Map.Entry<String, List<Candidate>> entry : defaults().entrySet()) {
                Candidate first = entry.getValue()
                    .get(0);
                String key = first.auto() ? first.name() + "@auto" : first.name() + "@" + first.metadata();
                built.putIfAbsent(key, typeId(entry.getKey()));
            }
            defaultIds = map = built;
        }
        String exact = map.get(name + "@" + metadata);
        return exact != null ? exact : map.get(name + "@auto");
    }

    /** Whether the block is the one the palette places for an id (never air: a support check must not pass on air). */
    public static boolean places(String id, Block block) {
        Choice choice = choice(id);
        return choice != null && block != null && choice.block() == block && block != net.minecraft.init.Blocks.air;
    }

    /** Whether the user's file sets the id. */
    public static boolean overridden(String id) {
        return overrides.containsKey(id);
    }

    /** The user's choice for an id the defaults leave to Biomic's own mapping (vanilla blocks), or null. */
    public static Choice override(String id) {
        if (!overrides.containsKey(id)) return null;
        return choice(id);
    }

    /** The user's choice for an id Biomic maps itself (one without a default entry, like minecraft:stone), or null. */
    public static Choice builtinOverride(String id) {
        if (overrides.isEmpty() || !overrides.containsKey(id) || defaults().containsKey(id)) return null;
        return choice(id);
    }

    /** The id the user mapped onto this placed block, if the user's file maps one onto it; else null. */
    public static String overriddenId(Block block, int metadata) {
        if (overrides.isEmpty()) return null;
        Map<String, Choice> map = chosen;
        if (map == null) map = choose();
        String byBlock = null;
        for (String id : overrides.keySet()) {
            Choice choice = map.get(id);
            if (choice == null || choice.block() != block) continue;
            if (!choice.auto() && choice.fixedMetadata() == metadata) return typeId(id);
            if (byBlock == null) byBlock = typeId(id);
        }
        return byBlock;
    }

    /** Every id with an entry, defaults and overrides alike. */
    public static java.util.Set<String> ids() {
        java.util.Set<String> ids = new TreeSet<>(defaults().keySet());
        ids.addAll(overrides.keySet());
        return ids;
    }

    /**
     * The choices: fixed once every mod registered its blocks (resolveAll, post-init). Asked earlier (a unit test, or a
     * mod's early code), they are worked out afresh each time and never kept, so blocks registered later still count;
     * an id with no installed block then has no choice.
     */
    private static synchronized Map<String, Choice> choose() {
        if (chosen != null) return chosen;
        return select().chosen();
    }

    private record Selection(Map<String, Choice> chosen, List<String> missing) {}

    private static Selection select() {
        Map<String, List<Candidate>> base = defaults();
        Map<String, Choice> result = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        for (String id : ids()) {
            boolean user = overrides.containsKey(id);
            List<Candidate> candidates = user ? overrides.get(id) : base.get(id);
            Choice choice = null;
            for (int index = 0; index < candidates.size() && choice == null; index++) {
                Candidate candidate = candidates.get(index);
                Block block = registered(candidate.name());
                if (block != null) choice = new Choice(
                    block,
                    candidate.metadata(),
                    candidate.auto(),
                    index > 0,
                    user,
                    candidate.name());
            }
            if (choice == null) missing.add(
                id + " -> "
                    + String.join(
                        " | ",
                        candidates.stream()
                            .map(Candidate::spelled)
                            .toList())
                    + (user ? " (your " + FILE + ")" : ""));
            else result.put(id, choice);
        }
        return new Selection(Collections.unmodifiableMap(result), missing);
    }

    private static Block registered(String name) {
        if (AIR.equals(name)) return net.minecraft.init.Blocks.air;
        if (!Block.blockRegistry.containsKey(name)) return null;
        return (Block) Block.blockRegistry.getObject(name);
    }

    private static Map<String, List<Candidate>> defaults() {
        Map<String, List<Candidate>> map = defaults;
        if (map == null) {
            synchronized (BlockPalette.class) {
                if (defaults == null) defaults = readDefaults();
                map = defaults;
            }
        }
        return map;
    }

    private static Map<String, List<Candidate>> readDefaults() {
        try (InputStream stream = BlockPalette.class.getResourceAsStream(DEFAULTS)) {
            if (stream == null) throw new IllegalStateException("Biomic's default palette is missing: " + DEFAULTS);
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return entries(
                    new JsonParser().parse(reader)
                        .getAsJsonObject()
                        .getAsJsonObject("blocks"),
                    "Biomic's default palette");
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read Biomic's default palette: " + error.getMessage(), error);
        }
    }

    private static Map<String, List<Candidate>> entries(JsonObject blocks, String source) {
        Map<String, List<Candidate>> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
            if (entry.getKey()
                .startsWith("_")) continue;
            String id = entry.getKey()
                .contains(":") ? entry.getKey() : "minecraft:" + entry.getKey();
            List<Candidate> candidates = new ArrayList<>();
            JsonElement value = entry.getValue();
            if (value.isJsonArray())
                for (JsonElement element : value.getAsJsonArray()) candidates.add(candidate(element, id, source));
            else candidates.add(candidate(value, id, source));
            if (candidates.isEmpty()) throw new IllegalArgumentException(source + ": " + id + " lists no block");
            result.put(id, List.copyOf(candidates));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Candidate candidate(JsonElement element, String id, String source) {
        if (element.isJsonPrimitive()) return parse(element.getAsString(), id, source);
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (!object.has("block")) throw new IllegalArgumentException(source + ": " + id + " needs a \"block\"");
            Candidate named = parse(
                object.get("block")
                    .getAsString(),
                id,
                source);
            if (!object.has("meta")) return named;
            JsonElement meta = object.get("meta");
            if (meta.isJsonPrimitive() && meta.getAsJsonPrimitive()
                .isString() && "auto".equals(meta.getAsString())) return new Candidate(named.name(), 0, true);
            return new Candidate(named.name(), metadata(meta.getAsString(), id, source), false);
        }
        throw new IllegalArgumentException(source + ": " + id + " must be a block, a list of blocks or an object");
    }

    /** {@code mod:block}, {@code mod:block:meta} or {@code mod:block:auto}; a bare name is Minecraft's. */
    private static Candidate parse(String text, String id, String source) {
        String value = text.trim();
        String[] parts = value.split(":");
        if (parts.length == 1) return new Candidate("minecraft:" + parts[0], 0, false);
        if (parts.length == 2) return new Candidate(parts[0] + ":" + parts[1], 0, false);
        if (parts.length == 3) {
            String name = parts[0] + ":" + parts[1];
            if ("auto".equals(parts[2])) return new Candidate(name, 0, true);
            return new Candidate(name, metadata(parts[2], id, source), false);
        }
        throw new IllegalArgumentException(source + ": " + id + ": cannot read the block \"" + text + "\"");
    }

    private static int metadata(String text, String id, String source) {
        try {
            int metadata = Integer.parseInt(text.trim());
            if (metadata < 0 || metadata > 15) throw new NumberFormatException();
            return metadata;
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                source + ": " + id + ": metadata must be 0 to 15 or \"auto\", not " + text);
        }
    }

    /** An entry's id without state properties: {@code minecraft:stone_slab[type=double]} is a stone slab. */
    static String typeId(String id) {
        int bracket = id.indexOf('[');
        return bracket < 0 ? id : id.substring(0, bracket);
    }

    private static JsonObject template() {
        JsonObject root = new JsonObject();
        root.addProperty(
            "_about",
            "Replaces blocks Biomic generates. Keys are the ids Biomic names its blocks by (see " + REFERENCE
                + " for every one, its default and what it uses now). A value is \"mod:block\", \"mod:block:meta\", "
                + "a list of those (the first installed one is used) or {\"block\": \"mod:block\", \"meta\": 3}; "
                + "meta \"auto\" keeps the metadata Biomic computes from the state, for blocks laid out like the default. "
                + "\"minecraft:air\" places nothing. Applies to chunks generated afterwards. Map every Et Futurum "
                + "Requiem block elsewhere and Biomic runs without it.");
        JsonObject example = new JsonObject();
        example.addProperty("minecraft:deepslate", "minecraft:stone");
        example.add("minecraft:tuff", array("minecraft:gravel"));
        root.add("_example", example);
        root.add("blocks", new JsonObject());
        return root;
    }

    private static JsonObject reference(Map<String, Choice> result) {
        JsonObject root = new JsonObject();
        root.addProperty(
            "_about",
            "Written at every start; edits here are lost. Every id Biomic's palette knows: its default blocks, the user's "
                + "override if any, and the block in use. Override ids in "
                + FILE
                + ", including ids not listed here that Biomic places (vanilla ones such as minecraft:stone).");
        JsonObject blocks = new JsonObject();
        Map<String, List<Candidate>> base = defaults();
        for (String id : ids()) {
            JsonObject entry = new JsonObject();
            if (base.containsKey(id)) entry.add("default", spelled(base.get(id)));
            if (overrides.containsKey(id)) entry.add("override", spelled(overrides.get(id)));
            Choice choice = result.get(id);
            if (choice != null) entry.addProperty(
                "uses",
                choice.auto() ? choice.name() + ":auto"
                    : choice.fixedMetadata() == 0 ? choice.name() : choice.name() + ":" + choice.fixedMetadata());
            blocks.add(id, entry);
        }
        root.add("blocks", blocks);
        return root;
    }

    private static JsonArray spelled(List<Candidate> candidates) {
        JsonArray array = new JsonArray();
        for (Candidate candidate : candidates) array.add(new JsonPrimitive(candidate.spelled()));
        return array;
    }

    private static JsonArray array(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(new JsonPrimitive(value));
        return array;
    }

    private static void write(File file, JsonObject root) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(
                new GsonBuilder().setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create()
                    .toJson(root));
        }
    }

    /** Ids whose blocks are none of them installed: names them and how to fix it. */
    public static final class PaletteException extends RuntimeException {

        PaletteException(List<String> missing) {
            super(message(missing), null, false, false);
        }

        private static String message(List<String> missing) {
            boolean efr = missing.stream()
                .anyMatch(line -> line.contains("etfuturum:"));
            return "Biomic's block palette names blocks that are not installed, for " + missing.size()
                + " id(s):\n  "
                + String.join("\n  ", missing)
                + "\n"
                + (efr ? "Install Et Futurum Requiem, or map these ids" : "Map these ids")
                + " to installed blocks in config/"
                + FILE
                + " (config/"
                + REFERENCE
                + " lists every id; config/"
                + VANILLA_EXAMPLE
                + " maps every one to a 1.7.10 block, ready to copy).";
        }
    }
}
