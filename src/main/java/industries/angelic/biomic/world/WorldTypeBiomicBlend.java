package industries.angelic.biomic.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.chunk.IChunkProvider;

import industries.angelic.biomic.Biomic;

/** Biomic's world type: Origen terrain with hosted habitats, caves and sky islands, 256 or 512 high. */
public final class WorldTypeBiomicBlend extends WorldType {

    public static final String ID = "biomic_blend";
    public static final WorldTypeBiomicBlend INSTANCE = new WorldTypeBiomicBlend();

    private WorldTypeBiomicBlend() {
        super(ID);
    }

    public static void register() {
        if (WorldType.parseWorldType(ID) != INSTANCE) throw new IllegalStateException("World type occupied: " + ID);
        Biomic.LOG.info("Registered the Biomic world type at dynamic ID {}", INSTANCE.getWorldTypeID());
    }

    @Override
    public WorldChunkManager getChunkManager(World world) {
        return new WorldChunkManagerBiomic(WorldgenContext.forWorld(world));
    }

    @Override
    public IChunkProvider getChunkGenerator(World world, String options) {
        WorldgenContext context = WorldgenContext.forWorld(world);
        return TallWorlds.liveTall(world) ? new ChunkProviderBiomicTall(world, context)
            : new ChunkProviderBiomic(world, context);
    }

    @Override
    public int getMinimumSpawnHeight(World world) {
        return TallWorlds.liveTall(world) ? 192 : 64;
    }

    @Override
    public double getHorizon(World world) {
        return TallWorlds.liveTall(world) ? 191 : 63;
    }

    /**
     * Where the clouds float. Vanilla's clouds, Angelica's and the shaders' cloudHeight uniform (Complementary moves
     * its cloud layers by it) all read WorldProvider#getCloudHeight, which asks the world type. At Hohen Angst's 512
     * Origen's modern terrain stands 128 higher, so clouds take modern's 192 raised as much (review, 2026-10-24:
     * "make HA tell the shaders the appropriate cloud height for its 512 world"); Hohen Angst itself can't know, as
     * a vanilla world keeps vanilla's heights there.
     */
    @Override
    public float getCloudHeight() {
        return Biomic.proxy.cloudHeight();
    }
}
