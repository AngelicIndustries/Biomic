package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.biomic.Biomic;

/** Client presentation only: never changes registry contents or the saved terrain recipe. */
@Config(modid = Biomic.MODID, category = "visual", filename = Biomic.MODID)
public final class VisualConfig {

    private VisualConfig() {}

    @Config.Comment("Render Silver Birch's protruding 3D branches. Blocks remain registered and saved when disabled.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean silverBirch3dBranches = false;

    @Config.Comment("Let Angelica shaders recognize Biomic leaves/plants using their vanilla foliage materials. Explicit shaderpack mappings take precedence.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean shaderVegetationMaterials = true;

}
