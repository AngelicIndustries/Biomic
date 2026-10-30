package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicDripleafBlocks;

/** Big-dripleaf crown or stem, preserving Origen's distinct modern block states in 1.7 metadata. */
public final class BlockBigDripleaf extends BlockDripleafBase {

    private final boolean stem;
    private IIcon topIcon;
    private IIcon sideIcon;
    private IIcon tipIcon;
    private IIcon stemIcon;

    public BlockBigDripleaf(boolean stem) {
        this(stem, false);
    }

    public BlockBigDripleaf(boolean stem, boolean waterlogged) {
        super(waterlogged);
        this.stem = stem;
        setBlockName(Biomic.MODID + (waterlogged ? ".wet_" : ".") + (stem ? "big_dripleaf_stem" : "big_dripleaf"));
        setBlockTextureName(Biomic.MODID + (stem ? ":big_dripleaf_stem" : ":big_dripleaf_top"));
        if (stem || waterlogged) setCreativeTab(null);
        if (stem) setBlockBounds(5.0F / 16.0F, 0.0F, 5.0F / 16.0F, 11.0F / 16.0F, 1.0F, 11.0F / 16.0F);
    }

    public boolean isStem() {
        return stem;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (isWaterlogged() && stem
            && !(world.getBlock(x, y + 1, z) instanceof BlockBigDripleaf
                && (world.getBlockMetadata(x, y + 1, z) & 3) == (world.getBlockMetadata(x, y, z) & 3)))
            return false;
        Block below = world.getBlock(x, y - 1, z);
        return !isWaterlogged() && below == BiomicDripleafBlocks.BIG_DRIPLEAF_STEM
            || below instanceof BlockBigDripleaf leaf && leaf.isStem()
                && (world.getBlockMetadata(x, y - 1, z) & 3) == (world.getBlockMetadata(x, y, z) & 3)
            || hasPlantableFloor(world, x, y, z);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return stem ? noDrop() : Item.getItemFromBlock(BiomicDripleafBlocks.BIG_DRIPLEAF);
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return isWaterlogged() ? Item.getItemFromBlock(BiomicDripleafBlocks.BIG_DRIPLEAF)
            : super.getItem(world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        stemIcon = register.registerIcon(Biomic.MODID + ":big_dripleaf_stem");
        if (stem) {
            blockIcon = stemIcon;
            return;
        }
        topIcon = register.registerIcon(Biomic.MODID + ":big_dripleaf_top");
        sideIcon = register.registerIcon(Biomic.MODID + ":big_dripleaf_side");
        tipIcon = register.registerIcon(Biomic.MODID + ":big_dripleaf_tip");
        blockIcon = topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getTopIcon() {
        return stem ? stemIcon : topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getSideIcon() {
        return stem ? stemIcon : sideIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getTipIcon() {
        return stem ? stemIcon : tipIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getStemIcon() {
        return stemIcon;
    }
}
