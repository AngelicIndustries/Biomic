package industries.angelic.biomic.world.structure;

import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.biome.VerticalBiomeSampler;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.modern.ModernBiomeCompatibility;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;
import industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler;
import industries.angelic.biomic.world.terrain.MarshGround;
import industries.angelic.biomic.world.terrain.ShiftedGroundHeights;
import industries.angelic.biomic.world.terrain.ShiftedSkyIslands;

/**
 * Terrain queries used to admit vanilla structures, in physical coordinates. Vanilla's underground pieces are laid
 * out in legacy Y (sea level 63) and moved up by {@link #verticalOffset()}: 0 on compressed 256 worlds, 128 on
 * shifted512 ones, where Origen's authored Y sits one-to-one on physical Y-128.
 */
public interface StructureTerrain {

    ChunkVerticalLayout layout();

    int verticalOffset();

    int seaLevel();

    /** Physical blocks per compressed-256 block of Origen relief (1 at 256, 1.5 uncompressed at 512). */
    default double reliefScale() {
        return 1.0D;
    }

    /** Lowest and one-past-highest physical Y of the terrain envelope. */
    int minY();

    int maxY();

    /** Cover above a mineshaft (the 256 adapter keeps its historical uncarved surface). */
    int surfaceHeight(int x, int z);

    int carvedGroundHeight(int x, int z);

    /** Highest solid Y of the authored terrain without carvers: cheap enough to plan villages over wide areas. */
    default int groundHeight(int x, int z) {
        return carvedGroundHeight(x, z);
    }

    /** 0 where the terrain has no separate river field. */
    double riverStrength(int x, int z);

    /** Host rock at one point, without caves (cheap). */
    boolean solidAt(int x, int y, int z);

    /** Complete physical materials of one chunk, caves included; only when {@link #exactMaterials()}. */
    byte[] materials(int chunkX, int chunkZ);

    /** False where only column heights are cheap enough for the server thread. */
    default boolean exactMaterials() {
        return true;
    }

    /** Sky terrain (floating islands) in one column: {@code bottom << 16 | top}, or -1 where it has none. */
    default int skySpan(int x, int z) {
        return -1;
    }

    /** {@link #skySpan} for lone columns spread over many chunks (a site search's probes). */
    default int skyColumn(int x, int z) {
        return skySpan(x, z);
    }

    /** A cheap estimate of the sky terrain's top in a column, or -1, for searching wide areas. */
    default int skyTopEstimate(int x, int z) {
        int span = skySpan(x, z);
        return span < 0 ? -1 : spanTop(span);
    }

    static int spanTop(int span) {
        return span & 0xFFFF;
    }

    static int spanBottom(int span) {
        return span >>> 16;
    }

    /** The island cluster of one cluster region (FloatingIslandDensitySampler.CLUSTER_REGION blocks), or null. */
    default FloatingIslandDensitySampler.Cluster skyCluster(int regionX, int regionZ) {
        return null;
    }

    /** The biome of the sky terrain at a point (its island's habitat), or null. */
    default BiomeGenBase skyBiome(int x, int y, int z) {
        return null;
    }

    /** Origen's authored Y for a physical Y. */
    int authoredY(int physicalY);

    /** The lowest physical Y whose authored Y reaches {@code authored} (the inverse of {@link #authoredY}). */
    default int physicalY(int authored) {
        for (int y = minY(); y < maxY(); y++) if (authoredY(y) >= authored) return y;
        return maxY() - 1;
    }

    private static BiomeGenBase habitatBiome(String habitat) {
        return habitat == null ? null : ModernBiomeCompatibility.biome(habitat);
    }

