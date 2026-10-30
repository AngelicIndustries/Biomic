package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.biomic.Biomic;

@Config(modid = Biomic.MODID, category = "expansion", filename = Biomic.MODID)
public final class ExpansionConfig {

    private ExpansionConfig() {}

    @Config.Comment("NEW Biomic worlds only: protected UltimateCaves Worley tunnels joined to existing caves. "
        + "Saved worlds retain their pinned setting. Origen-only and older worlds remain unchanged.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean ultimateCaves = true;

    @Config.Comment("NEW worlds with Ultimate Caves: YUNG's Better Caves caverns and liquid caverns, plus Ultimate's "
        + "large chambers, in sparse regions blended into the same protected cave field. Saved worlds keep their setting.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean yungCaves = true;

    @Config.Comment("NEW Hohen Angst 512 worlds: the blend's floating islands (sky donuts over open ocean or land, "
        + "Aeropelago over deep ocean) in the open sky above uncompressed Origen. Saved worlds keep their setting.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean skyIslands = true;

    @Config.Comment("NEW worlds: sky islands may have a biome of their own, unlike the ground below them. False (the "
        + "default): an island over land takes the biome below it (islands over water always choose their own). Saved "
        + "worlds keep their setting.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean skyIslandOwnBiomes = false;

    @Config.Comment("NEW worlds: how often an island of a group has a biome of its own rather than the group's, in "
        + "percent. 0: every group of islands shares one biome; 100: each island chooses for itself. Saved worlds keep "
        + "their setting.")
    @Config.DefaultInt(100)
    @Config.RangeInt(min = 0, max = 100)
    @Config.RequiresMcRestart
    public static int skyIslandBiomeVariety = 100;

    @Config.Comment("NEW expanded Biomic worlds: EFR native glow lichen and humid-cave vines on new bare "
        + "passages. Preserves RU cave ownership, Origen-only worlds and existing saves. No duplicate ore pass.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean efrCaveEcology = true;

    @Config.Comment("NEW Biomic worlds only: Alex's Caves' Abyssal Chasm (its Ocean Trench and waterlogged ecology) in deep "
        + "oceans, generated chunk by chunk at both heights. Natural terrain/ecology only: no rusty metal, built ruins, whale bones "
        + "or whale falls. Saved worlds retain their pinned choice.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean abyssalChasm = true;

    @Config.Comment("NEW Biomic worlds: the Nether is Jaddot's Tartarus terrain pack (12 biomes, 256 blocks tall), "
        + "generated chunk by chunk. Saved worlds keep the Nether they were created with.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean tartarusNether = true;

    @Config.Comment("NEW Biomic saves only: use installed SaltyMod Expanded 2.0.8 native marsh soil/plants "
        + "in a share of Origen wetland provinces. Existing saves keep their pinned providers. Soft dependency.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean saltyMarsh = true;

    @Config.Comment("NEW Biomic saves only: audited native BOP 2.1.0.2308 habitat profiles "
        + "on compatible Origen landforms and sky islands. Requires separately installed BOP; pins enabled profiles per save.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean bopHabitats = true;

    @Config.Comment("NEW Biomic saves only: false keeps the curated 11 Overworld BOP habitats "
        + "that complement RU/Origen. All 6 available Nether habitats remain selected in either mode. "
        + "True restores all available audited Overworld BOP profiles for full-catalogue reviews. "
        + "Does not enable native-disabled biomes, change BOP reference worlds, or alter existing saved selections.")
    @Config.DefaultBoolean(false)
    @Config.RequiresMcRestart
    public static boolean bopFullCatalogue = false;

    @Config.Comment("NEW worlds with Biomes O' Plenty: its Nether biomes (Undergarden, Polar Chasm, Boneyard) in the "
        + "Tartarus Nether. Saved worlds keep the Nether they were created with.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean bopNetherHabitats = true;

    @Config.Comment("NEW worlds with Netherlicious: its Nether biomes (Foxfire Swamp, Abyssal Shadowland, Ruptured Chasm, "
        + "Glow Jungle, Burning Desert, Tainted Shroom Cave) in the Tartarus Nether. Saved worlds keep the Nether they were "
        + "created with.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean netherliciousNetherHabitats = true;

    @Config.Comment("NEW worlds: Gardens of the Dead's Soulblight Forest and Whistling Woods in the Tartarus Nether. "
        + "Saved worlds keep the Nether they were created with.")
    @Config.DefaultBoolean(true)
    @Config.RequiresMcRestart
    public static boolean gardensNetherHabitats = true;
}
