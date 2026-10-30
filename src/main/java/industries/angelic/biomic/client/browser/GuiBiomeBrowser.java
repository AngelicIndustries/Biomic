package industries.angelic.biomic.client.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.BiomeGenBase;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.api.BiomicBiomeApi;
import industries.angelic.biomic.browser.BrowserCatalogue.Entry;
import industries.angelic.biomic.browser.BrowserCatalogue.Place;
import industries.angelic.biomic.browser.BrowserMessages;
import industries.angelic.biomic.browser.BrowserReplies;
import industries.angelic.biomic.network.BiomicNetwork;

/**
 * The biome browser (docs/biome-browser.md): every biome of the world in a searchable list, its details beside it.
 * A developer's screen: the server answers ops only.
 */
@SideOnly(Side.CLIENT)
public final class GuiBiomeBrowser extends GuiScreen {

    private static final int PANEL = 0xE0101418, PANEL_LIGHT = 0xE01C232C, BORDER = 0xFF2C3846, ACCENT = 0xFF4FC3A1,
        ACCENT_DIM = 0x704FC3A1, TEXT = 0xFFE6EDF3, MUTED = 0xFF8B98A5, STAR = 0xFFF2C14E, DISABLED = 0xFF55606C;
    private static final Map<Place, Integer> PLACE_COLORS = Map
        .of(Place.SURFACE, 0xFF6FBF73, Place.SKY, 0xFF7FB8E6, Place.CAVES, 0xFFA58BD6, Place.NETHER, 0xFFD9654B);
    private static final String STAR_GLYPH = "★";
    private static final int MARGIN = 10, ROW = 22, ISSUE = 0xFFE8913A;

    /** The catalogue of this session and the screen's settings, kept while the game runs. */
    private static List<Entry> catalogue;
    private static String denied;
    private static final Set<Place> places = EnumSet.noneOf(Place.class);
    private static final Set<String> sources = new java.util.HashSet<>();
    private static boolean favoritesOnly, missingThumbnailOnly, notesOnly;
    private static String query = "";
    private static String selectedKey;
    /** The place Go searches, per biome living in several; the first of its places otherwise. */
    private static final Map<String, Place> goPlaces = new java.util.HashMap<>();
    /** The latest line of a search the server reports. */
    private static String status;

    /** How the list is ordered. */
    private enum Sort {
        NAME,
        SOURCE,
        DISTANCE
    }

    private static Sort sort = Sort.NAME;
    /** The settings were read from state.json (once a game). */
    private static boolean restored;
    /** The nearest find of each biome in each place ("key|PLACE"), in blocks from where the survey began; -1: none. */
    private static final Map<String, Integer> distances = new java.util.HashMap<>();
    /** Where the latest survey began: dimension, x, z; null before any. */
    private static int[] surveyedFrom;

    private final List<Entry> shown = new ArrayList<>();
    private final List<Chip> chips = new ArrayList<>();
    private final List<Action> actions = new ArrayList<>();
    /** The detail panel's place badges, laid out as it draws them. */
    private final List<Chip> placeChips = new ArrayList<>();
    private GuiTextField search;
    private int scroll, listLeft, listRight, listTop, listBottom, detailLeft, detailRight;
    private boolean requested;
    /** Where the detail panel drew the large thumbnail: left, top, right, bottom. */
    private int[] thumbnail;
    private String hoverText;
    /** The top bar's height and where its filter chips start: beside the search, or on a second line when narrow. */
    private int top = 34, chipsX, chipsY, chipsEnd;
    /** The top of the detail panel's buttons, which wrap onto more rows when narrow. */
    private int actionsTop;

    /** What the line being written becomes. */
    private enum Compose {
        NOTE,
        ISSUE
    }

    private Compose composing;
    private GuiTextField note;
    /** Where an issue is being marked: the player's block and dimension, and the biome there. */
    private int[] issueAt;
    private Entry issueEntry;
    /** The detail panel's note and issue lines, laid out as it draws them. */
    private final List<Line> lines = new ArrayList<>();

    /** A note or issue line: hover text, and what a left or right click does. */
    private record Line(int x, int y, int w, String hover, Runnable left, Runnable right) {

        boolean hit(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + 10;
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        restore();
        search = new GuiTextField(fontRendererObj, MARGIN + 92, 10, 150, 14);
        search.setMaxStringLength(64);
        search.setText(query);
        search.setFocused(true);
        listLeft = MARGIN;
        listRight = Math.max(MARGIN + 180, width * 2 / 5);
        int filters = 0;
        for (String label : topChipLabels()) filters += fontRendererObj.getStringWidth(label) + 14;
        boolean wide = MARGIN + 250 + filters <= width - MARGIN - 90;
        chipsX = wide ? MARGIN + 250 : MARGIN;
        chipsY = wide ? 11 : 30;
        top = wide ? 34 : 48;
        listTop = top + 4;
        listBottom = height - MARGIN;
        detailLeft = listRight + 8;
        detailRight = width - MARGIN;
        if (catalogue == null && !requested) {
            BiomicNetwork.sendToServer(new BrowserMessages.Request(BrowserMessages.Action.CATALOGUE));
            requested = true;
        }
        refilter();
        reveal();
    }

    /** The settings of the last game, once a game: filters, search, selection, sort, Go's places. */
    private static void restore() {
        if (restored) return;
        restored = true;
        BrowserState.Prefs prefs = BrowserState.get()
            .prefs();
        places.clear();
        for (String place : prefs.places) try {
            places.add(Place.valueOf(place));
        } catch (IllegalArgumentException unknown) {
            // A place this version no longer has.
        }
        sources.clear();
        sources.addAll(prefs.sources);
        favoritesOnly = prefs.favoritesOnly;
        missingThumbnailOnly = prefs.missingThumbnailOnly;
        notesOnly = prefs.notesOnly;
        query = prefs.query;
        if (prefs.selected != null) selectedKey = prefs.selected;
        try {
            sort = Sort.valueOf(prefs.sort);
        } catch (IllegalArgumentException unknown) {
            sort = Sort.NAME;
        }
        // A survey's distances belong to the spot it began from, so the distance order starts again by name.
        if (sort == Sort.DISTANCE) sort = Sort.NAME;
        goPlaces.clear();
        for (Map.Entry<String, String> chosen : prefs.goPlaces.entrySet()) try {
            goPlaces.put(chosen.getKey(), Place.valueOf(chosen.getValue()));
        } catch (IllegalArgumentException unknown) {
            // As above.
        }
    }

