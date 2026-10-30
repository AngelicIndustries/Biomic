package industries.angelic.biomic.world.structure;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.WorldgenPhase;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;
import industries.angelic.biomic.world.terrain.MacroTerrainSampler;

/** Biomic-owned adapter around vanilla's village pieces and persisted start format. */
public final class BiomicVillageGenerator {

    /**
     * How far decoration keeps from a village's pieces (houses, farms, wells, roads and the lamps on them): plants stay
     * off them and a block from the walls; trees, cacti, bushes and boulders keep their crowns clear of the houses and
     * roads, and near the village only one site in {@link #TREE_THINNING} grows one, as where trees are cut.
     */
    private static final int PLANT_HOUSE_MARGIN = 1, TREE_HOUSE_MARGIN = 4, TREE_ROAD_MARGIN = 2, TREE_THINNING = 3,
        NEAR_VILLAGE = 8;

    private static final long POPULATION_SALT = 0xBBE0563303A4615FL;

    private final BiomicVillagePlacement placement;
    private final VillageTerrain terrain;
    private final MapGenVillage delegate;

    public BiomicVillageGenerator(WorldgenContext context, MacroTerrainSampler macroSampler,
        BiomicTerrainSampler terrainSampler) {
        placement = new BiomicVillagePlacement(context, macroSampler, terrainSampler);
        terrain = new VillageTerrain(placement, placement.terrain());
        delegate = new MapGenBiomicVillage(placement);
    }

    public BiomicVillageGenerator(WorldgenContext context, MacroTerrainSampler macroSampler, StructureTerrain terrain) {
        placement = new BiomicVillagePlacement(context, macroSampler, terrain);
        this.terrain = new VillageTerrain(placement, terrain);
        delegate = new MapGenBiomicVillage(placement);
    }

    /** Worker-safe: shapes a chunk's pure terrain for the villages planned near it, planning them if needed. */
    public void shapeTerrain(World world, int chunkX, int chunkZ, byte[] materials) {
        terrain.shape(world, chunkX, chunkZ, materials);
    }

    /** Starts planning the villages around a chunk, and their ground, on the workers (see VillageTerrain#warm). */
    public void warm(World world, int chunkX, int chunkZ, java.util.concurrent.Executor workers) {
        terrain.warm(world, chunkX, chunkZ, workers);
    }

    /** Whether village shaping may change this cell (the cell then belongs to the village, not to Origen). */
    public boolean ownsTerrain(World world, int x, int y, int z) {
        return terrain.owns(world, x, y, z);
    }

    public void prepare(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ, Block[] blocks) {
        delegate.func_151539_a(chunkProvider, world, chunkX, chunkZ, blocks);
    }

    /** Places the village pieces in this chunk, each village on the ground of its own layer. */
    public boolean populate(World world, WorldgenContext context, int chunkX, int chunkZ) {
        return populate(world, context, chunkX, chunkZ, null);
    }

    /** {@code provided}: order-independent population's view of the box (StructurePopulationBox), or null. */
    public boolean populate(World world, WorldgenContext context, int chunkX, int chunkZ,
        StructurePopulationBox.Terrain provided) {
        Random random = new Random(populationSeed(context, chunkX, chunkZ));
        long seed = context.getSettings()
            .getWorldSeed() ^ POPULATION_SALT;
        return VillageContext.populate(
            world,
            placement,
            terrain,
            () -> StructurePopulationBox.run(
                world,
                chunkX,
                chunkZ,
                provided,
                () -> provided == null ? delegate.generateStructuresInChunk(world, random, chunkX, chunkZ)
                    : ((MapGenBiomicVillage) delegate).draw(world, seed, chunkX, chunkZ, provided)));
    }

    /**
     * Restores the solid vanilla blacksmith basin after every later surface-decoration pass has completed: over the
     * chunk, or over its population box when structures run last in it (order-independent population).
     */
    public int sealBlacksmithLava(World world, int chunkX, int chunkZ, boolean box) {
        return ((MapGenBiomicVillage) delegate).sealBlacksmithLava(world, chunkX, chunkZ, box ? 8 : 0);
    }

    public void recreate(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ) {
        prepare(chunkProvider, world, chunkX, chunkZ, null);
    }

