package industries.angelic.biomic.compat.flora;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.compat.biome.NativeBlockCatalogue;
import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;

/** Explicit registry allow-list, not an arbitrary mod-block fallback. Metadata survives queued chunk writes. */
public final class NativeFloraStates {

    private NativeFloraStates() {}

    public static boolean handles(String state) {
        return ReviewedVanillaFloraStates.handles(state) || state.startsWith("minecraft:tallgrass[")
            || NativeBlockCatalogue.handles(state);
    }

    public static OrigenBlockStateResolver.ResolvedBlock resolve(String state) {
        if (ReviewedVanillaFloraStates.handles(state)) return ReviewedVanillaFloraStates.resolve(state);
        if (state.startsWith("minecraft:tallgrass[")) {
            if (!state.equals("minecraft:tallgrass[legacy_meta=0]"))
                throw new IllegalArgumentException("Unsupported legacy tallgrass state: " + state);
            return OrigenBlockStateResolver.imported(state, Blocks.tallgrass, 0);
        }
        return NativeBlockCatalogue.resolve(state);
    }

    public static String encodeIfSupported(Block block, int metadata) {
        if (block != null && block == Blocks.tallgrass && metadata == 0) return "minecraft:tallgrass[legacy_meta=0]";
        String nativeBiome = NativeBlockCatalogue.encode(block, metadata);
        // Native EFR adapters may explicitly encode every registered state, but ordinary source-world
        // reads must retain the existing modern nylium/fungus/etc semantics used by RU and Origen.
        return nativeBiome != null && !nativeBiome.startsWith("etfuturum:") ? nativeBiome : null;
    }
}
