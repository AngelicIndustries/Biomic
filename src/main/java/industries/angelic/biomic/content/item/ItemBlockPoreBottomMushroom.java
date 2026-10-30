package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import industries.angelic.biomic.content.block.BlockPoreBottomMushroom;

/** Exact brown/red item subtype; invalid command damage cannot turn into an approximate face model. */
public final class ItemBlockPoreBottomMushroom extends ItemBlock {

    public ItemBlockPoreBottomMushroom(Block block) {
        super(block);
        if (!(block instanceof BlockPoreBottomMushroom)) throw new IllegalArgumentException("Expected pore-bottom cap");
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return BlockPoreBottomMushroom.kind(damage);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + (getMetadata(stack.getItemDamage()) == 0 ? "brown" : "red");
    }
}
