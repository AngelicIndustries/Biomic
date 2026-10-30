package industries.angelic.biomic.content.block;

import java.util.Random;

import ganymedes01.etfuturum.blocks.BlockPinkPetals;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/**
 * Regions Unexplored clover adapted to EFR's compatible 1.7 ground-cover block and renderer.
 *
 * <p>
 * The low two metadata bits store one through four patches; the high two bits store rotation.
 */
public final class BlockClover extends BlockPinkPetals {

    private net.minecraft.util.IIcon stemIcon;

    public BlockClover() {
        super();
        setBlockName(Biomic.MODID + ".clover");
        setBlockTextureName(Biomic.MODID + ":clover");
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    public int quantityDropped(int metadata, int fortune, Random random) {
        return getPatchCount(metadata);
    }

    public static int getPatchCount(int metadata) {
        return (metadata & 3) + 1;
    }

    @Override
    public int getRenderType() {
        return industries.angelic.biomic.client.BiomicRenderIds.RU_CONEFLOWER;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon("biomic:ru_decor/clover");
        stemIcon = register.registerIcon("biomic:ru_decor/clover_stem");
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon getStemIcon() {
        return stemIcon;
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public int colorMultiplier(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
            .getBiomeGrassColor(x, y, z);
    }
}
