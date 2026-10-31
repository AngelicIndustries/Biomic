package industries.angelic.biomic.client;

import makamys.mclib.core.MCLib;
import makamys.mclib.ext.assetdirector.ADConfig;
import makamys.mclib.ext.assetdirector.AssetDirectorAPI;

/**
 * Asks MCLib's Asset Director, shaded into Biomic, to fetch Mojang's assets at the first launch: the 1.20.4 client jar
 * for the textures Biomic uses without shipping them (corals, kelp, dripleaf, smoke and a few more, listed in
 * {@link VanillaTextures}), and 1.21.10's Nether ambience and music (NetherAmbience). Et Futurum Requiem ships its own
 * copy of MCLib; the newest copy present runs the one Asset Director, for both mods.
 */
public final class VanillaArtwork {

    /** The Minecraft version whose client jar holds the textures. */
    public static final String VERSION = "1.20.4";
    /** The version whose sounds NetherAmbience plays, under {@code minecraft_1.21.10}: Et Futurum Requiem's too. */
    public static final String SOUND_VERSION = "1.21.10";
    private static final String[] NETHER_BIOMES = { "nether_wastes", "crimson_forest", "warped_forest",
        "soul_sand_valley", "basalt_deltas" };

    private VanillaArtwork() {}

    /** From Biomic's construction: MCLib starts on both sides; the downloads are the client's, during pre-init. */
    public static void request(boolean client) {
        MCLib.init();
        if (!client) return;
        ADConfig config = new ADConfig();
        config.addJar(VERSION);
        for (String biome : NETHER_BIOMES) {
            config.addSoundEvent(SOUND_VERSION, "music.nether." + biome, "music");
            for (String kind : new String[] { "additions", "loop", "mood" })
                config.addSoundEvent(SOUND_VERSION, "ambient." + biome + "." + kind, "ambient");
        }
        AssetDirectorAPI.register(config);
    }
}
