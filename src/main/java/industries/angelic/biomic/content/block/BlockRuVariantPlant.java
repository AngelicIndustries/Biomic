package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Metadata-backed crossed plants used by the pinned RU patch definitions. */
public final class BlockRuVariantPlant extends BlockBush {

    private final String[] variants;
    private final boolean biomeTint;
    private IIcon[] icons;

    public BlockRuVariantPlant(String name, boolean biomeTint, String... variants) {
        super(Material.plants);
        if (variants == null || variants.length == 0 || variants.length > 16) {
            throw new IllegalArgumentException("RU plant blocks require between one and sixteen variants");
        }
        this.variants = variants.clone();
        this.biomeTint = biomeTint;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    protected boolean canPlaceBlockOn(Block ground) {
        return ground == Blocks.grass || ground == Blocks.dirt
            || ground == Blocks.sand
            || ground == ModBlocks.MUD.get()
            || ground == ModBlocks.MOSS_BLOCK.get();
    }

    @Override
    public boolean canBlockStay(net.minecraft.world.World world, int x, int y, int z) {
        if (y <= 0 || y >= 256) return false;
        String plant = industries.angelic.biomic.world.modern.ModernStateCompatibility
            .modernState(this, world.getBlockMetadata(x, y, z));
        String below = industries.angelic.biomic.world.modern.ModernStateCompatibility
            .modernState(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z));
        Boolean supported = industries.angelic.biomic.world.modern.ModernGroundCoverSupport.testPinned(plant, below);
        // Grass sprouts explicitly allow tagged rock/terracotta, and these RU plants do not
        // inherit 1.7 BlockBush's light >= 8 requirement. Rendering/placement parity alone
        // cannot catch them disappearing on the first random tick beneath a dense canopy.
        return supported != null ? supported : super.canBlockStay(world, x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[variants.length];
        for (int index = 0; index < variants.length; index++) {
            icons[index] = register.registerIcon(Biomic.MODID + ":" + variants[index]);
        }
        blockIcon = icons[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return icons[normalize(metadata)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return biomeTint ? 0x7FB238 : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return isGrassTinted(metadata) ? net.minecraft.world.ColorizerGrass.getGrassColor(.5, 1) : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return isGrassTinted(world.getBlockMetadata(x, y, z))
            ? industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
                .getBiomeGrassColor(x, y, z)
            : 0xFFFFFF;
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
    public int damageDropped(int metadata) {
        return normalize(metadata);
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
        return variants[normalize(metadata)];
    }

    private boolean isGrassTinted(int metadata) {
        // RU registers a tint provider for grass sprouts, NOT for the already colored desert sprites.
        return biomeTint && "grass_sprouts".equals(getVariantName(metadata));
    }

    private int normalize(int metadata) {
        int normalized = metadata & 15;
        return normalized < variants.length ? normalized : 0;
    }
}
