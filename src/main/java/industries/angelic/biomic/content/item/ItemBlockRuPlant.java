package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.content.block.BlockRuDoublePlant;
import industries.angelic.biomic.content.block.BlockRuVariantPlant;

/** Preserve the species in inventory/pick-block, with the source's upper-sprite double-plant item. */
public final class ItemBlockRuPlant extends ItemBlock {

    public ItemBlockRuPlant(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int damage) {
        return damage;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        String variant = field_150939_a instanceof BlockRuVariantPlant plant
            ? plant.getVariantName(stack.getItemDamage())
            : ((BlockRuDoublePlant) field_150939_a).getVariantName(stack.getItemDamage());
        return "tile.biomic.plant." + variant;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int metadata) {
        return field_150939_a instanceof BlockRuDoublePlant plant ? plant.getUpperIcon(metadata)
            : field_150939_a.getIcon(1, metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return pass == 0 ? field_150939_a.getRenderColor(stack.getItemDamage()) : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean requiresMultipleRenderPasses() {
        return field_150939_a instanceof BlockRuDoublePlant;
    }

    @Override
    public int getRenderPasses(int metadata) {
        return field_150939_a instanceof BlockRuDoublePlant plant && plant.isTassel(metadata) ? 2 : 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamageForRenderPass(int metadata, int pass) {
        return pass == 1 ? ((BlockRuDoublePlant) field_150939_a).getFlowerOverlay(metadata | 8)
            : getIconFromDamage(metadata);
    }
}
