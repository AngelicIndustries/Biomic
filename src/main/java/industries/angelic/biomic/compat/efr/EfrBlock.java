package industries.angelic.biomic.compat.efr;

import net.minecraft.block.Block;

/**
 * Et Futurum Requiem's blocks that Biomic knows, by registry name ({@code etfuturum:} and the name of EFR's
 * {@code ModBlocks} entry in lower case): never EFR's classes, so Biomic loads without it. A block EFR does not
 * register
 * (EFR absent, or the block disabled in its config) is null. Biomic places blocks through the palette
 * (world.palette.BlockPalette); this is for recognising them.
 */
public enum EfrBlock {

    AMETHYST_BLOCK,
    AMETHYST_CLUSTER_1,
    AMETHYST_CLUSTER_2,
    ANCIENT_DEBRIS,
    ANDESITE_STAIRS,
    AZALEA,
    AZALEA_LEAVES,
    BAMBOO,
    BASALT,
    BEETROOTS,
    BEE_NEST,
    BLACKSTONE,
    BLUE_ICE,
    BONE,
    BUDDING_AMETHYST,
    CALCITE,
    CAVE_VINE,
    CAVE_VINE_PLANT,
    CHAIN,
    CHERRY_LOG,
    COARSE_DIRT,
    COBBLED_DEEPSLATE,
    CONCRETE,
    CONCRETE_POWDER,
    COPPER_BLOCK,
    COPPER_ORE,
    CORNFLOWER,
    CRIMSON_STEM,
    CRYING_OBSIDIAN,
    CUT_COPPER_SLAB,
    DEEPSLATE,
    DEEPSLATE_BRICKS,
    DEEPSLATE_COAL_ORE,
    DEEPSLATE_COPPER_ORE,
    DEEPSLATE_DIAMOND_ORE,
    DEEPSLATE_EMERALD_ORE,
    DEEPSLATE_GOLD_ORE,
    DEEPSLATE_IRON_ORE,
    DEEPSLATE_LAPIS_ORE,
    DEEPSLATE_REDSTONE_ORE,
    DEEPSLATE_TILE_STAIRS,
    DEEPSLATE_WALL,
    DIORITE_STAIRS,
    DOUBLE_STONE_SLAB,
    DRIPSTONE_BLOCK,
    FENCE_DARK_OAK,
    FENCE_SPRUCE,
    FROSTED_ICE,
    GLOW_LICHEN,
    GRANITE_STAIRS,
    IRON_TRAPDOOR,
    LEAVES,
    LILY_OF_THE_VALLEY,
    MAGMA,
    MANGROVE_LOG,
    MANGROVE_ROOTS,
    MOSS_BLOCK,
    MOSS_CARPET,
    MUD,
    MUDDY_MANGROVE_ROOTS,
    NETHER_FUNGUS,
    NETHER_GOLD_ORE,
    NETHER_ROOTS,
    NETHER_SPROUTS,
    NETHER_WART,
    NYLIUM,
    PACKED_MUD,
    PINK_PETALS,
    POINTED_DRIPSTONE,
    POLISHED_DEEPSLATE,
    PRISMARINE_BLOCK,
    PRISMARINE_STAIRS_DARK,
    RAW_ORE_BLOCK,
    RED_NETHERBRICK,
    RED_SANDSTONE,
    RED_SANDSTONE_STAIRS,
    SAPLING,
    SCULK,
    SCULK_CATALYST,
    SEA_LANTERN,
    SHROOMLIGHT,
    SMITHING_TABLE,
    SMOOTH_BASALT,
    SMOOTH_RED_SANDSTONE,
    SMOOTH_SANDSTONE,
    SMOOTH_STONE,
    SOUL_SOIL,
    SPONGE,
    STONE,
    STONE_SLAB,
    STONE_STAIRS,
    SWEET_BERRY_BUSH,
    TRAPDOOR_SPRUCE,
    TUFF,
    TWISTING_VINES,
    WARPED_STEM,
    WAXED_OXIDIZED_CUT_COPPER_STAIRS,
    WAXED_WEATHERED_CUT_COPPER_STAIRS,
    WEATHERED_CUT_COPPER_STAIRS,
    WEEPING_VINES,
    WOOD_STRIPPED;

    private final String name = "etfuturum:" + name().toLowerCase(java.util.Locale.ROOT);
    private volatile Block block;
    private volatile boolean bound;

    /** The registered block, or null; looked up again until the registry is complete (bindAll). */
    public Block get() {
        if (bound) return block;
        return lookup(name);
    }

    public String registryName() {
        return name;
    }

    public boolean isPresent() {
        return get() != null;
    }

    /** Once every mod registered its blocks (Biomic's post-init): later lookups are a field read. */
    public static void bindAll() {
        for (EfrBlock entry : values()) {
            entry.block = lookup(entry.name);
            entry.bound = true;
        }
    }

    private static Block lookup(String name) {
        if (!Block.blockRegistry.containsKey(name)) return null;
        return (Block) Block.blockRegistry.getObject(name);
    }
}
