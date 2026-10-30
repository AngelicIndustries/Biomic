package industries.angelic.biomic.client.render;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.content.block.BlockAquaticPlant;
import industries.angelic.biomic.content.block.BlockRuCaveModel;

/** Render-pass classification only; these blocks still contain water in the actual world. */
public final class AquaticRenderSemantics {

    private AquaticRenderSemantics() {}

    public static Material material(Block block, Material actual, int pass) {
        if (block instanceof industries.angelic.biomic.content.block.BlockSkylandsWetMangroveRoots)
            return pass == 0 ? Material.wood : Material.water;
        return pass == 0 && (block instanceof BlockAquaticPlant
            || block instanceof industries.angelic.biomic.content.block.WaterloggedRenderPlant wet
                && wet.containsSourceWater()
            || block instanceof BlockRuCaveModel cave && cave.isWaterlogged()) ? Material.plants : actual;
    }
}