    /** Keeps the settings for the next game. */
    private static void remember() {
        BrowserState state = BrowserState.get();
        BrowserState.Prefs prefs = state.prefs();
        prefs.places.clear();
        for (Place place : places) prefs.places.add(place.name());
        prefs.sources.clear();
        prefs.sources.addAll(sources);
        prefs.favoritesOnly = favoritesOnly;
        prefs.missingThumbnailOnly = missingThumbnailOnly;
        prefs.notesOnly = notesOnly;
        prefs.query = query;
        prefs.selected = selectedKey;
        prefs.sort = sort.name();
        prefs.goPlaces.clear();
        for (Map.Entry<String, Place> chosen : goPlaces.entrySet()) prefs.goPlaces.put(
            chosen.getKey(),
            chosen.getValue()
                .name());
        state.savePrefs();
    }

    /** Scrolls the list so the selection shows. */
    private void reveal() {
        int index = shown.indexOf(selected());
        if (index < 0) return;
        int top = index * ROW, bottom = top + ROW, view = listBottom - listTop;
        if (top < scroll) scroll = top;
        else if (bottom > scroll + view) scroll = bottom - view;
    }

    @Override
    public void onGuiClosed() {
        remember();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        search.updateCursorCounter();
        if (note != null) note.updateCursorCounter();
        BrowserReplies.Catalogue reply = BrowserReplies.takeCatalogue();
        if (reply != null) {
            if (reply.allowed()) {
                catalogue = new ArrayList<>(reply.entries());
                catalogue.sort(
                    Comparator.comparing(
                        entry -> entry.name()
                            .toLowerCase(Locale.ROOT)));
                denied = null;
            } else
                denied = "The biome browser is for ops (or the single player with cheats), and the server must allow it.";
            refilter();
        }
        List<BrowserMessages.Distance> measured = BrowserReplies.takeDistances();
        for (BrowserMessages.Distance distance : measured) distances.put(
            distance.key() + "|"
                + distance.place()
                    .name(),
            distance.distance());
        BrowserReplies.SurveyProgress progress = BrowserReplies.surveyProgress();
        if (!measured.isEmpty() && progress != null) {
            if (progress.done() == progress.total() && surveyedFrom != null)
                status = "Distances measured from " + surveyedFrom[1] + " " + surveyedFrom[2] + ".";
            refilter();
            if (sort == Sort.DISTANCE) reveal();
        }
        BrowserReplies.Status line = BrowserReplies.takeStatus();
        if (line != null) {
            status = line.text();
            if (line.progress() == BrowserMessages.Progress.LANDED) {
                // There: the world is what to look at now; the line goes to the chat.
                mc.displayGuiScreen(null);
                BrowserKeys.chat(line.text());
            }
        }
    }

    // ------------------------------------------------------------------------------------------------- filtering

    private void refilter() {
        shown.clear();
        if (catalogue != null) {
            String needle = query.toLowerCase(Locale.ROOT)
                .trim();
            BrowserState state = BrowserState.get();
            for (Entry entry : catalogue) {
                if (!places.isEmpty() && Collections.disjoint(places, entry.places())) continue;
                if (!sources.isEmpty() && !sources.contains(entry.source())) continue;
                if (favoritesOnly && !state.favorite(entry.key())) continue;
                if (missingThumbnailOnly && BrowserThumbnails.has(entry.key())) continue;
                if (notesOnly && !state.annotated(entry.key())) continue;
                if (!needle.isEmpty() && !(entry.name()
                    .toLowerCase(Locale.ROOT)
                    .contains(needle)
                    || entry.key()
                        .contains(needle)
                    || entry.habitat()
                        .toLowerCase(Locale.ROOT)
                        .contains(needle)
                    || entry.source()
                        .toLowerCase(Locale.ROOT)
                        .contains(needle)))
                    continue;
                shown.add(entry);
            }
        }
        Comparator<Entry> byName = Comparator.comparing(
            entry -> entry.name()
                .toLowerCase(Locale.ROOT));
        if (sort == Sort.SOURCE) shown.sort(
            Comparator.comparing((Entry entry) -> shortSource(entry.source()))
                .thenComparing(byName));
        else if (sort == Sort.DISTANCE) shown.sort(
            Comparator.comparingLong(GuiBiomeBrowser::distanceOrder)
                .thenComparing(byName));
        if (selected() == null && !shown.isEmpty()) selectedKey = shown.get(0)
            .key();
        layoutChips();
        scroll = MathHelper.clamp_int(scroll, 0, maxScroll());
        layoutActions();
    }

    /** Nearest first, then those not measured yet, then those with none within reach. */
    private static long distanceOrder(Entry entry) {
        Integer distance = distance(entry);
        return distance == null ? Integer.MAX_VALUE : distance < 0 ? Long.MAX_VALUE : distance;
    }

    /** The surveyed distance of a biome where Go looks, -1 for none within reach, null when not measured. */
    private static Integer distance(Entry entry) {
        Place place = goPlace(entry);
        return place == null ? Integer.valueOf(-1) : distances.get(entry.key() + "|" + place.name());
    }

    private static String distanceLabel(Entry entry) {
        Integer distance = distance(entry);
        if (distance == null) return "\u2026";
        if (distance < 0) return "none";
        return distance < 1000 ? distance + "" : String.format(Locale.ROOT, "%.1fk", distance / 1000.0);
    }

    /** Orders by distance: measures from here first, unless the last survey began within 256 blocks. */
    private void sortByDistance() {
        sort = Sort.DISTANCE;
        if (mc.thePlayer == null) return;
        int dimension = mc.thePlayer.dimension, x = (int) mc.thePlayer.posX, z = (int) mc.thePlayer.posZ;
        if (surveyedFrom != null && surveyedFrom[0] == dimension
            && Math.abs(surveyedFrom[1] - x) < 256
            && Math.abs(surveyedFrom[2] - z) < 256) return;
        surveyedFrom = new int[] { dimension, x, z };
        distances.clear();
        BrowserReplies.surveyStarted();
        BiomicNetwork.sendToServer(new BrowserMessages.Survey());
    }

