package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Visual worldgen implementation of vanilla's sculk sensor and can-summon shrieker. */
public final class BlockSculkFlora extends Block {

    private final Kind kind;
    private IIcon bottomIcon;
    private IIcon sideIcon;
    private IIcon topIcon;
    private IIcon detailIcon;

    public BlockSculkFlora(String name, Kind kind) {
        super(Material.rock);
        this.kind = kind;
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName("minecraft:" + name + "_bottom");
        setHardness(1.5F);
        setResistance(1.5F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, kind == Kind.SENSOR ? 0.5F : 15.0F / 16.0F, 1.0F);
    }

    public Kind getKind() {
        return kind;
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
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return canBlockStay(world, x, y, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        Block floor = world.getBlock(x, y - 1, z);
        return floor.isSideSolid(world, x, y - 1, z, ForgeDirection.UP);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        String prefix = kind == Kind.SENSOR ? "sculk_sensor" : "sculk_shrieker";
        bottomIcon = register.registerIcon("minecraft:" + prefix + "_bottom");
        sideIcon = register.registerIcon("minecraft:" + prefix + "_side");
        topIcon = register.registerIcon("minecraft:" + prefix + "_top");
        detailIcon = register.registerIcon(
            kind == Kind.SENSOR ? "minecraft:sculk_sensor_tendril_inactive"
                : "minecraft:sculk_shrieker_can_summon_inner_top");
        blockIcon = bottomIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        if (side == 0) return bottomIcon;
        if (side == 1) return topIcon;
        return sideIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBottomIcon() {
        return bottomIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getSideIcon() {
        return sideIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getTopIcon() {
        return topIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getDetailIcon() {
        return detailIcon;
    }

    public enum Kind {
        SENSOR,
        SHRIEKER
    }
}
