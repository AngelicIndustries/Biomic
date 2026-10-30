package industries.angelic.biomic.content.alexscaves;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Water-containing source state. Geometry is delegated to the dedicated Abyssal renderer. */
public class BlockAbyssalAquatic extends Block {

    public enum Kind {
        VENT_FULL,
        VENT_MEDIUM,
        VENT_THIN,
        TUBE_WORM,
        MUSSEL,
        SPONGE,
        DUSK,
        TWILIGHT,
        MIDNIGHT
    }

    private final Kind kind;
    private final String[] textures;
    private IIcon[] icons;
    private IIcon itemIcon;

    BlockAbyssalAquatic(String name, Kind kind, String... textures) {
        this(name, kind, kind == Kind.VENT_FULL ? Material.rock : Material.water, textures);
    }

    BlockAbyssalAquatic(String name, Kind kind, Material material, String... textures) {
        super(material);
        this.kind = kind;
        this.textures = textures.clone();
        setBlockName("biomic.alexs_abyssal_" + name);
        setBlockTextureName("biomic:alexscaves/" + textures[0]);
        setHardness(
            kind.name()
                .startsWith("VENT") || kind == Kind.TUBE_WORM ? 2F : kind == Kind.MUSSEL ? 1F : 0F);
        if (kind.name()
            .startsWith("VENT")) setResistance(5F);
        if (kind == Kind.MUSSEL) setTickRandomly(true);
        if (kind == Kind.SPONGE) setLightLevel(5 / 15F);
        setLightOpacity(kind == Kind.VENT_FULL ? 255 : 0);
        setStepSound(
            kind.name()
                .startsWith("VENT") ? soundTypeStone : soundTypeGrass);
    }

    public Kind kind() {
        return kind;
    }

    @SideOnly(Side.CLIENT)
    public IIcon icon(int index) {
        return icons[Math.min(index, icons.length - 1)];
    }

    public boolean isDryCarrier() {
        return false;
    }

    public boolean containsWater(int metadata) {
        return !isDryCarrier() && (kind == Kind.MUSSEL ? (metadata & 8) == 0
            : kind == Kind.VENT_MEDIUM || kind == Kind.VENT_THIN ? (metadata & 8) != 0 : kind != Kind.VENT_FULL);
    }

    /** Source-generated flat items exist only for mussels and ping-pong sponge. */
    @SideOnly(Side.CLIENT)
    public IIcon generatedItemIcon() {
        return kind == Kind.MUSSEL ? itemIcon : kind == Kind.SPONGE ? icon(1) : null;
    }

    public boolean generatedItemKind() {
        return kind == Kind.MUSSEL || kind == Kind.SPONGE;
    }

    @Override
    public boolean isOpaqueCube() {
        return kind == Kind.VENT_FULL;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return generatedItemKind() ? AbyssalClientRendering.generatedItemRenderId() : AbyssalClientRendering.renderId();
    }

