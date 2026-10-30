package industries.angelic.biomic.unused.alexscaves;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.content.alexscaves.BlockAbyssalSolid;

/**
 * The Abyssal Chasm's ruin and bone blocks (abyssmarine bricks, tiles, pillars, stairs and walls, rusty scrap, copper
 * and vanilla furnishings, depth glass, drains, magnetic activators, smooth and hollow bones, ribs, nodules, baleen,
 * bone worms). Not registered: Biomic's Abyssal planner places only the Chasm's ecology, so no world ever had these.
 * To bring them back, call {@link #register} from the common proxy's pre-init, add their states to
 * {@code AbyssalStateCodec} (see {@link AbyssalRuinStateCodec}), and render them through {@link AbyssalRuinRenderer}.
 */
public final class AbyssalRuinBlocks {

    public static final BlockAbyssalBone SMOOTH_BONE = bone("smooth_bone", BlockAbyssalBone.Kind.SMOOTH);
    public static final BlockAbyssalBone SMOOTH_BONE_SLAB = bone("smooth_bone_slab", BlockAbyssalBone.Kind.SLAB);
    public static final BlockAbyssalBone[] SMOOTH_BONE_STAIRS = {
        bone("smooth_bone_stairs", BlockAbyssalBone.Kind.STAIR_STRAIGHT),
        bone("smooth_bone_stairs_inner", BlockAbyssalBone.Kind.STAIR_INNER),
        bone("smooth_bone_stairs_outer", BlockAbyssalBone.Kind.STAIR_OUTER) };
    public static final BlockAbyssalBone SMOOTH_BONE_WALL = bone("smooth_bone_wall", BlockAbyssalBone.Kind.WALL);
    public static final BlockAbyssalBone HOLLOW_BONE = bone("hollow_bone", BlockAbyssalBone.Kind.HOLLOW);
    public static final BlockAbyssalBone THIN_BONE = bone("thin_bone", BlockAbyssalBone.Kind.THIN);
    public static final BlockAbyssalBone BONE_NODULE = bone("bone_nodule", BlockAbyssalBone.Kind.NODULE);
    public static final BlockAbyssalBone BONE_RIBS = bone("bone_ribs", BlockAbyssalBone.Kind.RIBS);
    public static final BlockAbyssalBone BALEEN_BONE = bone("baleen_bone", BlockAbyssalBone.Kind.BALEEN);
    public static final BlockAbyssalBone BONE_WORMS = bone("bone_worms", BlockAbyssalBone.Kind.WORMS);
    public static final Block ABYSSMARINE_BRICKS = new BlockAbyssalSolid("abyssmarine_bricks", 1.5F, 6F);
    public static final Block ABYSSMARINE_TILES = new BlockAbyssalSolid("abyssmarine_tiles", 1.5F, 6F);
    public static final Block ABYSSMARINE_PILLAR = new BlockAbyssmarinePillar();
    public static final Block RUSTY_SCRAP_METAL_PLATE = new BlockAbyssalSolid("rusty_scrap_metal_plate", 3F, 6F);
    public static final BlockAbyssalRuinShell RUSTY_SCRAP_METAL = new BlockAbyssalRuinShell(
        "rusty_scrap_metal",
        BlockAbyssalRuinShell.Kind.SCRAP,
        "rusty_scrap_metal_0",
        "rusty_scrap_metal_1",
        "rusty_scrap_metal_2",
        "rusty_scrap_metal_3",
        "rusty_scrap_metal_4",
        "rusty_scrap_metal_5",
        "rusty_scrap_metal_6");
    public static final BlockAbyssalRuinShell DEPTH_GLASS = new BlockAbyssalRuinShell(
        "depth_glass",
        BlockAbyssalRuinShell.Kind.DEPTH_GLASS,
        "depth_glass",
        "depth_glass_all",
        "depth_glass_blank",
        "depth_glass_down",
        "depth_glass_left",
        "depth_glass_right",
        "depth_glass_up");
    public static final BlockAbyssalRuinShell RUSTY_SCAFFOLDING = new BlockAbyssalRuinShell(
        "rusty_scaffolding",
        BlockAbyssalRuinShell.Kind.SCAFFOLDING,
        "rusty_scaffolding_side",
        "rusty_scaffolding_poles",
        "rusty_scaffolding_top");
    public static final BlockAbyssalRuinShell DRAIN = new BlockAbyssalRuinShell(
        "drain",
        BlockAbyssalRuinShell.Kind.DRAIN,
        "drain_closed",
        "drain_open",
        "drain_side");
    public static final BlockAbyssalRuinShell MAGNETIC_ACTIVATOR = new BlockAbyssalRuinShell(
        "magnetic_activator",
        BlockAbyssalRuinShell.Kind.MAGNETIC_ACTIVATOR,
        "magnetic_activator",
        "magnetic_activator_front",
        "scrap_metal_plate");
    public static final BlockAbyssalRuinShell RUSTY_LIGHTNING_ROD = new BlockAbyssalRuinShell(
        "rusty_lightning_rod",
        BlockAbyssalRuinShell.Kind.LIGHTNING_ROD,
        "lightning_rod");
    public static final BlockAbyssalRuinShell RUSTY_WET_IRON_TRAPDOOR = new BlockAbyssalRuinShell(
        "rusty_wet_iron_trapdoor",
        BlockAbyssalRuinShell.Kind.WET_IRON_TRAPDOOR,
        "iron_trapdoor");
    public static final BlockAbyssalRuinShell RUSTY_CEILING_BUTTON = new BlockAbyssalRuinShell(
        "rusty_ceiling_button",
        BlockAbyssalRuinShell.Kind.CEILING_BUTTON,
        "stone");
    public static final BlockAbyssalWetFurnishing RUSTY_WET_CHAIN = wet(
        "rusty_wet_chain",
        BlockAbyssalWetFurnishing.Kind.CHAIN,
        BlockAbyssalWetFurnishing.DryTarget.CHAIN,
        "chain");
    public static final BlockAbyssalWetFurnishing RUSTY_WET_WEATHERED_SLAB = wet(
        "rusty_wet_weathered_cut_copper_slab",
        BlockAbyssalWetFurnishing.Kind.SLAB,
        BlockAbyssalWetFurnishing.DryTarget.WEATHERED_SLAB,
        "weathered_cut_copper");
    public static final BlockAbyssalWetFurnishing RUSTY_WET_OXIDIZED_SLAB = wet(
        "rusty_wet_oxidized_cut_copper_slab",
        BlockAbyssalWetFurnishing.Kind.SLAB,
        BlockAbyssalWetFurnishing.DryTarget.OXIDIZED_SLAB,
        "oxidized_cut_copper");
    public static final BlockAbyssalWetFurnishing[] RUSTY_WET_WEATHERED_STAIRS = wetStairs(
        "rusty_wet_weathered_cut_copper_stairs",
        BlockAbyssalWetFurnishing.DryTarget.WEATHERED_STAIR,
        "weathered_cut_copper");
    public static final BlockAbyssalWetFurnishing[] RUSTY_WET_WAXED_WEATHERED_STAIRS = wetStairs(
        "rusty_wet_waxed_weathered_cut_copper_stairs",
        BlockAbyssalWetFurnishing.DryTarget.WAXED_WEATHERED_STAIR,
        "weathered_cut_copper");
    public static final BlockAbyssalWetFurnishing[] RUSTY_WET_WAXED_OXIDIZED_STAIRS = wetStairs(
        "rusty_wet_waxed_oxidized_cut_copper_stairs",
        BlockAbyssalWetFurnishing.DryTarget.WAXED_OXIDIZED_STAIR,
        "oxidized_cut_copper");
    public static final BlockAbyssalBone[] ABYSSMARINE_STAIRS = {
        new BlockAbyssalBone("abyssmarine_stairs", BlockAbyssalBone.Kind.STAIR_STRAIGHT, "abyssmarine"),
        new BlockAbyssalBone("abyssmarine_stairs_inner", BlockAbyssalBone.Kind.STAIR_INNER, "abyssmarine"),
        new BlockAbyssalBone("abyssmarine_stairs_outer", BlockAbyssalBone.Kind.STAIR_OUTER, "abyssmarine") };
    public static final BlockAbyssalBone ABYSSMARINE_WALL = new BlockAbyssalBone(
        "abyssmarine_wall",
        BlockAbyssalBone.Kind.WALL,
        "abyssmarine");
    public static final BlockAbyssalBone[] ABYSSMARINE_BRICK_STAIRS = {
        new BlockAbyssalBone("abyssmarine_brick_stairs", BlockAbyssalBone.Kind.STAIR_STRAIGHT, "abyssmarine_bricks"),
        new BlockAbyssalBone("abyssmarine_brick_stairs_inner", BlockAbyssalBone.Kind.STAIR_INNER, "abyssmarine_bricks"),
        new BlockAbyssalBone(
            "abyssmarine_brick_stairs_outer",
            BlockAbyssalBone.Kind.STAIR_OUTER,
            "abyssmarine_bricks") };
    public static final BlockAbyssalBone ABYSSMARINE_BRICK_WALL = new BlockAbyssalBone(
        "abyssmarine_brick_wall",
        BlockAbyssalBone.Kind.WALL,
        "abyssmarine_bricks");

