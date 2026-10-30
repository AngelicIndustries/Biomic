package industries.angelic.biomic.content;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.block.BlockAquaticPlant;
import industries.angelic.biomic.content.block.BlockBigDripleaf;
import industries.angelic.biomic.content.block.BlockChalk;
import industries.angelic.biomic.content.block.BlockChalkGrass;
import industries.angelic.biomic.content.block.BlockClover;
import industries.angelic.biomic.content.block.BlockCoral;
import industries.angelic.biomic.content.block.BlockEucalyptusBranch;
import industries.angelic.biomic.content.block.BlockEucalyptusLog;
import industries.angelic.biomic.content.block.BlockHangingMangrovePropagule;
import industries.angelic.biomic.content.block.BlockIcicle;
import industries.angelic.biomic.content.block.BlockJoshuaBeard;
import industries.angelic.biomic.content.block.BlockJoshuaLeaves;
import industries.angelic.biomic.content.block.BlockMushroomFlesh;
import industries.angelic.biomic.content.block.BlockOchreFroglight;
import industries.angelic.biomic.content.block.BlockPalmBeard;
import industries.angelic.biomic.content.block.BlockPalmLeaves;
import industries.angelic.biomic.content.block.BlockPowderSnow;
import industries.angelic.biomic.content.block.BlockRootedDirt;
import industries.angelic.biomic.content.block.BlockRuBranch;
import industries.angelic.biomic.content.block.BlockRuDoublePlant;
import industries.angelic.biomic.content.block.BlockRuLeaves;
import industries.angelic.biomic.content.block.BlockRuLilyPad;
import industries.angelic.biomic.content.block.BlockRuLog;
import industries.angelic.biomic.content.block.BlockRuPlant;
import industries.angelic.biomic.content.block.BlockRuTransitionLog;
import industries.angelic.biomic.content.block.BlockRuVariantPlant;
import industries.angelic.biomic.content.block.BlockSculkFlora;
import industries.angelic.biomic.content.block.BlockSculkVein;
import industries.angelic.biomic.content.block.BlockSmallDripleaf;
import industries.angelic.biomic.content.block.BlockSporeBlossom;
import industries.angelic.biomic.content.block.BlockWisteriaVine;
import industries.angelic.biomic.content.item.ItemBlockAquaticVariant;

