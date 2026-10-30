package industries.angelic.biomic.compat.endlessids;

import net.minecraft.world.chunk.Chunk;

import com.falsepattern.endlessids.constants.ExtendedConstants;
import com.falsepattern.endlessids.mixin.helpers.ChunkBiomeHook;

/** The only Biomic adapter allowed to access EndlessIDs' surface-biome array. */
public final class EndlessIdsBiomeStorage {

    public static final int CHUNK_WIDTH = 16;
    public static final int ENTRY_COUNT = CHUNK_WIDTH * CHUNK_WIDTH;
    public static final int UNINITIALIZED_BIOME_ID = ExtendedConstants.biomeIDNull;
    public static final int MAX_BIOME_ID = UNINITIALIZED_BIOME_ID - 1;

    private EndlessIdsBiomeStorage() {}

    /** Replaces all 256 surface-biome IDs atomically and marks the chunk once. */
    public static void setBiomeIds(Chunk chunk, int[] biomeIds) {
        setBiomeIds(biomeArray(chunk), biomeIds);
        chunk.setChunkModified();
    }

    static void setBiomeIds(short[] target, int[] biomeIds) {
        requireValidArray(target);
        if (biomeIds == null || biomeIds.length != ENTRY_COUNT) {
            throw new IllegalArgumentException("Surface-biome input must contain exactly " + ENTRY_COUNT + " IDs");
        }
        for (int biomeId : biomeIds) {
            requireRealBiomeId(biomeId);
        }

        for (int index = 0; index < biomeIds.length; index++) {
            target[index] = (short) biomeIds[index];
        }
    }

    private static void requireRealBiomeId(int biomeId) {
        if (biomeId < 0 || biomeId > MAX_BIOME_ID) {
            throw new IllegalArgumentException("Biome ID must be between 0 and " + MAX_BIOME_ID + ", got " + biomeId);
        }
    }

    private static short[] biomeArray(Chunk chunk) {
        if (chunk == null) {
            throw new IllegalArgumentException("Chunk cannot be null");
        }
        if (!(chunk instanceof ChunkBiomeHook)) {
            throw new IllegalStateException("Chunk does not implement EndlessIDs ChunkBiomeHook");
        }
        return ((ChunkBiomeHook) chunk).getBiomeShortArray();
    }

    private static void requireValidArray(short[] biomeIds) {
        if (biomeIds == null || biomeIds.length != ENTRY_COUNT) {
            throw new IllegalStateException("EndlessIDs biome array must contain exactly " + ENTRY_COUNT + " entries");
        }
    }

}
