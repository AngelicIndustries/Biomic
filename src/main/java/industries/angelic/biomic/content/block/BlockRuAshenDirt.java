package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** RU's smouldering property is a real surface state, not an additional ore pass. */
public final class BlockRuAshenDirt extends BlockRuSolid {

    private IIcon smoulderingIcon;

    public BlockRuAshenDirt() {
        super("imported_ashen_dirt", "ru_decor/ashen_dirt", "ru_decor/ashen_dirt", Material.ground, 0, true);
        setTickRandomly(true);
    }

    public static boolean isSmouldering(int metadata) {
        return (metadata & 1) != 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        smoulderingIcon = register.registerIcon("biomic:ru_decor/ashen_dirt_smouldering");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return isSmouldering(metadata) ? smoulderingIcon : super.getIcon(side, metadata);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!world.isRemote && !isSmouldering(world.getBlockMetadata(x, y, z))
            && world.getBlock(x, y + 1, z) == Blocks.fire) world.setBlockMetadataWithNotify(x, y, z, 1, 3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (isSmouldering(world.getBlockMetadata(x, y, z)) && random.nextInt(20) == 0) {
            world.spawnParticle("flame", x + random.nextDouble(), y + 1.1, z + random.nextDouble(), 0, 0, 0);
            world.spawnParticle("smoke", x + random.nextDouble(), y + 1.1, z + random.nextDouble(), 0, 0, 0);
        }
    }
}
