package industries.angelic.biomic.content;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;

/**
 * Biomic's own plain blocks (food plants, ported sea plants and shells, lichens) as block states for the generation
 * write path: {@code biomic:<registry name>[legacy_meta=N]}, metadata carried as it is.
 */
public final class BiomicLegacyStates {

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<Block, String> NAMES = new IdentityHashMap<>();

    private BiomicLegacyStates() {}

    public static synchronized void register(String name, Block block) {
        if (BLOCKS.putIfAbsent(name, block) != null) throw new IllegalStateException("Duplicate Biomic state " + name);
        NAMES.put(block, name);
    }

    public static String encode(Block block, int metadata) {
        String name = NAMES.get(block);
        return name == null || metadata < 0 || metadata > 15 ? null
            : "biomic:" + name + "[legacy_meta=" + metadata + "]";
    }

    public record Resolved(Block block, int metadata) {}

    public static Resolved resolve(String state) {
        if (state == null || !state.startsWith("biomic:") || BLOCKS.isEmpty()) return null;
        int bracket = state.indexOf('[');
        Block block = BLOCKS.get(state.substring(7, bracket < 0 ? state.length() : bracket));
        if (block == null) return null;
        int metadata = 0;
        if (bracket >= 0) {
            String prefix = "[legacy_meta=";
            if (!state.startsWith(prefix, bracket) || !state.endsWith("]")) return null;
            try {
                metadata = Integer.parseInt(state.substring(bracket + prefix.length(), state.length() - 1));
            } catch (NumberFormatException malformed) {
                return null;
            }
        }
        return metadata < 0 || metadata > 15 ? null : new Resolved(block, metadata);
    }
}
