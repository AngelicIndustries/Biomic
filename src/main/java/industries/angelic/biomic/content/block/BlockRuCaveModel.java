package industries.angelic.biomic.content.block;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.RuCaveModelData;

/** Source crystal/moss geometry, shape metadata and support; no custom terrain or global lighting hooks. */
public final class BlockRuCaveModel extends Block {

    private final String name;
    private final String particle;
    private final boolean solid;
    private final boolean waterlogged;
    private final int ageBank;
    private final Map<String, IIcon> icons = new HashMap<>();

    /**
     * Each block's own sound in Regions Unexplored (RUBlocks), through Et Futurum's modern sound types (vanilla ones
     * without it): its plants sound like plants (brimsprout like sculk, ivy and earlights like weeping vines), only
     * prismarite like amethyst.
     */
    private static SoundType sound(String name, boolean solid) {
        return switch (name) {
            case "prismarite_cluster" -> industries.angelic.biomic.compat.efr.EfrSounds
                .of("amethyst_cluster", soundTypeGlass);
            case "large_prismarite_cluster", "hanging_prismarite" -> industries.angelic.biomic.compat.efr.EfrSounds
                .of("amethyst_block", soundTypeGlass);
            case "blackstone_cluster" -> industries.angelic.biomic.compat.efr.EfrSounds
                .of("netherrack", soundTypeStone);
            case "deepslate_prismoss" -> industries.angelic.biomic.compat.efr.EfrSounds.of("deepslate", soundTypeStone);
            case "prismoss" -> soundTypeStone;
            case "prismoss_sprout" -> soundTypeGrass;
            case "brimsprout" -> industries.angelic.biomic.compat.efr.EfrSounds.of("sculk", soundTypeCloth);
            case "glister_bulb" -> industries.angelic.biomic.compat.efr.EfrSounds.of("crop_warts", soundTypeGrass);
            case "glister_spire" -> industries.angelic.biomic.compat.efr.EfrSounds.of("nether_sprouts", soundTypeGrass);
            case "glistering_fern" -> industries.angelic.biomic.compat.efr.EfrSounds.of("nylium", soundTypeStone);
            case "mycotoxic_mushrooms" -> industries.angelic.biomic.compat.efr.EfrSounds
                .of("shroomlight", soundTypeWood);
            case "hyacinth_bloom", "tall_hyacinth_stock" -> industries.angelic.biomic.compat.efr.EfrSounds
                .of("wet_grass", soundTypeGrass);
            case "hyacinth_flowers", "blue_magnolia_flowers", "pink_magnolia_flowers", "white_magnolia_flowers" -> industries.angelic.biomic.compat.efr.EfrSounds
                .of("vines", soundTypeGrass);
            case "dropleaf", "dropleaf_plant", "glistering_ivy", "glistering_ivy_plant", "hanging_earlight", "hanging_earlight_plant" -> industries.angelic.biomic.compat.efr.EfrSounds
                .of("weeping_vines", soundTypeGrass);
            default -> solid ? soundTypeStone : soundTypeGrass;
        };
    }

    public BlockRuCaveModel(String name, String particle, int light) {
        this(name, particle, light, false);
    }

    public BlockRuCaveModel(String name, String particle, int light, boolean waterlogged) {
        this(name, particle, light, waterlogged, 0);
    }

    public BlockRuCaveModel(String name, String particle, int light, boolean waterlogged, int ageBank) {
        super(
            waterlogged ? Material.water
                : name.endsWith("prismoss") || name.equals("hanging_prismarite") || name.equals("blackstone_cluster")
                    ? Material.rock
                    : Material.plants);
        this.name = name;
        this.particle = particle;
        this.waterlogged = waterlogged;
        this.ageBank = ageBank;
        solid = name.endsWith("prismoss");
        setBlockName("biomic.imported_" + name);
        if ((!waterlogged || RuCaveModelData.alwaysWaterFilled(name)) && ageBank == 0)
            setCreativeTab(BiomicBlocks.CREATIVE_TAB);
        setHardness(solid ? 1.5F : 0.0F);
        setLightOpacity(solid ? 255 : 0);
        setLightLevel(light / 15F);
        setStepSound(sound(name, solid));
        if (!solid) setBlockBounds(.25F, 0, .25F, .75F, 1, .75F);
        if (name.equals("blackstone_cluster")) setBlockBounds(.125F, 0, .125F, .875F, .5F, .875F);
    }

