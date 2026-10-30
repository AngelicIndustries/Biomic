package industries.angelic.biomic.unused.alexscaves;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Full source pillar with axis-aware cap texture. Metadata 0/1/2 is X/Y/Z. */
public final class BlockAbyssmarinePillar extends Block {

    private IIcon side, cap;

    BlockAbyssmarinePillar() {
        super(Material.rock);
        setBlockName("biomic.abyssmarine_pillar");
        setHardness(1.5F);
        setResistance(6F);
        setStepSound(soundTypeStone);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        side = register.registerIcon("biomic:alexscaves/abyssmarine_pillar");
        cap = register.registerIcon("biomic:alexscaves/abyssmarine_pillar_top");
        blockIcon = side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int sideIndex, int metadata) {
        int axis = metadata % 3;
        return axis == 0 && (sideIndex == 4 || sideIndex == 5) || axis == 1 && (sideIndex == 0 || sideIndex == 1)
            || axis == 2 && (sideIndex == 2 || sideIndex == 3) ? cap : side;
    }
}
