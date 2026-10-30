package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

/** Generation-owned trees awaiting neighboring Origen mushroom sources. Never scans or loads world chunks. */
final class HostedTreeLedger {

    // Pinned large_mixed_mushroom_procedural: height <=25, cap half-width <=19,
    // bend <=10. Nested disks reach less than 21. Keep a conservative 48-block halo.
    static final int SOURCE_REACH = 48;
    private final Map<Long, Tree> trees = new LinkedHashMap<>();
    private final Map<Pos, Cell> cells = new LinkedHashMap<>();
    private final Map<Long, Set<Long>> waiting = new HashMap<>();
    private final Map<Long, Set<Long>> spatial = new HashMap<>();
    private long nextId;

    interface Access {

        String read(Pos pos);

        void write(Pos pos, String state);
    }

    record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        boolean contains(Pos p) {
            return p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY && p.z >= minZ && p.z <= maxZ;
        }
    }

    private static final class Tree {

        final Bounds bounds;
        final Set<Pos> positions = new LinkedHashSet<>();
        final Set<Long> pending = new LinkedHashSet<>();

        Tree(Bounds bounds) {
            this.bounds = bounds;
        }
    }

    private static final class Cell {

        final String base;
        Layer first, last;
        int size;

        Cell(String base) {
            this.base = base;
        }

        String top() {
            return last == null ? base : last.state;
        }

        Layer find(long id) {
            for (Layer l = first; l != null; l = l.next) if (l.id == id) return l;
            return null;
        }

        void add(long id, String state) {
            Layer layer = new Layer(id, state);
            if (last == null) first = layer;
            else last.next = layer;
            last = layer;
            size++;
        }

        void remove(long id) {
            Layer previous = null;
            for (Layer layer = first; layer != null; layer = layer.next) {
                if (layer.id == id) {
                    if (previous == null) first = layer.next;
                    else previous.next = layer.next;
                    if (last == layer) last = previous;
                    size--;
                    return;
                }
                previous = layer;
            }
        }
    }

    /** Almost every cell has one owner; a linked chain avoids a hash table and boxed id per cell. */
    private static final class Layer {

        final long id;
        final String state;
        Layer next;

        Layer(long id, String state) {
            this.id = id;
            this.state = state;
        }
    }

    static Bounds bounds(Collection<Pos> positions) {
        if (positions.isEmpty() || positions.size() > 32768)
            throw new IllegalArgumentException("Unbounded/empty native tree");
        int lx = Integer.MAX_VALUE, ly = lx, lz = lx, hx = Integer.MIN_VALUE, hy = hx, hz = hx;
        for (Pos p : positions) {
            lx = Math.min(lx, p.x);
            ly = Math.min(ly, p.y);
            lz = Math.min(lz, p.z);
            hx = Math.max(hx, p.x);
            hy = Math.max(hy, p.y);
            hz = Math.max(hz, p.z);
        }
        if (hx - lx > 112 || hz - lz > 112 || hy - ly > 128)
            throw new IllegalArgumentException("Native tree envelope exceeds capture bound");
        return new Bounds(lx - 1, ly, lz - 1, hx + 1, hy + 1, hz + 1);
    }

    static boolean mushroom(String state) {
        int split = state.indexOf('[');
        String name = split < 0 ? state : state.substring(0, split);
        return name.equals("minecraft:red_mushroom_block") || name.equals("minecraft:brown_mushroom_block")
            || name.equals("minecraft:mushroom_stem")
            || name.equals("biomic:mushroom_flesh");
    }

    static boolean clear(Bounds b, Predicate<Pos> mushroom) {
        if ((long) (b.maxX - b.minX + 1) * (b.maxY - b.minY + 1) * (b.maxZ - b.minZ + 1) > 131072) return false;
        for (int x = b.minX; x <= b.maxX; x++) for (int z = b.minZ; z <= b.maxZ; z++)
            for (int y = b.minY; y <= b.maxY; y++) if (mushroom.test(new Pos(x, y, z))) return false;
        return true;
    }

    /** Writes are canonical lossless states; caller supplies only fully accepted tree transactions. */
    void commit(Map<Pos, String> writes, Access access, BiPredicate<Integer, Integer> sourceComplete) {
        if (nextId >= Long.MAX_VALUE - 1) throw new IllegalStateException("Hosted tree journal id space exhausted");
        Bounds bounds = bounds(writes.keySet());
        Tree tree = new Tree(bounds);
        long id = ++nextId;
        for (int x = (bounds.minX - SOURCE_REACH) >> 4; x <= (bounds.maxX + SOURCE_REACH) >> 4; x++)
            for (int z = (bounds.minZ - SOURCE_REACH) >> 4; z <= (bounds.maxZ + SOURCE_REACH) >> 4; z++)
                if (!sourceComplete.test(x, z)) tree.pending.add(chunk(x, z));
        for (var entry : writes.entrySet()) {
            Pos p = entry.getKey();
            String before = access.read(p);
            Cell cell = cells.get(p);
            if (cell != null && !cell.top()
                .equals(before)) {
                forget(p);
                cell = null;
            }
            if (cell == null) {
                cell = new Cell(before);
                cells.put(p, cell);
            }
            cell.add(id, entry.getValue());
            tree.positions.add(p);
            access.write(p, entry.getValue());
        }
        trees.put(id, tree);
        index(id, tree);
        if (tree.pending.isEmpty()) retire(id);
    }

    /** A later non-tree writer owns this cell. A rollback must never resurrect its earlier contents. */
    void forget(Pos p) {
        Cell cell = cells.remove(p);
        if (cell == null) return;
        for (Layer layer = cell.first; layer != null; layer = layer.next) {
            Tree tree = trees.get(layer.id);
            if (tree != null) tree.positions.remove(p);
        }
    }

    int mushroomAt(Pos pos, Access access) {
        Set<Long> candidates = spatial.get(chunk(pos.x >> 4, pos.z >> 4));
        if (candidates == null) return 0;
        int removed = 0;
        for (long id : new ArrayList<>(candidates)) {
            Tree tree = trees.get(id);
            if (tree == null || !tree.bounds.contains(pos)) continue;
            for (Pos p : new ArrayList<>(tree.positions)) {
                Cell cell = cells.get(p);
                if (cell == null) continue;
                String actual = access.read(p), previous = cell.top();
                cell.remove(id);
                if (!actual.equals(previous)) {
                    forget(p);
                    continue;
                }
                String replacement = cell.top();
                if (!previous.equals(replacement)) access.write(p, replacement);
                if (cell.size == 0) cells.remove(p);
            }
            remove(id, tree);
            removed++;
        }
        return removed;
    }

    void sourceComplete(int x, int z) {
        long key = chunk(x, z);
        Set<Long> ids = waiting.remove(key);
        if (ids == null) return;
        for (long id : new ArrayList<>(ids)) {
            Tree tree = trees.get(id);
            if (tree == null) continue;
            tree.pending.remove(key);
            if (tree.pending.isEmpty()) retire(id);
        }
    }

    private void index(long id, Tree tree) {
        for (long key : tree.pending) waiting.computeIfAbsent(key, k -> new LinkedHashSet<>())
            .add(id);
        Bounds b = tree.bounds;
        for (int x = b.minX >> 4; x <= b.maxX >> 4; x++) for (int z = b.minZ >> 4; z <= b.maxZ >> 4; z++)
            spatial.computeIfAbsent(chunk(x, z), k -> new LinkedHashSet<>())
                .add(id);
    }

    private void remove(long id, Tree tree) {
        trees.remove(id);
        for (long key : tree.pending) removeIndex(waiting, key, id);
        Bounds b = tree.bounds;
        for (int x = b.minX >> 4; x <= b.maxX >> 4; x++)
            for (int z = b.minZ >> 4; z <= b.maxZ >> 4; z++) removeIndex(spatial, chunk(x, z), id);
    }

    private static void removeIndex(Map<Long, Set<Long>> index, long key, long id) {
        Set<Long> ids = index.get(key);
        if (ids != null) {
            ids.remove(id);
            if (ids.isEmpty()) index.remove(key);
        }
    }

    /** Finished trees are permanent: bake through their layer, retaining any newer unfinished tree layers. */
    private void retire(long id) {
        Tree tree = trees.get(id);
        if (tree == null) return;
        for (Pos p : new ArrayList<>(tree.positions)) {
            Cell cell = cells.get(p);
            if (cell == null) continue;
            Layer settled = cell.find(id);
            if (settled == null) continue;
            Cell next = new Cell(settled.state);
            boolean after = false;
            for (Layer layer = cell.first; layer != null; layer = layer.next) {
                if (after) next.add(layer.id, layer.state);
                else {
                    Tree old = trees.get(layer.id);
                    if (old != null && old != tree) old.positions.remove(p);
                }
                if (layer.id == id) after = true;
            }
            if (next.size == 0) cells.remove(p);
            else cells.put(p, next);
        }
        remove(id, tree);
    }

    int treeCount() {
        return trees.size();
    }

    private static long chunk(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    NBTTagCompound save() {
        NBTTagCompound root = new NBTTagCompound();
        root.setLong("next", nextId);
        NBTTagList entries = new NBTTagList();
        for (var entry : trees.entrySet()) {
            Tree t = entry.getValue();
            Bounds b = t.bounds;
            NBTTagCompound n = new NBTTagCompound();
            n.setLong("id", entry.getKey());
            n.setIntArray("bounds", new int[] { b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ });
            int[] pending = new int[t.pending.size() * 2];
            int i = 0;
            for (long key : t.pending) {
                pending[i++] = (int) (key >> 32);
                pending[i++] = (int) key;
            }
            n.setIntArray("pending", pending);
            entries.appendTag(n);
        }
        root.setTag("trees", entries);
        root.setInteger("cell_format", 2);
        root.setInteger("cell_count", cells.size());
        Map<Long, Integer> treeIndices = new HashMap<>();
        for (long id : trees.keySet()) treeIndices.put(id, treeIndices.size());
        Map<String, Integer> palette = new LinkedHashMap<>();
        JournalIntPages blocks = new JournalIntPages();
        for (var entry : cells.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            blocks.add(p.x);
            blocks.add(p.y);
            blocks.add(p.z);
            blocks.add(palette.computeIfAbsent(c.base, k -> palette.size()));
            blocks.add(c.size);
            for (Layer layer = c.first; layer != null; layer = layer.next) {
                blocks.add(treeIndices.get(layer.id));
                blocks.add(palette.computeIfAbsent(layer.state, k -> palette.size()));
            }
        }
        NBTTagList states = new NBTTagList();
        for (String state : palette.keySet()) states.appendTag(new NBTTagString(state));
        root.setTag("cell_palette", states);
        root.setTag("cell_pages", blocks.finish());
        return root;
    }

    void load(NBTTagCompound root) {
        boolean formatTag = root.hasKey("cell_format"),
            compactKeys = root.hasKey("cell_count") || root.hasKey("cell_palette") || root.hasKey("cell_pages");
        if (formatTag && !root.hasKey("cell_format", 3) || compactKeys && !root.hasKey("cell_format", 3))
            throw new IllegalStateException("Malformed hosted tree journal format marker");
        int format = root.getInteger("cell_format");
        if (format != 0 && format != 2)
            throw new IllegalStateException("Unsupported hosted tree journal format: " + format);
        if (format == 0 && compactKeys)
            throw new IllegalStateException("Compact hosted tree journal lacks its format marker");
        if (root.hasKey("next") && !root.hasKey("next", 4))
            throw new IllegalStateException("Malformed hosted tree journal id");
        trees.clear();
        cells.clear();
        waiting.clear();
        spatial.clear();
        nextId = root.getLong("next");
        if (nextId < 0 || nextId == Long.MAX_VALUE) throw new IllegalStateException("Invalid hosted tree next id");
        if (root.hasKey("trees") && !root.hasKey("trees", 9))
            throw new IllegalStateException("Malformed hosted tree list");
        NBTTagList entries = root.getTagList("trees", 10);
        long previousTreeId = 0;
        for (int i = 0; i < entries.tagCount(); i++) {
            NBTTagCompound n = entries.getCompoundTagAt(i);
            int[] b = n.getIntArray("bounds"), pending = n.getIntArray("pending");
            if (b.length != 6 || pending.length % 2 != 0)
                throw new IllegalStateException("Malformed hosted tree journal");
            if (b[3] < b[0] || b[4] < b[1]
                || b[5] < b[2]
                || (long) b[3] - b[0] > 114
                || (long) b[4] - b[1] > 129
                || (long) b[5] - b[2] > 114
                || pending.length > 450) throw new IllegalStateException("Unbounded hosted tree journal");
            Tree tree = new Tree(new Bounds(b[0], b[1], b[2], b[3], b[4], b[5]));
            for (int j = 0; j < pending.length; j += 2) tree.pending.add(chunk(pending[j], pending[j + 1]));
            long id = n.getLong("id");
            if (id <= previousTreeId || id > nextId || trees.put(id, tree) != null)
                throw new IllegalStateException("Invalid hosted tree id/order");
            previousTreeId = id;
            index(id, tree);
        }
        if (format == 2) {
            loadCompactCells(root);
            return;
        }
        if (root.hasKey("cells") && !root.hasKey("cells", 9) || !trees.isEmpty() && !root.hasKey("cells", 9))
            throw new IllegalStateException("Malformed legacy hosted tree cells");
        NBTTagList blocks = root.getTagList("cells", 10);
        for (int i = 0; i < blocks.tagCount(); i++) {
            NBTTagCompound n = blocks.getCompoundTagAt(i);
            int[] xyz = n.getIntArray("pos");
            if (xyz.length != 3) throw new IllegalStateException("Malformed tree cell");
            String base = n.getString("base");
            if (base.isEmpty()) throw new IllegalStateException("Empty hosted tree base state");
            if (!n.hasKey("layers", 9)) throw new IllegalStateException("Malformed hosted tree layers");
            Pos p = new Pos(xyz[0], xyz[1], xyz[2]);
            Cell cell = new Cell(base);
            NBTTagList layers = n.getTagList("layers", 10);
            if (layers.tagCount() == 0) throw new IllegalStateException("Empty hosted tree layer chain");
            long previousLayerId = 0;
            for (int j = 0; j < layers.tagCount(); j++) {
                NBTTagCompound l = layers.getCompoundTagAt(j);
                long id = l.getLong("id");
                Tree tree = trees.get(id);
                if (tree == null || !tree.bounds.contains(p))
                    throw new IllegalStateException("Orphan hosted tree cell");
                String state = l.getString("state");
                if (id <= previousLayerId || state.isEmpty())
                    throw new IllegalStateException("Invalid hosted tree cell layer/order");
                previousLayerId = id;
                cell.add(id, state);
                tree.positions.add(p);
            }
            if (cells.put(p, cell) != null) throw new IllegalStateException("Duplicate hosted tree cell");
        }
    }

    private void loadCompactCells(NBTTagCompound root) {
        if (!root.hasKey("cell_count", 3) || !root.hasKey("cell_palette", 9) || !root.hasKey("cell_pages", 9))
            throw JournalIntPages.malformed();
        NBTTagList palette = root.getTagList("cell_palette", 8);
        JournalIntPages.Reader blocks = new JournalIntPages.Reader(root.getTagList("cell_pages", 11));
        int count = root.getInteger("cell_count");
        if (count < 0 || (long) count * 7 > blocks.remaining()) throw JournalIntPages.malformed();
        List<Long> ids = new ArrayList<>(trees.keySet());
        for (int i = 0; i < count; i++) {
            Pos p = new Pos(blocks.next(), blocks.next(), blocks.next());
            Cell cell = new Cell(paletteState(palette, blocks.next()));
            int layers = blocks.next();
            if (layers <= 0 || layers > ids.size() || (long) layers * 2 > blocks.remaining())
                throw JournalIntPages.malformed();
            int previousTreeIndex = -1;
            for (int j = 0; j < layers; j++) {
                int treeIndex = blocks.next();
                String state = paletteState(palette, blocks.next());
                if (treeIndex <= previousTreeIndex || treeIndex >= ids.size()) throw JournalIntPages.malformed();
                previousTreeIndex = treeIndex;
                long id = ids.get(treeIndex);
                Tree tree = trees.get(id);
                if (!tree.bounds.contains(p) || cell.find(id) != null) throw JournalIntPages.malformed();
                cell.add(id, state);
                tree.positions.add(p);
            }
            if (cells.put(p, cell) != null) throw JournalIntPages.malformed();
        }
        if (blocks.remaining() != 0) throw JournalIntPages.malformed();
    }

    private static String paletteState(NBTTagList palette, int index) {
        if (index < 0 || index >= palette.tagCount()) throw JournalIntPages.malformed();
        String state = palette.getStringTagAt(index);
        if (state.isEmpty()) throw JournalIntPages.malformed();
        return state;
    }
}
