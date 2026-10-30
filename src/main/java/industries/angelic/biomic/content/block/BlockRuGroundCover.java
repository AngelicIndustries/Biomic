package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Thin non-solid rendering for RU leaf litter captured beneath trees. */
public final class BlockRuGroundCover extends Block {

    private final String species;

    public BlockRuGroundCover(String name, String texture) {
        super(Material.plants);
        species = name;
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + texture);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F / 64.0F, 1.0F);
    }

    @Override
    public int getRenderType() {
        return industries.angelic.biomic.client.BiomicRenderIds.RU_LEAF_LITTER;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override
    public int getBlockColor() {
        return species.equals("imported_maple_leaf_litter") ? 0x48B518 : 0xFFFFFF;
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override
    public int getRenderColor(int metadata) {
        return getBlockColor();
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    @Override
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        if (species.equals("imported_maple_leaf_litter")) {
            return industries.angelic.biomic.client.HostedBiomeColors.biome(world, x, y, z)
                .getBiomeFoliageColor(x, y, z);
        }
        if (species.contains("silver_birch") || species.contains("enchanted_birch")) {
            return industries.angelic.biomic.content.RuVegetationColors.aspen(x, z, species.contains("enchanted"));
        }
        return 0xFFFFFF; // Red/orange litter is already colored in the source texture.
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
        return World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
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
