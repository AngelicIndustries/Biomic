package industries.angelic.biomic.content.gardens;

import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

final class BlockGardensStem extends BlockLog {

    private IIcon side, end;

    BlockGardensStem() {
        setBlockName("biomic.gardens_soulblight_stem");
        setHardness(2);
        setStepSound(soundTypeWood);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        side = register.registerIcon("biomic:gardens/soulblight_stem");
        end = register.registerIcon("biomic:gardens/soulblight_stem_top");
        blockIcon = side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int metadata) {
        int axis = metadata & 12;
        boolean cap = axis == 0 && (face < 2) || axis == 4 && (face == 4 || face == 5)
            || axis == 8 && (face == 2 || face == 3);
        return cap ? end : side;
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }
}
