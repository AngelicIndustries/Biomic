package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/** The five living and five dead vanilla coral-block variants absent from EFR. */
public final class BlockCoral extends Block implements AquaticVariantBlock {

    public static final int DEAD_OFFSET = 5;

    private static final String[] TEXTURES = { "brain_coral_block", "bubble_coral_block", "fire_coral_block",
        "horn_coral_block", "tube_coral_block", "dead_brain_coral_block", "dead_bubble_coral_block",
        "dead_fire_coral_block", "dead_horn_coral_block", "dead_tube_coral_block" };

    private IIcon[] icons;

    public BlockCoral() {
        super(Material.rock);
        setBlockName(Biomic.MODID + ".coral_block");
        setBlockTextureName(Biomic.MODID + ":brain_coral_block");
        setHardness(1.5F);
        setResistance(6.0F);
        setStepSound(soundTypeStone);
        setTickRandomly(true);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        int metadata = world.getBlockMetadata(x, y, z);
        if (metadata < DEAD_OFFSET && !touchesWater(world, x, y, z)) {
            world.setBlockMetadataWithNotify(x, y, z, metadata + DEAD_OFFSET, 2);
        }
    }

    @Override
    public int damageDropped(int metadata) {
        return normalize(metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[TEXTURES.length];
        for (int index = 0; index < TEXTURES.length; index++) {
            icons[index] = register.registerIcon(Biomic.MODID + ":" + TEXTURES[index]);
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
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List variants) {
        for (int metadata = 0; metadata < getVariantCount(); metadata++) {
            variants.add(new ItemStack(item, 1, metadata));
        }
    }

    @Override
    public int getVariantCount() {
        return TEXTURES.length;
    }

    @Override
    public String getVariantName(int metadata) {
        return TEXTURES[normalize(metadata)];
    }

    private static int normalize(int metadata) {
        int normalized = metadata & 15;
        return normalized < TEXTURES.length ? normalized : 0;
    }

    private static boolean touchesWater(World world, int x, int y, int z) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (world.getBlock(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
                .getMaterial() == Material.water) return true;
        }
        return false;
    }
}
