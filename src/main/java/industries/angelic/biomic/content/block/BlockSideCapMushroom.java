package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Exact schematic mushroom skirt: pore ends and one cardinal/two adjacent cap sides. */
public final class BlockSideCapMushroom extends BlockHugeMushroom {

    private static final int[] MASKS = { 1, 2, 4, 8, 3, 6, 12, 9 }; // N,E,S,W,NE,ES,SW,WN

    public BlockSideCapMushroom() {
        super(Material.wood, 0);
        setBlockName("biomic.mushroom_cap_side_only");
        setHardness(.2F);
        setStepSound(soundTypeWood);
        setCreativeTab(null);
    }

    public static int metadata(int value) {
        if (value < 0 || value > 15) throw new IllegalArgumentException("Unknown side-cap model");
        return value;
    }

    public static int horizontalMask(int value) {
        return MASKS[metadata(value) & 7];
    }

    public static int textureMetadata(int side, int value) {
        if (side < 0 || side > 5) throw new IllegalArgumentException("Unknown mushroom face");
        int bit = switch (side) {
            case 2 -> 1;
            case 3 -> 4;
            case 4 -> 8;
            case 5 -> 2;
            default -> 0;
        };
        return (horizontalMask(value) & bit) != 0 ? 14 : 0;
    }

    @Override
    public int damageDropped(int value) {
        metadata(value);
        return 0;
    }

    @Override
    protected ItemStack createStackedBlock(int value) {
        return new ItemStack(this, 1, metadata(value));
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return metadata(world.getBlockMetadata(x, y, z));
    }

    @Override
    public Item getItemDropped(int value, Random random, int fortune) {
        return (metadata(value) < 8 ? Blocks.brown_mushroom_block : Blocks.red_mushroom_block)
            .getItemDropped(14, random, fortune);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int value) {
        return (metadata(value) < 8 ? Blocks.brown_mushroom_block : Blocks.red_mushroom_block)
            .getIcon(side, textureMetadata(side, value));
    }
}
