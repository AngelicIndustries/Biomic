package industries.angelic.biomic.content.alexscaves;

import java.util.Random;

import net.minecraft.block.BlockSnow;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Eight legacy metadata layers; deliberately omits source mob/falling-entity behavior. */
final class BlockForlornGuanoLayer extends BlockSnow {

    BlockForlornGuanoLayer() {
        super();
        setBlockName("biomic.alexs_forlorn_guano_layer");
        setHardness(.3F);
        setStepSound(soundTypeSand);
        setTickRandomly(false);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {}

    // BlockSnow already supplies the source-equivalent layer selection and ordinary-entity collision heights.
    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int quantityDropped(int metadata, int fortune, Random random) {
        return 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("biomic:alexscaves/guano");
    }
}
