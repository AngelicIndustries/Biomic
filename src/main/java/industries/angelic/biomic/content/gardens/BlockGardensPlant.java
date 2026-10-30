package industries.angelic.biomic.content.gardens;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

class BlockGardensPlant extends Block {

    enum Support {
        SOUL,
        NYLIUM
    }

    private final String texture;
    private final Support support;
    private IIcon icon;

    BlockGardensPlant(String name, String texture, Support support, int light) {
        super(Material.plants);
        this.texture = texture;
        this.support = support;
        setBlockName("biomic." + name);
        setHardness(0);
        setLightOpacity(0);
        setStepSound(soundTypeGrass);
        if (light > 0) setLightLevel(light / 15F);
        setBlockBounds(.2F, 0, .2F, .8F, .875F, .8F);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (y < 1) return false;
        Block below = world.getBlock(x, y - 1, z);
        int metadata = world.getBlockMetadata(x, y - 1, z);
        return support == Support.SOUL ? GardensSupport.soul(below, metadata) : GardensSupport.nylium(below, metadata);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
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
    public int getRenderType() {
        return 1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icon = register.registerIcon("biomic:" + texture);
        blockIcon = icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return icon;
    }
}
