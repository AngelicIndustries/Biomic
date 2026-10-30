package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Thin RU-authored surface vegetation, including floating duckweed mats. */
public final class BlockRuFlatPlant extends Block {

    private final boolean biomeTinted;

    public BlockRuFlatPlant(String name, String texture, boolean biomeTinted, int lightValue) {
        super(Material.plants);
        if (lightValue < 0 || lightValue > 15) throw new IllegalArgumentException("Invalid RU flat-plant light value");
        this.biomeTinted = biomeTinted;
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + texture);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setLightLevel(lightValue / 15.0F);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F / 32.0F, 1.0F);
    }

    @Override
    public int getRenderType() {
        return 23;
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
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        Material below = world.getBlock(x, y - 1, z)
            .getMaterial();
        return below == Material.water || World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return biomeTinted ? 0x48B518 : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return getBlockColor();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return biomeTinted ? industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
            .getBiomeFoliageColor(x, y, z) : 0xFFFFFF;
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
