package industries.angelic.biomic.content.block;

import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Single-species orientable log used by the first RU biome ports. */
public class BlockRuLog extends BlockLog {

    private final String[] sideTextures;
    private final String[] topTextures;
    private final boolean verticalVariantsOnly;
    private IIcon[] sideIcons;
    private IIcon[] topIcons;

    public BlockRuLog(String species) {
        this(species, new String[] { species + "_log" }, new String[] { species + "_log_top" });
    }

    public BlockRuLog(String species, String sideTexture, String topTexture) {
        this(species, new String[] { sideTexture }, new String[] { topTexture });
    }

    public BlockRuLog(String species, String[] sideTextures, String[] topTextures) {
        this(species, sideTextures, topTextures, false);
    }

    public BlockRuLog(String species, String[] sideTextures, String[] topTextures, boolean verticalVariantsOnly) {
        if (sideTextures == null || topTextures == null
            || sideTextures.length == 0
            || sideTextures.length != topTextures.length
            || sideTextures.length > 4) {
            throw new IllegalArgumentException("RU logs need one to four matching side/end texture pairs");
        }
        this.sideTextures = sideTextures.clone();
        this.topTextures = topTextures.clone();
        this.verticalVariantsOnly = verticalVariantsOnly;
        setBlockName(Biomic.MODID + "." + species + "_log");
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcons = new IIcon[sideTextures.length];
        topIcons = new IIcon[topTextures.length];
        for (int index = 0; index < sideTextures.length; index++) {
            sideIcons[index] = register.registerIcon(textureName(sideTextures[index]));
            topIcons[index] = register.registerIcon(textureName(topTextures[index]));
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int variant = textureVariant(metadata, sideIcons.length, verticalVariantsOnly);
        int orientation = metadata & 12;
        boolean end = orientation == 0 && (side == 0 || side == 1) || orientation == 4 && (side == 4 || side == 5)
            || orientation == 8 && (side == 2 || side == 3);
        return end ? topIcons[variant] : sideIcons[variant];
    }

    static int textureVariant(int metadata, int variantCount, boolean verticalVariantsOnly) {
        // RU's Silver Birch model ignores is_base for axis=x/z. Preserve stored metadata, but
        // use ordinary bark for fallen logs (including chunks written before this correction).
        if (verticalVariantsOnly && (metadata & 12) != 0) return 0;
        return Math.min(metadata & 3, variantCount - 1);
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    private static String textureName(String texture) {
        return texture.indexOf(':') >= 0 ? texture : Biomic.MODID + ":" + texture;
    }
}
