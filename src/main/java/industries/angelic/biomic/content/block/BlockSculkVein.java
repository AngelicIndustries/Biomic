package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Six-face sculk-vein state split across legacy metadata and four internal vertical variants. */
public class BlockSculkVein extends Block {

    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;

    private final boolean up;
    private final boolean down;
    private IIcon veinIcon;

    public BlockSculkVein(String name, boolean up, boolean down) {
        this(name, up, down, Material.vine);
    }

    protected BlockSculkVein(String name, boolean up, boolean down, Material material) {
        super(material);
        this.up = up;
        this.down = down;
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName("minecraft:sculk_vein");
        setHardness(0.2F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    public boolean hasUp() {
        return up;
    }

    public boolean hasDown() {
        return down;
    }

    /** ForgeDirection-order six-face mask; the existing four metadata bits remain unchanged. */
    public final int faceMask(int metadata) {
        return (down ? 1 : 0) | (up ? 2 : 0)
            | ((metadata & NORTH) != 0 ? 4 : 0)
            | ((metadata & SOUTH) != 0 ? 8 : 0)
            | ((metadata & WEST) != 0 ? 16 : 0)
            | ((metadata & EAST) != 0 ? 32 : 0);
    }

    public static int horizontalMetadata(int faces) {
        if (faces < 0 || faces > 63) throw new IllegalArgumentException("Invalid sculk faces");
        return ((faces & 4) != 0 ? NORTH : 0) | ((faces & 32) != 0 ? EAST : 0)
            | ((faces & 8) != 0 ? SOUTH : 0)
            | ((faces & 16) != 0 ? WEST : 0);
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
        return BiomicRenderIds.SCULK;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {}

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        veinIcon = register.registerIcon("minecraft:sculk_vein");
        blockIcon = veinIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return veinIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getVeinIcon() {
        return veinIcon;
    }
}