    /**
     * Whether decoration may not root at a block: on or beside one of a village's pieces, at the village's own height
     * (a sky village shelters nothing on the land below, nor the reverse); see {@link #PLANT_HOUSE_MARGIN}. Only snow
     * cover is never kept off. From the plans, never from the starts generated so far: which of those exist depends on
     * the order chunks were generated in.
     */
    public boolean guardsDecoration(World world, String feature, boolean bulky, int x, int y, int z) {
        if ("minecraft:freeze_top_layer".equals(feature)) return false;
        int reach = bulky ? TREE_HOUSE_MARGIN + NEAR_VILLAGE : PLANT_HOUSE_MARGIN;
        for (var plan : plansNear(world, x, z, reach)) if (y >= plan.minPad() - 8 && y <= plan.maxPad() + 24
            && containsHorizontally(plan.bounds(), x, z, reach)
            && guards(plan, bulky, x, z)) return true;
        return false;
    }

    /** Whether a village lies within {@code reach} blocks of a block, at the village's height. */
    public boolean nearDecoration(World world, int x, int y, int z, int reach) {
        for (var plan : plansNear(world, x, z, reach))
            if (y >= plan.minPad() - 8 && y <= plan.maxPad() + 24 && containsHorizontally(plan.bounds(), x, z, reach))
                return true;
        return false;
    }

    /**
     * The plans that may come within {@code reach} of a block (callers test the bounds), remembered for the last
     * 32-block cell: decoration asks for every block it roots or writes, and a population's blocks share a few cells.
     */
    private java.util.List<BiomicVillagePlacement.Plan> plansNear(World world, int x, int z, int reach) {
        if (reach > PLANS_NEAR_REACH) return placement.near(x >> 4, z >> 4, reach, world);
        int cellX = x >> 5, cellZ = z >> 5;
        PlansNear known = plansNear;
        if (known != null && known.cellX == cellX && known.cellZ == cellZ) return known.plans;
        var plans = placement.near(cellX << 1, cellZ << 1, PLANS_NEAR_REACH + 16, world);
        plansNear = new PlansNear(cellX, cellZ, plans);
        return plans;
    }

    private record PlansNear(int cellX, int cellZ, java.util.List<BiomicVillagePlacement.Plan> plans) {}

    private static final int PLANS_NEAR_REACH = 24;
    private volatile PlansNear plansNear;

    private static boolean guards(BiomicVillagePlacement.Plan plan, boolean bulky, int x, int z) {
        for (var house : plan.houses())
            if (containsHorizontally(house.box(), x, z, bulky ? TREE_HOUSE_MARGIN : PLANT_HOUSE_MARGIN)) return true;
        int along = bulky ? TREE_ROAD_MARGIN : 0;
        for (var road : plan.roads()) if (containsHorizontally(road, x, z, along)) return true;
        for (var accent : plan.accents()) if (containsHorizontally(accent, x, z, along)) return true;
        return bulky
            && Math.floorMod(WorldgenContext.mix64(POPULATION_SALT ^ (long) x << 32 ^ z & 0xFFFF_FFFFL), TREE_THINNING)
                != 0;
    }

    static long populationSeed(WorldgenContext context, int chunkX, int chunkZ) {
        return WorldgenContext.mix64(context.seedForChunk(WorldgenPhase.STRUCTURES, chunkX, chunkZ) ^ POPULATION_SALT);
    }

    private static final class MapGenBiomicVillage extends MapGenVillage {

        private final BiomicVillagePlacement placement;

        private final BoxedStructureStarts boxed = new BoxedStructureStarts();

        private MapGenBiomicVillage(BiomicVillagePlacement placement) {
            this.placement = placement;
        }

        private boolean draw(World world, long seed, int chunkX, int chunkZ, StructurePopulationBox.Terrain provided) {
            return boxed.draw(structureMap, world, seed, chunkX, chunkZ, provided);
        }

        @Override
        protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
            return placement.startingAt(chunkX, chunkZ, worldObj) != null;
        }

