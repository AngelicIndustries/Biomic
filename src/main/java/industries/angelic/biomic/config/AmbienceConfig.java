package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.biomic.Biomic;

/**
 * The Tartarus Nether's modern biome ambience. Client only: each player chooses their own, and a server never reads
 * it. The sounds are the ones Et Futurum Requiem downloads from Mojang; a sound that isn't there is skipped.
 */
@Config(modid = Biomic.MODID, category = "ambience", filename = Biomic.MODID)
@Config.LangKeyPattern(pattern = "biomic.config.%cat.%field")
public final class AmbienceConfig {

    private AmbienceConfig() {}

    @Config.Comment("Play each Tartarus Nether biome's modern ambient loop and its random sounds. While on, "
        + "Netherlicious' and Et Futurum's own Nether loops stay quiet there.")
    @Config.DefaultBoolean(true)
    @Config.Order(0)
    public static boolean netherSounds = true;

    @Config.Comment("Replace the cave sound in the Tartarus Nether with the biome's modern mood sound.")
    @Config.DefaultBoolean(true)
    @Config.Order(1)
    public static boolean netherMood = true;

    @Config.Comment("Play each Tartarus Nether biome's modern music instead of C418's Nether tracks. Warped forests "
        + "keep C418's tracks.")
    @Config.DefaultBoolean(true)
    @Config.Order(2)
    public static boolean netherMusic = true;

    @Config.Comment("Show the biomes' ambient particles: crimson and warped spores, ash and white ash. They follow the "
        + "video settings' particle level.")
    @Config.DefaultBoolean(true)
    @Config.Order(3)
    public static boolean netherParticles = true;

    @Config.Comment("How many ambient particles to show. 1 is vanilla's density.")
    @Config.DefaultFloat(1F)
    @Config.RangeFloat(min = 0F, max = 4F)
    @Config.Order(4)
    public static float netherParticleScale = 1F;

    @Config.Comment("An extra scale for the basalt biomes' white ash, by far the densest: at vanilla's density it keeps "
        + "a few thousand particles alive.")
    @Config.DefaultFloat(.5F)
    @Config.RangeFloat(min = 0F, max = 1F)
    @Config.Order(5)
    public static float netherWhiteAshScale = .5F;
}