    private AbyssalRuinBlocks() {}

    /** Registers every ruin and bone block under its old name. */
    public static void register() {
        for (Map.Entry<String, Block> entry : blocks().entrySet())
            GameRegistry.registerBlock(entry.getValue(), entry.getKey());
    }

    private static BlockAbyssalBone bone(String name, BlockAbyssalBone.Kind kind) {
        return new BlockAbyssalBone(name, kind);
    }

    private static BlockAbyssalWetFurnishing wet(String name, BlockAbyssalWetFurnishing.Kind kind,
        BlockAbyssalWetFurnishing.DryTarget dry, String texture) {
        return new BlockAbyssalWetFurnishing(name, kind, dry, texture);
    }

    private static BlockAbyssalWetFurnishing[] wetStairs(String name, BlockAbyssalWetFurnishing.DryTarget dry,
        String texture) {
        return new BlockAbyssalWetFurnishing[] { wet(name, BlockAbyssalWetFurnishing.Kind.STAIR_STRAIGHT, dry, texture),
            wet(name + "_inner", BlockAbyssalWetFurnishing.Kind.STAIR_INNER, dry, texture) };
    }

    public static Map<String, Block> blocks() {
        Map<String, Block> out = new LinkedHashMap<>();
        out.put("alexs_abyssal_smooth_bone", SMOOTH_BONE);
        out.put("alexs_abyssal_smooth_bone_slab", SMOOTH_BONE_SLAB);
        out.put("alexs_abyssal_smooth_bone_stairs", SMOOTH_BONE_STAIRS[0]);
        out.put("alexs_abyssal_smooth_bone_stairs_inner", SMOOTH_BONE_STAIRS[1]);
        out.put("alexs_abyssal_smooth_bone_stairs_outer", SMOOTH_BONE_STAIRS[2]);
        out.put("alexs_abyssal_smooth_bone_wall", SMOOTH_BONE_WALL);
        out.put("alexs_abyssal_hollow_bone", HOLLOW_BONE);
        out.put("alexs_abyssal_thin_bone", THIN_BONE);
        out.put("alexs_abyssal_bone_nodule", BONE_NODULE);
        out.put("alexs_abyssal_bone_ribs", BONE_RIBS);
        out.put("alexs_abyssal_baleen_bone", BALEEN_BONE);
        out.put("alexs_abyssal_bone_worms", BONE_WORMS);
        out.put("alexs_abyssal_abyssmarine_bricks", ABYSSMARINE_BRICKS);
        out.put("alexs_abyssal_abyssmarine_tiles", ABYSSMARINE_TILES);
        out.put("alexs_abyssal_abyssmarine_pillar", ABYSSMARINE_PILLAR);
        out.put("alexs_abyssal_rusty_scrap_metal_plate", RUSTY_SCRAP_METAL_PLATE);
        out.put("alexs_abyssal_rusty_scrap_metal", RUSTY_SCRAP_METAL);
        out.put("alexs_abyssal_depth_glass", DEPTH_GLASS);
        out.put("alexs_abyssal_rusty_scaffolding", RUSTY_SCAFFOLDING);
        out.put("alexs_abyssal_drain", DRAIN);
        out.put("alexs_abyssal_magnetic_activator", MAGNETIC_ACTIVATOR);
        out.put("alexs_abyssal_rusty_lightning_rod", RUSTY_LIGHTNING_ROD);
        out.put("alexs_abyssal_rusty_wet_iron_trapdoor", RUSTY_WET_IRON_TRAPDOOR);
        out.put("alexs_abyssal_rusty_ceiling_button", RUSTY_CEILING_BUTTON);
        out.put("alexs_abyssal_rusty_wet_chain", RUSTY_WET_CHAIN);
        out.put("alexs_abyssal_rusty_wet_weathered_cut_copper_slab", RUSTY_WET_WEATHERED_SLAB);
        out.put("alexs_abyssal_rusty_wet_oxidized_cut_copper_slab", RUSTY_WET_OXIDIZED_SLAB);
        out.put("alexs_abyssal_rusty_wet_weathered_cut_copper_stairs", RUSTY_WET_WEATHERED_STAIRS[0]);
        out.put("alexs_abyssal_rusty_wet_weathered_cut_copper_stairs_inner", RUSTY_WET_WEATHERED_STAIRS[1]);
        out.put("alexs_abyssal_rusty_wet_waxed_weathered_cut_copper_stairs", RUSTY_WET_WAXED_WEATHERED_STAIRS[0]);
        out.put("alexs_abyssal_rusty_wet_waxed_weathered_cut_copper_stairs_inner", RUSTY_WET_WAXED_WEATHERED_STAIRS[1]);
        out.put("alexs_abyssal_rusty_wet_waxed_oxidized_cut_copper_stairs", RUSTY_WET_WAXED_OXIDIZED_STAIRS[0]);
        out.put("alexs_abyssal_rusty_wet_waxed_oxidized_cut_copper_stairs_inner", RUSTY_WET_WAXED_OXIDIZED_STAIRS[1]);
        out.put("alexs_abyssal_abyssmarine_stairs", ABYSSMARINE_STAIRS[0]);
        out.put("alexs_abyssal_abyssmarine_stairs_inner", ABYSSMARINE_STAIRS[1]);
        out.put("alexs_abyssal_abyssmarine_stairs_outer", ABYSSMARINE_STAIRS[2]);
        out.put("alexs_abyssal_abyssmarine_wall", ABYSSMARINE_WALL);
        out.put("alexs_abyssal_abyssmarine_brick_stairs", ABYSSMARINE_BRICK_STAIRS[0]);
        out.put("alexs_abyssal_abyssmarine_brick_stairs_inner", ABYSSMARINE_BRICK_STAIRS[1]);
        out.put("alexs_abyssal_abyssmarine_brick_stairs_outer", ABYSSMARINE_BRICK_STAIRS[2]);
        out.put("alexs_abyssal_abyssmarine_brick_wall", ABYSSMARINE_BRICK_WALL);
        return out;
    }
}
