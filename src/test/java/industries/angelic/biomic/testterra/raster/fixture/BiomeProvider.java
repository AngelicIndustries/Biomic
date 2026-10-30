package industries.angelic.biomic.testterra.raster.fixture;

public interface BiomeProvider {

    Biome getBiome(int x, int y, int z, long seed);

    java.util.Optional<Biome> getBaseBiome(int x, int z, long seed);

    default Column<Biome> getColumn(int x, int z, long seed, int min, int max) {
        return y -> getBiome(x, y, z, seed);
    }
}
