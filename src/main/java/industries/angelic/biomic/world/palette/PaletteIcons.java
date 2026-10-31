package industries.angelic.biomic.world.palette;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A face of the block the palette places for an id, for blocks that show it (a deepslate-bottomed block). */
@SideOnly(Side.CLIENT)
public final class PaletteIcons {

    private PaletteIcons() {}

    /** The palette block's icon for a side, or stone's when the id places nothing (air). */
    public static IIcon icon(String id, int side) {
        BlockPalette.Choice choice = BlockPalette.choice(id);
        Block block = choice == null || choice.block() == Blocks.air ? Blocks.stone : choice.block();
        return block.getIcon(side, choice == null ? 0 : choice.metadata(0));
    }
}
