package industries.angelic.biomic.content.gardens;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Metadata bit0 records whether this is the actively growing top. */
public final class BlockWhistlecane extends Block implements IGrowable {

    public static final int GROWING = 1, MAX_HEIGHT = 6;
    private IIcon side, top, leaves0, leaves1, item;
    private IIcon[] smoke;

    BlockWhistlecane() {
        super(Material.wood);
        setBlockName("biomic.gardens_whistlecane");
        setHardness(1);
        setResistance(1);
        setLightOpacity(0);
        setStepSound(soundTypeWood);
        setTickRandomly(true);
        setBlockBounds(5 / 16F, 0, 5 / 16F, 11 / 16F, 1, 11 / 16F);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (y < 1) return false;
        Block below = world.getBlock(x, y - 1, z);
        int metadata = world.getBlockMetadata(x, y - 1, z);
        return below == this || below == net.minecraft.init.Blocks.soul_sand
            || GardensSupport.soulSoil(below, metadata)
            || GardensSupport.nylium(below, metadata);
    }

    private int below(World world, int x, int y, int z) {
        int n = 0;
        while (n < MAX_HEIGHT && world.getBlock(x, y - n - 1, z) == this) n++;
        return n;
    }

    private int above(World world, int x, int y, int z) {
        int n = 0;
        while (n < MAX_HEIGHT && world.getBlock(x, y + n + 1, z) == this) n++;
        return n;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        return world.getBlock(x, y + 1, z) == this ? 0 : GROWING;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
            return;
        }
        boolean growing = world.getBlock(x, y + 1, z) != this;
        world.setBlockMetadataWithNotify(x, y, z, growing ? GROWING : 0, 2);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote) return;
        int metadata = world.getBlockMetadata(x, y, z);
        if ((metadata & GROWING) != 0 && world.isAirBlock(x, y + 1, z)
            && below(world, x, y, z) + 1 < MAX_HEIGHT
            && random.nextFloat() < .1F) grow(world, random, x, y, z, below(world, x, y, z) + 1);
        else if ((metadata & GROWING) == 0 && world.isAirBlock(x, y + 1, z) && random.nextInt(40) == 0) {
            world.playSoundEffect(
                x + .5,
                y + 1,
                z + .5,
                "biomic:gardens_whistlecane",
                1,
                random.nextFloat() * .3F + .85F);
            world.addBlockEvent(x, y, z, this, 0, 0);
        }
    }

    private void grow(World world, Random random, int x, int y, int z, int height) {
        boolean continues = height == 0 || random.nextFloat() >= 1F / (MAX_HEIGHT - height);
        world.setBlockMetadataWithNotify(x, y, z, 0, 2);
        world.setBlock(x, y + 1, z, this, continues ? GROWING : 0, 3);
    }

    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
        int top = above(world, x, y, z);
        return top + below(world, x, y, z) + 1 < MAX_HEIGHT && (world.getBlockMetadata(x, y + top, z) & GROWING) != 0;
    }

    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return true;
    }

    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        int top = above(world, x, y, z);
        for (int i = 0; i < 1 + random.nextInt(2); i++) {
            int ty = y + top;
            if (above(world, x, ty, z) + below(world, x, ty, z) + 1 >= MAX_HEIGHT || !world.isAirBlock(x, ty + 1, z))
                return;
            grow(world, random, x, ty, z, below(world, x, ty, z) + 1);
            top++;
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
    public boolean onBlockEventReceived(World world, int x, int y, int z, int event, int argument) {
        if (event != 0) return super.onBlockEventReceived(world, x, y, z, event, argument);
        if (world.isRemote) Biomic.proxy.gardensWhistle(x, y, z);
        return true;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.GARDENS_WHISTLECANE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        side = register.registerIcon("biomic:gardens/whistlecane1");
        top = register.registerIcon("biomic:gardens/whistlecane0");
        leaves0 = register.registerIcon("biomic:gardens/whistlecane_leaves0");
        leaves1 = register.registerIcon("biomic:gardens/whistlecane_leaves1");
        item = register.registerIcon("biomic:gardens/whistlecane_item");
        smoke = new IIcon[12];
        for (int i = 0; i < 12; i++) smoke[i] = register.registerIcon("biomic:gardens/whistle_smoke_" + i);
        blockIcon = item;
    }

    /**
     * Icon-only item consumers do not invoke the multipart renderer, so they must also receive the source item sprite.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int metadata) {
        return item;
    }

    @SideOnly(Side.CLIENT)
    public IIcon gardensModelIcon(int texture) {
        return texture == 0 ? top : texture == 1 ? side : texture == 2 ? leaves0 : texture == 3 ? leaves1 : item;
    }

    @SideOnly(Side.CLIENT)
    public IIcon whistleSmokeIcon(int frame) {
        return smoke[Math.max(0, Math.min(frame, 11))];
    }
}
