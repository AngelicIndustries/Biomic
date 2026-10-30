package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Decorative hanging wisteria curtains generated beneath grove canopies. */
public final class BlockWisteriaVine extends Block {

    private static final int[] COLORS = { 0x89BFFF, 0xB39DDB, 0xE89A91 };

    private IIcon bodyIcon;
    private IIcon tipIcon;

    public BlockWisteriaVine() {
        super(Material.plants);
        setBlockName(Biomic.MODID + ".wisteria_vine");
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 1.0F, 0.9F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        bodyIcon = register.registerIcon(Biomic.MODID + ":wisteria_vines");
        tipIcon = register.registerIcon(Biomic.MODID + ":wisteria_vines_tip");
        blockIcon = bodyIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return (metadata & 1) == 0 ? bodyIcon : tipIcon;
    }

    @Override
    public int getRenderType() {
        return 1;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return COLORS[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return COLORS[Math.floorMod(metadata >> 1, COLORS.length)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return getRenderColor(world.getBlockMetadata(x, y, z));
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
