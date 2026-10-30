package industries.angelic.biomic.content.gardens;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

final class GardensSupport {

    private GardensSupport() {}

    static boolean soul(Block block, int metadata) {
        return block == Blocks.soul_sand || block == GardensContent.BLIGHTWART_BLOCK
            || external(block, metadata, "minecraft:soul_soil")
            || external(block, metadata, "minecraft:nether_wart_block")
            || external(block, metadata, "minecraft:warped_wart_block");
    }

    static boolean nylium(Block block, int metadata) {
        return external(block, metadata, "minecraft:crimson_nylium")
            || external(block, metadata, "minecraft:warped_nylium");
    }

    static boolean soulSoil(Block block, int metadata) {
        return external(block, metadata, "minecraft:soul_soil");
    }

    private static boolean external(Block block, int metadata, String expected) {
        if (block == null) return false;
        try {
            return expected.equals(
                industries.angelic.biomic.world.modern.ModernStateCompatibility.modernState(block, metadata)
                    .replaceAll("\\[.*$", ""));
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
