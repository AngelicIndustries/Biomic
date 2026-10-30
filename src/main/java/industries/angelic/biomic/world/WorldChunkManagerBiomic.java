package industries.angelic.biomic.world;

import java.util.List;
import java.util.Random;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.structure.MapGenVillage;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.SurfaceBiomeSampler;
import industries.angelic.biomic.world.terrain.MacroTerrainSampler;

/** Deterministic surface-biome manager shared by generation and ordinary 2D biome consumers. */
public final class WorldChunkManagerBiomic extends WorldChunkManager implements BiomicVolumeChunkManager {

    private final MacroTerrainSampler macroTerrainSampler;
    private final SurfaceBiomeSampler biomeSampler;
    private final List<BiomeGenBase> spawnBiomes;

    public WorldChunkManagerBiomic(WorldgenContext context) {
        super();
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        this.macroTerrainSampler = new MacroTerrainSampler(context);
        this.biomeSampler = new SurfaceBiomeSampler(context, macroTerrainSampler);
        this.spawnBiomes = BiomicBiomes.getNaturalSpawnBiomes();
    }

    @Override
    public List<BiomeGenBase> getBiomesToSpawnIn() {
        return spawnBiomes;
    }

    @Override
    public BiomeGenBase getBiomeGenAt(int blockX, int blockZ) {
        // A village layout being built sees its own 3D biome everywhere (VillageContext).
        BiomeGenBase village = industries.angelic.biomic.world.structure.VillageContext.layoutBiome();
        return village != null ? village : biomeSampler.sample(blockX, blockZ);
    }

    @Override
    public float[] getRainfall(float[] rainfall, int blockX, int blockZ, int width, int length) {
        int size = checkedArea(width, length);
        if (rainfall == null || rainfall.length < size) {
            rainfall = new float[size];
        }
        for (int localZ = 0; localZ < length; localZ++) {
            for (int localX = 0; localX < width; localX++) {
                BiomeGenBase biome = biomeSampler.sample(blockX + localX, blockZ + localZ);
                rainfall[localZ * width + localX] = Math.min(1.0F, (float) biome.getIntRainfall() / 65536.0F);
            }
        }
        return rainfall;
    }

    @Override
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length) {
        return fillBiomes(biomes, blockX, blockZ, width, length, 4);
    }

    @Override
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length) {
        return fillBiomes(biomes, blockX, blockZ, width, length, 1);
    }

    @Override
    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length,
        boolean useCache) {
        return fillBiomes(biomes, blockX, blockZ, width, length, 1);
    }

    @Override
    public boolean areBiomesViable(int blockX, int blockZ, int radius, List<BiomeGenBase> allowedBiomes) {
        if (allowedBiomes == null) {
            return false;
        }
        if (allowedBiomes == MapGenVillage.villageSpawnBiomes) {
            // BiomicVillagePlacement previews the complete deterministic piece graph and certifies its exact carved
            // footprint before MapGenVillage accepts a start. These later vanilla queries only decide whether an
            // individual road or building may be appended; repeating a broad 2D biome predicate here can truncate an
            // already-certified village at harmless visual-biome boundaries.
            return true;
        }
        int minQuartX = blockX - radius >> 2;
        int minQuartZ = blockZ - radius >> 2;
        int maxQuartX = blockX + radius >> 2;
        int maxQuartZ = blockZ + radius >> 2;
        for (int quartZ = minQuartZ; quartZ <= maxQuartZ; quartZ++) {
            for (int quartX = minQuartX; quartX <= maxQuartX; quartX++) {
                BiomeGenBase biome = biomeSampler.sample(quartX << 2, quartZ << 2);
                if (!allowedBiomes.contains(biome)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ChunkPosition findBiomePosition(int blockX, int blockZ, int radius, List<BiomeGenBase> allowedBiomes,
        Random random) {
        if (allowedBiomes == null || random == null) {
            return null;
        }
        int minQuartX = blockX - radius >> 2;
        int minQuartZ = blockZ - radius >> 2;
        int maxQuartX = blockX + radius >> 2;
        int maxQuartZ = blockZ + radius >> 2;
        ChunkPosition selected = null;
        int matches = 0;
        for (int quartZ = minQuartZ; quartZ <= maxQuartZ; quartZ++) {
            for (int quartX = minQuartX; quartX <= maxQuartX; quartX++) {
                if (allowedBiomes.contains(biomeSampler.sample(quartX << 2, quartZ << 2))) {
                    matches++;
                    if (random.nextInt(matches) == 0) {
                        selected = new ChunkPosition(quartX << 2, 0, quartZ << 2);
                    }
                }
            }
        }
        return selected;
    }

    @Override
    public void cleanupCache() {}

    public MacroTerrainSampler getMacroTerrainSampler() {
        return macroTerrainSampler;
    }

    private BiomeGenBase[] fillBiomes(BiomeGenBase[] biomes, int startX, int startZ, int width, int length,
        int coordinateScale) {
        int size = checkedArea(width, length);
        if (biomes == null || biomes.length < size) {
            biomes = new BiomeGenBase[size];
        }
        for (int localZ = 0; localZ < length; localZ++) {
            for (int localX = 0; localX < width; localX++) {
                biomes[localZ * width + localX] = biomeSampler
                    .sample((startX + localX) * coordinateScale, (startZ + localZ) * coordinateScale);
            }
        }
        return biomes;
    }

    private static int checkedArea(int width, int length) {
        if (width < 0 || length < 0) {
            throw new IllegalArgumentException("Biome sample dimensions cannot be negative");
        }
        return Math.multiplyExact(width, length);
    }
}
