package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Small configurable full-cube used for imported RU feature materials. */
public class BlockRuSolid extends Block {

    private final String sideTexture;
    private final String topTexture;
    private final String bottomTexture;
    private final boolean plantSoil;
    private IIcon sideIcon;
    private IIcon topIcon;
    private IIcon bottomIcon;

    public BlockRuSolid(String name, String sideTexture, String topTexture, Material material, int lightValue) {
        this(name, sideTexture, topTexture, material, lightValue, false);
    }

    public BlockRuSolid(String name, String sideTexture, String topTexture, Material material, int lightValue,
        boolean plantSoil) {
        this(name, sideTexture, topTexture, topTexture, material, lightValue, plantSoil);
    }

    public BlockRuSolid(String name, String sideTexture, String topTexture, String bottomTexture, Material material,
        int lightValue, boolean plantSoil) {
        super(material);
        if (lightValue < 0 || lightValue > 15) throw new IllegalArgumentException("Invalid RU light value");
        this.sideTexture = sideTexture;
        this.topTexture = topTexture == null ? sideTexture : topTexture;
        this.bottomTexture = bottomTexture == null ? this.topTexture : bottomTexture;
        this.plantSoil = plantSoil;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(material == Material.wood ? 2.0F : 0.8F);
        setStepSound(
            material == Material.wood ? soundTypeWood : material == Material.ground ? soundTypeGravel : soundTypeStone);
        setLightLevel(lightValue / 15.0F);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    public boolean canSustainPlant(net.minecraft.world.IBlockAccess world, int x, int y, int z,
        net.minecraftforge.common.util.ForgeDirection direction, net.minecraftforge.common.IPlantable plant) {
        return plantSoil ? net.minecraft.init.Blocks.dirt.canSustainPlant(world, x, y, z, direction, plant)
            : super.canSustainPlant(world, x, y, z, direction, plant);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcon = register.registerIcon(textureName(sideTexture));
        topIcon = register.registerIcon(textureName(topTexture));
        // Deepslate belongs to EFR's atlas in 1.7, not minecraft:deepslate. Resolve after atlas registration.
        if (!bottomTexture.equals("minecraft:deepslate"))
            bottomIcon = register.registerIcon(textureName(bottomTexture));
        blockIcon = sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        if (side == 0)
            return bottomTexture.equals("minecraft:deepslate") ? ganymedes01.etfuturum.ModBlocks.DEEPSLATE.get()
                .getIcon(0, 0) : bottomIcon;
        return side == 1 ? topIcon : sideIcon;
    }

    private static String textureName(String texture) {
        return texture.indexOf(':') >= 0 ? texture : Biomic.MODID + ":" + texture;
    }
}