    static StructureTerrain legacy(WorldgenContext context, BiomicTerrainSampler terrain) {
        int seaLevel = context.getSeaLevel();
        return new StructureTerrain() {

            @Override
            public ChunkVerticalLayout layout() {
                return ChunkVerticalLayout.LEGACY_256;
            }

            @Override
            public int verticalOffset() {
                return 0;
            }

            @Override
            public int seaLevel() {
                return seaLevel;
            }

            @Override
            public int minY() {
                return 0;
            }

            @Override
            public int maxY() {
                return 256;
            }

            @Override
            public int surfaceHeight(int x, int z) {
                return terrain.sampleSurfaceHeight(x, z);
            }

            @Override
            public int carvedGroundHeight(int x, int z) {
                return terrain.sampleCarvedGroundSurfaceHeight(x, z);
            }

            @Override
            public int groundHeight(int x, int z) {
                return terrain.sampleAuthoredGroundHeight(x, z);
            }

            @Override
            public double riverStrength(int x, int z) {
                return terrain.sampleRiverStrength(x, z);
            }

            @Override
            public boolean solidAt(int x, int y, int z) {
                return terrain.sampleDensity(x, y, z) > 0.0D;
            }

            @Override
            public byte[] materials(int cx, int cz) {
                return terrain.createMaterialArray(cx, cz);
            }

            @Override
            public int skySpan(int x, int z) {
                return terrain.floatingSpan(x, z);
            }

            @Override
            public int skyTopEstimate(int x, int z) {
                return terrain.floatingTopEstimate(x, z);
            }

            @Override
            public FloatingIslandDensitySampler.Cluster skyCluster(int rx, int rz) {
                return terrain.floatingCluster(rx, rz);
            }

            @Override
            public BiomeGenBase skyBiome(int x, int y, int z) {
                return habitatBiome(terrain.floatingHabitatAt(x, y, z));
            }

            @Override
            public int authoredY(int physicalY) {
                return BiomicTerrainSampler.authoredY(physicalY);
            }
        };
    }

    /**
     * Column heights only: building the chunks a village footprint or mineshaft layout touches (and their cave
     * halos) on the server thread stalled a tick by a quarter second.
     */
    static StructureTerrain shifted512(WorldgenContext context, VerticalBiomeSampler biomes, ShiftedSkyIslands sky) {
        // The ground as the marsh leaves it: villages planned on Origen's stood on pillars of earth above a marsh.
        MarshGround ground = new MarshGround(context, new ShiftedGroundHeights(context));
        return new StructureTerrain() {

            @Override
            public ChunkVerticalLayout layout() {
                return ChunkVerticalLayout.EXTENDED_512;
            }

            @Override
            public int verticalOffset() {
                return 128;
            }

            @Override
            public int seaLevel() {
                return 191;
            }

            @Override
            public double reliefScale() {
                return 1.5D;
            }

            @Override
            public int minY() {
                return 64;
            }

            @Override
            public int maxY() {
                return 448;
            }

            @Override
            public int surfaceHeight(int x, int z) {
                return ground.ground(x, z);
            }

            @Override
            public int carvedGroundHeight(int x, int z) {
                return ground.ground(x, z);
            }

            @Override
            public double riverStrength(int x, int z) {
                return 0.0D;
            }

            @Override
            public boolean solidAt(int x, int y, int z) {
                return y <= ground.ground(x, z);
            }

            @Override
            public byte[] materials(int cx, int cz) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean exactMaterials() {
                return false;
            }

            @Override
            public int skySpan(int x, int z) {
                return sky == null ? -1 : sky.span(x, z);
            }

            @Override
            public int skyColumn(int x, int z) {
                return sky == null ? -1 : sky.columnSpan(x, z);
            }

            @Override
            public int skyTopEstimate(int x, int z) {
                return sky == null ? -1 : sky.estimateTop(x, z);
            }

            @Override
            public FloatingIslandDensitySampler.Cluster skyCluster(int rx, int rz) {
                return sky == null ? null : sky.cluster(rx, rz);
            }

            @Override
            public BiomeGenBase skyBiome(int x, int y, int z) {
                return sky == null ? null : habitatBiome(sky.habitatAt(x, y, z));
            }

            @Override
            public int authoredY(int physicalY) {
                return physicalY - 128;
            }
        };
    }
}
