package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

/**
 * Shifted512 ground heights as the native marsh (the salt marsh and BOP's lowland wetlands) leaves them: structures
 * planned on Origen's own ground stood several blocks above a marsh, their pads on pillars of earth. The marsh's floor
 * is a function of the column and its old ground, so no chunk is built.
 */
public final class MarshGround {

    private static final int SHIFT = 128;
    private static final int CACHE_SIZE = 1 << 16;

    private final ShiftedGroundHeights origen;
    private final HostedMarshTerrain marsh;
    private final Long2IntLinkedOpenHashMap heights = new Long2IntLinkedOpenHashMap(CACHE_SIZE + 1);

    /** Origen's ground, flattened where the save keeps the marsh. */
    public MarshGround(WorldgenContext context, ShiftedGroundHeights origen) {
        this.origen = origen;
        var habitats = context.getNativeHabitats();
        marsh = habitats.shifted512() && habitats.fullMarsh() ? HostedMarshTerrain.shifted512(context) : null;
        heights.defaultReturnValue(Integer.MIN_VALUE);
    }

    /** Highest solid physical Y of the column once the marsh has flattened it. */
    public int ground(int x, int z) {
        if (marsh == null) return origen.ground(x, z);
        long key = (long) x << 32 ^ (z & 0xFFFF_FFFFL);
        synchronized (heights) {
            int known = heights.getAndMoveToLast(key);
            if (known != Integer.MIN_VALUE) return known;
        }
        int ground = marsh.floor(x, z, origen.ground(x, z) - SHIFT) + SHIFT;
        synchronized (heights) {
            heights.putAndMoveToLast(key, ground);
            if (heights.size() > CACHE_SIZE) heights.removeFirstInt();
        }
        return ground;
    }
}