    private Entry selected() {
        for (Entry entry : shown) if (entry.key()
            .equals(selectedKey)) return entry;
        return null;
    }

    private int maxScroll() {
        return Math.max(0, shown.size() * ROW - (listBottom - listTop));
    }

    // ---------------------------------------------------------------------------------------------- chips, actions

    /** A small toggle: a place, the favourites filter or a source. */
    private record Chip(int x, int y, int w, String label, int color, boolean on, Runnable toggle) {

        boolean hit(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + 12;
        }
    }

    /** A button of the detail panel; disabled ones say why. */
    private record Action(int x, int y, int w, String label, boolean enabled, String why, String hint, Runnable run) {

        boolean hit(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + 16;
        }
    }

    private static final String MISSING = "No thumbnail", NOTED = "Notes";

    private static List<String> topChipLabels() {
        List<String> labels = new ArrayList<>();
        for (Place place : Place.values()) labels.add(placeLabel(place));
        labels.add(STAR_GLYPH);
        labels.add(MISSING);
        labels.add(NOTED);
        labels.add("Sort: distance 999/999");
        return labels;
    }

    /** The sort chip, with how far a running survey has got. */
    private static String sortLabel() {
        String label = "Sort: " + sort.name()
            .toLowerCase(Locale.ROOT);
        BrowserReplies.SurveyProgress progress = BrowserReplies.surveyProgress();
        if (sort == Sort.DISTANCE && progress != null && progress.done() < progress.total())
            label += " " + progress.done() + "/" + progress.total();
        return label;
    }

    /** Name, source, distance (measuring from here if need be), name. */
    private void cycleSort() {
        if (sort == Sort.NAME) sort = Sort.SOURCE;
        else if (sort == Sort.SOURCE) sortByDistance();
        else sort = Sort.NAME;
    }

    private static String placeLabel(Place place) {
        return place.name()
            .charAt(0)
            + place.name()
                .substring(1)
                .toLowerCase(Locale.ROOT);
    }

    private void layoutChips() {
        chips.clear();
        int x = chipsX;
        for (Place place : Place.values()) {
            String label = placeLabel(place);
            int w = fontRendererObj.getStringWidth(label) + 10;
            chips.add(new Chip(x, chipsY, w, label, PLACE_COLORS.get(place), places.contains(place), () -> {
                if (!places.remove(place)) places.add(place);
                refilter();
            }));
            x += w + 4;
        }
        x = chip(x, STAR_GLYPH, STAR, favoritesOnly, () -> favoritesOnly = !favoritesOnly);
        x = chip(x, MISSING, MUTED, missingThumbnailOnly, () -> missingThumbnailOnly = !missingThumbnailOnly);
        x = chip(x, NOTED, ISSUE, notesOnly, () -> notesOnly = !notesOnly);
        chipsEnd = chip(x, sortLabel(), ACCENT, false, this::cycleSort);
        listBottom = height - MARGIN;
        if (catalogue == null) return;
        // The sources under the list, in as many rows as its width needs; the list ends above them.
        Map<String, Integer> counts = new TreeMap<>();
        for (Entry entry : catalogue) counts.merge(entry.source(), 1, Integer::sum);
        List<List<Object[]>> rows = new ArrayList<>();
        int sx = listRight;
        for (Map.Entry<String, Integer> count : counts.entrySet()) {
            String label = shortSource(count.getKey()) + " " + count.getValue();
            int cw = fontRendererObj.getStringWidth(label) + 10;
            if (sx + cw > listRight) {
                rows.add(new ArrayList<>());
                sx = listLeft;
            }
            rows.get(rows.size() - 1)
                .add(new Object[] { count.getKey(), label, cw });
            sx += cw + 4;
        }
        listBottom = height - MARGIN - rows.size() * 14 - 4;
        int sy = listBottom + 6;
        for (List<Object[]> row : rows) {
            sx = listLeft;
            for (Object[] chip : row) {
                String source = (String) chip[0];
                int cw = (Integer) chip[2];
                chips.add(new Chip(sx, sy, cw, (String) chip[1], ACCENT, sources.contains(source), () -> {
                    if (!sources.remove(source)) sources.add(source);
                    refilter();
                }));
                sx += cw + 4;
            }
            sy += 14;
        }
    }

    /** A top-bar toggle filter; returns where the next one goes. */
    private int chip(int x, String label, int color, boolean on, Runnable toggle) {
        int w = fontRendererObj.getStringWidth(label) + 10;
        chips.add(new Chip(x, chipsY, w, label, color, on, () -> {
            toggle.run();
            refilter();
        }));
        return x + w + 4;
    }

    /** A detail-panel button before it is placed. */
    private record ActionSpec(String label, boolean enabled, String why, String hint, Runnable run) {}

    /** The detail panel's buttons, in rows from the bottom up as many as the width needs. */
    private void layoutActions() {
        actions.clear();
        actionsTop = height - MARGIN - 4;
        Entry entry = selected();
        if (entry == null) return;
        BrowserState state = BrowserState.get();
        boolean somewhere = !entry.places()
            .isEmpty();
        String nowhere = "This world never generates it";
        List<ActionSpec> specs = List.of(
            new ActionSpec("Go", somewhere, nowhere, "Enter", () -> go(entry, false)),
            new ActionSpec("Next", somewhere, nowhere, "Shift+Enter", () -> go(entry, true)),
            new ActionSpec(
                state.favorite(entry.key()) ? STAR_GLYPH + " Favourite" : "Favourite",
                true,
                null,
                "Ctrl+F",
                () -> {
                    state.toggleFavorite(entry.key());
                    refilter();
                }),
            new ActionSpec("Capture", true, null, "Ctrl+P", this::capture),
            new ActionSpec("Note", true, null, "Ctrl+N", () -> compose(Compose.NOTE)),
            new ActionSpec(
                "Issue here",
                mc.thePlayer != null,
                "Only in a world",
                "Ctrl+I",
                () -> compose(Compose.ISSUE)),
            new ActionSpec("Copy key", true, null, "Ctrl+K", () -> copyKey(entry)));
        int left = detailLeft + 10, right = detailRight - 10;
        List<List<ActionSpec>> rows = new ArrayList<>();
        int x = right;
        for (ActionSpec spec : specs) {
            int w = fontRendererObj.getStringWidth(spec.label()) + 14;
            if (x + w > right) {
                rows.add(new ArrayList<>());
                x = left;
            }
            rows.get(rows.size() - 1)
                .add(spec);
            x += w + 4;
        }
        actionsTop = height - MARGIN - 4 - rows.size() * 20;
        int y = actionsTop;
        for (List<ActionSpec> row : rows) {
            x = left;
            for (ActionSpec spec : row) x = action(x, y, spec);
            y += 20;
        }
    }

