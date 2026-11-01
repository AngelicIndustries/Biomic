package industries.angelic.biomic.content;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.block.BlockWoodPlanks;
import industries.angelic.biomic.content.block.BlockWoodSlab;
import industries.angelic.biomic.content.block.BlockWoodStairs;
import industries.angelic.biomic.content.item.ItemBlockWoodSlab;

/**
 * Regions Unexplored's woods for the logs Biomic grows: planks, slabs and stairs with RU's names and art, the
 * recipes RU gives them (a log of a wood makes four of its planks, smelts to charcoal where RU's logs_that_burn
 * says so) and the ore names other mods craft wood by. The logs RU turns into another mod's planks (small oak,
 * silver birch, bamboo) make those.
 */
public final class BiomicWoods {

    /** RU's woods: its id, the planks texture its models bind, and whether fire catches it (RU's fireproof sets). */
    public enum Wood {

        ALPHA("alpha", "alpha_oak_planks", true, tree("alpha_log")),
        BAOBAB("baobab", "baobab_planks", true, tree("baobab_log")),
        BLACKWOOD("blackwood", "blackwood_planks", true, tree("blackwood_log")),
        BLUE_BIOSHROOM("blue_bioshroom", "blue_bioshroom_planks", true, tree("blue_bioshroom_stem")),
        BRIMWOOD("brimwood", "brimwood_planks", false, unburnt(decoration("brimwood_log")),
            unburnt(decoration("brimwood_log_magma"))),
        COBALT("cobalt", "cobalt_planks", false, unburnt(decoration("cobalt_log"))),
        CYPRESS("cypress", "cypress_planks", true, own(() -> BiomicBlocks.CYPRESS_LOG)),
        DEAD("dead", "dead_planks", false, unburnt(tree("dead_log")), tree("ashen_log")),
        EUCALYPTUS("eucalyptus", "eucalyptus_planks", true, own(() -> BiomicBlocks.EUCALYPTUS_LOG)),
        GREEN_BIOSHROOM("green_bioshroom", "green_bioshroom_planks", true, decoration("green_bioshroom_stem")),
        JOSHUA("joshua", "joshua_planks", true, own(() -> BiomicBlocks.JOSHUA_LOG)),
        KAPOK("kapok", "kapok_planks", true, tree("kapok_log")),
        LARCH("larch", "larch_planks", true, tree("larch_log")),
        MAGNOLIA("magnolia", "magnolia_planks", true, tree("magnolia_log")),
        MAPLE("maple", "maple_planks", true, tree("maple_log")),
        PALM("palm", "palm_planks", true, own(() -> BiomicBlocks.PALM_LOG)),
        PINE("pine", "pine_planks", true, own(() -> BiomicBlocks.PINE_LOG), own(() -> BiomicBlocks.STRIPPED_PINE_LOG),
            own(() -> BiomicBlocks.PINE_TRANSITION_LOG)),
        PINK_BIOSHROOM("pink_bioshroom", "pink_bioshroom_planks", true, tree("pink_bioshroom_stem")),
        REDWOOD("redwood", "redwood_planks", true, own(() -> BiomicBlocks.REDWOOD_LOG)),
        SOCOTRA("socotra", "socotra_planks", true, tree("socotra_log")),
        WILLOW("willow", "willow_planks", true, own(() -> BiomicBlocks.WILLOW_LOG)),
        WISTERIA("wisteria", "wisteria_planks", true, own(() -> BiomicBlocks.WISTERIA_LOG)),
        YELLOW_BIOSHROOM("yellow_bioshroom", "yellow_bioshroom_planks", false,
            unburnt(decoration("yellow_bioshroom_stem")));

        public final String id;
        /** Under Biomic's textures/blocks: RU's own file, copied. */
        public final String texture;
        public final boolean flammable;
        private final Log[] logs;
        private BlockWoodPlanks planks;
        private BlockWoodSlab slab;
        private BlockWoodSlab doubleSlab;
        private BlockWoodStairs stairs;

        Wood(String id, String texture, boolean flammable, Log... logs) {
            this.id = id;
            this.texture = "ru/" + texture;
            this.flammable = flammable;
            this.logs = logs;
        }

        public BlockWoodPlanks planks() {
            return planks;
        }

        public BlockWoodSlab slab() {
            return slab;
        }

        public BlockWoodStairs stairs() {
            return stairs;
        }
    }

    /** A log a wood is cut from, and whether it smelts to charcoal. */
    private record Log(Supplier<Block> block, boolean burns) {}

