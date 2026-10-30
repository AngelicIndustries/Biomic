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

/** Exact layered RU grass cap used by peat, silt and argillite surface rules. */
public final class BlockRuSurfaceGrass extends Block implements LayeredGrassBlock {

    private final String bottomTexture;
    private final String topTexture;
    private final String sideTexture;
    private final String overlayTexture;
    private final String snowyTexture;
    private IIcon bottomIcon;
    private IIcon topIcon;
    private IIcon sideIcon;
    private IIcon overlayIcon;
    private IIcon snowyIcon;

    public BlockRuSurfaceGrass(String name, String bottom, String top, String side, String overlay, String snowy) {
        super(Material.grass);
        bottomTexture = bottom;
        topTexture = top;
        sideTexture = side;
        overlayTexture = overlay;
        snowyTexture = snowy;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.6F);
        setStepSound(soundTypeGrass);
        setTickRandomly(true);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        bottomIcon = register.registerIcon(texture(bottomTexture));
        topIcon = register.registerIcon(texture(topTexture));
        sideIcon = register.registerIcon(texture(sideTexture));
        overlayIcon = register.registerIcon(texture(overlayTexture));
        snowyIcon = register.registerIcon(texture(snowyTexture));
        blockIcon = sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 ? bottomIcon : side == 1 ? topIcon : sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        if (side == 0) return bottomIcon;
        if (side == 1) return topIcon;
        return LayeredGrassBlock.isSnowy(world, x, y, z) ? snowyIcon : sideIcon;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.CHALK_GRASS;
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
        // Vanilla grass's 3x3 blend, from the biome at this block's height: vanilla's own lookup is by column, so on a
        // floating island this grass took the ground's colour while vanilla grass beside it took the island's.
        int red = 0, green = 0, blue = 0;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int color = industries.angelic.biomic.client.HostedBiomeColors.biome(world, x + dx, y, z + dz)
                .getBiomeGrassColor(x + dx, y, z + dz);
            red += (color & 0xFF0000) >> 16;
            green += (color & 0xFF00) >> 8;
            blue += color & 0xFF;
        }
        return (red / 9 & 0xFF) << 16 | (green / 9 & 0xFF) << 8 | blue / 9 & 0xFF;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction,
        IPlantable plantable) {
        return Blocks.grass.canSustainPlant(world, x, y, z, direction, plantable);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        // Imported terrain blocks intentionally do not spread into unrelated substrates.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getBottomIcon() {
        return bottomIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getGrassTopIcon() {
        return topIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getGrassSideIcon() {
        return sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getGrassSideOverlayIcon() {
        return overlayIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getSnowySideIcon() {
        return snowyIcon;
    }

    private static String texture(String name) {
        return name.indexOf(':') >= 0 ? name : Biomic.MODID + ":" + name;
    }
}
