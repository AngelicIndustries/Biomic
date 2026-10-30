package industries.angelic.biomic.world.structure;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureStart;

import cpw.mods.fml.common.Loader;
import industries.angelic.biomic.config.StructureConfig;

/**
 * Village Names' village layouts (1.14-style buildings, names, signs) for Biomic's planned villages. Village Names
 * installs its generator through Forge's InitMapGenEvent, which Biomic's own village generator does not go through,
 * so Biomic builds its starts the way Village Names' generator would.
 */
final class VillageNamesLayouts {

    private static final boolean INSTALLED = Loader.isModLoaded("VillageNames");

    private VillageNamesLayouts() {}

    static boolean active(StructureConfig.Villages settings) {
        return INSTALLED && settings.villageNames() && Generator.enabled();
    }

    /** A small village takes Village Names' minimum size; a standard one draws its size as Village Names does. */
    static StructureStart start(World world, Random random, int chunkX, int chunkZ, boolean small) {
        return Generator.start(world, random, chunkX, chunkZ, small);
    }

    /** Touches Village Names classes; loaded only when it is installed. */
    private static final class Generator {

        static boolean enabled() {
            return astrotibs.villagenames.config.village.VillageGeneratorConfigHandler.newVillageGenerator;
        }

        static StructureStart start(World world, Random random, int chunkX, int chunkZ, boolean small) {
            float minimum = astrotibs.villagenames.config.village.VillageGeneratorConfigHandler.newVillageSizeMinimum;
            float maximum = astrotibs.villagenames.config.village.VillageGeneratorConfigHandler.newVillageSizeMaximum;
            float mode = astrotibs.villagenames.config.village.VillageGeneratorConfigHandler.newVillageSizeMode;
            int order = astrotibs.villagenames.config.village.VillageGeneratorConfigHandler.newVillageSizeNormalOrder;
            float size = minimum;
            if (!small) {
                // MapGenVillageVN.getStructureStart: a normal-like draw between minimum and maximum around the mode.
                double halfRange = Math.max(Math.abs(maximum - mode), Math.abs(mode - minimum));
                do {
                    size = 0;
                    for (int n = 0; n < order; n++) size += random.nextFloat();
                    size /= order;
                    size *= 2 * halfRange;
                    size += mode - halfRange;
                } while (size > maximum || size < minimum);
            }
            return new astrotibs.villagenames.village.MapGenVillageVN.Start(world, random, chunkX, chunkZ, size - 1F);
        }
    }
}
