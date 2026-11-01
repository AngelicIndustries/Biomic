package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemSlab;

import industries.angelic.biomic.content.block.BlockWoodSlab;

/** A wood slab's item: FML builds it by its arguments' classes, so the flag is a boxed Boolean. */
public final class ItemBlockWoodSlab extends ItemSlab {

    public ItemBlockWoodSlab(Block block, BlockWoodSlab single, BlockWoodSlab doubleSlab, Boolean isDouble) {
        super(block, single, doubleSlab, isDouble);
    }
}
