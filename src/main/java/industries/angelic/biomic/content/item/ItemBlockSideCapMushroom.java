package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import industries.angelic.biomic.content.block.BlockSideCapMushroom;

public final class ItemBlockSideCapMushroom extends ItemBlock {

    public ItemBlockSideCapMushroom(Block block) {
        super(block);
        if (!(block instanceof BlockSideCapMushroom)) throw new IllegalArgumentException("Expected side cap");
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return BlockSideCapMushroom.metadata(damage);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName() + "." + (getMetadata(stack.getItemDamage()) < 8 ? "brown" : "red");
    }
}
