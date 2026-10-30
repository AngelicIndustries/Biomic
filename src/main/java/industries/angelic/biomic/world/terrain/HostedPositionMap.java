package industries.angelic.biomic.world.terrain;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

/**
 * Insertion ordered transaction map with full-int coordinate probes, without temporary Pos keys.
 * Coordinates are compared individually: no world-border or height packing assumption is made.
 * Stored keys remain the original immutable Pos objects consumed by publication and audit code.
 */
final class HostedPositionMap extends AbstractMap<Pos, String> {

    private Node[] table;
    private Node first, last;
    private int size, modifications;
    /** Upper bound of every stored Y (never lowered by removal). */
    private int maxY = Integer.MIN_VALUE;
    private Set<Map.Entry<Pos, String>> entries;

    private static final class Node extends SimpleEntry<Pos, String> {

        Node previous, next;

        Node(Pos key, String value) {
            super(key, value);
        }
    }

    private static int hash(int x, int y, int z) {
        int h = x * 0x9e3779b9 + Integer.rotateLeft(y * 0x85ebca6b, 11) + Integer.rotateLeft(z * 0xc2b2ae35, 22);
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        return h ^ (h >>> 13);
    }

    private int slot(int x, int y, int z) {
        int slot = hash(x, y, z) & (table.length - 1);
        while (table[slot] != null) {
            Pos key = table[slot].getKey();
            if (key.x == x && key.y == y && key.z == z) break;
            slot = (slot + 1) & (table.length - 1);
        }
        return slot;
    }

    private Node find(int x, int y, int z) {
        return table == null ? null : table[slot(x, y, z)];
    }

    int maxY() {
        return maxY;
    }

    String get(int x, int y, int z) {
        Node node = find(x, y, z);
        return node == null ? null : node.getValue();
    }

    boolean containsKey(int x, int y, int z) {
        return find(x, y, z) != null;
    }

    @Override
    public String get(Object key) {
        return key instanceof Pos p ? get(p.x, p.y, p.z) : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof Pos p && containsKey(p.x, p.y, p.z);
    }

    @Override
    public int size() {
        return size;
    }

    private void rebuild(int capacity) {
        table = new Node[capacity];
        for (Node node = first; node != null; node = node.next) {
            Pos key = node.getKey();
            table[slot(key.x, key.y, key.z)] = node;
        }
    }

    @Override
    public String put(Pos key, String value) {
        java.util.Objects.requireNonNull(key, "position");
        if (table == null) rebuild(16);
        int slot = slot(key.x, key.y, key.z);
        if (table[slot] != null) return table[slot].setValue(value);
        if ((size + 1) * 2 > table.length) {
            rebuild(table.length * 2);
            slot = slot(key.x, key.y, key.z);
        }
        Node node = new Node(key, value);
        table[slot] = node;
        if (key.y > maxY) maxY = key.y;
        node.previous = last;
        if (last == null) first = node;
        else last.next = node;
        last = node;
        size++;
        modifications++;
        return null;
    }

    @Override
    public String remove(Object key) {
        if (!(key instanceof Pos p)) return null;
        Node node = find(p.x, p.y, p.z);
        if (node == null) return null;
        removeNode(node);
        return node.getValue();
    }

    private void removeNode(Node node) {
        if (node.previous == null) first = node.next;
        else node.previous.next = node.next;
        if (node.next == null) last = node.previous;
        else node.next.previous = node.previous;
        size--;
        modifications++;
        // Removal is uncommon for population transactions. Rebuild closes probe clusters correctly.
        rebuild(table.length);
    }

    @Override
    public void clear() {
        if (size != 0) modifications++;
        table = null;
        first = last = null;
        size = 0;
    }

    @Override
    public Set<Map.Entry<Pos, String>> entrySet() {
        if (entries == null) entries = new AbstractSet<>() {

            @Override
            public int size() {
                return HostedPositionMap.this.size;
            }

            @Override
            public void clear() {
                HostedPositionMap.this.clear();
            }

            @Override
            public Iterator<Map.Entry<Pos, String>> iterator() {
                return new Iterator<>() {

                    Node next = first, returned;
                    int expected = modifications;

                    private void check() {
                        if (expected != modifications) throw new ConcurrentModificationException();
                    }

                    @Override
                    public boolean hasNext() {
                        return next != null;
                    }

                    @Override
                    public Map.Entry<Pos, String> next() {
                        check();
                        if (next == null) throw new NoSuchElementException();
                        returned = next;
                        next = next.next;
                        return returned;
                    }

                    @Override
                    public void remove() {
                        check();
                        if (returned == null) throw new IllegalStateException();
                        removeNode(returned);
                        returned = null;
                        expected = modifications;
                    }
                };
            }
        };
        return entries;
    }

    static HostedPositionMap containing(java.util.Deque<HostedPositionMap> maps, int x, int y, int z) {
        HostedPositionMap newest = maps.peekFirst();
        if (newest == null || newest.containsKey(x, y, z)) return newest;
        if (maps.size() == 1) return null;
        Iterator<HostedPositionMap> iterator = maps.iterator();
        iterator.next();
        while (iterator.hasNext()) {
            HostedPositionMap candidate = iterator.next();
            if (candidate.containsKey(x, y, z)) return candidate;
        }
        return null;
    }
}
