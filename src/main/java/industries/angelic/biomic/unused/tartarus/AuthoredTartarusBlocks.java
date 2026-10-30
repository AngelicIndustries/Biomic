package industries.angelic.biomic.unused.tartarus;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Tartarus' authored soul fire, roots and fungus as blocks of their own. Not registered: the Nether places Et Futurum's
 * soul fire, roots and fungus, so no world ever used these. To bring them back, call {@link #register} from the
 * common proxy's pre-init and map Tartarus' states to them in the block state resolver.
 */
public final class AuthoredTartarusBlocks {

    private AuthoredTartarusBlocks() {}

    public static void register() {
        GameRegistry.registerBlock(new BlockAuthoredSoulFire(), null, "authored_soul_fire");
        if (Loader.isModLoaded("etfuturum")) {
            GameRegistry.registerBlock(new BlockAuthoredTartarusRoots(), null, "authored_tartarus_roots");
            GameRegistry.registerBlock(new BlockAuthoredTartarusFungus(), null, "authored_tartarus_fungus");
        }
    }
}
