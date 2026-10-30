package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** RU's seven cactus shapes, including handed corner faces, in three metadata bits. */
public final class BlockSaguaro extends Block {

    public static final String[] SHAPES = { "up_down", "east_west", "north_south", "south_up", "west_up", "north_up",
        "east_up" };
    private IIcon side, top, corner, flippedCorner;

    public BlockSaguaro() {
        super(Material.cactus);
        setBlockName("biomic.imported_saguaro_cactus");
        setHardness(0.8F);
        setStepSound(soundTypeCloth);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    public static int metadata(String shape, int rotation) {
        int index = 0;
        for (int i = 0; i < SHAPES.length; i++) if (SHAPES[i].equals(shape)) index = i;
        // Captured feature transforms 4..7 mirror X before their quarter-turn rotation.
        if (index >= 3 && (rotation & 4) != 0) index = 3 + ((3 - index) & 3);
        if (index >= 3) return 3 + ((index - 3 + rotation) & 3);
        if (index > 0 && (rotation & 1) != 0) return 3 - index;
        return index;
    }

    public static String shape(int metadata) {
        return SHAPES[metadata >= 0 && metadata < 7 ? metadata : 0];
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.SAGUARO;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        side = register.registerIcon("biomic:ru/saguaro_cactus");
        top = register.registerIcon("biomic:ru/saguaro_cactus_top");
        corner = register.registerIcon("biomic:ru/saguaro_cactus_corner");
        flippedCorner = register.registerIcon("biomic:ru/saguaro_cactus_corner_flip");
        blockIcon = side;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int face, int metadata) {
        return face < 2 ? top : side;
    }

    @SideOnly(Side.CLIENT)
    public IIcon modelIcon(int face, boolean bent) {
        if (!bent) return face < 2 ? top : side;
        return switch (face) {
            case 1, 3 -> top;
            case 4 -> flippedCorner;
            case 5 -> corner;
            default -> side;
        };
    }
}
