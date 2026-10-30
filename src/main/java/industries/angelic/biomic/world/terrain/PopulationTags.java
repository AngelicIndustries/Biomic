package industries.angelic.biomic.world.terrain;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagIntArray;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * Who wrote the cells of a chunk that populations reached into (order-independent population): a neighbour's chunk
 * key, or OWNER for a cell the chunk's own box population (or one of its structures) settled. Kept per loaded chunk
 * and saved with it, so a write that arrives later, from any neighbour, resolves the same way whatever the order.
 */
final class PopulationTags {

    static final long NONE = Long.MIN_VALUE;
    static final long OWNER = Long.MAX_VALUE;
    static final String NBT_KEY = "BiomicPopulationTags";

    private final Long2ObjectOpenHashMap<Long2LongOpenHashMap> chunks = new Long2ObjectOpenHashMap<>();
    /**
     * Per chunk and cell, the neighbours' writes that lost it to a write standing there (displaced by it, or refused
     * because of it; the owner's write included): when that one goes (its ground went to a later write from below, or
     * its two-block plant was broken), the best of them that can stand comes back. Rare: kept per cell only once a
     * write lost.
     */
    private final Long2ObjectOpenHashMap<Long2ObjectOpenHashMap<java.util.List<Claim>>> claims = new Long2ObjectOpenHashMap<>();

    record Claim(long writer, String state) {}

    static final String CLAIMS_KEY = "BiomicPopulationClaims";

    /** {@code writer}'s write, {@code state} as it stood, was displaced from a cell by a better one. */
    void displace(long chunkKey, long cell, long writer, String state) {
        claims.computeIfAbsent(chunkKey, ignored -> new Long2ObjectOpenHashMap<>())
            .computeIfAbsent(cell, ignored -> new java.util.ArrayList<>())
            .add(new Claim(writer, state));
    }

    /** The writes displaced from a cell whose own write is gone, best first; they leave the record. */
    java.util.List<Claim> displaced(long chunkKey, long cell) {
        var cells = claims.get(chunkKey);
        java.util.List<Claim> list = cells == null ? null : cells.remove(cell);
        if (list == null) return java.util.List.of();
        list.sort((a, b) -> beats(a.writer, b.writer) ? -1 : beats(b.writer, a.writer) ? 1 : 0);
        return list;
    }

    long get(long chunkKey, long cell) {
        Long2LongOpenHashMap cells = chunks.get(chunkKey);
        return cells == null ? NONE : cells.getOrDefault(cell, NONE);
    }

    void put(long chunkKey, long cell, long writer) {
        Long2LongOpenHashMap cells = chunks.get(chunkKey);
        if (cells == null) {
            cells = new Long2LongOpenHashMap();
            cells.defaultReturnValue(NONE);
            chunks.put(chunkKey, cells);
        }
        cells.put(cell, writer);
    }

    void remove(long chunkKey, long cell) {
        Long2LongOpenHashMap cells = chunks.get(chunkKey);
        if (cells != null) cells.remove(cell);
    }

    /** Whether writer {@code a} wins a cell over {@code b} (both neighbours' chunk keys): lower X, then lower Z. */
    static boolean beats(long a, long b) {
        int ax = (int) (a >> 32), bx = (int) (b >> 32);
        if (ax != bx) return ax < bx;
        return (int) a < (int) b;
    }

    void save(long chunkKey, NBTTagCompound level) {
        var cellClaims = claims.get(chunkKey);
        if (cellClaims != null && !cellClaims.isEmpty()) {
            net.minecraft.nbt.NBTTagList list = new net.minecraft.nbt.NBTTagList();
            for (var entry : cellClaims.long2ObjectEntrySet()) for (Claim claim : entry.getValue()) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setLong("cell", entry.getLongKey());
                tag.setLong("writer", claim.writer);
                tag.setString("state", claim.state);
                list.appendTag(tag);
            }
            level.setTag(CLAIMS_KEY, list);
        }
        Long2LongOpenHashMap cells = chunks.get(chunkKey);
        if (cells == null || cells.isEmpty()) return;
        int[] data = new int[cells.size() * 4];
        int i = 0;
        for (var entry : cells.long2LongEntrySet()) {
            data[i++] = (int) (entry.getLongKey() >>> 32);
            data[i++] = (int) entry.getLongKey();
            data[i++] = (int) (entry.getLongValue() >>> 32);
            data[i++] = (int) entry.getLongValue();
        }
        level.setTag(NBT_KEY, new NBTTagIntArray(data));
    }

    void load(long chunkKey, NBTTagCompound level) {
        chunks.remove(chunkKey);
        claims.remove(chunkKey);
        net.minecraft.nbt.NBTTagList list = level.getTagList(CLAIMS_KEY, 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            displace(chunkKey, tag.getLong("cell"), tag.getLong("writer"), tag.getString("state"));
        }
        if (!level.hasKey(NBT_KEY)) return;
        int[] data = level.getIntArray(NBT_KEY);
        for (int i = 0; i + 3 < data.length; i += 4) put(
            chunkKey,
            (long) data[i] << 32 | data[i + 1] & 0xFFFF_FFFFL,
            (long) data[i + 2] << 32 | data[i + 3] & 0xFFFF_FFFFL);
    }

    void unload(long chunkKey) {
        chunks.remove(chunkKey);
        claims.remove(chunkKey);
    }
}
