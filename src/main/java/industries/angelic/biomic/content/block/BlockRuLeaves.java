package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.BlockLeaves;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Cutout leaves with stable RU-authored color variants and vanilla decay behavior. */
public class BlockRuLeaves extends BlockLeaves {

    public enum TintMode {
        CONSTANT,
        BIOME_FOLIAGE,
        ASPEN
    }

    private final String species;
    private final String[] textures;
    private final TintMode tintMode;
    private final int[] colors;
    private IIcon coneIcon;

    public BlockRuLeaves(String species, int... colors) {
        this(species, new String[] { species + "_leaves" }, TintMode.CONSTANT, colors);
    }

    public BlockRuLeaves(String species, String texture, TintMode tintMode, int... colors) {
        this(species, new String[] { texture }, tintMode, colors);
    }

    public BlockRuLeaves(String species, String[] textures, TintMode tintMode, int... colors) {
        if (textures == null || textures.length == 0 || textures.length > 4) {
            throw new IllegalArgumentException("RU leaves need between one and four textures");
        }
        this.species = species;
        this.textures = textures.clone();
        this.tintMode = tintMode;
        this.colors = colors == null || colors.length == 0 ? new int[] { 0xFFFFFF } : colors.clone();
        setBlockName(Biomic.MODID + "." + species + "_leaves");
        setHardness(0.2F);
        setLightOpacity(1);
        setStepSound(soundTypeGrass);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        IIcon[] icons = new IIcon[textures.length];
        for (int index = 0; index < textures.length; index++) {
            icons[index] = register.registerIcon(textureName(textures[index]));
        }
        field_150129_M[0] = icons;
        field_150129_M[1] = icons.clone();
        if (textures[0].equals("ru/larch_leaves") || textures[0].equals("ru/golden_larch_leaves")) {
            coneIcon = register.registerIcon(textureName(textures[0] + "_cone"));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        // RU's weighted block model has six plain entries and one cone entry, independent of state.
        return coneIcon != null && hasCones(x, y, z) ? coneIcon : getIcon(side, world.getBlockMetadata(x, y, z));
    }

    public static boolean hasCones(int x, int y, int z) {
        long value = (long) (x * 3129871) ^ (long) z * 116129781L ^ y;
        value = value * value * 42317861L + value * 11L;
        return Math.abs((int) new java.util.Random(value >> 16).nextLong()) % 7 == 6;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        IIcon[] icons = field_150129_M[Blocks.leaves != null && Blocks.leaves.isOpaqueCube() ? 1 : 0];
        return icons[Math.min(metadata & 3, icons.length - 1)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isOpaqueCube() {
        return Blocks.leaves != null && Blocks.leaves.isOpaqueCube();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return Blocks.leaves == null || Blocks.leaves.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return tintMode == TintMode.CONSTANT ? colors[0] : 0x48B518;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        if (tintMode != TintMode.CONSTANT) return 0x48B518;
        return colors[Math.floorMod(metadata & 3, colors.length)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        if (tintMode == TintMode.BIOME_FOLIAGE) {
            return foliageColor(world, x, y, z);
        }
        if (tintMode == TintMode.ASPEN) {
            return industries.angelic.biomic.content.RuVegetationColors.aspen(x, z, false);
        }
        return getRenderColor(world.getBlockMetadata(x, y, z));
    }

    /** The source averages foliage; match vanilla vines without discarding the hosted height. */
    public static int foliageColor(IBlockAccess world, int x, int y, int z) {
        int red = 0, green = 0, blue = 0;
        for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
            int color = industries.angelic.biomic.client.HostedBiomeColors.biome(world, x + dx, y, z + dz)
                .getBiomeFoliageColor(x + dx, y, z + dz);
            red += (color >> 16) & 255;
            green += (color >> 8) & 255;
            blue += color & 255;
        }
        return (red / 9) << 16 | (green / 9) << 8 | blue / 9;
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
    public String[] func_150125_e() {
        return new String[] { species };
    }

    private static String textureName(String texture) {
        return texture.indexOf(':') >= 0 ? texture : Biomic.MODID + ":" + texture;
    }
}
