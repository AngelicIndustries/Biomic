package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Dormant standalone registry for the intrinsic, non-progression Forlorn palette. */
public final class ForlornContentBlocks {

    public static final Block GUANOSTONE = new BlockForlornSolid("guanostone", Material.rock, 1.3F, 2F);
    public static final Block COPROLITH = new BlockForlornSolid("coprolith", Material.rock, 1.75F, 4F);
    public static final Block COPROLITH_COAL_ORE = new BlockForlornOre(
        "coprolith_coal_ore",
        BlockForlornOre.Kind.COAL,
        1.75F,
        4F);
    public static final Block GUANOSTONE_REDSTONE_ORE = new BlockForlornOre(
        "guanostone_redstone_ore",
        BlockForlornOre.Kind.REDSTONE,
        1.3F,
        2F);
    public static final Block PEERING_COPROLITH = new BlockForlornPorousCoprolith("peering_coprolith", true);
    public static final Block POROUS_COPROLITH = new BlockForlornPorousCoprolith("porous_coprolith", false);
    public static final Block THORNWOOD_LOG = new BlockForlornLog(
        "thornwood_log",
        "alexscaves/thornwood_log",
        "alexscaves/thornwood_log_top");
    public static final Block THORNWOOD_WOOD = new BlockForlornLog(
        "thornwood_wood_log",
        "alexscaves/thornwood_log",
        "alexscaves/thornwood_log");
    public static final Block THORNWOOD_BRANCH = new BlockForlornBranch();
    public static final Block UNDERWEED = new BlockForlornUnderweed();
    public static final Block GUANO_BLOCK = new BlockForlornSolid("guano_block", Material.sand, .3F, .3F);
    public static final Block GUANO_LAYER = new BlockForlornGuanoLayer();

    private ForlornContentBlocks() {}

}