    // ------------------------------------------------------------------------------------------------ notes, issues

    /**
     * Starts writing a note on the selected biome, or an issue at the player's feet: filed under the biome there (its
     * 3D identity: a cave, an island, the surface), else under the selected one.
     */
    private void compose(Compose mode) {
        Entry entry = selected();
        if (entry == null) return;
        if (mode == Compose.ISSUE) {
            if (mc.thePlayer == null || mc.theWorld == null) return;
            int x = MathHelper.floor_double(mc.thePlayer.posX), z = MathHelper.floor_double(mc.thePlayer.posZ);
            int y = MathHelper.floor_double(mc.thePlayer.boundingBox.minY);
            issueAt = new int[] { x, y, z, mc.thePlayer.dimension };
            BiomeGenBase there = BiomicBiomeApi.getBiome(mc.theWorld, x, y, z);
            issueEntry = entry;
            if (there != null && catalogue != null)
                for (Entry candidate : catalogue) if (candidate.id() == there.biomeID) issueEntry = candidate;
        }
        composing = mode;
        note = new GuiTextField(fontRendererObj, detailLeft + 10, actionsTop - 20, detailRight - detailLeft - 20, 14);
        note.setMaxStringLength(240);
        note.setFocused(true);
        search.setFocused(false);
    }

    private void stopComposing() {
        composing = null;
        note = null;
        issueAt = null;
        issueEntry = null;
        search.setFocused(true);
    }

    private void commit() {
        String text = note.getText()
            .trim();
        Entry entry = composing == Compose.ISSUE ? issueEntry : selected();
        BrowserState state = BrowserState.get();
        if (entry == null || composing == Compose.NOTE && text.isEmpty()) {
            stopComposing();
            return;
        }
        if (composing == Compose.NOTE) {
            state.addNote(entry.key(), entry.name(), entry.source(), text);
            status = "Noted (in review.md)";
        } else {
            state.addIssue(
                entry.key(),
                entry.name(),
                entry.source(),
                worldLabel(),
                issueAt[3],
                issueAt[0],
                issueAt[1],
                issueAt[2],
                text);
            status = "Issue marked on " + entry.name() + " (in review.md)";
        }
        stopComposing();
        refilter();
    }

    /** The world an issue belongs to: the server's address, or the single player save's name. */
    private String worldLabel() {
        if (mc.isSingleplayer() && mc.getIntegratedServer() != null) return mc.getIntegratedServer()
            .getWorldName();
        return mc.func_147104_D() == null ? "" : mc.func_147104_D().serverIP;
    }

    /**
     * The selected biome's issues and notes from {@code y}: at most five lines, none past {@code bottom}, the rest
     * counted (review.md has them all). Returns the y below them.
     */
    private int drawNotes(Entry entry, int x, int y, int width, int bottom, int mx, int my) {
        lines.clear();
        BrowserState state = BrowserState.get();
        List<BrowserState.Note> notes = state.notes(entry.key());
        List<BrowserState.Issue> issues = state.issues(entry.key());
        int total = notes.size() + issues.size();
        if (total == 0 || y + 21 > bottom) return y;
        fontRendererObj.drawString("Notes and issues", x, y, MUTED);
        if (mx >= x && mx < x + fontRendererObj.getStringWidth("Notes and issues") && my >= y && my < y + 10)
            hoverText = "All of them, for review:\n" + state.review()
                .getPath();
        y += 11;
        int room = Math.min(5, (bottom - y) / 10), shownLines = total <= room ? total : room - 1, drawn = 0;
        String here = worldLabel();
        for (BrowserState.Issue issue : issues) {
            if (drawn == shownLines) break;
            String at = issue.x() + " " + issue.y() + " " + issue.z();
            int atWidth = fontRendererObj.getStringWidth(at);
            fontRendererObj.drawString("!", x + 2, y, ISSUE);
            String text = issue.text()
                .isEmpty() ? "(no text)" : issue.text();
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(text, width - atWidth - 16), x + 8, y, TEXT);
            fontRendererObj.drawString(at, x + width - atWidth, y, MUTED);
            boolean sameWorld = issue.world()
                .equals(here);
            lines.add(
                new Line(
                    x,
                    y,
                    width,
                    text + "\n"
                        + at
                        + " in "
                        + (issue.dimension() == -1 ? "the Nether"
                            : issue.dimension() == 0 ? "the Overworld" : "dimension " + issue.dimension())
                        + (sameWorld ? "\nClick: go back there" : "\nFrom " + issue.world())
                        + "\nRight-click: delete",
                    () -> {
                        if (!sameWorld) {
                            status = "That issue is from " + issue.world() + ".";
                            return;
                        }
                        BiomicNetwork
                            .sendToServer(new BrowserMessages.GoTo(issue.dimension(), issue.x(), issue.y(), issue.z()));
                        status = "Going back to " + at + "\u2026";
                    },
                    () -> {
                        state.removeIssue(issue);
                        refilter();
                    }));
            y += 10;
            drawn++;
        }
        for (BrowserState.Note entryNote : notes) {
            if (drawn == shownLines) break;
            fontRendererObj.drawString("\u2022", x + 1, y, MUTED);
            fontRendererObj
                .drawString(fontRendererObj.trimStringToWidth(entryNote.text(), width - 8), x + 8, y, 0xFFC9D3DC);
            lines.add(new Line(x, y, width, entryNote.text() + "\nRight-click: delete", null, () -> {
                state.removeNote(entry.key(), entryNote);
                refilter();
            }));
            y += 10;
            drawn++;
        }
        if (drawn < total) {
            fontRendererObj.drawString("+" + (total - drawn) + " more in review.md", x + 8, y, MUTED);
            y += 10;
        }
        for (Line line : lines) if (line.hit(mx, my)) hoverText = line.hover();
        return y + 4;
    }

