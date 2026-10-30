package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;
import java.util.function.BooleanSupplier;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Water-preserving 1.7 representations of Origen's kelp, seagrass, coral, and coral-fan states. */
public final class BlockAquaticPlant extends Block implements AquaticVariantBlock {

    private static final int LIVE_CORAL_VARIANTS = 5;

    private final Kind kind;
    private final String[] textures;
    private IIcon[] icons;
    private IIcon itemIcon;

    public BlockAquaticPlant(String name, Kind kind, String... textures) {
        super(Material.water);
        this.kind = kind;
        this.textures = textures.clone();
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + textures[0]);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setTickRandomly(kind.isCoral() || kind.needsSupportTick());
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return kind == Kind.SEAGRASS ? BiomicRenderIds.AQUATIC_SPRITE : BiomicRenderIds.AQUATIC_PLANTS;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {}

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return world.getBlock(x, y, z)
            .getMaterial() == Material.water && canBlockStay(world, x, y, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        Block below = world.getBlock(x, y - 1, z);
        if (kind == Kind.SEAGRASS && metadata == 2) {
            return below == this && world.getBlockMetadata(x, y - 1, z) == 1;
        }
        if (kind == Kind.KELP && below == this) return true;
        if (kind == Kind.WALL) {
            int[] step = WALL_STEPS[metadata & 3];
            ForgeDirection face = ForgeDirection.VALID_DIRECTIONS[WALL_FACES[metadata & 3]];
            return world.getBlock(x + step[0], y, z + step[1])
                .isSideSolid(world, x + step[0], y, z + step[1], face);
        }
        return below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP);
    }

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        if (kind == Kind.SEA_PICKLE && world.getBlock(x, y, z) == this) {
            return 6 + Math.min(3, world.getBlockMetadata(x, y, z)) * 3;
        }
        return super.getLightValue(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            world.setBlock(x, y, z, net.minecraft.init.Blocks.water, 0, 3);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (shouldRepairSupport(kind, () -> canBlockStay(world, x, y, z))) {
            world.setBlock(x, y, z, net.minecraft.init.Blocks.water, 0, 3);
            return;
        }
        int metadata = world.getBlockMetadata(x, y, z);
        if (kind.isCoral() && metadata < LIVE_CORAL_VARIANTS && !touchesWater(world, x, y, z)) {
            world.setBlockMetadataWithNotify(x, y, z, metadata + LIVE_CORAL_VARIANTS, 2);
        }
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return kind == Kind.FLAT ? Item.getItemFromBlock(this) : null;
    }

    @Override
    public int damageDropped(int metadata) {
        return normalize(metadata);
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[textures.length];
        for (int index = 0; index < textures.length; index++) {
            icons[index] = register.registerIcon(Biomic.MODID + ":" + textures[index]);
        }
        blockIcon = icons[0];
        if (kind == Kind.SEAGRASS) itemIcon = register.registerIcon("biomic:seagrass_item");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return itemIcon != null ? itemIcon : getPlantIcon(metadata);
    }

    @SideOnly(Side.CLIENT)
    public IIcon getPlantIcon(int metadata) {
        return icons[kind == Kind.WALL ? Math.min(icons.length - 1, (metadata >> 2) & 3) : normalize(metadata)];
    }

    /** WALL: the step to the support for each side, and the support's face toward the plant (ForgeDirection). */
    public static final int[][] WALL_STEPS = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
    private static final int[] WALL_FACES = { 3, 2, 5, 4 };

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List variants) {
        for (int metadata = 0; metadata < getVariantCount(); metadata++) {
            variants.add(new ItemStack(item, 1, metadata));
        }
    }

    @Override
    public int getVariantCount() {
        return textures.length;
    }

    @Override
    public String getVariantName(int metadata) {
        return textures[normalize(metadata)];
    }

    private int normalize(int metadata) {
        int normalized = metadata & 15;
        return normalized < textures.length ? normalized : 0;
    }

    private static boolean touchesWater(World world, int x, int y, int z) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            if (world.getBlock(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ)
                .getMaterial() == Material.water) return true;
        }
        return false;
    }

    static boolean shouldRepairSupport(Kind kind, BooleanSupplier supported) {
        return kind.needsSupportTick() && !supported.getAsBoolean();
    }

    public enum Kind {

        KELP,
        SEAGRASS,
        SEA_PICKLE,
        CORAL_PLANT,
        CORAL_FAN,
        /** A ported water plant: a cross on the floor (seagrasses, algae, corals of other mods). */
        PLANT,
        /** A ported thing lying on the sea floor (shells): a small model; it drops itself. */
        FLAT,
        /**
         * A ported growth on a rock face (barnacles): metadata is the support side (0 north, 1 south, 2 west,
         * 3 east) plus four times the variant.
         */
        WALL;

        private boolean isCoral() {
            return this == CORAL_PLANT || this == CORAL_FAN;
        }

        private boolean needsSupportTick() {
            return this == KELP || this == SEAGRASS || this == PLANT || this == FLAT || this == WALL;
        }
    }
}
