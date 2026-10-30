package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** Vanilla-rendered two-block RU shrubs with up to eight texture pairs per block ID. */
public class BlockRuDoublePlant extends BlockDoublePlant {

    public enum TintMode {
        AUTO,
        CONSTANT,
        BIOME_GRASS,
        BIOME_FOLIAGE
    }

    private final String[] variants;
    private final String[] lowerTextures;
    private final String[] upperTextures;
    private final TintMode tintMode;
    private IIcon[] lowerIcons;
    private IIcon[] upperIcons;
    private IIcon tasselLowerFlowers;
    private IIcon tasselUpperFlowers;
    private final java.util.Map<String, IIcon> sourceModelIcons = new java.util.HashMap<>();

    public BlockRuDoublePlant(String name, String[] variants, String[] lowerTextures, String[] upperTextures) {
        this(name, variants, lowerTextures, upperTextures, TintMode.AUTO);
    }

    public BlockRuDoublePlant(String name, String[] variants, String[] lowerTextures, String[] upperTextures,
        TintMode tintMode) {
        if (variants == null || lowerTextures == null
            || upperTextures == null
            || variants.length == 0
            || variants.length > 8
            || lowerTextures.length != variants.length
            || upperTextures.length != variants.length) {
            throw new IllegalArgumentException(
                "RU double-plant names and texture pairs must contain one to eight rows");
        }
        this.variants = variants.clone();
        this.lowerTextures = lowerTextures.clone();
        this.upperTextures = upperTextures.clone();
        this.tintMode = tintMode == null ? TintMode.AUTO : tintMode;
        setBlockName(Biomic.MODID + "." + name);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    protected boolean canPlaceBlockOn(Block ground) {
        return ground == Blocks.grass || ground == Blocks.dirt
            || ground == Blocks.sand
            || ground == BiomicBlocks.CHALK_GRASS
            || ground == ModBlocks.MUD.get()
            || ground == ModBlocks.MOSS_BLOCK.get()
            || ground.getMaterial()
                .isSolid();
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        String modern = "regions_unexplored:" + variants[normalize(metadata)];
        if (industries.angelic.biomic.world.modern.ModernGroundCoverSupport.isNetherPlant(modern)) {
            if (y < 1 || y >= 255) return false;
            if ((metadata & 8) != 0)
                return world.getBlock(x, y - 1, z) == this && (world.getBlockMetadata(x, y - 1, z) & 8) == 0;
            return Boolean.TRUE.equals(
                industries.angelic.biomic.world.modern.ModernGroundCoverSupport.testPinned(
                    modern,
                    industries.angelic.biomic.world.modern.ModernStateCompatibility
                        .modernState(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z))))
                && (world.getBlock(x, y, z) != this
                    || world.getBlock(x, y + 1, z) == this && (world.getBlockMetadata(x, y + 1, z) & 8) != 0);
        }
        if (!industries.angelic.biomic.world.modern.ModernCavePlantSupport.handles(modern))
            return super.canBlockStay(world, x, y, z);
        if (y < 1 || y >= 255) return false;
        if ((metadata & 8) != 0)
            return world.getBlock(x, y - 1, z) == this && (world.getBlockMetadata(x, y - 1, z) & 8) == 0;
        return world.getBlock(x, y - 1, z)
            .isOpaqueCube()
            && (world.getBlock(x, y, z) != this
                || world.getBlock(x, y + 1, z) == this && (world.getBlockMetadata(x, y + 1, z) & 8) != 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        lowerIcons = new IIcon[variants.length];
        upperIcons = new IIcon[variants.length];
        for (int index = 0; index < variants.length; index++) {
            if (isTassel(index)) {
                // Keep the grayscale foliage and precolored white flowers separate. Tinting their old
                // alpha composite turned the white blooms yellow/green with the surrounding grass.
                lowerIcons[index] = register.registerIcon(textureName("ru_decor/tassel_bottom"));
                upperIcons[index] = register.registerIcon(textureName("ru_decor/tassel_top"));
                tasselLowerFlowers = register.registerIcon(textureName("ru_decor/tassel_bottom_flowers"));
                tasselUpperFlowers = register.registerIcon(textureName("ru_decor/tassel_top_flowers"));
            } else {
                lowerIcons[index] = register.registerIcon(textureName(lowerTextures[index]));
                upperIcons[index] = register.registerIcon(textureName(upperTextures[index]));
            }
        }
        blockIcon = lowerIcons[0];
        if (sourceModelName(0) != null) {
            for (var texture : industries.angelic.biomic.content.RuCaveModelData.textures()) {
                String key = texture.getAsString();
                if (key.startsWith("biomic:")) sourceModelIcons.put(key, register.registerIcon(key));
            }
            upperIcons[0] = register
                .registerIcon(industries.angelic.biomic.content.RuCaveModelData.itemTexture(variants[0]));
        }
    }

    public String sourceModelName(int metadata) {
        String name = variants[normalize(metadata)];
        return industries.angelic.biomic.content.RuCaveModelData.legacyDoublePlant(name) ? name : null;
    }

    @SideOnly(Side.CLIENT)
    public IIcon sourceModelIcon(String texture) {
        IIcon icon = sourceModelIcons.get(texture);
        if (icon == null) throw new IllegalArgumentException("Missing RU double-plant model texture " + texture);
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int variant = normalize(metadata);
        return func_149887_c(metadata) ? upperIcons[variant] : lowerIcons[variant];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon func_149888_a(boolean upper, int variant) {
        int normalized = normalize(variant);
        return upper ? upperIcons[normalized] : lowerIcons[normalized];
    }

    @SideOnly(Side.CLIENT)
    public IIcon getLowerIcon(int metadata) {
        return lowerIcons[normalize(metadata)];
    }

    @SideOnly(Side.CLIENT)
    public IIcon getUpperIcon(int metadata) {
        return upperIcons[normalize(metadata)];
    }

    public boolean isUpperHalf(int metadata) {
        return func_149887_c(metadata);
    }

    public boolean isElephantEar(int metadata) {
        return "elephant_ear".equals(variants[normalize(metadata)]);
    }

    public boolean isTassel(int metadata) {
        return "tassel".equals(variants[normalize(metadata)]);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getFlowerOverlay(int metadata) {
        return isTassel(metadata) ? (isUpperHalf(metadata) ? tasselUpperFlowers : tasselLowerFlowers) : null;
    }

    /**
     * Vanilla render type 40 unconditionally adds sunflower face geometry for variant zero and reaches into
     * {@link BlockDoublePlant#sunflowerIcons}. RU shrubs are plain two-high crossed models, so they need their own
     * renderer instead of inheriting that sunflower-only contract.
     */
    @Override
    public int getRenderType() {
        return BiomicRenderIds.RU_DOUBLE_PLANT;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        if (tintMode == TintMode.BIOME_GRASS) return net.minecraft.world.ColorizerGrass.getGrassColor(.5, 1);
        if (tintMode == TintMode.BIOME_FOLIAGE) return net.minecraft.world.ColorizerFoliage.getFoliageColor(.5, 1);
        if (tintMode == TintMode.CONSTANT) return 0xFFFFFF;
        if (isElephantEar(metadata)) return net.minecraft.world.ColorizerFoliage.getFoliageColor(.5, 1);
        if (isGrassTinted(metadata)) return net.minecraft.world.ColorizerGrass.getGrassColor(.5, 1);
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (tintMode == TintMode.BIOME_GRASS) {
            return industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
                .getBiomeGrassColor(x, y, z);
        }
        if (tintMode == TintMode.BIOME_FOLIAGE) {
            return industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
                .getBiomeFoliageColor(x, y, z);
        }
        if (tintMode == TintMode.CONSTANT) return 0xFFFFFF;
        if (isElephantEar(metadata)) return industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
            .getBiomeFoliageColor(x, y, z);
        if (isGrassTinted(metadata)) return industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
            .getBiomeGrassColor(x, y, z);
        return 0xFFFFFF;
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
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List variantsOut) {
        for (int metadata = 0; metadata < variants.length; metadata++) {
            variantsOut.add(new ItemStack(item, 1, metadata));
        }
    }

    public String getVariantName(int metadata) {
        String variant = variants[normalize(metadata)];
        return tintMode == TintMode.AUTO && !isElephantEar(metadata) && !isTassel(metadata) ? variant + "_shrub"
            : variant;
    }

    private int normalize(int metadata) {
        int normalized = metadata & 7;
        return normalized < variants.length ? normalized : 0;
    }

    private boolean isGrassTinted(int metadata) {
        return isTassel(metadata);
    }

    private static String textureName(String texture) {
        return texture.indexOf(':') >= 0 ? texture : Biomic.MODID + ":" + texture;
    }
}
