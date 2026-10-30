package industries.angelic.biomic.content;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.block.BlockRuBranch;
import industries.angelic.biomic.content.block.BlockRuGroundCover;
import industries.angelic.biomic.content.block.BlockRuLeaves;
import industries.angelic.biomic.content.block.BlockRuLeaves.TintMode;
import industries.angelic.biomic.content.block.BlockRuLog;
import industries.angelic.biomic.content.block.BlockRuPlant;
import industries.angelic.biomic.content.block.BlockRuSolid;

/**
 * Exact block palette required by the pinned modern-RU tree captures beyond
 * the hand-authored first eleven biome slices.
 */
public final class RuImportedTreeBlocks {

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();
    private static boolean registered;

    static {
        leaves("alpha_leaves", "ru/alpha_oak_leaves", TintMode.CONSTANT, 0xFFFFFF);
        log("alpha_log", "ru/alpha_oak_log", "ru/alpha_oak_log_top");
        leaves(
            "apple_oak_leaves",
            new String[] { "ru/apple_oak_leaves_stage_0", "ru/apple_oak_leaves_stage_1",
                "ru/apple_oak_leaves_stage_2" },
            TintMode.BIOME_FOLIAGE,
            0xFFFFFF);
        // Precolored source texture: RuColors does not register an ashen-leaf tint.
        leaves("ashen_leaves", "ru/ashen_leaves", TintMode.CONSTANT, 0xFFFFFF);
        log("ashen_log", "ru/ashen_log", "ru/ashen_log_top");
        leaves("bamboo_leaves", "ru/bamboo_leaves", TintMode.CONSTANT, 0xFFFFFF);
        put(
            "bamboo_log",
            new industries.angelic.biomic.content.block.BlockSmallOakLog(
                "bamboo_log",
                "ru/bamboo_log",
                "ru/bamboo_log_leaves"));
        branch("baobab_branch");
        leaves("baobab_leaves", "ru/baobab_leaves", TintMode.BIOME_FOLIAGE, 0xFFFFFF);
        log("baobab_log", "ru/baobab_log", "ru/baobab_log_top");
        branch("birch_branch");
        branch("blackwood_branch");
        // RU's texture is already green; it does not register a Blackwood block tint.
        leaves("blackwood_leaves", "ru/blackwood_leaves", TintMode.CONSTANT, 0xFFFFFF);
        log("blackwood_log", "ru/blackwood_log", "ru/blackwood_log_top");
        solid("blue_bioshroom_block", "ru/blue_bioshroom_block", "ru/blue_bioshroom_block", Material.wood, 0);
        log("blue_bioshroom_stem", "ru/blue_bioshroom_stem", "ru/blue_bioshroom_stem_top");
        leaves("blue_magnolia_leaves", "ru/blue_magnolia_leaves", TintMode.CONSTANT, 0xFFFFFF);
        branch("dead_branch");
        leaves("dead_leaves", "ru/dead_leaves", TintMode.CONSTANT, 0x654630);
        log("dead_log", "ru/dead_log", "ru/dead_log_top");
        leaves("golden_larch_leaves", "ru/golden_larch_leaves", TintMode.CONSTANT, 0xFFFFFF);
        branch("kapok_branch");
        leaves("kapok_leaves", "ru/kapok_leaves", TintMode.BIOME_FOLIAGE, 0xFFFFFF);
        log("kapok_log", "ru/kapok_log", "ru/kapok_log_top");
        foliagePlant("kapok_vines", "ru/kapok_vines_1");
        foliagePlant("kapok_vines_plant", "ru/kapok_vines_plant_1");
        branch("larch_branch");
        leaves("larch_leaves", "ru/larch_leaves", TintMode.CONSTANT, 0xFFFFFF);
        log("larch_log", "ru/larch_log", "ru/larch_log_top");
        branch("magnolia_branch");
        leaves("magnolia_leaves", "ru/magnolia_leaves", TintMode.BIOME_FOLIAGE, 0xFFFFFF);
        log("magnolia_log", "ru/magnolia_log", "ru/magnolia_log_top");
        branch("maple_branch");
        leaves("maple_leaves", "ru/maple_leaves", TintMode.BIOME_FOLIAGE, 0xFFFFFF);
        log("maple_log", "ru/maple_log", "ru/maple_log_top");
        cover("maple_leaf_litter", "ru/maple_leaf_litter");
        branch("oak_branch");
        leaves("orange_maple_leaves", "ru/orange_maple_leaves", TintMode.CONSTANT, 0x98541F);
        cover("orange_maple_leaf_litter", "ru/orange_maple_leaf_litter");
        solid("pink_bioshroom_block", "ru/pink_bioshroom_block", "ru/pink_bioshroom_block", Material.wood, 0);
        log("pink_bioshroom_stem", "ru/pink_bioshroom_stem", "ru/pink_bioshroom_stem_top");
        leaves("pink_magnolia_leaves", "ru/pink_magnolia_leaves", TintMode.CONSTANT, 0xFFFFFF);
        leaves("red_maple_leaves", "ru/red_maple_leaves", TintMode.CONSTANT, 0x8F2320);
        cover("red_maple_leaf_litter", "ru/red_maple_leaf_litter");
        put("saguaro_cactus", new industries.angelic.biomic.content.block.BlockSaguaro());
        plant("saguaro_cactus_flower", "ru/cactus_flower", 0xFFFFFF, 0);
        branch("silver_birch_branch");
        leaves("silver_birch_leaves", "ru/silver_birch_leaves", TintMode.ASPEN, 0xC8B34E);
        put(
            "silver_birch_log",
            new BlockRuLog(
                "imported_silver_birch_log",
                new String[] { "ru/silver_birch_log", "ru/silver_birch_log_base" },
                new String[] { "minecraft:log_birch_top", "minecraft:log_birch_top" },
                true));
        cover("silver_birch_leaf_litter", "ru/silver_birch_leaf_litter");
        put("small_oak_log", new industries.angelic.biomic.content.block.BlockSmallOakLog());
        branch("socotra_branch");
        leaves("socotra_leaves", "ru/socotra_leaves", TintMode.BIOME_FOLIAGE, 0xFFFFFF);
        log("socotra_log", "ru/socotra_log", "ru/socotra_log_top");
        leaves("white_magnolia_leaves", "ru/white_magnolia_leaves", TintMode.CONSTANT, 0xFFFFFF);
    }