        /**
         * The start the plan was made from: same pieces from the same random state, vanilla's or Village Names'. Built
         * from a fresh vanilla random, not this generator's {@code rand}: Hodgepodge swaps that for its own generator,
         * whose numbers differ, and the village would no longer be the one its ground was shaped for.
         */
        @Override
        protected StructureStart getStructureStart(int chunkX, int chunkZ) {
            var plan = placement.startingAt(chunkX, chunkZ, worldObj);
            StructureStart start = placement.build(worldObj, placement.structureRandom(chunkX, chunkZ), plan);
            // Terrain was shaped and decoration kept away for the plan's layout: the two must be the same village.
            StructureBoundingBox built = start.getBoundingBox(), planned = plan.bounds();
            if (built.minX != planned.minX || built.maxX != planned.maxX
                || built.minZ != planned.minZ
                || built.maxZ != planned.maxZ)
                industries.angelic.biomic.Biomic.LOG.warn(
                    "Village at chunk {},{} was built as {} but planned as {}; its ground was shaped for the plan",
                    chunkX,
                    chunkZ,
                    built,
                    planned);
            return start;
        }

        private int sealBlacksmithLava(World world, int chunkX, int chunkZ, int offset) {
            int minimumX = (chunkX << 4) + offset;
            int maximumX = minimumX + 15;
            int minimumZ = (chunkZ << 4) + offset;
            int maximumZ = minimumZ + 15;
            int repaired = 0;
            for (StructureStart start : structureMap.values()) {
                if (!start.isSizeableStructure() || !start.getBoundingBox()
                    .intersectsWith(minimumX, minimumZ, maximumX, maximumZ)) continue;
                for (StructureComponent component : start.getComponents()) {
                    if (!(component instanceof StructureVillagePieces.House2)) continue;
                    StructureBoundingBox bounds = component.getBoundingBox();
                    int scanMinimumX = Math.max(minimumX, bounds.minX);
                    int scanMaximumX = Math.min(maximumX, bounds.maxX);
                    int scanMinimumZ = Math.max(minimumZ, bounds.minZ);
                    int scanMaximumZ = Math.min(maximumZ, bounds.maxZ);
                    int poolY = bounds.minY + 1;
                    for (int z = scanMinimumZ; z <= scanMaximumZ; z++) {
                        for (int x = scanMinimumX; x <= scanMaximumX; x++) {
                            Block block = world.getBlock(x, poolY, z);
                            if (block != Blocks.flowing_lava && block != Blocks.lava) continue;
                            repaired += sealLavaCell(world, x, poolY, z);
                        }
                    }
                }
            }
            return repaired;
        }

        private static int sealLavaCell(World world, int blockX, int blockY, int blockZ) {
            int repaired = replaceLavaBottom(world, blockX, blockY - 1, blockZ);
            repaired += replaceLavaSide(world, blockX - 1, blockY, blockZ);
            repaired += replaceLavaSide(world, blockX + 1, blockY, blockZ);
            repaired += replaceLavaSide(world, blockX, blockY, blockZ - 1);
            repaired += replaceLavaSide(world, blockX, blockY, blockZ + 1);
            return repaired;
        }

        private static int replaceLavaBottom(World world, int blockX, int blockY, int blockZ) {
            Block existing = world.getBlock(blockX, blockY, blockZ);
            if (existing == Blocks.cobblestone) return 0;
            world.setBlock(blockX, blockY, blockZ, Blocks.cobblestone, 0, 2);
            return 1;
        }

        private static int replaceLavaSide(World world, int blockX, int blockY, int blockZ) {
            Block existing = world.getBlock(blockX, blockY, blockZ);
            if (existing == Blocks.flowing_lava || existing == Blocks.lava) return 0;
            if (existing.getMaterial()
                .isSolid() && existing != Blocks.snow
                && existing != Blocks.ice) return 0;
            world.setBlock(blockX, blockY, blockZ, Blocks.cobblestone, 0, 2);
            return 1;
        }
    }

    static boolean containsHorizontally(StructureBoundingBox bounds, int blockX, int blockZ, int margin) {
        return bounds != null && margin >= 0
            && blockX >= bounds.minX - margin
            && blockX <= bounds.maxX + margin
            && blockZ >= bounds.minZ - margin
            && blockZ <= bounds.maxZ + margin;
    }
}
