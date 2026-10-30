package industries.angelic.biomic.client.browser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/**
 * The biome browser's own record, per user (config/biomic/browser/state.json): favourites, notes per biome and issues
 * marked where they were seen. Every change also rewrites review.md beside it, the notes and issues grouped by biome
 * with teleports, for whoever reviews them (Claude reads it).
 */
@SideOnly(Side.CLIENT)
final class BrowserState {

    /** A note on a biome. */
    record Note(String text, long time) {}

    /** Something wrong seen at a spot: the biome there, the world (server or save) and the dimension. */
    record Issue(String key, String world, int dimension, int x, int y, int z, String text, long time) {}

    /** The screen's settings, kept between games: filters, search, selection, sort and where Go looks per biome. */
    static final class Prefs {

        Set<String> places = new LinkedHashSet<>(), sources = new LinkedHashSet<>();
        boolean favoritesOnly, missingThumbnailOnly, notesOnly;
        String query = "", selected, sort = "NAME";
        Map<String, String> goPlaces = new HashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static BrowserState instance;

    private final File directory;
    private final Set<String> favorites = new LinkedHashSet<>();
    private final Map<String, List<Note>> notes = new LinkedHashMap<>();
    private final List<Issue> issues = new ArrayList<>();
    /** Each annotated biome's name and source as the catalogue gave them, for the review. */
    private final Map<String, String[]> labels = new HashMap<>();
    private Prefs prefs = new Prefs();

    BrowserState(File directory) {
        this.directory = directory;
    }

    static synchronized BrowserState get() {
        if (instance == null) {
            instance = new BrowserState(directory());
            instance.load();
        }
        return instance;
    }

    /** config/biomic/browser, where the browser keeps its files. */
    static File directory() {
        return new File(
            new File(
                Loader.instance()
                    .getConfigDir(),
                Biomic.MODID),
            "browser");
    }

    File review() {
        return new File(directory, "review.md");
    }

    /** The screen's settings; changes are kept by {@link #savePrefs}. */
    Prefs prefs() {
        return prefs;
    }

    void savePrefs() {
        writeState();
    }

    boolean favorite(String key) {
        return favorites.contains(key);
    }

    void toggleFavorite(String key) {
        if (!favorites.remove(key)) favorites.add(key);
        save();
    }

    List<Note> notes(String key) {
        return Collections.unmodifiableList(notes.getOrDefault(key, List.of()));
    }

    List<Issue> issues(String key) {
        List<Issue> found = new ArrayList<>();
        for (Issue issue : issues) if (issue.key()
            .equals(key)) found.add(issue);
        return found;
    }

    /** Whether a biome has notes or issues. */
    boolean annotated(String key) {
        if (notes.containsKey(key)) return true;
        for (Issue issue : issues) if (issue.key()
            .equals(key)) return true;
        return false;
    }

    void addNote(String key, String name, String source, String text) {
        labels.put(key, new String[] { name, source });
        notes.computeIfAbsent(key, k -> new ArrayList<>())
            .add(new Note(text, System.currentTimeMillis()));
        save();
    }

    void removeNote(String key, Note note) {
        List<Note> list = notes.get(key);
        if (list == null || !list.remove(note)) return;
        if (list.isEmpty()) notes.remove(key);
        save();
    }

    void addIssue(String key, String name, String source, String world, int dimension, int x, int y, int z,
        String text) {
        labels.put(key, new String[] { name, source });
        issues.add(new Issue(key, world, dimension, x, y, z, text, System.currentTimeMillis()));
        save();
    }

    /** The latest issue marked, or null. */
    Issue lastIssue() {
        return issues.isEmpty() ? null : issues.get(issues.size() - 1);
    }

    void removeIssue(Issue issue) {
        if (issues.remove(issue)) save();
    }

    void load() {
        File file = new File(directory, "state.json");
        if (!file.isFile()) return;
        try {
            JsonObject root = new JsonParser()
                .parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                .getAsJsonObject();
            JsonArray list = root.getAsJsonArray("favorites");
            if (list != null) for (JsonElement key : list) favorites.add(key.getAsString());
            JsonObject noted = root.getAsJsonObject("notes");
            if (noted != null) for (Map.Entry<String, JsonElement> biome : noted.entrySet()) {
                List<Note> read = new ArrayList<>();
                for (JsonElement note : biome.getValue()
                    .getAsJsonArray()) {
                    JsonObject object = note.getAsJsonObject();
                    read.add(
                        new Note(
                            object.get("text")
                                .getAsString(),
                            object.get("time")
                                .getAsLong()));
                }
                if (!read.isEmpty()) notes.put(biome.getKey(), read);
            }
            JsonArray marked = root.getAsJsonArray("issues");
            if (marked != null) for (JsonElement issue : marked) {
                JsonObject o = issue.getAsJsonObject();
                issues.add(
                    new Issue(
                        o.get("key")
                            .getAsString(),
                        o.has("world") ? o.get("world")
                            .getAsString() : "",
                        o.get("dimension")
                            .getAsInt(),
                        o.get("x")
                            .getAsInt(),
                        o.get("y")
                            .getAsInt(),
                        o.get("z")
                            .getAsInt(),
                        o.get("text")
                            .getAsString(),
                        o.get("time")
                            .getAsLong()));
            }
            if (root.has("prefs")) {
                Prefs read = GSON.fromJson(root.get("prefs"), Prefs.class);
                if (read != null) prefs = read;
                if (prefs.places == null) prefs.places = new LinkedHashSet<>();
                if (prefs.sources == null) prefs.sources = new LinkedHashSet<>();
                if (prefs.goPlaces == null) prefs.goPlaces = new HashMap<>();
                if (prefs.query == null) prefs.query = "";
                if (prefs.sort == null) prefs.sort = "NAME";
            }
            JsonObject named = root.getAsJsonObject("labels");
            if (named != null) for (Map.Entry<String, JsonElement> label : named.entrySet()) {
                JsonObject o = label.getValue()
                    .getAsJsonObject();
                labels.put(
                    label.getKey(),
                    new String[] { o.get("name")
                        .getAsString(),
                        o.get("source")
                            .getAsString() });
            }
        } catch (IOException | RuntimeException failure) {
            Biomic.LOG.warn("Could not read the biome browser's state from {}: {}", file, failure.toString());
        }
    }

