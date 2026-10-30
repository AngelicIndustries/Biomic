package industries.angelic.biomic.content.gardens;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.content.BiomicBlocks;

/** Intrinsic Gardens of the Dead biome content only; construction families remain deliberately out of scope. */
public final class GardensContent {

    public static final BlockSoulSpore SOUL_SPORE = new BlockSoulSpore("gardens_soul_spore", false);
    public static final BlockSoulSpore GLOWING_SOUL_SPORE = new BlockSoulSpore("gardens_glowing_soul_spore", true);
    public static final Block SOULBLIGHT_FUNGUS = new BlockSoulblightFungus();
    public static final Block SOULBLIGHT_SPROUTS = new BlockSoulblightSprouts();
    public static final Block SOULBLIGHT_STEM = new BlockGardensStem();
    public static final Block BLIGHTWART_BLOCK = new BlockGardensSolid(
        "gardens_blightwart_block",
        "gardens/blightwart_block",
        1);
    public static final Block BLISTERCROWN = new BlockGardensPlant(
        "gardens_blistercrown",
        "gardens/blistercrown",
        BlockGardensPlant.Support.NYLIUM,
        5);
    public static final Block TALL_BLISTERCROWN = new BlockTallBlistercrown();
    public static final BlockWhistlecane WHISTLECANE = new BlockWhistlecane();
    private static final Map<String, Block> STATES = states();
    private static boolean registered;

    private GardensContent() {}

    public record Resolved(Block block, int metadata) {}

    public static Map<String, Block> intrinsicBlocks() {
        return java.util.Collections.unmodifiableMap(STATES);
    }

    public static String encode(Block block, int metadata) {
        for (var entry : STATES.entrySet()) if (entry.getValue() == block)
            return validMetadata(block, metadata) ? "biomic:" + entry.getKey() + "[legacy_meta=" + metadata + "]"
                : null;
        return null;
    }

    public static Resolved resolve(String state) {
        if (state == null || !state.startsWith("biomic:gardens_")) return null;
        int bracket = state.indexOf('[');
        String id = state.substring(7, bracket < 0 ? state.length() : bracket);
        Block block = STATES.get(id);
        if (block == null) return null;
        int metadata = 0;
        if (bracket >= 0) {
            String prefix = "[legacy_meta=";
            if (!state.substring(bracket)
                .startsWith(prefix) || !state.endsWith("]")) return null;
            try {
                metadata = Integer.parseInt(state.substring(bracket + prefix.length(), state.length() - 1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return validMetadata(block, metadata) ? new Resolved(block, metadata) : null;
    }

    public static synchronized void register() {
        if (registered) return;
        for (Block block : STATES.values()) block.setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        for (var entry : STATES.entrySet()) {
            if (entry.getValue() == TALL_BLISTERCROWN)
                GameRegistry.registerBlock(entry.getValue(), ItemBlockTallBlistercrown.class, entry.getKey());
            else GameRegistry.registerBlock(entry.getValue(), entry.getKey());
        }
        registered = true;
    }

    static boolean validMetadata(Block block, int metadata) {
        if (block == SOUL_SPORE) return metadata >= 0 && metadata <= 7;
        if (block == GLOWING_SOUL_SPORE) return metadata == 0 || metadata == BlockSoulSpore.DOWN;
        if (block == SOULBLIGHT_STEM) return metadata == 0 || metadata == 4 || metadata == 8;
        if (block == TALL_BLISTERCROWN) return metadata == 0 || metadata == 8;
        if (block == WHISTLECANE) return metadata == 0 || metadata == BlockWhistlecane.GROWING;
        return metadata == 0;
    }

    private static Map<String, Block> states() {
        Map<String, Block> values = new LinkedHashMap<>();
        values.put("gardens_soul_spore", SOUL_SPORE);
        values.put("gardens_glowing_soul_spore", GLOWING_SOUL_SPORE);
        values.put("gardens_soulblight_fungus", SOULBLIGHT_FUNGUS);
        values.put("gardens_soulblight_sprouts", SOULBLIGHT_SPROUTS);
        values.put("gardens_soulblight_stem", SOULBLIGHT_STEM);
        values.put("gardens_blightwart_block", BLIGHTWART_BLOCK);
        values.put("gardens_blistercrown", BLISTERCROWN);
        values.put("gardens_tall_blistercrown", TALL_BLISTERCROWN);
        values.put("gardens_whistlecane", WHISTLECANE);
        return values;
    }
}
