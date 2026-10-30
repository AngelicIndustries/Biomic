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
import industries.angelic.biomic.content.BiomicBlocks;

/** Lightweight crossed-plane backport for RU tree accents and hanging vegetation. */
public class BlockRuPlant extends Block {

    public enum TintMode {
        CONSTANT,
        BIOME_GRASS,
        BIOME_FOLIAGE
    }

    private final String texture;
    private final String tipTexture;
    private final int color;
    private final TintMode tintMode;
    private final boolean caveGroundSupport;
    private final String modernName;
    private IIcon bodyIcon;
    private IIcon tipIcon;

    public BlockRuPlant(String name, String texture) {
        this(name, texture, null, 0xFFFFFF, 0);
    }

    public BlockRuPlant(String name, String texture, int lightValue) {
        this(name, texture, null, 0xFFFFFF, lightValue);
    }

    public BlockRuPlant(String name, String texture, String tipTexture, int color) {
        this(name, texture, tipTexture, color, 0);
    }

    public BlockRuPlant(String name, String texture, String tipTexture, int color, int lightValue) {
        this(name, texture, tipTexture, TintMode.CONSTANT, color, lightValue);
    }

    public BlockRuPlant(String name, String texture, String tipTexture, TintMode tintMode, int color, int lightValue) {
        super(Material.plants);
        if (lightValue < 0 || lightValue > 15) {
            throw new IllegalArgumentException("Plant light value must be between zero and fifteen");
        }
        this.texture = texture;
        this.tipTexture = tipTexture;
        this.color = color;
        this.tintMode = tintMode == null ? TintMode.CONSTANT : tintMode;
        modernName = "regions_unexplored:" + (name.startsWith("imported_") ? name.substring(9) : name);
        caveGroundSupport = industries.angelic.biomic.world.modern.ModernCavePlantSupport.handles(modernName)
            || modernName.endsWith("_bioshroom");
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setLightLevel(lightValue / 15.0F);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 1.0F, 0.9F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        bodyIcon = register.registerIcon(textureName(texture));
        tipIcon = tipTexture == null ? bodyIcon : register.registerIcon(textureName(tipTexture));
        blockIcon = bodyIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return tipTexture != null && (metadata & 1) != 0 ? tipIcon : bodyIcon;
    }

    @Override
    public int getRenderType() {
        if ("regions_unexplored:frozen_grass".equals(modernName))
            return industries.angelic.biomic.client.BiomicRenderIds.FROZEN_GRASS;
        return 1;
    }

    public boolean hasCaveGroundSupport() {
        return caveGroundSupport
            || industries.angelic.biomic.world.modern.ModernGroundCoverSupport.isNetherPlant(modernName);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (industries.angelic.biomic.world.modern.ModernGroundCoverSupport.isNetherPlant(modernName)) {
            if (y < 1) return false;
            return Boolean.TRUE.equals(
                industries.angelic.biomic.world.modern.ModernGroundCoverSupport.testPinned(
                    modernName,
                    industries.angelic.biomic.world.modern.ModernStateCompatibility
                        .modernState(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z))));
        }
        return !caveGroundSupport || y > 0 && world.getBlock(x, y - 1, z)
            .isOpaqueCube();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (hasCaveGroundSupport() && !canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
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
        return tintMode == TintMode.CONSTANT ? color : 0x48B518;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return getBlockColor();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        if (tintMode == TintMode.BIOME_GRASS) {
            return industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
                .getBiomeGrassColor(x, y, z);
        }
        if (tintMode == TintMode.BIOME_FOLIAGE) {
            return BlockRuLeaves.foliageColor(world, x, y, z);
        }
        return color;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    private static String textureName(String texture) {
        return texture.indexOf(':') >= 0 ? texture : Biomic.MODID + ":" + texture;
    }
}
