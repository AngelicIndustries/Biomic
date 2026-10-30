package industries.angelic.biomic.world.structure;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.structure.MapGenStronghold;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureStrongholdPieces;

import industries.angelic.biomic.biome.BiomicBiomes;

/** Biomic-owned adapter around vanilla's persisted stronghold implementation. */
public final class BiomicStrongholdGenerator {

    public static final String STRUCTURE_NAME = "Stronghold";

    private static final long POPULATION_SALT = 0x5B1E6C9D2A4F8317L, LAYOUT_SALT = 0x2D7E4B1F93C6A805L;

    private final MapGenStronghold delegate;
    private final BoxedStructureStarts boxed = new BoxedStructureStarts();
    private java.util.Map<Long, net.minecraft.world.gen.structure.StructureStart> starts;

    public BiomicStrongholdGenerator(StructureTerrain terrain) {
        var settings = industries.angelic.biomic.config.StructureConfig.get().strongholds;
        java.util.Map<String, String> options = new java.util.HashMap<>();
        options.put("count", String.valueOf(settings.count()));
        options.put("distance", String.valueOf(settings.distance()));
        options.put("spread", String.valueOf(settings.spread()));
        Shelter shelter = new Shelter(terrain);
        delegate = new MapGenStronghold(options) {

            {
                starts = structureMap;
            }

            private java.util.Set<Long> sites;
            private List<ChunkPosition> siteList;

            @Override
            protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
                if (!settings.enabled()) return false;
                lay();
                return sites.contains(ChunkCoordIntPair.chunkXZ2Int(chunkX, chunkZ));
            }

            /** Where eyes of ender lead: the moved sites. */
            @Override
            protected List<ChunkPosition> getCoordList() {
                lay();
                return siteList;
            }

            /**
             * Vanilla's ring, each site then moved to the nearest chunk where the whole stronghold fits under ground.
             */
            private synchronized void lay() {
                if (sites != null) return;
                super.canSpawnStructureAtCoords(0, 0);
                java.util.Set<Long> chosen = new java.util.HashSet<>();
                List<ChunkPosition> list = new java.util.ArrayList<>();
                for (ChunkPosition ring : super.getCoordList()) {
                    long site = shelter.site(ring.chunkPosX >> 4, ring.chunkPosZ >> 4, this::build);
                    int x = (int) site, z = (int) (site >>> 32);
                    chosen.add(site);
                    list.add(new ChunkPosition((x << 4) + 8, ring.chunkPosY, (z << 4) + 8));
                    industries.angelic.biomic.Biomic.LOG.info(
                        "Stronghold at chunk {} {} (ring site {} {})",
                        x,
                        z,
                        ring.chunkPosX >> 4,
                        ring.chunkPosZ >> 4);
                }
                siteList = Collections.unmodifiableList(list);
                sites = chosen;
            }

            @Override
            protected StructureStart getStructureStart(int chunkX, int chunkZ) {
                StructureStart start = build(chunkX, chunkZ);
                return StructureOffsets.raise(start, shelter.burial(start));
            }

            /**
             * Vanilla's layout (retried until it has a portal room), from a random of the chunk alone, so the layout
             * tried while choosing a site is the one generated there.
             */
            private StructureStart build(int chunkX, int chunkZ) {
                Random random = new Random(
                    worldObj.getSeed() ^ LAYOUT_SALT ^ chunkX * 0x9E3779B97F4A7C15L ^ chunkZ * 0xC2B2AE3D27D4EB4FL);
                MapGenStronghold.Start start;
                do start = new MapGenStronghold.Start(worldObj, random, chunkX, chunkZ); while (start.getComponents()
                    .isEmpty()
                    || ((StructureStrongholdPieces.Stairs2) start.getComponents()
                        .get(0)).strongholdPortalRoom == null);
                return StructureOffsets.raise(start, terrain.verticalOffset());
            }
        };
        delegate.field_151546_e.clear();
        delegate.field_151546_e.addAll(BiomicBiomes.getStrongholdSurfaceBiomes());
    }

    public void prepare(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ, Block[] blocks) {
        delegate.func_151539_a(chunkProvider, world, chunkX, chunkZ, blocks);
    }

    public boolean populate(World world, Random random, int chunkX, int chunkZ) {
        return delegate.generateStructuresInChunk(world, random, chunkX, chunkZ);
    }

    /** Order-independent population: the box as provided (StructurePopulationBox), see BoxedStructureStarts. */
    public boolean populate(World world, int chunkX, int chunkZ, StructurePopulationBox.Terrain provided) {
        return boxed.draw(starts, world, world.getSeed() ^ POPULATION_SALT, chunkX, chunkZ, provided);
    }

    public ChunkPosition findClosest(World world, int blockX, int blockY, int blockZ) {
        return delegate.func_151545_a(world, blockX, blockY, blockZ);
    }

    public void recreate(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ) {
        prepare(chunkProvider, world, chunkX, chunkZ, null);
    }

    public static boolean handles(String structureName) {
        return STRUCTURE_NAME.equals(structureName);
    }

    /**
     * Keeps strongholds under ground (review, 2026-10-24: one floated in the air over a sinkhole). Vanilla checks the
     * biome at one point only, and a stronghold reaches up to 112 blocks from its start, so each site is moved to the
     * nearest chunk where the stronghold laid out there can sink under the ground above every piece without reaching
     * the world's floor, and is lowered that far.
     */
    static final class Shelter {

        /** Rock kept over every piece, and over the world's bottom. */
        private static final int COVER = 8, FLOOR = 6;
        /** How far a site may move, in chunks, and the lattice the ground is read on. */
        private static final int SEARCH = 12, LATTICE = 8;
        private static final int[][] OFFSETS = offsets(SEARCH);

        private final StructureTerrain terrain;
        private final java.util.Map<Long, Integer> ground = new java.util.HashMap<>();

        Shelter(StructureTerrain terrain) {
            this.terrain = terrain;
        }

        /** The nearest chunk to {@code (chunkX, chunkZ)} whose stronghold fits under ground, packed x | z << 32. */
        long site(int chunkX, int chunkZ, java.util.function.BiFunction<Integer, Integer, StructureStart> layout) {
            for (int[] offset : OFFSETS) {
                int x = chunkX + offset[0], z = chunkZ + offset[1];
                if (fits(layout.apply(x, z))) return pack(x, z);
            }
            return pack(chunkX, chunkZ);
        }

        private boolean fits(StructureStart start) {
            int sink = sink(start);
            return start.getBoundingBox().minY + sink >= terrain.minY() + FLOOR;
        }

        /**
         * How far to lower a laid-out start so it fits (0 where it can't: lowered to the floor, it would still show).
         */
        int burial(StructureStart start) {
            return start == null || !fits(start) ? 0 : sink(start);
        }

        /** How far (0 or negative) every piece must sink to stay COVER under the lowest ground above it. */
        private int sink(StructureStart start) {
            int shift = 0;
            for (Object component : start.getComponents()) {
                var box = ((net.minecraft.world.gen.structure.StructureComponent) component).getBoundingBox();
                int low = Integer.MAX_VALUE;
                for (int lx = Math.floorDiv(box.minX - LATTICE / 2, LATTICE); lx
                    <= Math.floorDiv(box.maxX + LATTICE / 2, LATTICE); lx++)
                    for (int lz = Math.floorDiv(box.minZ - LATTICE / 2, LATTICE); lz
                        <= Math.floorDiv(box.maxZ + LATTICE / 2, LATTICE); lz++) low = Math.min(low, ground(lx, lz));
                shift = Math.min(shift, low - COVER - box.maxY);
            }
            return shift;
        }

        private synchronized int ground(int lx, int lz) {
            return ground.computeIfAbsent(pack(lx, lz), k -> terrain.groundHeight(lx * LATTICE, lz * LATTICE));
        }

        private static long pack(int x, int z) {
            return x & 0xFFFF_FFFFL | (long) z << 32;
        }

        /** Chunk offsets within {@code radius}, nearest first. */
        private static int[][] offsets(int radius) {
            List<int[]> offsets = new java.util.ArrayList<>();
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++)
                    if (dx * dx + dz * dz <= radius * radius) offsets.add(new int[] { dx, dz });
            }
            offsets.sort(java.util.Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
            return offsets.toArray(new int[0][]);
        }
    }
}
