package industries.angelic.biomic.api;

/**
 * The version of the hooks in this package, for mods that build on Biomic. Changes are additive: a new hook raises it,
 * an existing one keeps its meaning.
 *
 * 1: ForeignGenerators, BlockSubstitutes, EcologyOverlays, PlantLayer, client.PaintedSprite.
 * 2: BiomeCatalogue (every key with its tags, biome and kind; getTags without a world), BiomicBiomeApi.lookup (biome,
 * key and tags in one lookup) and BiomicBiomeApi.columnKeys (the keys of a loaded column by height).
 */
public final class BiomicApi {

    public static final int VERSION = 2;

    private BiomicApi() {}
}