    @Override
    public int getRenderBlockPass() {
        return kind == Kind.VENT_FULL ? 0 : 1;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || (pass == 1 && kind != Kind.VENT_FULL);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setSourceBounds(world.getBlockMetadata(x, y, z), sourceOffset(x, z));
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setSourceBounds(0);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        if (!AbyssalModelGeometry.hasCollision(kind)) return null;
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {
        if (!AbyssalModelGeometry.hasCollision(kind)) return;
        int metadata = world.getBlockMetadata(x, y, z);
        if (kind != Kind.TUBE_WORM || metadata / 4 != 2) {
            super.addCollisionBoxesToList(world, x, y, z, mask, boxes, entity);
            return;
        }
        addBox(mask, boxes, x, y, z, new double[] { 4, 0, 4, 12, 16, 12 });
        addBox(mask, boxes, x, y, z, AbyssalModelGeometry.selectionBounds(kind, 4 + (metadata & 3)));
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(AbyssalContentBlocks.visibleItemBlock(this));
    }

    @Override
    public int damageDropped(int metadata) {
        return kind == Kind.SPONGE ? 1 : metadata;
    }

    @Override
    public int getDamageValue(World world, int x, int y, int z) {
        return kind == Kind.SPONGE ? 1 : world.getBlockMetadata(x, y, z);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        return new ItemStack(AbyssalContentBlocks.visibleItemBlock(this), 1, getDamageValue(world, x, y, z));
    }

    @Override
    protected ItemStack createStackedBlock(int metadata) {
        return new ItemStack(AbyssalContentBlocks.visibleItemBlock(this), 1, damageDropped(metadata));
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        if (AbyssalVentBehavior.vent(kind)) refreshVent(world, x, y, z);
        else if (kind == Kind.SPONGE) refreshSponge(world, x, y, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        if (kind == Kind.VENT_FULL || kind == Kind.VENT_MEDIUM || kind == Kind.VENT_THIN) return true;
        if (kind == Kind.MUSSEL) {
            int face = metadataFacing(world.getBlockMetadata(x, y, z));
            ForgeDirection direction = ForgeDirection.getOrientation(face);
            int sx = x - direction.offsetX, sy = y - direction.offsetY, sz = z - direction.offsetZ;
            return world.getBlock(sx, sy, sz)
                .isSideSolid(world, sx, sy, sz, direction);
        }
        if (kind == Kind.TUBE_WORM) {
            if (y > 0 && world.getBlock(x, y - 1, z)
                .isSideSolid(world, x, y - 1, z, ForgeDirection.UP)) return true;
            if (y > 0 && world.getBlock(x, y - 1, z) == this && world.getBlockMetadata(x, y - 1, z) / 4 != 1)
                return true;
            int metadata = world.getBlockMetadata(x, y, z);
            if (metadata / 4 != 2) return false;
            ForgeDirection facing = horizontal(metadata & 3);
            int sx = x + facing.offsetX, sz = z + facing.offsetZ;
            Block support = world.getBlock(sx, y, sz);
            return support == this || support.isSideSolid(world, sx, y, sz, facing.getOpposite());
        }
        if (kind == Kind.SPONGE) {
            if (y <= 0) return false;
            Block below = world.getBlock(x, y - 1, z);
            return AbyssalMusselGrowth.spongeSupport(
                below == this,
                below.isSideSolid(world, x, y - 1, z, ForgeDirection.UP),
                below == ganymedes01.etfuturum.ModBlocks.MAGMA.get());
        }
        return y > 0 && world.getBlock(x, y - 1, z)
            .isSideSolid(world, x, y - 1, z, ForgeDirection.UP);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (AbyssalVentBehavior.vent(kind)) {
            refreshVent(world, x, y, z);
            return;
        }
        if (!canBlockStay(world, x, y, z)) {
            if (containsWater(world.getBlockMetadata(x, y, z)))
                world.setBlock(x, y, z, net.minecraft.init.Blocks.water, 0, 3);
            else world.setBlockToAir(x, y, z);
            return;
        }
        if (kind == Kind.SPONGE) refreshSponge(world, x, y, z);
    }

    private void refreshSponge(World world, int x, int y, int z) {
        if (world.isRemote || !world.blockExists(x, y, z) || y < 0 || y >= world.getActualHeight()) return;
        if (y + 1 < world.getActualHeight() && !world.blockExists(x, y + 1, z)) return;
        int before = world.getBlockMetadata(x, y, z), after = AbyssalMusselGrowth
            .spongeMetadata(before, y + 1 < world.getActualHeight() && world.getBlock(x, y + 1, z) == this);
        if (before != after) world.setBlockMetadataWithNotify(x, y, z, after, 2);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (kind != Kind.MUSSEL || world.isRemote || !world.blockExists(x, y, z) || world.getBlock(x, y, z) != this)
            return;
        var origin = new AbyssalMusselGrowth.Position(x, y, z);
        var state = colony(this, world.getBlockMetadata(x, y, z));
        if (state == null) return;
        var cells = new AbyssalMusselGrowth.Cells() {

            public boolean inBuildHeight(AbyssalMusselGrowth.Position p) {
                return p.y() >= 0 && p.y() < world.getActualHeight();
            }

            public boolean loaded(AbyssalMusselGrowth.Position p) {
                return p.y() >= 0 && p.y() < world.getActualHeight() && world.blockExists(p.x(), p.y(), p.z());
            }

            public AbyssalMusselGrowth.Cell read(AbyssalMusselGrowth.Position p) {
                Block block = world.getBlock(p.x(), p.y(), p.z());
                int meta = world.getBlockMetadata(p.x(), p.y(), p.z());
                boolean grows = block == AbyssalContentBlocks.VENT;
                if (!grows) try {
                    String authored = industries.angelic.biomic.world.modern.ModernStateCompatibility
                        .modernState(block, meta);
                    grows = AbyssalMusselGrowth
                        .growsOn(industries.angelic.biomic.world.modern.ModernDecorationEngine.blockName(authored));
                } catch (IllegalArgumentException unknown) {/* Unknown is neither water nor a growth substrate. */}
                return new AbyssalMusselGrowth.Cell(
                    block == net.minecraft.init.Blocks.water && meta == 0,
                    grows,
                    colony(block, meta));
            }

            public boolean sturdy(AbyssalMusselGrowth.Position p, AbyssalMusselGrowth.Facing face) {
                return world.getBlock(p.x(), p.y(), p.z())
                    .isSideSolid(world, p.x(), p.y(), p.z(), ForgeDirection.getOrientation(face.ordinal()));
            }
        };
        AbyssalMusselGrowth.propose(origin, state, cells, random)
            .ifPresent(growth -> {
                var p = growth.position();
                if (!cells.loaded(p) || !cells.read(p)
                    .equals(growth.before())) return;
                var after = growth.after();
                world.setBlock(
                    p.x(),
                    p.y(),
                    p.z(),
                    AbyssalContentBlocks.mussel(after.count(), after.wet()),
                    after.metadata(),
                    3);
            });
    }

    private static AbyssalMusselGrowth.Colony colony(Block block, int metadata) {
        if (metadata < 0 || metadata > 15 || (metadata & 7) > 5) return null;
        int count = AbyssalContentBlocks.musselCount(block);
        if (count > 0) return new AbyssalMusselGrowth.Colony(
            count,
            AbyssalMusselGrowth.Facing.values()[metadata & 7],
            block instanceof BlockAbyssalAquatic aquatic && aquatic.containsWater(metadata));
        return null;
    }

    private void refreshVent(World world, int x, int y, int z) {
        if (world.isRemote || y < 1
            || y + 1 >= world.getActualHeight()
            || !world.blockExists(x, y - 1, z)
            || !world.blockExists(x, y + 1, z)) return;
        Block below = world.getBlock(x, y - 1, z), above = world.getBlock(x, y + 1, z);
        Integer lower = below instanceof BlockAbyssalAquatic aquatic && AbyssalVentBehavior.vent(aquatic.kind())
            ? world.getBlockMetadata(x, y - 1, z)
            : null;
        AbyssalVentBehavior.Fluid fluid = below.getMaterial() == Material.water ? AbyssalVentBehavior.Fluid.WATER
            : below.getMaterial() == Material.lava ? AbyssalVentBehavior.Fluid.LAVA : AbyssalVentBehavior.Fluid.NONE;
        boolean ventAbove = above instanceof BlockAbyssalAquatic aquatic && AbyssalVentBehavior.vent(aquatic.kind());
        int before = world.getBlockMetadata(x, y, z), after = AbyssalVentBehavior.refreshedMetadata(
            before,
            lower,
            fluid,
            ventAbove,
            above.getMaterial()
                .blocksMovement());
        if (before != after) {
            world.setBlockMetadataWithNotify(x, y, z, after, 2);
            // Vertical source inheritance only; don't notify an unloaded horizontal native neighbor.
            if (ventAbove) above.onNeighborBlockChange(world, x, y + 1, z, this);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if (AbyssalVentBehavior.vent(kind)) industries.angelic.biomic.client.AbyssalVentEffects.track(x, y, z);
        if (kind == Kind.TUBE_WORM && random.nextInt(3) == 0
            && supportsEmergingWorm(world, x, y, z)
            && world instanceof net.minecraft.client.multiplayer.WorldClient)
            industries.angelic.biomic.client.AbyssalTubeWormEffects.add(x, y, z, random);
    }

    public static boolean supportsEmergingWorm(IBlockAccess world, int x, int y, int z) {
        if (world instanceof World loaded && !loaded.blockExists(x, y, z)) return false;
        // Only an open end: the next tube segment is water-material too, and a worm under it would sway
        // through that segment's walls.
        Block above = world.getBlock(x, y + 1, z);
        return world.getBlock(x, y, z) == AbyssalContentBlocks.TUBE_WORM && world.getBlockMetadata(x, y, z) / 4 != 1
            && (above == net.minecraft.init.Blocks.water || above == net.minecraft.init.Blocks.flowing_water);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icons = new IIcon[textures.length];
        for (int i = 0; i < textures.length; i++) icons[i] = register.registerIcon("biomic:alexscaves/" + textures[i]);
        if (kind == Kind.MUSSEL) itemIcon = register.registerIcon("biomic:alexscaves/mussel_item");
        blockIcon = icons[0];
    }

    private void setSourceBounds(int metadata) {
        setSourceBounds(metadata, null);
    }

    private void setSourceBounds(int metadata, double[] offset) {
        double[] bounds = AbyssalModelGeometry.selectionBounds(kind, metadata);
        float ox = offset == null ? 0 : (float) offset[0], oz = offset == null ? 0 : (float) offset[1];
        setBlockBounds(
            (float) bounds[0] / 16F + ox,
            (float) bounds[1] / 16F,
            (float) bounds[2] / 16F + oz,
            (float) bounds[3] / 16F + ox,
            (float) bounds[4] / 16F,
            (float) bounds[5] / 16F + oz);
    }

    public double[] sourceOffset(int x, int z) {
        return kind == Kind.SPONGE ? AbyssalModelGeometry.xzOffset(x, z, .4D)
            : kind == Kind.DUSK || kind == Kind.TWILIGHT || kind == Kind.MIDNIGHT
                ? AbyssalModelGeometry.xzOffset(x, z, .2D)
                : null;
    }

    private static void addBox(AxisAlignedBB mask, List<AxisAlignedBB> boxes, int x, int y, int z, double[] b) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
            x + b[0] / 16D,
            y + b[1] / 16D,
            z + b[2] / 16D,
            x + b[3] / 16D,
            y + b[4] / 16D,
            z + b[5] / 16D);
        if (mask.intersectsWith(box)) boxes.add(box);
    }

    private static ForgeDirection horizontal(int metadata) {
        return switch (metadata & 3) {
            case 0 -> ForgeDirection.NORTH;
            case 1 -> ForgeDirection.EAST;
            case 2 -> ForgeDirection.SOUTH;
            default -> ForgeDirection.WEST;
        };
    }

    private static int metadataFacing(int metadata) {
        return metadata & 7;
    }
}
