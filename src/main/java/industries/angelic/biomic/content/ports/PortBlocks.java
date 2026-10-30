package industries.angelic.biomic.content.ports;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.BiomicLegacyStates;
import industries.angelic.biomic.content.block.BlockAquaticPlant;
import industries.angelic.biomic.content.block.BlockAquaticPlant.Kind;
import industries.angelic.biomic.content.item.ItemBlockAquaticVariant;

/**
 * Plants and shells ported from TerraFirmaCraft, Tropicraft and Mariculture (ecology proposal, ports; their textures
 * and licences are listed in THIRD_PARTY_NOTICES.md): warm seagrasses, algae and corals for the sea, shells for the
 * sea floor and the strand line, coral sand and pianguas mud, lichens for cave mouths. Metadata picks the species.
 */
public final class PortBlocks {

    /** Shells and beach litter, in metadata order; the same for the strand line and the sea floor. */
    public static final String[] SHELLS = { "tfc_clam", "tfc_mollusk", "tfc_mussel", "tfc_sea_urchin",
        "trop_frox_conch", "trop_rube_nautilus", "trop_pab_shell", "trop_solonox_shell", "trop_turtle_shell",
        "tfc_driftwood", "tfc_seaweed", "tfc_guano", "mari_oyster" };
    public static final int CLAM = 0, MOLLUSK = 1, MUSSEL = 2, URCHIN = 3, CONCH = 4, NAUTILUS = 5, PAB = 6,
        SOLONOX = 7, TURTLE = 8, DRIFTWOOD = 9, SEAWEED = 10, GUANO = 11, OYSTER = 12;

    /** TFC's barnacles on rock faces under water: metadata is the support side plus four times the variant. */
    public static final BlockAquaticPlant BARNACLES = new BlockAquaticPlant(
        "port_barnacles",
        Kind.WALL,
        "ports/tfc_barnacles_0",
        "ports/tfc_barnacles_1",
        "ports/tfc_barnacles_2",
        "ports/tfc_barnacles_3");
    /** Tropicraft's matted seagrass, the meadow floor: eel grass (0), fern seagrass (1), sickle seagrass (2). */
    public static final BlockMattedSeagrass MATTED_SEAGRASS = new BlockMattedSeagrass(
        "port_matted_seagrass",
        "eel_grass",
        "fern_seagrass",
        "sickle_seagrass");

    /** Turtle grass (0-3), manatee grass (4-7), fern seagrass (8), sickle seagrass (9). */
    public static final BlockAquaticPlant SEAGRASS = new BlockAquaticPlant(
        "port_seagrass",
        Kind.PLANT,
        "ports/tfc_turtle_grass_0",
        "ports/tfc_turtle_grass_1",
        "ports/tfc_turtle_grass_2",
        "ports/tfc_turtle_grass_3",
        "ports/tfc_manatee_grass_0",
        "ports/tfc_manatee_grass_1",
        "ports/tfc_manatee_grass_2",
        "ports/tfc_manatee_grass_3",
        "ports/trop_fern_seagrass",
        "ports/trop_sickle_seagrass");
    public static final int TURTLE_GRASS = 0, MANATEE_GRASS = 4, FERN_SEAGRASS = 8, SICKLE_SEAGRASS = 9;

    /** Sargassum (0), red algae (1), gutweed (2), sea palm (3). */
    public static final BlockAquaticPlant ALGAE = new BlockAquaticPlant(
        "port_algae",
        Kind.PLANT,
        "ports/tfc_sargassum",
        "ports/tfc_red_algae",
        "ports/tfc_gutweed",
        "ports/tfc_sea_palm");
    public static final int SARGASSUM = 0, RED_ALGAE = 1, GUTWEED = 2, SEA_PALM = 3;

    /** Mariculture's corals: pink, orange, blue, purple, yellow, red, white. */
    public static final BlockAquaticPlant CORAL = new BlockAquaticPlant(
        "port_coral",
        Kind.PLANT,
        "ports/mari_coral_pink",
        "ports/mari_coral_orange",
        "ports/mari_coral_blue",
        "ports/mari_coral_purple",
        "ports/mari_coral_yellow",
        "ports/mari_coral_red",
        "ports/mari_coral_white");

    public static final BlockAquaticPlant SEA_SHELL = new BlockAquaticPlant("port_sea_shell", Kind.FLAT, ports(SHELLS));
    public static final BlockGroundShell SHELL = new BlockGroundShell("port_shell", ports(SHELLS));

    public static final Block CORAL_SAND = new BlockPortGround(
        "port_coral_sand",
        "ports/trop_coral_sand",
        Material.sand,
        Block.soundTypeSand);
    public static final Block PIANGUAS_MUD = new BlockPortGround(
        "port_pianguas_mud",
        "ports/trop_mud_with_pianguas",
        Material.ground,
        Block.soundTypeGravel);

    public static final BlockWallLichen COBBLESTONE_LICHEN = new BlockWallLichen(
        "port_cobblestone_lichen",
        "ports/tfc_cobblestone_lichen");
    public static final BlockWallLichen SUNBURST_LICHEN = new BlockWallLichen(
        "port_sunburst_lichen",
        "ports/tfc_sunburst_lichen");
    public static final BlockFloorLichen REINDEER_LICHEN = new BlockFloorLichen(
        "port_reindeer_lichen",
        "ports/tfc_reindeer_lichen");

    private static boolean registered;

    private PortBlocks() {}

    private static String[] ports(String[] names) {
        String[] textures = new String[names.length];
        for (int i = 0; i < names.length; i++) textures[i] = "ports/" + names[i];
        return textures;
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        for (BlockAquaticPlant block : new BlockAquaticPlant[] { SEAGRASS, ALGAE, CORAL, SEA_SHELL, BARNACLES }) {
            block.setCreativeTab(BiomicBlocks.CREATIVE_TAB);
            String name = block.getUnlocalizedName()
                .substring(("tile." + Biomic.MODID + ".").length());
            GameRegistry.registerBlock(block, ItemBlockAquaticVariant.class, name);
            BiomicLegacyStates.register(name, block);
        }
        GameRegistry.registerBlock(SHELL, ItemBlockGroundShell.class, "port_shell");
        MATTED_SEAGRASS.setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        GameRegistry.registerBlock(
            MATTED_SEAGRASS,
            industries.angelic.biomic.content.ItemBlockSubtypes.class,
            "port_matted_seagrass");
        BiomicLegacyStates.register("port_matted_seagrass", MATTED_SEAGRASS);
        BiomicLegacyStates.register("port_shell", SHELL);
        for (Block block : new Block[] { CORAL_SAND, PIANGUAS_MUD, COBBLESTONE_LICHEN, SUNBURST_LICHEN,
            REINDEER_LICHEN }) {
            block.setCreativeTab(BiomicBlocks.CREATIVE_TAB);
            String name = block.getUnlocalizedName()
                .substring(("tile." + Biomic.MODID + ".").length());
            GameRegistry.registerBlock(block, name);
            BiomicLegacyStates.register(name, block);
        }
    }
}
