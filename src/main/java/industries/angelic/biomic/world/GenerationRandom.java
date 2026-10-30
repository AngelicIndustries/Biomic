package industries.angelic.biomic.world;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

/**
 * While a chunk of a Biomic world populates, world.rand is a random of that chunk's own, seeded from the world seed and
 * the chunk (MixinChunkProviderServerGenerating), and the world's random is put back afterwards, untouched. Structures
 * and other mods' generators draw from world.rand (Village Names grows its pens' saplings with it), whose state
 * otherwise depends on everything that used it before, down to the JVM's identity hash codes: the same seed gave a
 * different world after any unrelated change to the code (2026-10-25).
 */
public final class GenerationRandom {

    private GenerationRandom() {}

    /** A chunk of any Biomic dimension: its population draws from its own random. */
    public static boolean applies(World world, IChunkProvider provider) {
        return world != null && !world.isRemote
            && (provider instanceof ChunkProviderBiomic || provider instanceof ChunkProviderBiomicTall
                || provider instanceof ChunkProviderBiomicNether);
    }

    public static Random forChunk(World world, int chunkX, int chunkZ) {
        return new Random(
            WorldgenContext.mix64(
                world.getSeed() ^ chunkX * 0x9E3779B97F4A7C15L ^ chunkZ * 0xC2B2AE3D27D4EB4FL ^ 0x776F726C6472616EL));
    }
}
