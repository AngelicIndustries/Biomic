package industries.angelic.biomic.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * Other mods' world generators in the Tartarus Nether keep their own content (Netherlicious ores, Natura's clouds
 * and bushes) but not their copies of blocks Tartarus already places, except inside their structures. EFR's generators
 * are suppressed outright in
 * {@link industries.angelic.biomic.compat.efr.EfrGenerationFirewall}.
 */
public final class NetherForeignGeneration {

    /** Netherlicious' own blackstone and magma, scattered as blobs through every biome. */
    private static final String[] DUPLICATES = { "netherlicious:Blackstone", "netherlicious:MagmaBlock" };

    /**
     * One Forge world-generator pass. Passes nest: a generator that touches a neighbouring chunk can make Forge
     * populate
     * it inside this one, and the inner pass must neither end the outer pass's refusal nor inherit its structure.
     */
    private static final class Pass {

        final boolean tartarus;
        int structures;

        Pass(boolean tartarus) {
            this.tartarus = tartarus;
        }
    }

    private static final ThreadLocal<ArrayDeque<Pass>> PASSES = ThreadLocal.withInitial(ArrayDeque::new);
    /** Passes open on any thread, so ordinary block writes skip the thread-local lookup. */
    private static final AtomicInteger OPEN = new AtomicInteger();
    private static Set<Block> duplicates;

    private NetherForeignGeneration() {}

    public static void begin(IChunkProvider generator) {
        PASSES.get()
            .push(new Pass(generator instanceof ChunkProviderBiomicNether));
        OPEN.incrementAndGet();
    }

    public static void end() {
        if (PASSES.get()
            .poll() != null) OPEN.decrementAndGet();
    }

    /** A foreign structure (Netherlicious' ruined portals) is being built: it keeps its blocks. */
    public static void beginStructure() {
        Pass pass = PASSES.get()
            .peek();
        if (pass != null) pass.structures++;
    }

    public static void endStructure() {
        Pass pass = PASSES.get()
            .peek();
        if (pass != null && pass.structures > 0) pass.structures--;
    }

    /** Whether a foreign generator's placement of this block is refused. */
    public static boolean refuses(Block block) {
        if (OPEN.get() == 0) return false;
        Pass pass = PASSES.get()
            .peek();
        if (pass == null || !pass.tartarus || pass.structures > 0) return false;
        if (duplicates == null) {
            Set<Block> found = new HashSet<>();
            for (String id : DUPLICATES) {
                Block duplicate = Block.getBlockFromName(id);
                if (duplicate != null && duplicate != Blocks.air) found.add(duplicate);
            }
            duplicates = found;
        }
        return duplicates.contains(block);
    }
}
