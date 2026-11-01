package industries.angelic.biomic.content;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.oredict.OreDictionary;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.block.BlockWoodButton;
import industries.angelic.biomic.content.block.BlockWoodDoor;
import industries.angelic.biomic.content.block.BlockWoodFence;
import industries.angelic.biomic.content.block.BlockWoodFenceGate;
import industries.angelic.biomic.content.block.BlockWoodPlanks;
import industries.angelic.biomic.content.block.BlockWoodPressurePlate;
import industries.angelic.biomic.content.block.BlockWoodSign;
import industries.angelic.biomic.content.block.BlockWoodSlab;
import industries.angelic.biomic.content.block.BlockWoodStairs;
import industries.angelic.biomic.content.block.BlockWoodTrapdoor;
import industries.angelic.biomic.content.block.TileEntityWoodSign;
import industries.angelic.biomic.content.entity.EntityWoodBoat;
import industries.angelic.biomic.content.item.ItemBlockWoodSlab;
import industries.angelic.biomic.content.item.ItemWoodBoat;
import industries.angelic.biomic.content.item.ItemWoodDoor;
import industries.angelic.biomic.content.item.ItemWoodSign;

/**
 * Regions Unexplored's woods for the logs Biomic grows: planks, slabs and stairs with RU's names and art, the
 * recipes RU gives them (a log of a wood makes four of its planks, smelts to charcoal where RU's logs_that_burn
 * says so) and the ore names other mods craft wood by. The logs RU turns into another mod's planks (small oak,
 * silver birch, bamboo) make those. Every wood but alpha oak also has RU's fence, gate, door, trapdoor, button,
 * pressure plate and sign, and the woods RU gives boats have boats: Et Futurum Requiem's modern boats and chest boats
 * where its new boats are on (EfrBoats), vanilla's boat of the wood otherwise. RU's hanging signs have no 1.7.10
 * block to be.
 */
public final class BiomicWoods {

    /**
     * RU's woods: its id, the planks texture its models bind, whether fire catches it (RU's fireproof sets) and the
     * pieces RU makes of it.
     */
    public enum Wood {

        ALPHA("alpha", "alpha_oak_planks", true, Kit.NONE, tree("alpha_log")),
        BAOBAB("baobab", "baobab_planks", true, Kit.FULL, tree("baobab_log")),
        BLACKWOOD("blackwood", "blackwood_planks", true, Kit.FULL, tree("blackwood_log")),
        BLUE_BIOSHROOM("blue_bioshroom", "blue_bioshroom_planks", true, Kit.NO_BOAT, tree("blue_bioshroom_stem")),
        BRIMWOOD("brimwood", "brimwood_planks", false, Kit.NO_BOAT, unburnt(decoration("brimwood_log")),
            unburnt(decoration("brimwood_log_magma"))),
        COBALT("cobalt", "cobalt_planks", false, Kit.NO_BOAT, unburnt(decoration("cobalt_log"))),
        CYPRESS("cypress", "cypress_planks", true, Kit.FULL, own(() -> BiomicBlocks.CYPRESS_LOG)),
        DEAD("dead", "dead_planks", false, Kit.FULL, unburnt(tree("dead_log")), tree("ashen_log")),
        EUCALYPTUS("eucalyptus", "eucalyptus_planks", true, Kit.FULL, own(() -> BiomicBlocks.EUCALYPTUS_LOG)),
        GREEN_BIOSHROOM("green_bioshroom", "green_bioshroom_planks", true, Kit.NO_BOAT,
            decoration("green_bioshroom_stem")),
        JOSHUA("joshua", "joshua_planks", true, Kit.FULL, own(() -> BiomicBlocks.JOSHUA_LOG)),
        KAPOK("kapok", "kapok_planks", true, Kit.FULL, tree("kapok_log")),
        LARCH("larch", "larch_planks", true, Kit.FULL, tree("larch_log")),
        MAGNOLIA("magnolia", "magnolia_planks", true, Kit.FULL, tree("magnolia_log")),
        MAPLE("maple", "maple_planks", true, Kit.FULL, tree("maple_log")),
        PALM("palm", "palm_planks", true, Kit.FULL, own(() -> BiomicBlocks.PALM_LOG)),
        PINE("pine", "pine_planks", true, Kit.FULL, own(() -> BiomicBlocks.PINE_LOG),
            own(() -> BiomicBlocks.STRIPPED_PINE_LOG), own(() -> BiomicBlocks.PINE_TRANSITION_LOG)),
        PINK_BIOSHROOM("pink_bioshroom", "pink_bioshroom_planks", true, Kit.NO_BOAT, tree("pink_bioshroom_stem")),
        REDWOOD("redwood", "redwood_planks", true, Kit.FULL, own(() -> BiomicBlocks.REDWOOD_LOG)),
        SOCOTRA("socotra", "socotra_planks", true, Kit.FULL, tree("socotra_log")),
        WILLOW("willow", "willow_planks", true, Kit.FULL, own(() -> BiomicBlocks.WILLOW_LOG)),
        WISTERIA("wisteria", "wisteria_planks", true, Kit.FULL, own(() -> BiomicBlocks.WISTERIA_LOG)),
        YELLOW_BIOSHROOM("yellow_bioshroom", "yellow_bioshroom_planks", false, Kit.NO_BOAT,
            unburnt(decoration("yellow_bioshroom_stem")));

