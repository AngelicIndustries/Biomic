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

/**
 * Modern all-cap-except-bottom mushroom disk. One ID, brown0/red1, no borrowed-face approximation.
 * Vanilla cap/inside sprites and full-cube geometry are reused directly; no new asset or renderer.
 */
public final class BlockPoreBottomMushroom extends BlockHugeMushroom {

    public BlockPoreBottomMushroom() {
        super(Material.wood, 0);
        setBlockName("biomic.mushroom_cap_pore_bottom");
        setHardness(.2F);
        setStepSound(soundTypeWood);
        setCreativeTab(null);
    }

    public static int kind(int metadata) {
        if (metadata < 0 || metadata > 1) throw new IllegalArgumentException("Unknown pore-bottom mushroom color");
        return metadata;
    }

    public static int textureMetadata(int side) {
        if (side < 0 || side > 5) throw new IllegalArgumentException("Unknown mushroom face");
        return side == 0 ? 0 : 14;
    }

    @Override
    public int damageDropped(int metadata) {
        kind(metadata);
        return 0;
    }

    @Override
    protected ItemStack createStackedBlock(int metadata) {
        return new ItemStack(this, 1, kind(metadata));
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return kind(world.getBlockMetadata(x, y, z));
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return (kind(metadata) == 0 ? Blocks.brown_mushroom_block : Blocks.red_mushroom_block)
            .getItemDropped(14, random, fortune);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {/* Borrow vanilla atlas sprites. */}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return (kind(metadata) == 0 ? Blocks.brown_mushroom_block : Blocks.red_mushroom_block)
            .getIcon(side, textureMetadata(side));
    }
}
