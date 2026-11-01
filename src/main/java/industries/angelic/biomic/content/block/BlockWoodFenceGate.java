package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's fence gate, faced with its planks. */
public final class BlockWoodFenceGate extends BlockFenceGate {

    private final Block planks;

    public BlockWoodFenceGate(String name, Block planks) {
        this.planks = planks;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return planks.getIcon(side, 0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {}
}
