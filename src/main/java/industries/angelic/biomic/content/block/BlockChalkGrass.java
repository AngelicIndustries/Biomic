package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** RU's spreading grass cap over chalk, including its layered biome-tinted side model. */
public final class BlockChalkGrass extends Block implements LayeredGrassBlock {

    @SideOnly(Side.CLIENT)
    private IIcon chalkIcon;
    @SideOnly(Side.CLIENT)
    private IIcon grassTopIcon;
    @SideOnly(Side.CLIENT)
    private IIcon grassSideIcon;
    @SideOnly(Side.CLIENT)
    private IIcon grassSideOverlayIcon;
    @SideOnly(Side.CLIENT)
    private IIcon snowySideIcon;

    public BlockChalkGrass() {
        super(Material.grass);
        setBlockName(Biomic.MODID + ".chalk_grass");
        setHardness(0.6F);
        setResistance(3.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
        setTickRandomly(true);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        chalkIcon = register.registerIcon(Biomic.MODID + ":chalk");
        grassTopIcon = register.registerIcon(Biomic.MODID + ":ru_grass_block_top");
        grassSideIcon = register.registerIcon(Biomic.MODID + ":chalk_grass_side");
        grassSideOverlayIcon = register.registerIcon(Biomic.MODID + ":ru_grass_block_side_overlay");
        snowySideIcon = register.registerIcon(Biomic.MODID + ":chalk_grass_snow");
        blockIcon = grassSideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 ? chalkIcon : side == 1 ? grassTopIcon : grassSideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 0) return chalkIcon;
        if (side == 1) return grassTopIcon;
        return LayeredGrassBlock.isSnowy(world, x, y, z) ? snowySideIcon : grassSideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return Blocks.grass.getBlockColor();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return Blocks.grass.getRenderColor(metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return Blocks.grass.colorMultiplier(world, x, y, z);
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.CHALK_GRASS;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(BiomicBlocks.CHALK);
    }

    @Override
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction,
        IPlantable plantable) {
        return Blocks.grass.canSustainPlant(world, x, y, z, direction, plantable);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        if (world.getBlockLightValue(x, y + 1, z) < 4 && world.getBlockLightOpacity(x, y + 1, z) > 2) {
            world.setBlock(x, y, z, BiomicBlocks.CHALK);
            return;
        }
        if (world.getBlockLightValue(x, y + 1, z) < 9) return;
        for (int attempt = 0; attempt < 4; attempt++) {
            int targetX = x + random.nextInt(3) - 1;
            int targetY = y + random.nextInt(5) - 3;
            int targetZ = z + random.nextInt(3) - 1;
            if (world.getBlock(targetX, targetY, targetZ) == BiomicBlocks.CHALK
                && world.getBlockLightValue(targetX, targetY + 1, targetZ) >= 4
                && world.getBlockLightOpacity(targetX, targetY + 1, targetZ) <= 2) {
                world.setBlock(targetX, targetY, targetZ, this);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBottomIcon() {
        return chalkIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getGrassTopIcon() {
        return grassTopIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getGrassSideIcon() {
        return grassSideIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getGrassSideOverlayIcon() {
        return grassSideOverlayIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getSnowySideIcon() {
        return snowySideIcon;
    }

}