        public final String id;
        /** Under Biomic's textures/blocks: RU's own file, copied. */
        public final String texture;
        public final boolean flammable;
        public final Kit kit;
        private final Log[] logs;
        private BlockWoodPlanks planks;
        private BlockWoodSlab slab;
        private BlockWoodSlab doubleSlab;
        private BlockWoodStairs stairs;
        private BlockWoodFence fence;
        private BlockWoodFenceGate fenceGate;
        private BlockWoodDoor door;
        private Item doorItem;
        private BlockWoodTrapdoor trapdoor;
        private BlockWoodButton button;
        private BlockWoodPressurePlate pressurePlate;
        private BlockWoodSign sign;
        private BlockWoodSign wallSign;
        private Item signItem;
        private Item boat;
        private Item chestBoat;

        Wood(String id, String texture, boolean flammable, Kit kit, Log... logs) {
            this.id = id;
            this.texture = "ru/" + texture;
            this.flammable = flammable;
            this.kit = kit;
            this.logs = logs;
        }

        public static Wood byId(String id) {
            for (Wood wood : values()) if (wood.id.equals(id)) return wood;
            return null;
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

        public BlockWoodFence fence() {
            return fence;
        }

        public BlockWoodFenceGate fenceGate() {
            return fenceGate;
        }

        public BlockWoodDoor door() {
            return door;
        }

        public Item doorItem() {
            return doorItem;
        }

        public BlockWoodTrapdoor trapdoor() {
            return trapdoor;
        }

        public BlockWoodButton button() {
            return button;
        }

        public BlockWoodPressurePlate pressurePlate() {
            return pressurePlate;
        }

        public BlockWoodSign sign() {
            return sign;
        }

        public BlockWoodSign wallSign() {
            return wallSign;
        }

        public Item signItem() {
            return signItem;
        }

        /** Its boat item, or null for a wood without one. */
        public Item boat() {
            return boat;
        }

        /** Its chest boat item: only Et Futurum Requiem's boats carry chests. */
        public Item chestBoat() {
            return chestBoat;
        }
    }

    /** The pieces RU makes of a wood besides its planks, slab and stairs. */
    public enum Kit {
        /** Alpha oak: planks only. */
        NONE,
        /** Fence, gate, door, trapdoor, button, pressure plate and sign. */
        NO_BOAT,
        /** Those and a boat. */
        FULL
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
            if (wood.kit != Kit.NONE) registerPieces(wood);
        }
        GameRegistry.registerTileEntity(TileEntityWoodSign.class, Biomic.MODID + ":wood_sign");
        EntityRegistry.registerModEntity(EntityWoodBoat.class, "wood_boat", 0, Biomic.instance, 80, 3, true);
        registered = true;
        Biomic.LOG.info("Registered planks, slabs, stairs and pieces of {} woods", Wood.values().length);
    }

