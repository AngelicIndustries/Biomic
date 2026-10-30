package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.BlockLeaves;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Biome-tinted cube retaining RU's distinct palm side and top leaf sprites. */
public final class BlockPalmLeaves extends BlockLeaves {

    private static final int FALLBACK_COLOR = 0x77B14D;

    private IIcon sideIcon;
    private IIcon topIcon;

    public BlockPalmLeaves() {
        setBlockName(Biomic.MODID + ".palm_leaves");
        setHardness(0.2F);
        setLightOpacity(1);
        setStepSound(soundTypeGrass);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcon = register.registerIcon(Biomic.MODID + ":palm_leaves_side");
        topIcon = register.registerIcon(Biomic.MODID + ":palm_leaves_top");
        field_150129_M[0] = new IIcon[] { sideIcon };
        field_150129_M[1] = new IIcon[] { sideIcon };
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 || side == 1 ? topIcon : sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isOpaqueCube() {
        return Blocks.leaves.isOpaqueCube();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return Blocks.leaves.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return FALLBACK_COLOR;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return FALLBACK_COLOR;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        BiomeGenBase biome = industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z);
        return biome == null ? FALLBACK_COLOR : BlockRuLeaves.foliageColor(world, x, y, z);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public String[] func_150125_e() {
        return new String[] { "palm" };
    }
}