    // BrowserScreenTest's handles: what a click or a keystroke would do.

    boolean testSelect(String key) {
        query = "";
        search.setText("");
        refilter();
        for (int i = 0; i < shown.size(); i++) if (shown.get(i)
            .key()
            .equals(key)) {
                select(i);
                return true;
            }
        return false;
    }

    void testWrite(boolean issue, String text) {
        compose(issue ? Compose.ISSUE : Compose.NOTE);
        if (note == null) return;
        note.setText(text);
        commit();
    }

    /** Clicks the first issue line drawn, as a left click would. */
    boolean testGoBack() {
        for (Line line : lines) if (line.left() != null) {
            line.left()
                .run();
            return true;
        }
        return false;
    }

    static String testStatus() {
        return status;
    }

    void testSortByDistance() {
        sortByDistance();
        refilter();
    }

    void testGo(String key) {
        if (testSelect(key)) go(selected(), false);
    }

    /** The first entries of the list as shown: name and the distance label. */
    String testTop(int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < Math.min(count, shown.size()); i++) out.append(
            shown.get(i)
                .name())
            .append(' ')
            .append(distanceLabel(shown.get(i)))
            .append("; ");
        return out.toString();
    }

    /** How a capture went, for the status line when the browser shows again. */
    static void captured(String text) {
        status = text;
    }

    /** Asks the server for the nearest spot of the biome, or (next) the next one further out. */
    private static void go(Entry entry, boolean next) {
        Place place = goPlace(entry);
        if (place == null) return;
        BiomicNetwork.sendToServer(new BrowserMessages.Go(entry.key(), place, next));
        status = "Asking the server…";
    }

    /** Where Go searches for the biome, or null when this world has it nowhere. */
    private static Place goPlace(Entry entry) {
        Place chosen = goPlaces.get(entry.key());
        if (chosen != null && entry.places()
            .contains(chosen)) return chosen;
        return entry.places()
            .isEmpty() ? null
                : entry.places()
                    .iterator()
                    .next();
    }

    private int action(int x, int y, ActionSpec spec) {
        int w = fontRendererObj.getStringWidth(spec.label()) + 14;
        actions.add(new Action(x, y, w, spec.label(), spec.enabled(), spec.why(), spec.hint(), spec.run()));
        return x + w + 4;
    }

    private void capture() {
        Entry entry = selected();
        if (entry != null) mc.displayGuiScreen(new GuiCapture(entry.key(), entry.name(), this));
    }

    private static void copyKey(Entry entry) {
        setClipboardString(entry.key());
        status = "Copied " + entry.key();
    }

    // ------------------------------------------------------------------------------------------------------ input

    @Override
    protected void keyTyped(char character, int key) {
        if (composing != null && note.isFocused()) {
            if (key == Keyboard.KEY_ESCAPE) stopComposing();
            else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) commit();
            else note.textboxKeyTyped(character, key);
            return;
        }
        if (key == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            Entry entry = selected();
            if (entry != null) go(entry, isShiftKeyDown());
            return;
        }
        int index = shown.indexOf(selected());
        if (isCtrlKeyDown() && selected() != null) {
            Entry entry = selected();
            switch (key) {
                case Keyboard.KEY_N -> compose(Compose.NOTE);
                case Keyboard.KEY_I -> {
                    if (mc.thePlayer != null) compose(Compose.ISSUE);
                }
                case Keyboard.KEY_P -> capture();
                case Keyboard.KEY_K -> copyKey(entry);
                case Keyboard.KEY_S -> {
                    cycleSort();
                    refilter();
                    reveal();
                }
                default -> {}
            }
            if (key != Keyboard.KEY_F && key != Keyboard.KEY_A
                && key != Keyboard.KEY_C
                && key != Keyboard.KEY_V
                && key != Keyboard.KEY_X) return;
        }
        if (key == Keyboard.KEY_HOME || key == Keyboard.KEY_END) {
            if (!shown.isEmpty()) select(key == Keyboard.KEY_HOME ? 0 : shown.size() - 1);
            return;
        }
        if (key == Keyboard.KEY_DOWN || key == Keyboard.KEY_UP) {
            if (!shown.isEmpty())
                select(MathHelper.clamp_int(index + (key == Keyboard.KEY_DOWN ? 1 : -1), 0, shown.size() - 1));
            return;
        }
        if (key == Keyboard.KEY_NEXT || key == Keyboard.KEY_PRIOR) {
            int page = Math.max(1, (listBottom - listTop) / ROW - 1);
            if (!shown.isEmpty())
                select(MathHelper.clamp_int(index + (key == Keyboard.KEY_NEXT ? page : -page), 0, shown.size() - 1));
            return;
        }
        if (key == Keyboard.KEY_F && isCtrlKeyDown()) {
            Entry entry = selected();
            if (entry != null) {
                BrowserState.get()
                    .toggleFavorite(entry.key());
                refilter();
            }
            return;
        }
        if (search.textboxKeyTyped(character, key)) {
            query = search.getText();
            scroll = 0;
            refilter();
        }
    }

    private void select(int index) {
        selectedKey = shown.get(index)
            .key();
        int top = index * ROW, bottom = top + ROW, view = listBottom - listTop;
        if (top < scroll) scroll = top;
        else if (bottom > scroll + view) scroll = bottom - view;
        layoutActions();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll = MathHelper.clamp_int(scroll - Integer.signum(wheel) * ROW * 3, 0, maxScroll());
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        super.mouseClicked(mx, my, button);
        search.mouseClicked(mx, my, button);
        if (note != null) {
            note.mouseClicked(mx, my, button);
            if (note.isFocused()) search.setFocused(false);
        }
        for (Line line : new ArrayList<>(lines)) if (line.hit(mx, my)) {
            Runnable run = button == 0 ? line.left() : button == 1 ? line.right() : null;
            if (run != null) {
                run.run();
                return;
            }
        }
        Entry shownEntry = selected();
        if (button == 1 && shownEntry != null
            && thumbnail != null
            && mx >= thumbnail[0]
            && mx < thumbnail[2]
            && my >= thumbnail[1]
            && my < thumbnail[3]
            && BrowserThumbnails.captured(shownEntry.key())) {
            BrowserThumbnails.remove(shownEntry.key());
            status = "Removed the capture of " + shownEntry.name();
            refilter();
            return;
        }
        if (button != 0) return;
        for (Chip chip : new ArrayList<>(chips)) if (chip.hit(mx, my)) {
            chip.toggle()
                .run();
            return;
        }
        for (Chip chip : new ArrayList<>(placeChips)) if (chip.hit(mx, my)) {
            chip.toggle()
                .run();
            return;
        }
        for (Action action : new ArrayList<>(actions)) if (action.hit(mx, my) && action.enabled()) {
            action.run()
                .run();
            return;
        }
        if (mx >= listLeft && mx < listRight && my >= listTop && my < listBottom) {
            int index = (my - listTop + scroll) / ROW;
            if (index >= 0 && index < shown.size()) {
                Entry entry = shown.get(index);
                // A click on the star toggles the favourite; anywhere else selects.
                if (mx < listLeft + 50 && mx >= listLeft + 38) {
                    BrowserState.get()
                        .toggleFavorite(entry.key());
                    refilter();
                } else select(index);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------- drawing

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        hoverText = null;
        drawDefaultBackground();
        drawTopBar(mx, my);
        drawList(mx, my);
        drawDetail(mx, my);
        for (Chip chip : chips) drawChip(chip, mx, my);
        if (hoverText != null)
            drawHoveringText(java.util.Arrays.asList(hoverText.split("\n")), mx, my, fontRendererObj);
    }

    private void drawTopBar(int mx, int my) {
        drawRect(0, 0, width, top - 4, PANEL);
        drawRect(0, top - 5, width, top - 4, BORDER);
        fontRendererObj.drawStringWithShadow("Biome browser", MARGIN, 13, ACCENT);
        search.drawTextBox();
        if (query.isEmpty() && !search.isFocused())
            fontRendererObj.drawString("search", search.xPosition + 4, search.yPosition + 3, MUTED);
        if (catalogue != null) {
            // How many the filters leave, at the search box's end while the typing leaves room.
            String count = shown.size() + " of " + catalogue.size();
            int countX = search.xPosition + search.width - 4 - fontRendererObj.getStringWidth(count);
            if (search.xPosition + 8 + fontRendererObj.getStringWidth(query) < countX - 6)
                fontRendererObj.drawString(count, countX, search.yPosition + 3, MUTED);
        }
        String here = here();
        if (here != null) {
            String text = "Here: " + here;
            int x = width - MARGIN - fontRendererObj.getStringWidth(text);
            if (x > (chipsY == 11 ? chipsEnd : search.xPosition + search.width) + 6)
                fontRendererObj.drawString(text, x, 13, MUTED);
        }
    }

    /** The biome at the player's feet: its 3D identity, so a cave or an island shows as itself. */
    private String here() {
        if (mc.thePlayer == null || mc.theWorld == null) return null;
        int x = MathHelper.floor_double(mc.thePlayer.posX), z = MathHelper.floor_double(mc.thePlayer.posZ);
        BiomeGenBase biome = BiomicBiomeApi
            .getBiome(mc.theWorld, x, MathHelper.floor_double(mc.thePlayer.boundingBox.minY), z);
        return biome == null ? null : biome.biomeName;
    }

    private void drawChip(Chip chip, int mx, int my) {
        int background = chip.on() ? (chip.color() & 0x00FFFFFF | 0x90000000) : chip.hit(mx, my) ? PANEL_LIGHT : PANEL;
        drawRect(chip.x(), chip.y(), chip.x() + chip.w(), chip.y() + 12, background);
        drawRect(chip.x(), chip.y() + 11, chip.x() + chip.w(), chip.y() + 12, chip.color());
        fontRendererObj.drawString(chip.label(), chip.x() + 5, chip.y() + 2, chip.on() ? TEXT : MUTED);
    }

    private void drawList(int mx, int my) {
        drawRect(listLeft - 2, listTop - 2, listRight + 2, listBottom + 2, PANEL);
        if (catalogue == null) {
            String text = denied != null ? denied : "Asking the server for its biomes…";
            List<String> lines = fontRendererObj.listFormattedStringToWidth(text, listRight - listLeft - 12);
            for (int i = 0; i < lines.size(); i++)
                fontRendererObj.drawString(lines.get(i), listLeft + 6, listTop + 8 + i * 10, MUTED);
            return;
        }
        if (shown.isEmpty()) {
            fontRendererObj.drawString("No biome matches.", listLeft + 6, listTop + 8, MUTED);
            return;
        }
        BrowserState state = BrowserState.get();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        scissor(listLeft - 2, listTop, listRight + 2, listBottom);
        int first = scroll / ROW;
        for (int index = first; index < shown.size(); index++) {
            int y = listTop + index * ROW - scroll;
            if (y > listBottom) break;
            Entry entry = shown.get(index);
            boolean selected = entry.key()
                .equals(selectedKey);
            boolean hover = mx >= listLeft && mx < listRight
                && my >= Math.max(y, listTop)
                && my < Math.min(y + ROW, listBottom);
            if (selected) {
                drawRect(listLeft, y, listRight, y + ROW, ACCENT_DIM);
                drawRect(listLeft, y, listLeft + 2, y + ROW, ACCENT);
            } else if (hover) drawRect(listLeft, y, listRight, y + ROW, PANEL_LIGHT);
            drawSwatch(entry, listLeft + 4, y + 2, 32, 18);
            boolean favorite = state.favorite(entry.key());
            fontRendererObj.drawString(STAR_GLYPH, listLeft + 40, y + 7, favorite ? STAR : 0xFF333C46);
            String source = sort == Sort.DISTANCE ? distanceLabel(entry) : shortSource(entry.source());
            int sourceWidth = fontRendererObj.getStringWidth(source);
            String name = fontRendererObj.trimStringToWidth(entry.name(), listRight - listLeft - 64 - sourceWidth);
            int nameColor = entry.places()
                .isEmpty() ? DISABLED : selected ? TEXT : 0xFFC9D3DC;
            fontRendererObj.drawString(name, listLeft + 52, y + 7, nameColor);
            fontRendererObj.drawString(source, listRight - 6 - sourceWidth, y + 7, MUTED);
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (maxScroll() > 0) {
            int view = listBottom - listTop, total = shown.size() * ROW;
            int barHeight = Math.max(12, view * view / total),
                barTop = listTop + (view - barHeight) * scroll / maxScroll();
            drawRect(listRight - 1, barTop, listRight + 1, barTop + barHeight, BORDER);
        }
    }

    /** The biome's thumbnail as a small card, or its colour while it has none. */
    private void drawSwatch(Entry entry, int x, int y, int w, int h) {
        ResourceLocation small = BrowserThumbnails.small(entry.key());
        if (small != null) {
            drawTexture(small, x, y, w, h);
            return;
        }
        int color = biomeColor(entry);
        drawGradientRect(x, y, x + w, y + h, 0xFF000000 | lighten(color), 0xFF000000 | darken(color));
        drawRect(x, y + h - 1, x + w, y + h, 0x60000000);
    }

    /**
     * The selected biome: its thumbnail (up to about half the height), name and places first, then as much of the rest
     * as fits (key, notes, plants, climate, types), above the status line, or the line being written, and the buttons.
     */
    private void drawDetail(int mx, int my) {
        drawRect(detailLeft - 2, listTop - 2, detailRight + 2, height - MARGIN + 2, PANEL);
        Entry entry = selected();
        if (entry == null) return;
        int x = detailLeft + 10, width = detailRight - detailLeft - 20, y = listTop + 8;
        int limit = actionsTop - (composing != null ? 38 : 16);
        // The name, badges and key line take 43; a biome's notes and issues keep room for two lines under them.
        BrowserState state = BrowserState.get();
        int annotations = state.notes(entry.key())
            .size()
            + state.issues(entry.key())
                .size();
        int notesRoom = annotations == 0 ? 0 : 11 + Math.min(annotations, 2) * 10 + 4;
        int thumbHeight = Math.min(Math.min(width * 9 / 16, (limit - y) * 45 / 100), limit - y - 43 - notesRoom - 6);
        thumbnail = null;
        if (thumbHeight >= 36) {
            drawThumbnail(entry, x, y, thumbHeight * 16 / 9, thumbHeight, mx, my);
            y += thumbHeight + 6;
        }
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(1.5F, 1.5F, 1);
        fontRendererObj
            .drawStringWithShadow(fontRendererObj.trimStringToWidth(entry.name(), width * 2 / 3), 0, 0, TEXT);
        GL11.glPopMatrix();
        y += 15;
        drawBadges(entry, x, y, mx, my);
        y += 16;
        if (y + 10 <= limit) {
            Integer distance = distance(entry);
            String away = distance == null ? ""
                : distance < 0 ? "  \u00b7  none within reach"
                    : String.format(Locale.ROOT, "  \u00b7  %,d blocks away", distance)
                        + (goPlace(entry) == Place.NETHER ? " in the Nether" : "");
            fontRendererObj.drawString(
                fontRendererObj.trimStringToWidth(entry.key() + "  \u00b7  id " + entry.id() + away, width),
                x,
                y,
                MUTED);
            y += 12;
        }
        y = drawNotes(entry, x, y, width, limit, mx, my);
        if (!entry.plants()
            .isEmpty() && y + 29 <= limit) {
            fontRendererObj.drawString("Plants", x, y, MUTED);
            drawPlants(entry, x, y + 10, mx, my);
            y += 31;
        }
        if (y + 10 <= limit) {
            fontRendererObj.drawString(
                String.format(
                    Locale.ROOT,
                    "Temperature %.2f  \u00b7  Rainfall %.2f",
                    entry.temperature(),
                    entry.rainfall()),
                x,
                y,
                0xFFC9D3DC);
            y += 11;
        }
        if (!entry.types()
            .isEmpty())
            for (String line : fontRendererObj.listFormattedStringToWidth(String.join(", ", entry.types()), width)) {
                if (y + 10 > limit) break;
                fontRendererObj.drawString(line, x, y, MUTED);
                y += 10;
            }
        if (composing != null) {
            String prompt = composing == Compose.NOTE ? "Note on " + entry.name() + " (Enter saves, Esc drops):"
                : "Issue in " + issueEntry
                    .name() + " at " + issueAt[0] + " " + issueAt[1] + " " + issueAt[2] + " (Enter saves, Esc drops):";
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(prompt, width), x, actionsTop - 32, TEXT);
            note.yPosition = actionsTop - 20;
            note.drawTextBox();
        } else if (status != null) {
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(status, width), x, actionsTop - 12, MUTED);
            if (mx >= x && mx < x + width && my >= actionsTop - 12 && my < actionsTop - 2) hoverText = status;
        }
        for (Action action : actions) drawAction(action, mx, my);
    }

    private void drawThumbnail(Entry entry, int x, int y, int w, int h, int mx, int my) {
        thumbnail = new int[] { x, y, x + w, y + h };
        ResourceLocation large = BrowserThumbnails.large(entry.key());
        if (large != null) {
            drawTexture(large, x, y, w, h);
            if (mx >= x && mx < x + w && my >= y && my < y + h) hoverText = BrowserThumbnails.captured(entry.key())
                ? "Your capture: Capture replaces it, right-click removes it"
                : "The pack's thumbnail: Capture puts your own over it";
            return;
        }
        int color = biomeColor(entry);
        drawGradientRect(x, y, x + w, y + h, 0xFF000000 | lighten(color), 0xFF000000 | darken(darken(color)));
        drawRect(x, y, x + w, y + 1, 0x30FFFFFF);
        List<String> none = fontRendererObj.listFormattedStringToWidth("No thumbnail yet: Capture takes one", w - 8);
        // A small card says less, a tiny one nothing.
        if (none.size() * 10 > h - 4) none = fontRendererObj.listFormattedStringToWidth("No thumbnail", w - 8);
        if (none.size() * 10 > h - 4) none = List.of();
        int ty = y + (h - none.size() * 10) / 2;
        for (String line : none) {
            fontRendererObj
                .drawStringWithShadow(line, x + (w - fontRendererObj.getStringWidth(line)) / 2, ty, 0xC0FFFFFF);
            ty += 10;
        }
    }

    /** The source, and the places: a switch for where Go searches when there are several. */
    private void drawBadges(Entry entry, int x, int y, int mx, int my) {
        int bx = badge(x, y, shortSource(entry.source()), ACCENT);
        if (entry.places()
            .isEmpty()) badge(bx, y, "Not in this world", DISABLED);
        placeChips.clear();
        Place goPlace = goPlace(entry);
        for (Place place : entry.places()) {
            String label = placeLabel(place);
            if (entry.places()
                .size() == 1) {
                bx = badge(bx, y, label, PLACE_COLORS.get(place));
                continue;
            }
            Chip chip = new Chip(
                bx,
                y,
                fontRendererObj.getStringWidth(label) + 10,
                label,
                PLACE_COLORS.get(place),
                place == goPlace,
                () -> {
                    goPlaces.put(entry.key(), place);
                    remember();
                    if (sort == Sort.DISTANCE) refilter();
                });
            placeChips.add(chip);
            drawChip(chip, mx, my);
            if (chip.hit(mx, my))
                hoverText = "Go searches " + label.toLowerCase(Locale.ROOT) + (place == goPlace ? "" : " (click)");
            bx += chip.w() + 4;
        }
    }

    private void drawPlants(Entry entry, int x, int y, int mx, int my) {
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        int px = x;
        for (String plant : entry.plants()) {
            ItemStack stack = stack(plant);
            drawRect(px, y, px + 18, y + 18, PANEL_LIGHT);
            if (stack != null) {
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, px + 1, y + 1);
                if (mx >= px && mx < px + 18 && my >= y && my < y + 18) hoverText = stack.getDisplayName();
            } else if (mx >= px && mx < px + 18 && my >= y && my < y + 18) hoverText = plant + " (not installed)";
            px += 20;
        }
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        RenderHelper.disableStandardItemLighting();
    }

    /** A palette plant ("mod:block" or "mod:block:meta") as an item stack, or null when its mod is absent. */
    private static ItemStack stack(String plant) {
        String[] parts = plant.split(":");
        if (parts.length < 2) return null;
        Block block = Block.getBlockFromName(parts[0] + ":" + parts[1]);
        Item item = block == null ? null : Item.getItemFromBlock(block);
        if (item == null) return null;
        int meta = 0;
        if (parts.length > 2) try {
            meta = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ignored) {}
        return new ItemStack(item, 1, meta);
    }

    private int badge(int x, int y, String label, int color) {
        int w = fontRendererObj.getStringWidth(label) + 8;
        drawRect(x, y, x + w, y + 12, color & 0x00FFFFFF | 0x50000000);
        drawRect(x, y, x + 2, y + 12, color);
        fontRendererObj.drawString(label, x + 5, y + 2, TEXT);
        return x + w + 4;
    }

    private void drawAction(Action action, int mx, int my) {
        boolean hover = action.hit(mx, my);
        int background = !action.enabled() ? PANEL : hover ? ACCENT_DIM : PANEL_LIGHT;
        drawRect(action.x(), action.y(), action.x() + action.w(), action.y() + 16, background);
        drawRect(
            action.x(),
            action.y() + 15,
            action.x() + action.w(),
            action.y() + 16,
            action.enabled() ? ACCENT : BORDER);
        fontRendererObj.drawString(action.label(), action.x() + 7, action.y() + 4, action.enabled() ? TEXT : DISABLED);
        if (hover) hoverText = !action.enabled() ? action.why() : action.hint();
    }

    private void drawTexture(ResourceLocation texture, int x, int y, int w, int h) {
        GL11.glColor4f(1, 1, 1, 1);
        mc.getTextureManager()
            .bindTexture(texture);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + h, zLevel, 0, 1);
        tessellator.addVertexWithUV(x + w, y + h, zLevel, 1, 1);
        tessellator.addVertexWithUV(x + w, y, zLevel, 1, 0);
        tessellator.addVertexWithUV(x, y, zLevel, 0, 0);
        tessellator.draw();
    }

    private void scissor(int left, int top, int right, int bottom) {
        int scale = new net.minecraft.client.gui.ScaledResolution(mc, mc.displayWidth, mc.displayHeight)
            .getScaleFactor();
        GL11.glScissor(left * scale, mc.displayHeight - bottom * scale, (right - left) * scale, (bottom - top) * scale);
    }

    private static int biomeColor(Entry entry) {
        BiomeGenBase[] all = BiomeGenBase.getBiomeGenArray();
        BiomeGenBase biome = entry.id() >= 0 && entry.id() < all.length ? all[entry.id()] : null;
        return biome == null ? 0x405060 : biome.color & 0xFFFFFF;
    }

    private static int lighten(int color) {
        return scale(color, 1.15F, 24);
    }

    private static int darken(int color) {
        return scale(color, 0.55F, 0);
    }

    private static int scale(int color, float factor, int add) {
        int r = Math.min(255, (int) ((color >> 16 & 255) * factor) + add);
        int g = Math.min(255, (int) ((color >> 8 & 255) * factor) + add);
        int b = Math.min(255, (int) ((color & 255) * factor) + add);
        return r << 16 | g << 8 | b;
    }

    private static String shortSource(String source) {
        return switch (source) {
            case "Regions Unexplored" -> "RU";
            case "Biomes O' Plenty" -> "BOP";
            case "Gardens of the Dead" -> "Gardens";
            default -> source;
        };
    }
}
