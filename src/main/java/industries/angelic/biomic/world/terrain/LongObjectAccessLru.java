package industries.angelic.biomic.world.terrain;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/** Small primitive-key access-order cache; null values are reserved as the miss sentinel. */
final class LongObjectAccessLru<V> {

    private final int capacity;
    private final Long2ObjectLinkedOpenHashMap<V> entries;

    LongObjectAccessLru(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("LRU capacity must be positive");
        this.capacity = capacity;
        entries = new Long2ObjectLinkedOpenHashMap<>(capacity);
    }

    V get(long key) {
        return entries.getAndMoveToLast(key);
    }

    void put(long key, V value) {
        if (value == null) throw new IllegalArgumentException("LRU values cannot be null");
        entries.putAndMoveToLast(key, value);
        if (entries.size() > capacity) entries.removeFirst();
    }

}