    public String modelName() {
        return name;
    }

    public boolean isWaterlogged() {
        return waterlogged;
    }

    public int modelMetadata(int metadata) {
        return RuCaveModelData.multiface(name) ? (metadata & 15) | ageBank : metadata;
    }

    public int supportedAttachments(IBlockAccess world, int x, int y, int z, int mask) {
        int supported = 0;
        for (int bit = 0; bit < RuCaveModelData.ATTACHMENT_FACES.length; bit++) {
            if ((mask & (1 << bit)) == 0) continue;
            ForgeDirection direction = ForgeDirection
                .valueOf(RuCaveModelData.ATTACHMENT_FACES[bit].toUpperCase(java.util.Locale.ROOT));
            int nx = x + direction.offsetX, ny = y + direction.offsetY, nz = z + direction.offsetZ;
            if (world.getBlock(nx, ny, nz)
                .isSideSolid(world, nx, ny, nz, direction.getOpposite())) supported |= 1 << bit;
        }
        return supported;
    }

    public String modernState(int metadata) {
        return RuCaveModelData.state("regions_unexplored:" + name, metadata, ageBank)
            .replace("waterlogged=false", "waterlogged=" + waterlogged);
    }

    @Override
    public int getRenderType() {
        return RuCaveModelData.itemTexture(name) != null ? BiomicRenderIds.RU_MODEL_SPRITE
            : BiomicRenderIds.RU_CAVE_MODEL;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return solid;
    }

    @Override
    public boolean isOpaqueCube() {
        return solid;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return solid;
    }