    /** Writes state.json and review.md. */
    private void save() {
        writeState();
        try {
            Files.write(review().toPath(), review(System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
        } catch (IOException failure) {
            Biomic.LOG.warn("Could not write the biome review to {}: {}", review(), failure.toString());
        }
    }

    private void writeState() {
        JsonObject root = new JsonObject();
        JsonArray list = new JsonArray();
        for (String key : favorites) list.add(new JsonPrimitive(key));
        root.add("favorites", list);
        JsonObject noted = new JsonObject();
        for (Map.Entry<String, List<Note>> biome : notes.entrySet()) {
            JsonArray read = new JsonArray();
            for (Note note : biome.getValue()) {
                JsonObject object = new JsonObject();
                object.addProperty("text", note.text());
                object.addProperty("time", note.time());
                read.add(object);
            }
            noted.add(biome.getKey(), read);
        }
        root.add("notes", noted);
        JsonArray marked = new JsonArray();
        for (Issue issue : issues) {
            JsonObject o = new JsonObject();
            o.addProperty("key", issue.key());
            o.addProperty("world", issue.world());
            o.addProperty("dimension", issue.dimension());
            o.addProperty("x", issue.x());
            o.addProperty("y", issue.y());
            o.addProperty("z", issue.z());
            o.addProperty("text", issue.text());
            o.addProperty("time", issue.time());
            marked.add(o);
        }
        root.add("issues", marked);
        JsonObject named = new JsonObject();
        for (Map.Entry<String, String[]> label : labels.entrySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("name", label.getValue()[0]);
            o.addProperty("source", label.getValue()[1]);
            named.add(label.getKey(), o);
        }
        root.add("labels", named);
        root.add("prefs", GSON.toJsonTree(prefs));
        try {
            Files.createDirectories(directory.toPath());
            Files.write(
                new File(directory, "state.json").toPath(),
                GSON.toJson(root)
                    .getBytes(StandardCharsets.UTF_8));
        } catch (IOException failure) {
            Biomic.LOG.warn("Could not save the biome browser's state to {}: {}", directory, failure.toString());
        }
    }

    /** review.md: the annotated biomes by name, each with its notes and issues in time order, then the favourites. */
    String review(long now) {
        StringBuilder out = new StringBuilder("# Biome review\n\nWritten by Biomic's biome browser on ")
            .append(time(now))
            .append(". Positions are block coordinates in the dimension named; a `/tp` works there for an op.\n");
        Set<String> annotated = new TreeSet<>(
            Comparator.comparing((String key) -> name(key).toLowerCase(Locale.ROOT))
                .thenComparing(key -> key));
        annotated.addAll(notes.keySet());
        for (Issue issue : issues) annotated.add(issue.key());
        if (annotated.isEmpty()) out.append("\nNo notes or issues yet.\n");
        for (String key : annotated) {
            out.append("\n## ")
                .append(name(key))
                .append("\n\n`")
                .append(key)
                .append('`');
            String[] label = labels.get(key);
            if (label != null && !label[1].isEmpty()) out.append(", ")
                .append(label[1]);
            if (favorites.contains(key)) out.append(", favourite");
            String thumbnail = "thumbnails/" + BrowserThumbnails.relative(key);
            if (new File(directory, thumbnail).isFile()) out.append(". Thumbnail: `")
                .append(thumbnail)
                .append('`');
            out.append(".\n\n");
            List<Object[]> lines = new ArrayList<>();
            for (Note note : notes.getOrDefault(key, List.of()))
                lines.add(new Object[] { note.time(), "note: " + note.text() });
            for (Issue issue : issues) {
                if (!issue.key()
                    .equals(key)) continue;
                String at = issue.x() + " " + issue.y() + " " + issue.z();
                lines.add(
                    new Object[] { issue.time(),
                        "issue at " + at
                            + " in "
                            + dimension(issue.dimension())
                            + (issue.world()
                                .isEmpty() ? "" : " of " + issue.world())
                            + ": "
                            + (issue.text()
                                .isEmpty() ? "(no text)" : issue.text())
                            + " `/tp "
                            + at
                            + "`" });
            }
            lines.sort(Comparator.comparingLong(line -> (Long) line[0]));
            for (Object[] line : lines) out.append("- ")
                .append(time((Long) line[0]))
                .append(", ")
                .append(line[1])
                .append('\n');
        }
        List<String> plain = new ArrayList<>();
        for (String key : favorites) if (!annotated.contains(key)) plain.add(key);
        if (!plain.isEmpty()) {
            out.append("\n## Favourites without notes\n\n");
            for (String key : plain) out.append("- `")
                .append(key)
                .append("`\n");
        }
        return out.toString();
    }

    private String name(String key) {
        String[] label = labels.get(key);
        return label == null || label[0].isEmpty() ? key : label[0];
    }

    private static String dimension(int dimension) {
        return switch (dimension) {
            case 0 -> "the Overworld";
            case -1 -> "the Nether";
            case 1 -> "the End";
            default -> "dimension " + dimension;
        };
    }

    private static String time(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(new Date(millis));
    }
}
