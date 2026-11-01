package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's button (arrows press it, as vanilla's wooden one), faced with its planks. */
public final class BlockWoodButton extends BlockButton {

    private final Block planks;

    public BlockWoodButton(String name, Block planks) {
        super(true);
        this.planks = planks;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.5F);
        setStepSound(soundTypeWood);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return planks.getIcon(1, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}
}
