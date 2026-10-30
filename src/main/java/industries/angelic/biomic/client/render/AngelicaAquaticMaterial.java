package industries.angelic.biomic.client.render;

import net.coderbot.iris.Iris;
import net.minecraft.init.Blocks;

/** Optional bridge, loaded only with Angelica. Never assign the water material to plant geometry. */
final class AngelicaAquaticMaterial {

    private AngelicaAquaticMaterial() {}

    static void beginWater() {
        Iris.setShaderMaterialOverride(Blocks.water, 0);
    }

    static void beginSnow(int layersMetadata) {
        Iris.setShaderMaterialOverride(Blocks.snow_layer, layersMetadata);
    }

    static void endWater() {
        Iris.resetShaderMaterialOverride();
    }
}
