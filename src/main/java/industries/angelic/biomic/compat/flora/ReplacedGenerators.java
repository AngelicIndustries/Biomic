package industries.angelic.biomic.compat.flora;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import industries.angelic.biomic.api.ForeignGenerators;
import industries.angelic.biomic.api.ForeignGenerators.Scope;
import industries.angelic.biomic.world.ChunkProviderBiomic;
import industries.angelic.biomic.world.ChunkProviderBiomicNether;
import industries.angelic.biomic.world.ChunkProviderBiomicTall;

/**
 * Other mods' own world generators that stand down in the chunks Biomic generates: Forge's pass skips those registered
 * through ForeignGenerators. Biomic registers SaltyMod's flowers and salt lakes; Adventura Arcana Core registers the
 * generators whose plants it has Biomic place (PMP's, Natura's, HarvestCraft's, Harvest the Nether's).
 */
public final class ReplacedGenerators {

    private ReplacedGenerators() {}

    /** The generators whose plants Biomic places itself (ForeignGenerators). */
    public static void register() {
        // SaltyMod's flowers and salt lakes. The flora ledger places the salt flowers. Both passes scattered around
        // the chunk's own corner instead of vanilla's +8 offset, so they loaded unpopulated neighbours mid-population
        // (about 80 cascades in a 17-window bench), and the lakes, groups of five 20-block lakes from one chunk, read
        // block light that is not yet computed. At 512 no lake ever fitted; at 256 one per 10,000 chunks. Salt ore,
        // crystals and the salt marsh stay.
        ForeignGenerators.standDown("darkbum.saltymod.world.generator.SaltFlowerGenerator", Scope.OVERWORLD);
        ForeignGenerators.standDown("darkbum.saltymod.world.generator.SaltLakeGenerator", Scope.OVERWORLD);
    }

    /** Whether Forge's pass skips this generator in this chunk. */
    public static boolean standsDown(Object generator, World world, IChunkProvider chunkGenerator) {
        Scope scope = ForeignGenerators.scope(
            generator.getClass()
                .getName());
        if (scope == null) return false;
        return scope == Scope.OVERWORLD ? overworld(world, chunkGenerator) : any(world, chunkGenerator);
    }

    /** A chunk of Biomic's overworld (256 or 512 high). */
    public static boolean overworld(World world, IChunkProvider generator) {
        return world != null && !world.isRemote
            && (generator instanceof ChunkProviderBiomic || generator instanceof ChunkProviderBiomicTall);
    }

    /** A chunk of any Biomic dimension, the Tartarus Nether included. */
    public static boolean any(World world, IChunkProvider generator) {
        return overworld(world, generator)
            || world != null && !world.isRemote && generator instanceof ChunkProviderBiomicNether;
    }
}
