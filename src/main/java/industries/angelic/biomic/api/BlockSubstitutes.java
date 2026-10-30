package industries.angelic.biomic.api;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.Chunk;

/**
 * Blocks that another block stands in for in Biomic's worlds, metadata kept: whoever writes the block (Biomic's
 * population, structures, other mods' generators, players), the world gets the substitute. Register at init or
 * postInit, once both blocks exist; one substitute per block.
 */
public final class BlockSubstitutes {

    /** Which writes are replaced. */
    public enum Scope {
        /** Every write, players' included. */
        ALWAYS,
        /**
         * Writes while a chunk generates (its population, structures, other mods' generators); players may still place
         * it.
         */
        GENERATION
    }

    /** What stands for the block at a column of a chunk (local x and z); null keeps the block. */
    @FunctionalInterface
    public interface Substitute {

        Block replace(Chunk chunk, int localX, int localZ);
    }

    private record Entry(Block from, Scope scope, Substitute to) {}

    /** Copied on registration: every block write in a Biomic world reads it. */
    private static volatile Entry[] entries = new Entry[0];

    private BlockSubstitutes() {}

    public static synchronized void register(Block from, Scope scope, Substitute to) {
        if (from == null || from == Blocks.air || scope == null || to == null)
            throw new IllegalArgumentException("A block, a scope and a substitute are required");
        for (Entry entry : entries) if (entry.from == from) throw new IllegalStateException(
            "A substitute for " + Block.blockRegistry.getNameForObject(from) + " is registered already");
        Entry[] next = Arrays.copyOf(entries, entries.length + 1);
        next[entries.length] = new Entry(from, scope, to);
        entries = next;
    }

    /** Whether a write of the block may be replaced (the cheap test Biomic's hooks make before anything else). */
    public static boolean watched(Block block, boolean generating) {
        for (Entry entry : entries) if (entry.from == block) return generating || entry.scope == Scope.ALWAYS;
        return false;
    }

    /**
     * What the world gets for a write of the block at a chunk's local column: the block itself if nothing stands in.
     */
    public static Block substitute(Block block, Chunk chunk, int localX, int localZ, boolean generating) {
        for (Entry entry : entries) {
            if (entry.from != block) continue;
            if (!generating && entry.scope != Scope.ALWAYS) return block;
            Block to = entry.to.replace(chunk, localX, localZ);
            return to == null ? block : to;
        }
        return block;
    }
}
