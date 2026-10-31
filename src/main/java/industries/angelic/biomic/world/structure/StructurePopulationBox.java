package industries.angelic.biomic.world.structure;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/**
 * What structure pieces (villages, mineshafts, strongholds) may touch while one chunk is populated: the 16 × 16
 * columns vanilla hands them (the chunk's corner plus 8). Village Names reads and writes the world over a building's
 * whole footprint, unclipped, which loaded and generated unpopulated neighbours mid-population (cascading worldgen).
 * Outside the box a write is dropped (the population whose box holds that column writes it too: every column of a
 * piece lies in exactly one box), a read sees air, holds no tile entity (no chest filled twice), and an entity is not
 * spawned; ground heights come from the village's shaped terrain (VillageContext#groundAbove).
 *
 * <p>
 * With order-independent population ({@link Terrain}) reads inside the box see the chunk as provided and the box's
 * own structure writes, never what neighbours wrote into it, and each write is reported as the box's own.
 */
public final class StructurePopulationBox implements NativeFeatureCapture.BoxedAccess {

    /** The provided terrain, and the owner's record of its writes (OrigenFeatureRuntime's journaled population). */
    public interface Terrain {

        Block block(int x, int y, int z);

        int metadata(int x, int y, int z);

        void owned(int x, int y, int z, Block block, int metadata);

        /** The highest provided cell of a column that covers what lies below (ground, not trees), or -1. */
        int surface(int x, int z);

        /** The highest non-air provided cell of a column, or -1. */
        int top(int x, int z);
    }

    private final World world;
    private final int minX, minZ;
    private final Terrain terrain;
    /** Cells this run wrote (with {@link #terrain}): a piece reads back its own work, not the provided terrain. */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet written = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /** Each column's highest cell this run wrote. */
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap writtenTop = new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();

    private StructurePopulationBox(World world, int chunkX, int chunkZ, Terrain terrain) {
        this.world = world;
        this.minX = (chunkX << 4) + 8;
        this.minZ = (chunkZ << 4) + 8;
        this.terrain = terrain;
    }

    /** Runs a structure population confined to the chunk's box; {@code terrain} may be null (reads see the world). */
    public static <T> T run(World world, int chunkX, int chunkZ, Terrain terrain, Supplier<T> generation) {
        return NativeFeatureCapture.run(world, new StructurePopulationBox(world, chunkX, chunkZ, terrain), generation);
    }

    /**
     * Mineshafts, villages and strongholds of one chunk's box under order-independent population: last, after the
     * journal, each confined to the box and reading it as provided; a blacksmith's basin is sealed in the box too.
     */
    public static boolean populate(World world, industries.angelic.biomic.world.WorldgenContext context, int chunkX,
        int chunkZ, Terrain provided, BiomicMineshaftGenerator mineshafts, BiomicVillageGenerator villages,
        BiomicStrongholdGenerator strongholds, BiomicScatteredFeatures scattered) {
        run(world, chunkX, chunkZ, provided, () -> mineshafts.populate(world, context, chunkX, chunkZ, provided));
        boolean village = villages.populate(world, context, chunkX, chunkZ, provided);
        run(world, chunkX, chunkZ, provided, () -> strongholds.populate(world, chunkX, chunkZ, provided));
        if (village)
            run(world, chunkX, chunkZ, provided, () -> villages.sealBlacksmithLava(world, chunkX, chunkZ, true));
        if (scattered != null) run(world, chunkX, chunkZ, provided, () -> {
            scattered.populate(world, chunkX, chunkZ);
            return null;
        });
        return village;
    }

    private boolean inside(int x, int z) {
        return x >= minX && x < minX + 16 && z >= minZ && z < minZ + 16;
    }

    private static long cell(int x, int y, int z) {
        return (long) (x & 0x3FF_FFFF) << 38 | (long) (z & 0x3FF_FFFF) << 12 | y & 0xFFF;
    }

    private boolean provided(int x, int y, int z) {
        return terrain != null && !written.contains(cell(x, y, z));
    }

