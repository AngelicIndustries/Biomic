package industries.angelic.biomic.compat.efr;

import industries.angelic.biomic.Biomic;
import makamys.mclib.ext.assetdirector.ADConfig;
import makamys.mclib.ext.assetdirector.AssetDirectorAPI;

/**
 * Asks MCLib's Asset Director, which Et Futurum Requiem ships, to fetch Minecraft's client jar from Mojang at the
 * first launch. Biomic uses some of Mojang's textures (corals, kelp, dripleaf, smoke and a few more, listed in
 * {@code client.VanillaTextures}) and does not ship them.
 */
public final class VanillaArtwork {

    /** The Minecraft version whose client jar holds the textures. */
    public static final String VERSION = "1.20.4";

    private VanillaArtwork() {}

    /** From Biomic's construction, on the client: Asset Director downloads during pre-init. */
    public static void request() {
        try {
            ADConfig config = new ADConfig();
            config.addJar(VERSION);
            AssetDirectorAPI.register(config);
        } catch (LinkageError absent) {
            Biomic.LOG
                .warn("Et Futurum Requiem's Asset Director is missing: Minecraft's textures will be missing", absent);
        }
    }
}
