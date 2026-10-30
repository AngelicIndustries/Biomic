package industries.angelic.biomic.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;

import com.google.gson.JsonObject;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.world.terrain.TartarusManifest;
import industries.angelic.biomic.world.terrain.TerraPackChunkRasterizer;

/**
 * The Tartarus Nether's biome map: Tartarus' twelve biomes and the other mods' biomes this world pinned. It also
 * owns the (neighbour-joined) pack manifest and rasterizer the Nether chunk provider generates with.
 */
public final class WorldChunkManagerBiomicNether extends WorldChunkManager {

    private final JsonObject manifest;
    private final TerraPackChunkRasterizer rasterizer;
    /** Biome by index: Tartarus' in enum order, then the neighbours. Chunk payloads store the index. */
    private final BiomeGenBase[] biomes;
    private final NetherNeighbour[] neighbourByIndex;
    private final Map<String, Integer> indexByPackId = new HashMap<>();
    private final List<BiomeGenBase> allBiomes = new ArrayList<>();

    public static WorldChunkManagerBiomicNether forWorld(World world) {
        return new WorldChunkManagerBiomicNether(world.getSeed(), NetherWorldData.neighbours(world));
    }

    WorldChunkManagerBiomicNether(long seed, List<NetherNeighbour> neighbours) {
        super();
        List<TartarusManifest.Neighbour> specs = new ArrayList<>();
        for (NetherNeighbour neighbour : neighbours) specs.add(neighbour.spec());
        if (!specs.isEmpty()) industries.angelic.biomic.Biomic.LOG.info(
            "Tartarus Nether neighbours: {}",
            specs.stream()
                .map(spec -> spec.id() + " on " + spec.terrainOf() + " " + spec.top() + "/" + spec.filler())
                .toList());
        manifest = TartarusManifest.load(specs);
        rasterizer = TerraPackChunkRasterizer.tartarus(manifest, seed);
        int count = TartarusBiomes.values().length + neighbours.size();
        biomes = new BiomeGenBase[count];
        neighbourByIndex = new NetherNeighbour[count];
        for (TartarusBiomes biome : TartarusBiomes.values()) {
            biomes[biome.ordinal()] = BiomicBiomes.getTartarusBiome(biome);
            indexByPackId.put(biome.name(), biome.ordinal());
        }
        for (int i = 0; i < neighbours.size(); i++) {
            int index = TartarusBiomes.values().length + i;
            biomes[index] = neighbours.get(i)
                .biome();
            neighbourByIndex[index] = neighbours.get(i);
            indexByPackId.put(
                neighbours.get(i)
                    .packId(),
                index);
        }
        allBiomes.addAll(List.of(biomes));
    }

    JsonObject manifest() {
        return manifest;
    }

    /**
     * Pack id to modern key of Regions Unexplored's Nether biomes in this map, which are decorated with the terrain.
     */
    Map<String, String> ru() {
        Map<String, String> ru = new HashMap<>();
        for (NetherNeighbour neighbour : neighbourByIndex)
            if (neighbour != null && neighbour.dressing == NetherNeighbour.Dressing.RU)
                ru.put(neighbour.packId(), RuNetherDressing.modernKey(neighbour.key));
        return ru;
    }

    /** Pack id to biome key of the Gardens biomes in this map, which are decorated with the terrain. */
    Map<String, String> gardens() {
        Map<String, String> gardens = new HashMap<>();
        for (NetherNeighbour neighbour : neighbourByIndex)
            if (neighbour != null && neighbour.dressing == NetherNeighbour.Dressing.GARDENS)
                gardens.put(neighbour.packId(), neighbour.key);
        return gardens;
    }

    TerraPackChunkRasterizer rasterizer() {
        return rasterizer;
    }

    /** Index of the column's biome in this map. */
    int biomeIndexAt(int blockX, int blockZ) {
        String id = rasterizer.surfaceBiome(blockX, blockZ);
        Integer index = indexByPackId.get(id);
        if (index == null) throw new IllegalStateException("Not a generated Nether biome: " + id);
        return index;
    }

    BiomeGenBase biome(int index) {
        return biomes[index];
    }

    /** The neighbour a biome index stands for, or null for Tartarus' own. */
    NetherNeighbour neighbour(int index) {
        return neighbourByIndex[index];
    }

    private BiomeGenBase sample(int blockX, int blockZ) {
        return biomes[biomeIndexAt(blockX, blockZ)];
    }

    @Override
    public List<BiomeGenBase> getBiomesToSpawnIn() {
        return allBiomes;
    }

    @Override
    public BiomeGenBase getBiomeGenAt(int blockX, int blockZ) {
        return sample(blockX, blockZ);
    }

    @Override
    public float[] getRainfall(float[] rainfall, int blockX, int blockZ, int width, int length) {
        int size = checkedArea(width, length);
        if (rainfall == null || rainfall.length < size) rainfall = new float[size];
        java.util.Arrays.fill(rainfall, 0, size, 0F);
        return rainfall;
    }

    @Override
    public BiomeGenBase[] getBiomesForGeneration(BiomeGenBase[] biomes, int quartX, int quartZ, int width, int length) {
        return fill(biomes, quartX, quartZ, width, length, 4);
    }

    @Override
    public BiomeGenBase[] loadBlockGeneratorData(BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length) {
        return fill(biomes, blockX, blockZ, width, length, 1);
    }

    @Override
    public BiomeGenBase[] getBiomeGenAt(BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length,
        boolean useCache) {
        return fill(biomes, blockX, blockZ, width, length, 1);
    }

    @Override
    public boolean areBiomesViable(int blockX, int blockZ, int radius, List<BiomeGenBase> allowedBiomes) {
        if (allowedBiomes == null) return false;
        for (int quartZ = blockZ - radius >> 2; quartZ <= blockZ + radius >> 2; quartZ++)
            for (int quartX = blockX - radius >> 2; quartX <= blockX + radius >> 2; quartX++)
                if (!allowedBiomes.contains(sample(quartX << 2, quartZ << 2))) return false;
        return true;
    }

    @Override
    public ChunkPosition findBiomePosition(int blockX, int blockZ, int radius, List<BiomeGenBase> allowedBiomes,
        Random random) {
        if (allowedBiomes == null || random == null) return null;
        ChunkPosition selected = null;
        int matches = 0;
        for (int quartZ = blockZ - radius >> 2; quartZ <= blockZ + radius >> 2; quartZ++)
            for (int quartX = blockX - radius >> 2; quartX <= blockX + radius >> 2; quartX++)
                if (allowedBiomes.contains(sample(quartX << 2, quartZ << 2)) && random.nextInt(++matches) == 0)
                    selected = new ChunkPosition(quartX << 2, 0, quartZ << 2);
        return selected;
    }

    @Override
    public void cleanupCache() {}

    private BiomeGenBase[] fill(BiomeGenBase[] out, int startX, int startZ, int width, int length, int scale) {
        int size = checkedArea(width, length);
        if (out == null || out.length < size) out = new BiomeGenBase[size];
        for (int localZ = 0; localZ < length; localZ++) for (int localX = 0; localX < width; localX++)
            out[localZ * width + localX] = sample((startX + localX) * scale, (startZ + localZ) * scale);
        return out;
    }

    private static int checkedArea(int width, int length) {
        if (width < 0 || length < 0) throw new IllegalArgumentException("Biome sample dimensions cannot be negative");
        return Math.multiplyExact(width, length);
    }
}
