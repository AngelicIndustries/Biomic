package industries.angelic.biomic.content.ports;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** One item per shell. */
public final class ItemBlockGroundShell extends ItemBlock {

    public ItemBlockGroundShell(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    /** ItemBlock asks the block for metadata 0: every shell would show the clam. */
    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon getIconFromDamage(int damage) {
        return field_150939_a.getIcon(1, damage);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        int meta = Math.max(0, Math.min(stack.getItemDamage(), PortBlocks.SHELLS.length - 1));
        return super.getUnlocalizedName() + "." + PortBlocks.SHELLS[meta];
    }
}