    @Override
    public int getRenderBlockPass() {
        return waterlogged || name.contains("prismarite") ? 1 : 0;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return waterlogged ? (pass == 0 || pass == 1) : pass == getRenderBlockPass();
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return solid || name.equals("hanging_prismarite") || name.equals("blackstone_cluster")
            ? super.getCollisionBoundingBoxFromPool(world, x, y, z)
            : null;
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (solid) return true;
        int metadata = world.getBlockMetadata(x, y, z);
        if (RuCaveModelData.multiface(name)) return supportedAttachments(world, x, y, z, modelMetadata(metadata)) != 0;
        String family = RuCaveModelData.hangingFamily(name);
        int offset = family != null || name.equals("hanging_prismarite") ? 1 : -1;
        Block support = world.getBlock(x, y + offset, z);
        if (name.equals("tall_hyacinth_stock") && support == this) return true;
        if (name.equals("hyacinth_bloom")
            && support == industries.angelic.biomic.world.palette.BlockPalette.block("minecraft:magma_block", null))
            return false;
        if (family != null && support instanceof BlockRuCaveModel other
            && family.equals(RuCaveModelData.hangingFamily(other.name))) return true;
        if (industries.angelic.biomic.world.modern.ModernGroundCoverSupport.isNetherPlant("regions_unexplored:" + name))
            return Boolean.TRUE.equals(
                industries.angelic.biomic.world.modern.ModernGroundCoverSupport.testPinned(
                    "regions_unexplored:" + name,
                    industries.angelic.biomic.world.modern.ModernStateCompatibility
                        .modernState(support, world.getBlockMetadata(x, y - 1, z))));
        if (name.equals("large_prismarite_cluster") && (metadata & 8) != 0)
            return support == this && (world.getBlockMetadata(x, y - 1, z) & 8) == 0;
        if (support instanceof BlockRuCaveModel other && other.name.equals(name) && name.equals("hanging_prismarite"))
            return true;
        return support.isSideSolid(world, x, y + offset, z, offset > 0 ? ForgeDirection.DOWN : ForgeDirection.UP);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (world.isRemote) return;
        if (RuCaveModelData.multiface(name)) {
            int oldMask = modelMetadata(world.getBlockMetadata(x, y, z));
            // Old generic flowers saved metadata zero and lost their faces. Only
            // recover real adjacent support; never invent an unsupported surface.
            int mask = supportedAttachments(world, x, y, z, oldMask == 0 ? 63 : oldMask);
            if (mask == 0) world.setBlock(x, y, z, waterlogged ? Blocks.water : Blocks.air, 0, 3);
            else if (mask != oldMask) world.setBlock(
                x,
                y,
                z,
                industries.angelic.biomic.content.RuImportedDecorationBlocks
                    .multiface("regions_unexplored:" + name, mask, waterlogged),
                mask & 15,
                3);
            return;
        }
        if (!solid && !canBlockStay(world, x, y, z)) {
            world.setBlock(x, y, z, waterlogged ? Blocks.water : Blocks.air, 0, 3);
        } else if (name.equals("tall_hyacinth_stock")) {
            int shape = RuCaveModelData.hyacinthShape(
                world.getBlock(x, y - 1, z) == this,
                world.getBlock(x, y + 1, z) == this,
                world.getBlock(x, y + 2, z) == this);
            if (shape != world.getBlockMetadata(x, y, z)) world.setBlockMetadataWithNotify(x, y, z, shape, 3);
        }
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        if (RuCaveModelData.multiface(name)) return supportedAttachments(world, x, y, z, 63) != 0
            && (world.isAirBlock(x, y, z) || world.getBlock(x, y, z)
                .getMaterial() == Material.water);
        if (RuCaveModelData.alwaysWaterFilled(name)) return world.getBlock(x, y, z)
            .getMaterial() == Material.water && canBlockStay(world, x, y, z);
        return super.canPlaceBlockAt(world, x, y, z);
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ,
        int metadata) {
        // Temporary item-placement marker consumed below, never used by worldgen.
        if (RuCaveModelData.multiface(name)) return world.getBlock(x, y, z)
            .getMaterial() == Material.water ? 1 : 0;
        return super.onBlockPlaced(world, x, y, z, side, hitX, hitY, hitZ, metadata);
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, net.minecraft.entity.EntityLivingBase placer,
        net.minecraft.item.ItemStack stack) {
        if (world.isRemote) return;
        if (RuCaveModelData.multiface(name)) {
            boolean water = world.getBlockMetadata(x, y, z) == 1;
            int mask = supportedAttachments(world, x, y, z, 63);
            if (mask != 0) world.setBlock(
                x,
                y,
                z,
                industries.angelic.biomic.content.RuImportedDecorationBlocks
                    .multiface("regions_unexplored:" + name, mask, water),
                mask & 15,
                3);
            else world.setBlock(x, y, z, water ? Blocks.water : Blocks.air, 0, 3);
        } else if (name.equals("tall_hyacinth_stock")) onNeighborBlockChange(world, x, y, z, this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        for (var value : RuCaveModelData.textures()) {
            String texture = value.getAsString();
            if (!texture.startsWith("minecraft:")) icons.put(texture, register.registerIcon(texture));
        }
        String item = RuCaveModelData.itemTexture(name);
        blockIcon = register.registerIcon(item != null ? item : particleTexture(particle));
    }

    /** EFR registers modern vanilla sprites in minecraft, not in Biomic's namespace. */
    static String particleTexture(String particle) {
        return particle.indexOf(':') >= 0 ? particle : "biomic:" + particle;
    }

    @SideOnly(Side.CLIENT)
    public IIcon modelIcon(String texture) {
        if (texture.equals("minecraft:block/stone")) return Blocks.stone.getIcon(1, 0);
        if (texture.equals("minecraft:block/deepslate"))
            return industries.angelic.biomic.world.palette.PaletteIcons.icon("minecraft:deepslate", 1);
        if (texture.equals("minecraft:block/blackstone"))
            return industries.angelic.biomic.world.palette.PaletteIcons.icon("minecraft:blackstone", 1);
        IIcon icon = icons.get(texture);
        if (icon == null) throw new IllegalArgumentException("Missing cave texture " + texture);
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return rainbow() ? RuCaveModelData.rainbow(x, z) : 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return rainbow() ? 0x48B518 : 0xFFFFFF;
    }

    private boolean rainbow() {
        return name.contains("prismarite") || name.contains("prismoss");
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        return name.equals("mycotoxic_mushrooms") ? 6 + 3 * (world.getBlockMetadata(x, y, z) & 3)
            : super.getLightValue(world, x, y, z);
    }
}
