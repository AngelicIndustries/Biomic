package industries.angelic.biomic.world.structure;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.MapGenMineshaft;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureMineshaftPieces;
import net.minecraft.world.gen.structure.StructureStart;

import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.WorldgenPhase;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;

/** Biomic-owned adapter around vanilla's ordinary mineshaft pieces and persistence. */
public final class BiomicMineshaftGenerator {

    private static final long POPULATION_SALT = 0xDB4F0B9175AE2165L;

    private final MapGenMineshaft delegate;

    public BiomicMineshaftGenerator(WorldgenContext context, BiomicTerrainSampler terrainSampler) {
        delegate = new MapGenBiomicMineshaft(new BiomicMineshaftPlacement(context, terrainSampler));
    }

    public BiomicMineshaftGenerator(WorldgenContext context, StructureTerrain terrain) {
        delegate = new MapGenBiomicMineshaft(new BiomicMineshaftPlacement(context, terrain));
    }

    public void prepare(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ, Block[] blocks) {
        delegate.func_151539_a(chunkProvider, world, chunkX, chunkZ, blocks);
    }

    public boolean populate(World world, WorldgenContext context, int chunkX, int chunkZ) {
        Random random = new Random(populationSeed(context, chunkX, chunkZ));
        return delegate.generateStructuresInChunk(world, random, chunkX, chunkZ);
    }

    /** Order-independent population: the box as provided (StructurePopulationBox), see BoxedStructureStarts. */
    public boolean populate(World world, WorldgenContext context, int chunkX, int chunkZ,
        StructurePopulationBox.Terrain provided) {
        if (provided == null) return populate(world, context, chunkX, chunkZ);
        MapGenBiomicMineshaft shafts = (MapGenBiomicMineshaft) delegate;
        boolean drawn = shafts.draw(
            world,
            context.getSettings()
                .getWorldSeed() ^ POPULATION_SALT,
            chunkX,
            chunkZ,
            provided);
        shafts.supports(world, chunkX, chunkZ);
        return drawn;
    }

    public void recreate(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ) {
        prepare(chunkProvider, world, chunkX, chunkZ, null);
    }

    static long populationSeed(WorldgenContext context, int chunkX, int chunkZ) {
        return WorldgenContext.mix64(context.seedForChunk(WorldgenPhase.STRUCTURES, chunkX, chunkZ) ^ POPULATION_SALT);
    }

    private static final class MapGenBiomicMineshaft extends MapGenMineshaft {

        private final BiomicMineshaftPlacement placement;
        private StructureStart admittedStart;
        private int admittedX, admittedZ;
        private final java.util.Map<Long, Boolean> rejected = new java.util.LinkedHashMap<Long, Boolean>(
            256,
            .75F,
            true) {

            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<Long, Boolean> entry) {
                return size() > 256;
            }
        };

        private final BoxedStructureStarts boxed = new BoxedStructureStarts();

        private MapGenBiomicMineshaft(BiomicMineshaftPlacement placement) {
            this.placement = placement;
        }

        private boolean draw(World world, long seed, int chunkX, int chunkZ, StructurePopulationBox.Terrain provided) {
            return boxed.draw(structureMap, world, seed, chunkX, chunkZ, provided);
        }

        /**
         * Where a corridor crosses a cave on vanilla's plank bridge, fence posts carry it down to the cave's floor
         * under its support frames (every five blocks, both sides), as newer mineshafts are propped (review: posts
         * rather than keeping caves away). Mineshafts stand in rock pockets, so this is for the odd small gap: none
         * where
         * the floor is liquid or farther down than {@link #POST_REACH} ("the lower support sometimes are ridiculously
         * long").
         * Read from the box as provided and the corridor's own writes, so every box agrees on its columns.
         */
        private void supports(World world, int chunkX, int chunkZ) {
            int minX = (chunkX << 4) + 8, minZ = (chunkZ << 4) + 8;
            StructureBoundingBox box = new StructureBoundingBox(minX, minZ, minX + 15, minZ + 15);
            for (Object value : structureMap.values()) {
                StructureStart start = (StructureStart) value;
                if (!start.getBoundingBox()
                    .intersectsWith(box)) continue;
                for (Object component : start.getComponents()) {
                    if (!(component instanceof StructureMineshaftPieces.Corridor corridor) || !corridor.getBoundingBox()
                        .intersectsWith(box)) continue;
                    var piece = (industries.angelic.biomic.mixins.early.minecraft.AccessorStructureComponent) corridor;
                    int sections = ((industries.angelic.biomic.mixins.early.minecraft.AccessorMineshaftCorridor) corridor)
                        .biomic$sectionCount();
                    for (int section = 0; section < sections; section++) for (int side = 0; side <= 2; side += 2) {
                        int along = 2 + section * 5;
                        int x = piece.biomic$x(side, along), z = piece.biomic$z(side, along), y = piece.biomic$y(-1);
                        if (x < minX || x > minX + 15 || z < minZ || z > minZ + 15) continue;
                        if (world.getBlock(x, y, z) != Blocks.planks || world.getBlock(x, y - 1, z)
                            .getMaterial() != Material.air) continue;
                        int floor = y - 1;
                        while (floor > y - POST_REACH && world.getBlock(x, floor, z)
                            .getMaterial() == Material.air) floor--;
                        Material ground = world.getBlock(x, floor, z)
                            .getMaterial();
                        if (ground == Material.air || ground.isLiquid()) continue;
                        for (int post = y - 1; post > floor; post--) world.setBlock(x, post, z, Blocks.fence, 0, 2);
                    }
                }
            }
        }

        private static final int POST_REACH = 6;

        @Override
        protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
            admittedStart = null;
            if (!placement.canSpawnAt(chunkX, chunkZ)) return false;
            long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
            if (rejected.containsKey(key)) return false;
            StructureStart candidate = StructureOffsets
                .raise(super.getStructureStart(chunkX, chunkZ), placement.verticalOffset());
            candidate = StructureOffsets.raise(candidate, placement.pocketShift(candidate));
            Integer burial = placement.burial(candidate);
            if (burial == null || !placement.hasHostRockForLayout(StructureOffsets.raise(candidate, burial))) {
                rejected.put(key, Boolean.TRUE);
                return false;
            }
            admittedStart = candidate;
            admittedX = chunkX;
            admittedZ = chunkZ;
            return true;
        }

        @Override
        protected StructureStart getStructureStart(int chunkX, int chunkZ) {
            if (admittedStart == null || admittedX != chunkX || admittedZ != chunkZ) {
                throw new IllegalStateException("Mineshaft layout was not checked before placement");
            }
            StructureStart result = admittedStart;
            admittedStart = null;
            // The actual vanilla start is persisted normally, including corridor/spawner state and RNG.
            return result;
        }
    }
}