    private RuImportedTreeBlocks() {}

    public static Block get(String modernId) {
        Entry entry = ENTRIES.get(modernId);
        return entry == null ? null : entry.block;
    }

    public static Map<String, Block> blocks() {
        Map<String, Block> result = new LinkedHashMap<>();
        for (Map.Entry<String, Entry> entry : ENTRIES.entrySet()) result.put(entry.getKey(), entry.getValue().block);
        return Collections.unmodifiableMap(result);
    }

    public static synchronized void register() {
        if (registered) return;
        for (Entry entry : ENTRIES.values()) {
            GameRegistry.registerBlock(entry.block, entry.registryName);
            if (entry.block instanceof BlockLeaves) Blocks.fire.setFireInfo(entry.block, 30, 60);
            else if (entry.block instanceof BlockLog || entry.block instanceof BlockRuBranch) {
                Blocks.fire.setFireInfo(entry.block, 5, 5);
            } else if (entry.block.getMaterial() == Material.plants) {
                Blocks.fire.setFireInfo(entry.block, 30, 100);
            }
        }
        registered = true;
        Biomic.LOG.info("Registered {} imported RU tree-palette blocks", ENTRIES.size());
    }

    private static void leaves(String name, String texture, TintMode tintMode, int... colors) {
        put(name, new BlockRuLeaves("imported_" + name, texture, tintMode, colors));
    }

    private static void leaves(String name, String[] textures, TintMode tintMode, int... colors) {
        put(name, new BlockRuLeaves("imported_" + name, textures, tintMode, colors));
    }

    private static void log(String name, String side, String top) {
        put(name, new BlockRuLog("imported_" + name, side, top));
    }

    private static void branch(String name) {
        String log = switch (name) {
            case "birch_branch" -> "minecraft:log_birch";
            case "oak_branch" -> "minecraft:log_oak";
            default -> "ru/" + name.replace("_branch", "_log");
        };
        put(name, new BlockRuBranch("imported_" + name, "ru/" + name, log));
    }

    private static void solid(String name, String side, String top, Material material, int lightValue) {
        put(name, new BlockRuSolid("imported_" + name, side, top, material, lightValue));
    }

    private static void plant(String name, String texture, int color, int lightValue) {
        put(name, new BlockRuPlant("imported_" + name, texture, null, color, lightValue));
    }

    private static void foliagePlant(String name, String texture) {
        put(
            name,
            new BlockRuPlant("imported_" + name, texture, null, BlockRuPlant.TintMode.BIOME_FOLIAGE, 0xFFFFFF, 0));
    }

    private static void cover(String name, String texture) {
        put(name, new BlockRuGroundCover("imported_" + name, texture));
    }

    private static void put(String name, Block block) {
        String modernId = "regions_unexplored:" + name;
        if (ENTRIES.put(modernId, new Entry("ru_imported_" + name, block)) != null) {
            throw new IllegalStateException("Duplicate imported RU tree block " + modernId);
        }
    }

    private record Entry(String registryName, Block block) {}
}
