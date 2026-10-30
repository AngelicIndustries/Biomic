package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** Directional block hosting RU's exact six-element eucalyptus branch model. */
public final class BlockEucalyptusBranch extends Block {

    private IIcon logIcon;
    private IIcon branchIcon;

    public BlockEucalyptusBranch() {
        super(Material.wood);
        setBlockName(Biomic.MODID + ".eucalyptus_branch");
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setLightOpacity(0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBlockBounds(0.25F, 0.25F, 0.25F, 0.75F, 0.75F, 0.75F);
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
        return BiomicRenderIds.EUCALYPTUS_BRANCH;
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
        logIcon = register.registerIcon(Biomic.MODID + ":eucalyptus_log_branch");
        branchIcon = register.registerIcon(Biomic.MODID + ":eucalyptus_branch");
        blockIcon = logIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getLogIcon() {
        return logIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBranchIcon() {
        return branchIcon;
    }
}
