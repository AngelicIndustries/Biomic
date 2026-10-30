package industries.angelic.biomic.content.ports;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/** Tropicraft's matted seagrass: sand grown over by a seagrass mat, the floor of a meadow. Metadata is the grass. */
public final class BlockMattedSeagrass extends Block {

    private final String[] kinds;
    @SideOnly(Side.CLIENT)
    private IIcon[] tops, sides;

    BlockMattedSeagrass(String name, String... kinds) {
        super(Material.sand);
        this.kinds = kinds.clone();
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.5F);
        setStepSound(soundTypeSand);
        setHarvestLevel("shovel", 0);
    }

    @Override
    public Item getItemDropped(int metadata, java.util.Random random, int fortune) {
        return Item.getItemFromBlock(Blocks.sand);
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < kinds.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int kind = Math.min(metadata, kinds.length - 1);
        return side == 1 ? tops[kind] : side == 0 ? Blocks.sand.getIcon(0, 0) : sides[kind];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        tops = new IIcon[kinds.length];
        sides = new IIcon[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            tops[i] = register.registerIcon(Biomic.MODID + ":ports/trop_matted_" + kinds[i] + "_top");
            sides[i] = register.registerIcon(Biomic.MODID + ":ports/trop_matted_" + kinds[i] + "_side");
        }
    }
}
