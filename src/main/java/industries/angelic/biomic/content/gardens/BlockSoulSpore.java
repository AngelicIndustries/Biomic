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
import industries.angelic.biomic.client.BiomicRenderIds;

/** Source metadata: bit0 down, bit1 terminal/top, bit2 actively growing. */
public final class BlockSoulSpore extends Block {

    public static final int DOWN = 1, TOP = 2, GROWING = 4;
    private final boolean glowing;
    private IIcon body, tip;

    BlockSoulSpore(String name, boolean glowing) {
        super(Material.plants);
        this.glowing = glowing;
        setBlockName("biomic." + name);
        setHardness(0);
        setLightOpacity(0);
        setStepSound(soundTypeGrass);
        setTickRandomly(!glowing);
        if (glowing) setLightLevel(7 / 15F);
        setBlockBounds(.0625F, 0, .0625F, .9375F, 1, .9375F);
    }

    public static int metadata(boolean down, boolean top, boolean growing) {
        return (down ? DOWN : 0) | (top ? TOP : 0) | (growing ? GROWING : 0);
    }

    public static boolean down(int metadata) {
        return (metadata & DOWN) != 0;
    }

    /** Upstream flips every glowing spore, but only the terminal model of an ordinary spore column. */
    @SideOnly(Side.CLIENT)
    public boolean flipModel(int metadata) {
        return down(metadata) && (glowing || (metadata & TOP) != 0);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        boolean down = side == 0;
        return glowing ? (down ? DOWN : 0) : BlockSoulSpore.metadata(down, true, true);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        int dy = down(metadata) ? 1 : -1;
        if (y + dy < 0 || y + dy >= world.getHeight()) return false;
        Block support = world.getBlock(x, y + dy, z);
        int supportMeta = world.getBlockMetadata(x, y + dy, z);
        return GardensSupport.soul(support, supportMeta)
            || support == GardensContent.SOUL_SPORE && down(supportMeta) == down(metadata);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
            return;
        }
        if (glowing) return;
        int metadata = world.getBlockMetadata(x, y, z);
        int dy = down(metadata) ? -1 : 1;
        Block next = world.getBlock(x, y + dy, z);
        int nextMeta = world.getBlockMetadata(x, y + dy, z);
        boolean terminal = !((next == this || next == GardensContent.GLOWING_SOUL_SPORE)
            && down(nextMeta) == down(metadata));
        int updated = terminal ? metadata | TOP : metadata & ~TOP;
        if (updated != metadata) world.setBlockMetadataWithNotify(x, y, z, updated, 2);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (glowing || world.isRemote) return;
        int metadata = world.getBlockMetadata(x, y, z);
        if ((metadata & GROWING) == 0 || (metadata & TOP) == 0 || random.nextFloat() >= .1F) return;
        int direction = down(metadata) ? -1 : 1, ny = y + direction;
        if (ny < 1 || ny >= world.getHeight() - 1 || !world.isAirBlock(x, ny, z)) return;
        int length = 1, back = -direction;
        while (length < 9 && world.getBlock(x, y + back * length, z) == this) length++;
        Block support = world.getBlock(x, y + back * length, z);
        int supportMeta = world.getBlockMetadata(x, y + back * length, z);
        int maximum = support == net.minecraft.init.Blocks.soul_sand ? 8 : 3;
        if (!GardensSupport.soul(support, supportMeta) || length >= maximum
            || random.nextInt(maximum - length + 1) == 0) {
            if (length > 1 && random.nextInt(8) == 0)
                world.setBlock(x, y, z, GardensContent.GLOWING_SOUL_SPORE, down(metadata) ? DOWN : 0, 2);
            else world.setBlockMetadataWithNotify(x, y, z, metadata & ~GROWING, 2);
            return;
        }
        world.setBlockMetadataWithNotify(x, y, z, metadata & ~TOP, 2);
        world.setBlock(x, ny, z, this, BlockSoulSpore.metadata(down(metadata), true, true), 2);
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
        return BiomicRenderIds.GARDENS_SOUL_SPORE;
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
        body = register.registerIcon("biomic:" + (glowing ? "gardens/glowing_soul_spore" : "gardens/soul_spore"));
        tip = glowing ? body : register.registerIcon("biomic:gardens/soul_spore_top");
        blockIcon = body;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return !glowing && (metadata & TOP) != 0 ? tip : body;
    }
}
