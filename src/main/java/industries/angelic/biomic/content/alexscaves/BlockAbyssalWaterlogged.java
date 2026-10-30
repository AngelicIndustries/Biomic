package industries.angelic.biomic.content.alexscaves;

import industries.angelic.biomic.content.block.WaterloggedRenderPlant;

/** Type-level marker is deliberately absent from the dry, full geothermal vent. */
final class BlockAbyssalWaterlogged extends BlockAbyssalAquatic implements WaterloggedRenderPlant {

    BlockAbyssalWaterlogged(String name, Kind kind, String... textures) {
        super(name, kind, textures);
    }
}
