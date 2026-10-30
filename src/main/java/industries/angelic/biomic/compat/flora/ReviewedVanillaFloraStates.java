package industries.angelic.biomic.compat.flora;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;

/** Only the vanilla palettes written by audited Natura/Pam whole native generators. */
public final class ReviewedVanillaFloraStates {

    private ReviewedVanillaFloraStates() {}

    public static boolean handles(String state) {
        return state != null && (state.equals("minecraft:dirt") || state.startsWith("minecraft:dirt[legacy_meta=")
            || state.startsWith("minecraft:log[legacy_meta=")
            || state.startsWith("minecraft:leaves[legacy_meta=")
            || state.startsWith("minecraft:vine[legacy_meta="));
    }

    public static OrigenBlockStateResolver.ResolvedBlock resolve(String value) {
        Parsed parsed = parse(value);
        Block block = switch (parsed.key) {
            case "minecraft:dirt" -> Blocks.dirt;
            case "minecraft:log" -> Blocks.log;
            case "minecraft:leaves" -> Blocks.leaves;
            case "minecraft:vine" -> Blocks.vine;
            default -> throw new IllegalArgumentException("Not an audited vanilla flora state");
        };
        return OrigenBlockStateResolver.imported(value, block, parsed.metadata);
    }

    static String state(String key, int metadata) {
        if (metadata < 0 || metadata > 15) throw new IllegalArgumentException("Invalid exact flora metadata");
        // Keep Origen's historical dirt grammar: ordinary dirt is plain, only native bare dirt is explicit.
        if (key.equals("minecraft:dirt")) {
            if (metadata == 0) return key;
            if (metadata != 1) throw new IllegalArgumentException("Unsupported explicit legacy dirt metadata");
        }
        return key + "[legacy_meta=" + metadata + "]";
    }

    static Parsed parse(String value) {
        if ("minecraft:dirt".equals(value)) return new Parsed(value, 0);
        int split = value == null ? -1 : value.indexOf("[legacy_meta=");
        if (split < 1 || !value.endsWith("]"))
            throw new IllegalArgumentException("Explicit exact flora metadata required");
        String key = value.substring(0, split);
        int metadata;
        try {
            metadata = Integer.parseInt(value.substring(split + 13, value.length() - 1));
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException("Invalid exact flora metadata", failure);
        }
        if (!state(key, metadata).equals(value)) throw new IllegalArgumentException("Noncanonical exact flora state");
        return new Parsed(key, metadata);
    }

    record Parsed(String key, int metadata) {}
}
