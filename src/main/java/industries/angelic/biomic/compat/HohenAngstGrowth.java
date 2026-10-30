package industries.angelic.biomic.compat;

import java.util.function.Predicate;

import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/**
 * Hohen Angst lets planted Natura and HarvestCraft saplings grow above 256 by lifting their tree generators' height
 * limits. Biomic replays those generators for its own terrain in a 256-high frame (NativeFeatureCapture), where they
 * keep their limits: Biomic tells Hohen Angst when that is so (api.PlantedGrowth).
 */
public final class HohenAngstGrowth {

    private HohenAngstGrowth() {}

    public static void register() {
        if (!cpw.mods.fml.common.Loader.isModLoaded("hohenangst")) return;
        try {
            Predicate<World> capture = NativeFeatureCapture::activeOnThread;
            Class.forName("industries.angelic.hohenangst.api.PlantedGrowth")
                .getMethod("keepLimitsWhile", Predicate.class)
                .invoke(null, capture);
        } catch (ClassNotFoundException older) {
            Biomic.LOG.warn("Hohen Angst has no planted growth hook; planted saplings keep their height limits");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot register Biomic's generation frame with Hohen Angst", error);
        }
    }
}