    private static void registerPieces(Wood wood) {
        String id = wood.id;
        wood.fence = new BlockWoodFence(id + "_fence", wood.texture);
        wood.fenceGate = new BlockWoodFenceGate(id + "_fence_gate", wood.planks);
        wood.door = new BlockWoodDoor(id + "_door", "ru/" + id + "_door");
        wood.doorItem = new ItemWoodDoor("ru/" + id + "_door", wood.door);
        wood.door.setItem(wood.doorItem);
        wood.trapdoor = new BlockWoodTrapdoor(id + "_trapdoor", "ru/" + id + "_trapdoor");
        wood.button = new BlockWoodButton(id + "_button", wood.planks);
        wood.pressurePlate = new BlockWoodPressurePlate(id + "_pressure_plate", wood.texture);
        wood.sign = new BlockWoodSign(id + "_sign", wood.planks, true, id);
        wood.wallSign = new BlockWoodSign(id + "_sign", wood.planks, false, id);
        wood.signItem = new ItemWoodSign("ru/" + id + "_sign", wood.sign, wood.wallSign);
        wood.sign.setItem(wood.signItem);
        wood.wallSign.setItem(wood.signItem);
        GameRegistry.registerBlock(wood.fence, id + "_fence");
        GameRegistry.registerBlock(wood.fenceGate, id + "_fence_gate");
        GameRegistry.registerBlock(wood.door, null, id + "_door");
        GameRegistry.registerItem(wood.doorItem, id + "_door");
        GameRegistry.registerBlock(wood.trapdoor, id + "_trapdoor");
        GameRegistry.registerBlock(wood.button, id + "_button");
        GameRegistry.registerBlock(wood.pressurePlate, id + "_pressure_plate");
        GameRegistry.registerBlock(wood.sign, null, id + "_sign");
        GameRegistry.registerBlock(wood.wallSign, null, id + "_wall_sign");
        GameRegistry.registerItem(wood.signItem, id + "_sign");
        if (wood.flammable)
            for (Block block : new Block[] { wood.fence, wood.fenceGate }) Blocks.fire.setFireInfo(block, 5, 20);
        if (wood.kit != Kit.FULL) return;
        if (efrBoats()) {
            wood.boat = industries.angelic.biomic.compat.efr.EfrBoats.boat(wood, false);
            wood.chestBoat = industries.angelic.biomic.compat.efr.EfrBoats.boat(wood, true);
            GameRegistry.registerItem(wood.chestBoat, id + "_chest_boat");
        } else wood.boat = new ItemWoodBoat(id + "_boat", "ru/" + id + "_boat", wood);
        GameRegistry.registerItem(wood.boat, id + "_boat");
    }

    /** Whether the woods' boats are Et Futurum Requiem's (installed, with its new boats on). */
    public static boolean efrBoats() {
        return Loader.isModLoaded("etfuturum") && industries.angelic.biomic.compat.efr.EfrBoats.enabled();
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
            if (wood.kit != Kit.NONE) pieceRecipes(wood);
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

    /**
     * Ahead of every other recipe: Forge turns vanilla's planks recipes (door, trapdoor, button, plate, sign, gate,
     * boat) into plankWood ones, which take these planks too and would make oak pieces of them.
     */
    private static void pieceRecipes(Wood wood) {
        Block planks = wood.planks;
        Item stick = Items.stick;
        firstRecipe(new ItemStack(wood.fence, 3), "W#W", "W#W", 'W', planks, '#', stick);
        firstRecipe(new ItemStack(wood.fenceGate), "#W#", "#W#", 'W', planks, '#', stick);
        firstRecipe(new ItemStack(wood.doorItem, 3), "WW", "WW", "WW", 'W', planks);
        firstRecipe(new ItemStack(wood.trapdoor, 2), "WWW", "WWW", 'W', planks);
        firstRecipe(new ItemStack(wood.button), "W", 'W', planks);
        firstRecipe(new ItemStack(wood.pressurePlate), "WW", 'W', planks);
        firstRecipe(new ItemStack(wood.signItem, 3), "WWW", "WWW", " # ", 'W', planks, '#', stick);
        // Et Futurum Requiem crafts the boats it carries, as its own.
        if (wood.boat instanceof ItemWoodBoat) firstRecipe(new ItemStack(wood.boat), "W W", "WWW", 'W', planks);
        OreDictionary.registerOre("fenceWood", wood.fence);
        OreDictionary.registerOre("fenceGateWood", wood.fenceGate);
    }

    @SuppressWarnings("unchecked")
    private static void firstRecipe(ItemStack output, Object... pattern) {
        GameRegistry.addRecipe(output, pattern);
        List<IRecipe> recipes = CraftingManager.getInstance()
            .getRecipeList();
        recipes.add(0, recipes.remove(recipes.size() - 1));
    }

    private static void logRecipes(Block log, ItemStack planks, boolean burns) {
        ItemStack any = new ItemStack(log, 1, OreDictionary.WILDCARD_VALUE);
        GameRegistry.addShapelessRecipe(planks, any);
        OreDictionary.registerOre("logWood", any);
        if (burns) FurnaceRecipes.smelting()
            .func_151393_a(log, new ItemStack(Items.coal, 1, 1), 0.15F);
    }
}
