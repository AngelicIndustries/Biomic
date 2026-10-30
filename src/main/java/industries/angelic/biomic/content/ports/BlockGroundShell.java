package industries.angelic.biomic.content.ports;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/**
 * A shell or bit of beach litter on the strand line, a small model on the ground; picked up whole. Metadata is the
 * kind.
 */
public final class BlockGroundShell extends Block {

    private final String[] textures;
    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    BlockGroundShell(String name, String... textures) {
        super(Material.circuits);
        this.textures = textures.clone();
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.0F);
        setStepSound(soundTypeStone);
        setLightOpacity(0);
        setBlockBounds(0.1F, 0.0F, 0.1F, 0.9F, 1.0F / 16.0F, 0.9F);
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return super.canPlaceBlockAt(world, x, y, z) && canBlockStay(world, x, y, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return World.doesBlockHaveSolidTopSurface(world, x, y - 1, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbour) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
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
        return BiomicRenderIds.GROUND_SHELL;
    }

    @Override
    public int damageDropped(int metadata) {
        return Math.min(metadata, textures.length - 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (int i = 0; i < textures.length; i++) list.add(new ItemStack(item, 1, i));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return icons[Math.min(metadata, icons.length - 1)];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[textures.length];
        for (int i = 0; i < textures.length; i++) icons[i] = register.registerIcon(Biomic.MODID + ":" + textures[i]);
        blockIcon = icons[0];
        industries.angelic.biomic.client.render.ShellModels.registerIcons(register, PortBlocks.SHELLS);
    }
}
