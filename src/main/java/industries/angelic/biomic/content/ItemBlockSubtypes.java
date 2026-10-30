package industries.angelic.biomic.content;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlockWithMetadata;

/** An item block with one item per metadata (registerBlock needs a class with a Block constructor). */
public final class ItemBlockSubtypes extends ItemBlockWithMetadata {

    public ItemBlockSubtypes(Block block) {
        super(block, block);
    }
}
