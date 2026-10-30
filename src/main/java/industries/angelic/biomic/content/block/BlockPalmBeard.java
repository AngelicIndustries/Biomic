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
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** Four-way natural palm crown accent using RU's authored beard model envelope. */
public final class BlockPalmBeard extends Block {

    private IIcon beardIcon;

    public BlockPalmBeard() {
        super(Material.plants);
        setBlockName(Biomic.MODID + ".palm_beard");
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBoundsForMetadata(0);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setBoundsForMetadata(world.getBlockMetadata(x, y, z));
    }

    private void setBoundsForMetadata(int metadata) {
        switch (metadata & 3) {
            case 1:
                setBlockBounds(0.0F, 0.0F, 0.0F, 0.1875F, 1.0F, 1.0F);
                break;
            case 2:
                setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.1875F);
                break;
            case 3:
                setBlockBounds(0.8125F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
                break;
            default:
                setBlockBounds(0.0F, 0.0F, 0.8125F, 1.0F, 1.0F, 1.0F);
                break;
        }
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
    public int getRenderType() {
        return BiomicRenderIds.PALM_BEARD;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        beardIcon = register.registerIcon(Biomic.MODID + ":palm_beard");
        blockIcon = beardIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBeardIcon() {
        return beardIcon;
    }
}
