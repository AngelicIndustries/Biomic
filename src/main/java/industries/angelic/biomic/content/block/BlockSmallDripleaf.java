package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/** Two-block small dripleaf; low metadata bits store facing and bit two stores the upper half. */
public final class BlockSmallDripleaf extends BlockDripleafBase {

    public static final int UPPER_BIT = 4;

    private IIcon topIcon;
    private IIcon sideIcon;
    private IIcon lowerStemIcon;
    private IIcon upperStemIcon;

    public BlockSmallDripleaf() {
        this(false);
    }

    public BlockSmallDripleaf(boolean waterlogged) {
        super(waterlogged);
        setBlockName(Biomic.MODID + (waterlogged ? ".wet_small_dripleaf" : ".small_dripleaf"));
        setBlockTextureName(Biomic.MODID + ":small_dripleaf_top");
        if (waterlogged) setCreativeTab(null);
    }

    public static boolean isUpper(int metadata) {
        return (metadata & UPPER_BIT) != 0;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (isUpper(metadata)) {
            if (!isWaterlogged() && world.getBlock(x, y - 1, z) == this)
                return !isUpper(world.getBlockMetadata(x, y - 1, z));
            return world.getBlock(x, y - 1, z) instanceof BlockSmallDripleaf
                && !isUpper(world.getBlockMetadata(x, y - 1, z))
                && (world.getBlockMetadata(x, y - 1, z) & 3) == (metadata & 3);
        }
        if (isWaterlogged() && !(world.getBlock(x, y + 1, z) instanceof BlockSmallDripleaf
            && isUpper(world.getBlockMetadata(x, y + 1, z))
            && (world.getBlockMetadata(x, y + 1, z) & 3) == (metadata & 3))) return false;
        return hasPlantableFloor(world, x, y, z);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return isUpper(metadata) ? noDrop()
            : Item.getItemFromBlock(industries.angelic.biomic.content.BiomicDripleafBlocks.SMALL_DRIPLEAF);
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(industries.angelic.biomic.content.BiomicDripleafBlocks.SMALL_DRIPLEAF);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        topIcon = register.registerIcon(Biomic.MODID + ":small_dripleaf_top");
        sideIcon = register.registerIcon(Biomic.MODID + ":small_dripleaf_side");
        lowerStemIcon = register.registerIcon(Biomic.MODID + ":small_dripleaf_stem_bottom");
        upperStemIcon = register.registerIcon(Biomic.MODID + ":small_dripleaf_stem_top");
        blockIcon = topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getTopIcon() {
        return topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getSideIcon() {
        return sideIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getStemIcon(boolean upper) {
        return upper ? upperStemIcon : lowerStemIcon;
    }
}