/** Owns Biomic block construction and FML registration. */
public final class BiomicBlocks {

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(Biomic.MODID) {

        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(CLOVER);
        }
    };

    public static final BlockClover CLOVER = new BlockClover();
    public static final BlockChalk CHALK = new BlockChalk();
    public static final BlockChalkGrass CHALK_GRASS = new BlockChalkGrass();
    public static final BlockBigDripleaf BIG_DRIPLEAF = creative(BiomicDripleafBlocks.BIG_DRIPLEAF);
    public static final BlockBigDripleaf BIG_DRIPLEAF_STEM = BiomicDripleafBlocks.BIG_DRIPLEAF_STEM;
    public static final BlockSmallDripleaf SMALL_DRIPLEAF = creative(BiomicDripleafBlocks.SMALL_DRIPLEAF);
    public static final BlockSporeBlossom SPORE_BLOSSOM = creative(BiomicLushPlantBlocks.SPORE_BLOSSOM);
    public static final BlockHangingMangrovePropagule HANGING_MANGROVE_PROPAGULE = BiomicLushPlantBlocks.HANGING_MANGROVE_PROPAGULE;
    public static final BlockAquaticPlant KELP = creative(BiomicAquaticBlocks.KELP);
    public static final BlockAquaticPlant SEAGRASS = creative(BiomicAquaticBlocks.SEAGRASS);
    public static final BlockAquaticPlant SEA_PICKLE = creative(BiomicAquaticBlocks.SEA_PICKLE);
    public static final BlockAquaticPlant CORAL_PLANT = creative(BiomicAquaticBlocks.CORAL_PLANT);
    public static final BlockAquaticPlant CORAL_FAN = creative(BiomicAquaticBlocks.CORAL_FAN);
    public static final BlockCoral CORAL_BLOCK = creative(BiomicAquaticBlocks.CORAL_BLOCK);
    public static final industries.angelic.biomic.content.block.BlockSkylandsWetMangroveRoots SKYLANDS_WET_MANGROVE_ROOTS = new industries.angelic.biomic.content.block.BlockSkylandsWetMangroveRoots();
    public static final industries.angelic.biomic.content.block.BlockSkylandsWetGlowLichen SKYLANDS_WET_GLOW_LICHEN = new industries.angelic.biomic.content.block.BlockSkylandsWetGlowLichen();
    public static final BlockSculkFlora SCULK_SENSOR = creative(BiomicSculkBlocks.SCULK_SENSOR);
    public static final BlockSculkFlora SCULK_SHRIEKER = creative(BiomicSculkBlocks.SCULK_SHRIEKER);
    public static final BlockSculkVein SCULK_VEIN = creative(BiomicSculkBlocks.SCULK_VEIN);
    public static final BlockSculkVein SCULK_VEIN_UP = BiomicSculkBlocks.SCULK_VEIN_UP;
    public static final BlockSculkVein SCULK_VEIN_DOWN = BiomicSculkBlocks.SCULK_VEIN_DOWN;
    public static final BlockSculkVein SCULK_VEIN_VERTICAL = BiomicSculkBlocks.SCULK_VEIN_VERTICAL;
    public static final BlockPowderSnow POWDER_SNOW = creative(BiomicFrozenBlocks.POWDER_SNOW);
    public static final BlockOchreFroglight OCHRE_FROGLIGHT = creative(BiomicExpansionBlocks.OCHRE_FROGLIGHT);
    public static final BlockRootedDirt ROOTED_DIRT = creative(BiomicGroundBlocks.ROOTED_DIRT);
    public static final BlockIcicle ICICLE = creative(new BlockIcicle());
    public static final BlockMushroomFlesh MUSHROOM_FLESH = creative(BiomicMushroomBlocks.MUSHROOM_FLESH);
    public static final BlockRuLog WISTERIA_LOG = new BlockRuLog("wisteria");
    public static final BlockRuBranch WISTERIA_BRANCH = new BlockRuBranch("wisteria");
    public static final BlockRuLeaves WISTERIA_LEAVES = new BlockRuLeaves("wisteria", 0x89BFFF, 0xB39DDB, 0xE89A91);
    public static final BlockWisteriaVine WISTERIA_VINE = new BlockWisteriaVine();
    public static final BlockRuLog PINE_LOG = new BlockRuLog("pine");
    public static final BlockRuLog STRIPPED_PINE_LOG = new BlockRuLog("stripped_pine");
    public static final BlockRuTransitionLog PINE_TRANSITION_LOG = new BlockRuTransitionLog();
    public static final BlockRuBranch PINE_BRANCH = new BlockRuBranch("pine");
    public static final BlockRuLeaves PINE_LEAVES = new BlockRuLeaves(
        "pine",
        "pine_leaves",
        BlockRuLeaves.TintMode.BIOME_FOLIAGE);
    public static final BlockRuLeaves DEAD_PINE_LEAVES = new BlockRuLeaves("dead_pine", 0x8B8166);
    public static final BlockRuLog PALM_LOG = new BlockRuLog("palm");
    public static final BlockPalmLeaves PALM_LEAVES = new BlockPalmLeaves();
    public static final BlockPalmBeard PALM_BEARD = new BlockPalmBeard();
    public static final BlockRuLeaves FLOWERING_LEAVES = new industries.angelic.biomic.content.block.BlockRuFloweringLeaves();
    public static final BlockRuLog REDWOOD_LOG = new BlockRuLog("redwood");
    public static final BlockRuBranch REDWOOD_BRANCH = new BlockRuBranch("redwood");
    public static final BlockRuLeaves REDWOOD_LEAVES = new BlockRuLeaves("redwood", 0x7E9539);
    public static final BlockRuLog CYPRESS_LOG = new BlockRuLog("cypress");
    public static final BlockRuBranch CYPRESS_BRANCH = new BlockRuBranch("cypress");
    public static final BlockRuLeaves CYPRESS_LEAVES = new BlockRuLeaves(
        "cypress",
        "cypress_leaves",
        BlockRuLeaves.TintMode.BIOME_FOLIAGE);
    public static final BlockRuLog WILLOW_LOG = new BlockRuLog("willow");
    public static final BlockRuLeaves WILLOW_LEAVES = new BlockRuLeaves(
        "willow",
        "willow_leaves",
        BlockRuLeaves.TintMode.BIOME_FOLIAGE);
    public static final BlockRuLog JOSHUA_LOG = new BlockRuLog("joshua");
    public static final BlockJoshuaLeaves JOSHUA_LEAVES = new BlockJoshuaLeaves();
    public static final BlockEucalyptusLog EUCALYPTUS_LOG = new BlockEucalyptusLog();
    public static final BlockEucalyptusBranch EUCALYPTUS_BRANCH = new BlockEucalyptusBranch();
    public static final BlockRuLeaves EUCALYPTUS_LEAVES = new BlockRuLeaves(
        "eucalyptus",
        "eucalyptus_leaves",
        BlockRuLeaves.TintMode.BIOME_FOLIAGE);
    public static final BlockRuPlant SPANISH_MOSS = new BlockRuPlant(
        "spanish_moss",
        "spanish_moss_plant",
        "spanish_moss",
        0xFFFFFF);
    public static final BlockJoshuaBeard JOSHUA_BEARD = new BlockJoshuaBeard();
    public static final BlockRuPlant GREEN_BIOSHROOM = new BlockRuPlant("green_bioshroom", "green_bioshroom", 10);
    public static final BlockRuPlant HANGING_ROOTS = new BlockRuPlant("hanging_roots", "hanging_roots");
    public static final BlockRuVariantPlant RU_FLOWERS = new BlockRuVariantPlant(
        "ru_flowers",
        false,
        "aster",
        "lupine_blue",
        "lupine_pink",
        "lupine_purple",
        "lupine_red",
        "lupine_yellow",
        "daisy",
        "fireweed",
        "hyssop",
        "mallow",
        "waratah");
    public static final BlockRuVariantPlant RU_TINTED_PLANTS = new BlockRuVariantPlant(
        "ru_tinted_plants",
        true,
        "grass_sprouts",
        "sandy_grass",
        "small_desert_shrub",
        "red_sandy_grass");
    public static final BlockRuPlant SALMONBERRY_BUSH = new BlockRuPlant("salmonberry_bush", "salmonberry_bush_stage3");
    public static final BlockRuDoublePlant RU_SHRUBS_A = new BlockRuDoublePlant(
        "ru_shrubs_a",
        new String[] { "cypress", "dead_pine", "flowering", "joshua", "lavender_wisteria", "sky_wisteria",
            "salmon_wisteria", "pine" },
        new String[] { "cypress_shrub_bottom", "dead_pine_shrub_bottom", "flowering_shrub_bottom",
            "joshua_shrub_bottom", "lavender_wisteria_shrub_bottom", "sky_wisteria_shrub_bottom",
            "salmon_wisteria_shrub_bottom", "pine_shrub_bottom" },
        new String[] { "cypress_shrub_top", "dead_pine_shrub_top", "flowering_shrub_top", "joshua_shrub_top",
            "lavender_wisteria_shrub_top", "sky_wisteria_shrub_top", "salmon_wisteria_shrub_top", "pine_shrub_top" });
    public static final BlockRuDoublePlant RU_SHRUBS_B = new BlockRuDoublePlant(
        "ru_shrubs_b",
        new String[] { "redwood", "willow", "elephant_ear", "tassel", "eucalyptus" },
        new String[] { "redwood_shrub_bottom", "willow_shrub_bottom", "elephant_ear_plant", "tassel_bottom",
            "eucalyptus_shrub_bottom" },
        new String[] { "redwood_shrub_top", "willow_shrub_top", "elephant_ear_leaf", "tassel_top",
            "eucalyptus_shrub_top" });
    public static final BlockRuLilyPad FLOWERING_LILY_PAD = new BlockRuLilyPad(
        "flowering_lily_pad",
        BlockRuLilyPad.Kind.FLOWERING,
        "flowering_lily_pad",
        "flowering_lily_pad_bottom",
        "flowering_lily_pad_flower");
    public static final BlockRuLilyPad GIANT_LILY_PAD = new BlockRuLilyPad(
        "giant_lily_pad",
        BlockRuLilyPad.Kind.GIANT,
        "giant_lily_pad",
        "giant_lily_pad_bottom",
        null);

    private static boolean registered;

    private BiomicBlocks() {}

    private static <T extends net.minecraft.block.Block> T creative(T block) {
        block.setCreativeTab(CREATIVE_TAB);
        return block;
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        GameRegistry.registerBlock(CLOVER, "clover");
        GameRegistry.registerBlock(CHALK, "chalk");
        GameRegistry.registerBlock(CHALK_GRASS, "chalk_grass");
        GameRegistry.registerBlock(BIG_DRIPLEAF, "big_dripleaf");
        GameRegistry.registerBlock(BIG_DRIPLEAF_STEM, "big_dripleaf_stem");
        GameRegistry.registerBlock(SMALL_DRIPLEAF, "small_dripleaf");
        GameRegistry.registerBlock(
            BiomicDripleafBlocks.WET_BIG_DRIPLEAF,
            industries.angelic.biomic.content.item.ItemBlockWetDripleaf.class,
            "wet_big_dripleaf");
        GameRegistry.registerBlock(
            BiomicDripleafBlocks.WET_BIG_DRIPLEAF_STEM,
            industries.angelic.biomic.content.item.ItemBlockWetDripleaf.class,
            "wet_big_dripleaf_stem");
        GameRegistry.registerBlock(
            BiomicDripleafBlocks.WET_SMALL_DRIPLEAF,
            industries.angelic.biomic.content.item.ItemBlockWetDripleaf.class,
            "wet_small_dripleaf");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new industries.angelic.biomic.content.block.DripleafBucket());
        GameRegistry.registerBlock(SPORE_BLOSSOM, "spore_blossom");
        GameRegistry.registerBlock(HANGING_MANGROVE_PROPAGULE, "hanging_mangrove_propagule");
        GameRegistry.registerBlock(KELP, ItemBlockAquaticVariant.class, "kelp");
        GameRegistry.registerBlock(SEAGRASS, ItemBlockAquaticVariant.class, "seagrass");
        GameRegistry.registerBlock(SEA_PICKLE, ItemBlockAquaticVariant.class, "sea_pickle");
        GameRegistry.registerBlock(CORAL_PLANT, ItemBlockAquaticVariant.class, "coral_plant");
        GameRegistry.registerBlock(CORAL_FAN, ItemBlockAquaticVariant.class, "coral_fan");
        GameRegistry.registerBlock(CORAL_BLOCK, ItemBlockAquaticVariant.class, "coral_block");
        GameRegistry.registerBlock(
            SKYLANDS_WET_MANGROVE_ROOTS,
            industries.angelic.biomic.content.item.ItemBlockSkylandsWetMangroveRoots.class,
            "skylands_wet_mangrove_roots");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new industries.angelic.biomic.content.block.SkylandsWetMangroveBucket());
        GameRegistry.registerBlock(
            SKYLANDS_WET_GLOW_LICHEN,
            industries.angelic.biomic.content.item.ItemBlockSkylandsWetGlowLichen.class,
            "skylands_wet_glow_lichen");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new industries.angelic.biomic.content.block.SkylandsWetGlowLichenBucket());
        GameRegistry.registerBlock(SCULK_SENSOR, "sculk_sensor");
        GameRegistry.registerBlock(SCULK_SHRIEKER, "sculk_shrieker");
        GameRegistry.registerBlock(SCULK_VEIN, "sculk_vein");
        GameRegistry.registerBlock(SCULK_VEIN_UP, "sculk_vein_up");
        GameRegistry.registerBlock(SCULK_VEIN_DOWN, "sculk_vein_down");
        GameRegistry.registerBlock(SCULK_VEIN_VERTICAL, "sculk_vein_vertical");
        GameRegistry.registerBlock(
            BiomicWetSculkBlocks.VEIN,
            industries.angelic.biomic.content.item.ItemBlockWetSculkVein.class,
            "wet_sculk_vein");
        GameRegistry.registerBlock(
            BiomicWetSculkBlocks.VEIN_UP,
            industries.angelic.biomic.content.item.ItemBlockWetSculkVein.class,
            "wet_sculk_vein_up");
        GameRegistry.registerBlock(
            BiomicWetSculkBlocks.VEIN_DOWN,
            industries.angelic.biomic.content.item.ItemBlockWetSculkVein.class,
            "wet_sculk_vein_down");
        GameRegistry.registerBlock(
            BiomicWetSculkBlocks.VEIN_VERTICAL,
            industries.angelic.biomic.content.item.ItemBlockWetSculkVein.class,
            "wet_sculk_vein_vertical");
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new industries.angelic.biomic.content.block.WetSculkVeinBucket());
        GameRegistry.registerBlock(POWDER_SNOW, "powder_snow");
        GameRegistry.registerBlock(OCHRE_FROGLIGHT, "ochre_froglight");
        GameRegistry.registerBlock(
            BiomicMushroomBlocks.PORE_BOTTOM_CAP,
            industries.angelic.biomic.content.item.ItemBlockPoreBottomMushroom.class,
            "mushroom_cap_pore_bottom");
        GameRegistry.registerBlock(
            BiomicMushroomBlocks.SIDE_CAP,
            industries.angelic.biomic.content.item.ItemBlockSideCapMushroom.class,
            "mushroom_cap_side_only");
        for (int species = 0; species < 5; species++)
            for (boolean wet : new boolean[] { false, true }) GameRegistry.registerBlock(
                BiomicCoralWallFans.get(species, wet),
                industries.angelic.biomic.content.item.ItemBlockCoralWallFan.class,
                BiomicCoralWallFans.registryName(species, wet));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS
            .register(new industries.angelic.biomic.content.block.CoralWallFanBucket());
        GameRegistry.registerBlock(ROOTED_DIRT, "rooted_dirt");
        GameRegistry.registerBlock(ICICLE, "icicle");
        GameRegistry.registerBlock(MUSHROOM_FLESH, "mushroom_flesh");
        GameRegistry.registerBlock(WISTERIA_LOG, "wisteria_log");
        GameRegistry.registerBlock(WISTERIA_BRANCH, "wisteria_branch");
        GameRegistry.registerBlock(WISTERIA_LEAVES, "wisteria_leaves");
        GameRegistry.registerBlock(WISTERIA_VINE, "wisteria_vine");
        GameRegistry.registerBlock(PINE_LOG, "pine_log");
        GameRegistry.registerBlock(STRIPPED_PINE_LOG, "stripped_pine_log");
        GameRegistry.registerBlock(PINE_TRANSITION_LOG, "pine_transition_log");
        GameRegistry.registerBlock(PINE_BRANCH, "pine_branch");
        GameRegistry.registerBlock(PINE_LEAVES, "pine_leaves");
        GameRegistry.registerBlock(DEAD_PINE_LEAVES, "dead_pine_leaves");
        GameRegistry.registerBlock(PALM_LOG, "palm_log");
        GameRegistry.registerBlock(PALM_LEAVES, "palm_leaves");
        GameRegistry.registerBlock(PALM_BEARD, "palm_beard");
        GameRegistry.registerBlock(FLOWERING_LEAVES, "flowering_leaves");
        GameRegistry.registerBlock(REDWOOD_LOG, "redwood_log");
        GameRegistry.registerBlock(REDWOOD_BRANCH, "redwood_branch");
        GameRegistry.registerBlock(REDWOOD_LEAVES, "redwood_leaves");
        GameRegistry.registerBlock(CYPRESS_LOG, "cypress_log");
        GameRegistry.registerBlock(CYPRESS_BRANCH, "cypress_branch");
        GameRegistry.registerBlock(CYPRESS_LEAVES, "cypress_leaves");
        GameRegistry.registerBlock(WILLOW_LOG, "willow_log");
        GameRegistry.registerBlock(WILLOW_LEAVES, "willow_leaves");
        GameRegistry.registerBlock(JOSHUA_LOG, "joshua_log");
        GameRegistry.registerBlock(JOSHUA_LEAVES, "joshua_leaves");
        GameRegistry.registerBlock(EUCALYPTUS_LOG, "eucalyptus_log");
        GameRegistry.registerBlock(EUCALYPTUS_BRANCH, "eucalyptus_branch");
        GameRegistry.registerBlock(EUCALYPTUS_LEAVES, "eucalyptus_leaves");
        GameRegistry.registerBlock(SPANISH_MOSS, "spanish_moss");
        GameRegistry.registerBlock(JOSHUA_BEARD, "joshua_beard");
        GameRegistry.registerBlock(GREEN_BIOSHROOM, "green_bioshroom");
        GameRegistry.registerBlock(HANGING_ROOTS, "hanging_roots");
        GameRegistry
            .registerBlock(RU_FLOWERS, industries.angelic.biomic.content.item.ItemBlockRuPlant.class, "ru_flowers");
        GameRegistry.registerBlock(
            RU_TINTED_PLANTS,
            industries.angelic.biomic.content.item.ItemBlockRuPlant.class,
            "ru_tinted_plants");
        GameRegistry.registerBlock(SALMONBERRY_BUSH, "salmonberry_bush");
        GameRegistry
            .registerBlock(RU_SHRUBS_A, industries.angelic.biomic.content.item.ItemBlockRuPlant.class, "ru_shrubs_a");
        GameRegistry
            .registerBlock(RU_SHRUBS_B, industries.angelic.biomic.content.item.ItemBlockRuPlant.class, "ru_shrubs_b");
        GameRegistry.registerBlock(FLOWERING_LILY_PAD, "flowering_lily_pad");
        GameRegistry.registerBlock(GIANT_LILY_PAD, "giant_lily_pad");
        industries.angelic.biomic.content.gardens.GardensContent.register();
        industries.angelic.biomic.content.ports.PortBlocks.register();
        RuImportedTreeBlocks.register();
        RuImportedDecorationBlocks.register();
        Blocks.fire.setFireInfo(WISTERIA_LOG, 5, 5);
        Blocks.fire.setFireInfo(WISTERIA_BRANCH, 5, 5);
        Blocks.fire.setFireInfo(WISTERIA_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(WISTERIA_VINE, 15, 100);
        Blocks.fire.setFireInfo(PINE_LOG, 5, 5);
        Blocks.fire.setFireInfo(STRIPPED_PINE_LOG, 5, 5);
        Blocks.fire.setFireInfo(PINE_TRANSITION_LOG, 5, 5);
        Blocks.fire.setFireInfo(PINE_BRANCH, 5, 5);
        Blocks.fire.setFireInfo(PINE_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(DEAD_PINE_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(PALM_LOG, 5, 5);
        Blocks.fire.setFireInfo(PALM_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(PALM_BEARD, 30, 100);
        Blocks.fire.setFireInfo(FLOWERING_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(REDWOOD_LOG, 5, 5);
        Blocks.fire.setFireInfo(REDWOOD_BRANCH, 5, 5);
        Blocks.fire.setFireInfo(REDWOOD_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(CYPRESS_LOG, 5, 5);
        Blocks.fire.setFireInfo(CYPRESS_BRANCH, 5, 5);
        Blocks.fire.setFireInfo(CYPRESS_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(WILLOW_LOG, 5, 5);
        Blocks.fire.setFireInfo(WILLOW_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(JOSHUA_LOG, 5, 5);
        Blocks.fire.setFireInfo(JOSHUA_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(EUCALYPTUS_LOG, 5, 5);
        Blocks.fire.setFireInfo(EUCALYPTUS_BRANCH, 5, 5);
        Blocks.fire.setFireInfo(EUCALYPTUS_LEAVES, 30, 60);
        Blocks.fire.setFireInfo(SPANISH_MOSS, 15, 100);
        Blocks.fire.setFireInfo(JOSHUA_BEARD, 15, 100);
        Blocks.fire.setFireInfo(GREEN_BIOSHROOM, 15, 100);
        Blocks.fire.setFireInfo(HANGING_ROOTS, 15, 100);
        Blocks.fire.setFireInfo(RU_FLOWERS, 30, 100);
        Blocks.fire.setFireInfo(RU_TINTED_PLANTS, 30, 100);
        Blocks.fire.setFireInfo(SALMONBERRY_BUSH, 30, 100);
        Blocks.fire.setFireInfo(RU_SHRUBS_A, 30, 100);
        Blocks.fire.setFireInfo(RU_SHRUBS_B, 30, 100);
        registered = true;
        Biomic.LOG.info(
            "Registered Biomic blocks: clover, chalk and chalk grass, dripleaf, lush, aquatic, sculk, frozen and mushroom materials, coral, wisteria, pine, palm, redwood, cypress, willow, Joshua, eucalyptus and the complete imported RU tree palette");
    }
}
