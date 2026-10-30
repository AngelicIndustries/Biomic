package industries.angelic.biomic.unused.tartarus;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ducks.ISoulFireInfo;

/**
 * Explicit source-authored soul fire, including Tartarus' crystalline solid shelves.
 * Ordinary vanilla/EFR contextual fire is untouched. Vanilla renderType3 supplies its original
 * fire geometry; icons are borrowed from installed EFR, not copied/recolored assets. All inherited
 * spreading/portal-ignition entrypoints are replaced by solid-top support validation only.
 */
public final class BlockAuthoredSoulFire extends BlockFire implements ISoulFireInfo {

    public BlockAuthoredSoulFire() {
        super();
        setBlockName("biomic.authored_soul_fire");
        setLightLevel(10F / 15F);
        setLightOpacity(0);
        setHardness(0);
        setCreativeTab(null);
        setTickRandomly(true);
    }

    @Override
    public boolean isSoulFire(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public int getLightValue() {
        return 10;
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return 10;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return null;
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
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return y > 0 && world.checkChunksExist(x, y - 1, z, x, y - 1, z)
            && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return canPlaceBlockAt(world, x, y, z);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        checkSupport(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block changed) {
        checkSupport(world, x, y, z);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        checkSupport(world, x, y, z);
    }

    private void checkSupport(World world, int x, int y, int z) {
        if (world.isRemote) return;
        checkSupport(new SupportAccess() {

            public boolean available() {
                return y <= 0 || world.checkChunksExist(x, y - 1, z, x, y - 1, z);
            }

            public boolean solidTop() {
                return y > 0 && World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
            }

            public void remove() {
                world.setBlockToAir(x, y, z);
            }
        });
    }

    /** Unknown support is deferred, never interpreted as AIR or demanded through a chunk load. */
    interface SupportAccess {

        boolean available();

        boolean solidTop();

        void remove();
    }

    void checkSupport(SupportAccess access) {
        if (access.available() && !access.solidTop()) access.remove();
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        // Entity's vanilla bounding-box fire check recognizes Blocks.fire, not an explicit custom block.
        if (!world.isRemote && !entity.isImmuneToFire()) {
            entity.attackEntityFrom(DamageSource.inFire, 2F);
            entity.setFire(8);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        // EFR registers the two existing animated atlas sprites on vanilla fire.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getSoulFireIcon(int frame) {
        if (!(Blocks.fire instanceof ISoulFireInfo fire))
            throw new IllegalStateException("Authored soul fire requires installed EFR icons");
        IIcon icon = fire.getSoulFireIcon(Math.floorMod(frame, 2));
        if (icon == null) throw new IllegalStateException("EFR soul-fire atlas has not been registered");
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getFireIcon(int frame) {
        return getSoulFireIcon(frame);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return getSoulFireIcon(0);
    }
}
