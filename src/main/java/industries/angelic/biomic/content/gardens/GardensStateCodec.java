package industries.angelic.biomic.content.gardens;

import net.minecraft.block.Block;

import industries.angelic.biomic.world.modern.ModernStateCompatibility;
import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;

/** Explicit codec boundary for intrinsic Gardens states plus the vanilla/EFR states used by its features. */
public final class GardensStateCodec {

    public record State(Block block, int metadata, String canonical) {

        public State {
            if (block == null || canonical == null) throw new IllegalArgumentException("Incomplete Gardens state");
            if (metadata < 0 || metadata > 15)
                throw new IllegalArgumentException("Invalid legacy metadata " + metadata);
        }
    }

    private GardensStateCodec() {}

    public static State resolve(String state) {
        GardensContent.Resolved own = GardensContent.resolve(state);
        if (own != null)
            return new State(own.block(), own.metadata(), GardensContent.encode(own.block(), own.metadata()));
        OrigenBlockStateResolver.ResolvedBlock external = new OrigenBlockStateResolver().resolve(state);
        if (external.compatibilitySubstitute())
            throw new IllegalArgumentException("Gardens state requires a compatibility substitute: " + state);
        return new State(external.block(), external.metadata(), state);
    }

    public static String encode(Block block, int metadata) {
        String own = GardensContent.encode(block, metadata);
        if (own != null) return own;
        if (GardensContent.intrinsicBlocks()
            .containsValue(block)) throw new IllegalArgumentException("Invalid Gardens legacy metadata " + metadata);
        return ModernStateCompatibility.modernState(block, metadata);
    }
}