    private static boolean registered;

    private BiomicWoods() {}

    private static Log tree(String name) {
        return new Log(() -> RuImportedTreeBlocks.get("regions_unexplored:" + name), true);
    }

    private static Log decoration(String name) {
        return new Log(() -> RuImportedDecorationBlocks.get("regions_unexplored:" + name), true);
    }

    private static Log own(Supplier<Block> block) {
        return new Log(block, true);
    }

    private static Log unburnt(Log log) {
        return new Log(log.block, false);
    }

    /** From Biomic's block registration (pre-init), after the logs. */
    public static synchronized void register() {
        if (registered) return;
        for (Wood wood : Wood.values()) {
            String planks = wood.id + "_planks", slab = wood.id + "_slab";
            wood.planks = new BlockWoodPlanks(planks, wood.texture);
            boolean stoneLike = wood == Wood.ALPHA;
            wood.slab = new BlockWoodSlab(false, wood.planks, slab, stoneLike);
            wood.doubleSlab = new BlockWoodSlab(true, wood.planks, slab, stoneLike).single(wood.slab);
            wood.stairs = new BlockWoodStairs(wood.planks, wood.id + "_stairs");
            GameRegistry.registerBlock(wood.planks, planks);
            GameRegistry.registerBlock(wood.slab, ItemBlockWoodSlab.class, slab, wood.slab, wood.doubleSlab, false);
            GameRegistry.registerBlock(
                wood.doubleSlab,
                ItemBlockWoodSlab.class,
                wood.id + "_double_slab",
                wood.slab,
                wood.doubleSlab,
                true);
            GameRegistry.registerBlock(wood.stairs, wood.id + "_stairs");
            if (wood.flammable) for (Block block : new Block[] { wood.planks, wood.slab, wood.doubleSlab, wood.stairs })
                Blocks.fire.setFireInfo(block, 5, 20);
        }
        registered = true;
        Biomic.LOG.info("Registered planks, slabs and stairs of {} woods", Wood.values().length);
    }

    /** From Biomic's init: recipes, smelting and ore names. */
    public static void addRecipes() {
        for (Wood wood : Wood.values()) {
            for (Log log : wood.logs) {
                Block block = log.block.get();
                if (block == null) throw new IllegalStateException("No " + wood.id + " log registered");
                logRecipes(block, new ItemStack(wood.planks, 4), log.burns);
            }
            GameRegistry.addRecipe(new ItemStack(wood.slab, 6), "###", '#', wood.planks);
            GameRegistry.addRecipe(new ItemStack(wood.stairs, 4), "#  ", "## ", "###", '#', wood.planks);
            OreDictionary.registerOre("plankWood", wood.planks);
            if (wood != Wood.ALPHA) OreDictionary.registerOre("slabWood", wood.slab);
            OreDictionary.registerOre("stairWood", wood.stairs);
        }
        // RU makes vanilla's planks of these: oak, birch, and Et Futurum Requiem's bamboo where it is installed.
        logRecipes(
            RuImportedTreeBlocks.get("regions_unexplored:small_oak_log"),
            new ItemStack(Blocks.planks, 4, 0),
            true);
        logRecipes(
            RuImportedTreeBlocks.get("regions_unexplored:silver_birch_log"),
            new ItemStack(Blocks.planks, 4, 2),
            true);
        Block bamboo = (Block) Block.blockRegistry.getObject("etfuturum:wood_planks");
        logRecipes(
            RuImportedTreeBlocks.get("regions_unexplored:bamboo_log"),
            bamboo != null && bamboo != Blocks.air ? new ItemStack(bamboo, 4, 4) : new ItemStack(Blocks.planks, 4, 2),
            true);
        for (Object name : Block.blockRegistry.getKeys()) {
            if (!((String) name).startsWith(Biomic.MODID + ":")) continue;
            Block block = (Block) Block.blockRegistry.getObject(name);
            if (block instanceof BlockLeaves)
                OreDictionary.registerOre("treeLeaves", new ItemStack(block, 1, OreDictionary.WILDCARD_VALUE));
        }
    }

    private static void logRecipes(Block log, ItemStack planks, boolean burns) {
        ItemStack any = new ItemStack(log, 1, OreDictionary.WILDCARD_VALUE);
        GameRegistry.addShapelessRecipe(planks, any);
        OreDictionary.registerOre("logWood", any);
        if (burns) FurnaceRecipes.smelting()
            .func_151393_a(log, new ItemStack(Items.coal, 1, 1), 0.15F);
    }
}
