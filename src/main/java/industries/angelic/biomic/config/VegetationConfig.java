package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.biomic.Biomic;

/** Pack choices for new hosted RU vegetation; does not alter the standalone reference-parity generator. */
@Config(modid = Biomic.MODID, category = "vegetation", filename = Biomic.MODID)
public final class VegetationConfig {

    private VegetationConfig() {}

    @Config.Comment("Generate large Blackwood bioshrooms in new blended-world chunks. Existing mushrooms are retained.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean blackwoodLargeMushrooms = false;
    @Config.Comment("Generate small and two-block-tall Blackwood bioshrooms in new blended-world chunks.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean blackwoodSmallMushrooms = false;
    @Config.Comment("Put shroomlights inside large Blackwood bioshrooms. When off, use matching cap blocks instead.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean blackwoodShroomlights = false;
}