    @Override
    public Block block(int x, int y, int z) {
        if (!inside(x, z)) return Blocks.air;
        return provided(x, y, z) ? terrain.block(x, y, z) : world.getBlock(x, y, z);
    }

    @Override
    public int metadata(int x, int y, int z) {
        if (!inside(x, z)) return 0;
        return provided(x, y, z) ? terrain.metadata(x, y, z) : world.getBlockMetadata(x, y, z);
    }

    private static Thread deferringBlockAdded;

    /** Whether Chunk#func_150807_a holds back onBlockAdded for a box write on this thread (MixinChunk). */
    public static boolean defersBlockAdded() {
        return deferringBlockAdded == Thread.currentThread();
    }

    @Override
    public boolean set(int x, int y, int z, Block given, int metadata) {
        if (!inside(x, z)) return false;
        // Liquids are laid still (a well's or fountain's flowing water ticked and ran out over the ground where the
        // rest of the structure, in a box not populated yet, was still missing). Still water flows only when disturbed.
        Block block = given == net.minecraft.init.Blocks.flowing_water ? net.minecraft.init.Blocks.water
            : given == net.minecraft.init.Blocks.flowing_lava ? net.minecraft.init.Blocks.lava : given;
        if (terrain == null) return world.setBlock(x, y, z, block, metadata, 2);
        written.add(cell(x, y, z));
        long column = (long) x << 32 ^ z & 0xFFFF_FFFFL;
        if (!writtenTop.containsKey(column) || writtenTop.get(column) < y) writtenTop.put(column, y);
        Thread previous = deferringBlockAdded;
        deferringBlockAdded = Thread.currentThread();
        boolean changed;
        try {
            changed = world.setBlock(x, y, z, block, metadata, 2);
        } finally {
            deferringBlockAdded = previous;
        }
        // Owned even when the cell already held it (a neighbour's write): no later neighbour may replace it.
        terrain.owned(x, y, z, block, metadata);
        // The block's own reaction (a torch turning to its wall, a rail joining its neighbours, a chest facing) reads
        // the box too: read from the world it depended on what neighbours had placed, so on chunk order.
        if (changed) NativeFeatureCapture.captured(() -> {
            block.onBlockAdded(world, x, y, z);
            return null;
        });
        return changed;
    }

    @Override
    public net.minecraft.tileentity.TileEntity tileEntity(int x, int y, int z) {
        return inside(x, z) ? world.getTileEntity(x, y, z) : null;
    }

    @Override
    public boolean spawnEntity(net.minecraft.entity.Entity entity) {
        return inside((int) Math.floor(entity.posX), (int) Math.floor(entity.posZ)) && world.spawnEntityInWorld(entity);
    }

    /** With {@link #terrain}: the highest cell of a column the box can see (provided, or written by this run). */
    private int columnTop(int x, int z) {
        long column = (long) x << 32 ^ z & 0xFFFF_FFFFL;
        return Math.max(terrain.top(x, z), writtenTop.containsKey(column) ? writtenTop.get(column) : -1);
    }

    @Override
    public int heightValue(int x, int z) {
        if (!inside(x, z)) return 0;
        if (terrain == null) return world.getHeightValue(x, z);
        // As the height map: over the highest block that stops light. The world's own depended on chunk order.
        for (int y = columnTop(x, z); y >= 0; y--) if (block(x, y, z).getLightOpacity() != 0) return y + 1;
        return 0;
    }

    @Override
    public int topSolidOrLiquid(int x, int z) {
        int ground = VillageContext.groundAbove(world, x, z, false);
        if (ground != Integer.MIN_VALUE) return ground;
        if (!inside(x, z)) return -1;
        if (terrain == null) return world.getTopSolidOrLiquidBlock(x, z);
        for (int y = columnTop(x, z); y > 0; y--) {
            net.minecraft.block.material.Material material = block(x, y, z).getMaterial();
            if (material.blocksMovement() && material != net.minecraft.block.material.Material.leaves) return y + 1;
        }
        return -1;
    }

    @Override
    public int minimumY() {
        return 0;
    }

    @Override
    public int maximumYExclusive() {
        return world.getHeight();
    }
}
