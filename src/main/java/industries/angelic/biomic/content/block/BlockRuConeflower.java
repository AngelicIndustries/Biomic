package industries.angelic.biomic.content.block;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/** RU's low multipart flowerbed, not a crossed-sprite flower. Metadata stores amount and facing. */
public final class BlockRuConeflower extends BlockRuPlant {

    private final String stemTexture;
    private IIcon stemIcon;

    public BlockRuConeflower(String name, String petals, String stem) {
        super(name, petals, null, TintMode.BIOME_GRASS, 0xFFFFFF, 0);
        stemTexture = stem;
        setBlockBounds(0, 0, 0, 1, 3.0F / 16.0F, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        super.registerBlockIcons(register);
        stemIcon = register.registerIcon(Biomic.MODID + ":" + stemTexture);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getStemIcon() {
        return stemIcon;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.RU_CONEFLOWER;
    }
}
