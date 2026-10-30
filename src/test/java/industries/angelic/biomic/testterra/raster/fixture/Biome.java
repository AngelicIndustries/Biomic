package industries.angelic.biomic.testterra.raster.fixture;

import industries.angelic.biomic.testterra.raster.config.noise.BiomeNoiseProperties;

public record Biome(String id, BiomeNoiseProperties properties) {

    public Context getContext() {
        return new Context(properties);
    }

    public record Context(BiomeNoiseProperties properties) {

        public BiomeNoiseProperties get(PropertyKey<BiomeNoiseProperties> key) {
            return properties;
        }
    }
}
