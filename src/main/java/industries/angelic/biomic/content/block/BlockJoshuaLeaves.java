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
import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** Two-part crown retaining RU's oversized four-plane Joshua-leaf model. */
public final class BlockJoshuaLeaves extends Block {

    private static final int FALLBACK_COLOR = 0x48B518;

    private IIcon bottomLeftIcon;
    private IIcon bottomRightIcon;
    private IIcon topLeftIcon;
    private IIcon topRightIcon;
    private IIcon beardIcon;

    public BlockJoshuaLeaves() {
        super(Material.leaves);
        setBlockName(Biomic.MODID + ".joshua_leaves");
        setHardness(0.2F);
        setStepSound(soundTypeGrass);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setLightOpacity(0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        bottomLeftIcon = register.registerIcon(Biomic.MODID + ":joshua_leaves_bottom");
        bottomRightIcon = register.registerIcon(Biomic.MODID + ":joshua_leaves_bottom_right");
        topLeftIcon = register.registerIcon(Biomic.MODID + ":joshua_leaves_top");
        topRightIcon = register.registerIcon(Biomic.MODID + ":joshua_leaves_top_right");
        beardIcon = register.registerIcon(Biomic.MODID + ":joshua_beard");
        blockIcon = bottomLeftIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return isUpper(metadata) ? topLeftIcon : bottomLeftIcon;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.JOSHUA_CROWN;
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
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome == null ? FALLBACK_COLOR : biome.getBiomeFoliageColor(x, y, z);
    }

    public boolean isUpper(int metadata) {
        return (metadata & 1) != 0;
    }

    /** Metadata zero remains natural for compatibility with already-generated Biomic worlds. */
    public boolean isNaturalLower(int metadata) {
        return !isUpper(metadata) && (metadata & 2) == 0;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getLeftIcon(int metadata) {
        return isUpper(metadata) ? topLeftIcon : bottomLeftIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getRightIcon(int metadata) {
        return isUpper(metadata) ? topRightIcon : bottomRightIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBeardIcon() {
        return beardIcon;
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
