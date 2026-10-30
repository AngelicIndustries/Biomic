package industries.angelic.biomic.content.gardens;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

final class BlockTallBlistercrown extends BlockDoublePlant {

    private IIcon lower, upper;

    BlockTallBlistercrown() {
        setBlockName("biomic.gardens_tall_blistercrown");
        setLightLevel(5 / 15F);
        setHardness(0);
        setStepSound(soundTypeGrass);
    }

    @Override
    protected boolean canPlaceBlockOn(Block block) {
        return block != null;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if ((metadata & 8) != 0)
            return y > 0 && world.getBlock(x, y - 1, z) == this && (world.getBlockMetadata(x, y - 1, z) & 8) == 0;
        return y > 0 && y < world.getHeight() - 1
            && GardensSupport.nylium(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z))
            && world.getBlock(x, y + 1, z) == this
            && (world.getBlockMetadata(x, y + 1, z) & 8) != 0;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return (metadata & 8) == 0 ? Item.getItemFromBlock(this) : null;
    }

    /**
     * Vanilla render type 40 reaches into BlockDoublePlant's private sunflower icon table for variant zero.
     * Blistercrown is the source-authored pair of ordinary crossed quads, with the half selected by metadata.
     */
    @Override
    public int getRenderType() {
        return 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        lower = register.registerIcon("biomic:gardens/tall_blistercrown_bottom");
        upper = register.registerIcon("biomic:gardens/tall_blistercrown_top");
        blockIcon = lower;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return (metadata & 8) != 0 ? upper : lower;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon func_149888_a(boolean isUpper, int variant) {
        return isUpper ? upper : lower;
    }

    /**
     * BlockDoublePlant enumerates its vanilla variants from private icon arrays. This block deliberately owns
     * only one authored lower-half item, so consulting those uninitialised vanilla arrays is both wrong and unsafe.
     */
    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List variants) {
        variants.add(new ItemStack(item, 1, 0));
    }
}
