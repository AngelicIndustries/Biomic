package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicAquaticBlocks;

/** Five species, separate water/dry materials, facing2 bits plus dead bit4. No legacy fan mutation. */
public final class BlockCoralWallFan extends Block implements WaterloggedRenderPlant {

    private final int species;
    private final boolean wet;

    public BlockCoralWallFan(int species, boolean wet) {
        super(wet ? Material.water : Material.plants);
        if (species < 0 || species >= 5) throw new IllegalArgumentException("Invalid coral species");
        this.species = species;
        this.wet = wet;
        setBlockName("biomic." + speciesName(species) + "_coral_wall_fan" + (wet ? "_wet" : ""));
        setHardness(0);
        setStepSound(soundTypeGrass);
        setLightOpacity(wet ? 3 : 0);
    }

    public int species() {
        return species;
    }

    public boolean isWaterlogged() {
        return wet;
    }

    @Override
    public boolean containsSourceWater() {
        return wet;
    }

    public static String speciesName(int species) {
        return switch (species) {
            case 0 -> "brain";
            case 1 -> "bubble";
            case 2 -> "fire";
            case 3 -> "horn";
            case 4 -> "tube";
            default -> throw new IllegalArgumentException("Invalid coral species");
        };
    }

    public static boolean validMetadata(int meta) {
        return meta >= 0 && meta < 8;
    }

    public static ForgeDirection facing(int meta) {
        if (!validMetadata(meta)) throw new IllegalArgumentException("Invalid wall fan metadata");
        return switch (meta & 3) {
            case 0 -> ForgeDirection.NORTH;
            case 1 -> ForgeDirection.EAST;
            case 2 -> ForgeDirection.SOUTH;
            default -> ForgeDirection.WEST;
        };
    }

    public static int facingMetadata(ForgeDirection side) {
        return switch (side) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException("Horizontal wall required");
        };
    }

    public static int deadMetadata(int metadata) {
        if (!validMetadata(metadata)) throw new IllegalArgumentException("Invalid wall fan metadata");
        return metadata | 4;
    }

    public static int deathDelay(Random random) {
        return 60 + random.nextInt(40);
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
        return BiomicRenderIds.CORAL_WALL_FANS;
    }

    @Override
    public int getRenderBlockPass() {
        return wet ? 1 : 0;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || wet && pass == 1;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    public boolean supported(World world, int x, int y, int z, int meta) {
        if (!validMetadata(meta) || y < 0 || y >= world.getActualHeight()) return false;
        ForgeDirection side = facing(meta), attachment = side.getOpposite();
        int ax = x + attachment.offsetX, az = z + attachment.offsetZ;
        return world.blockExists(ax, y, az) && world.getBlock(ax, y, az)
            .isSideSolid(world, ax, y, az, side);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return supported(world, x, y, z, world.getBlockMetadata(x, y, z));
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        int meta = world.getBlockMetadata(x, y, z);
        if (!validMetadata(meta)) {
            setBlockBounds(0, 0, 0, 1, 1, 1);
            return;
        }
        switch (meta & 3) {
            case 0 -> setBlockBounds(0, .25F, .3125F, 1, .75F, 1);
            case 1 -> setBlockBounds(0, .25F, 0, .6875F, .75F, 1);
            case 2 -> setBlockBounds(0, .25F, 0, 1, .75F, .6875F);
            case 3 -> setBlockBounds(.3125F, .25F, 0, 1, .75F, 1);
        }
    }

    private boolean hasWater(World world, int x, int y, int z) {
        if (wet) return true;
        for (var side : ForgeDirection.VALID_DIRECTIONS) {
            int ax = x + side.offsetX, ay = y + side.offsetY, az = z + side.offsetZ;
            if (world.blockExists(ax, ay, az) && world.getBlock(ax, ay, az)
                .getMaterial() == Material.water) return true;
        }
        return false;
    }

    private void schedule(World world, int x, int y, int z) {
        if (!world.isRemote) {
            world.scheduleBlockUpdate(x, y, z, this, wet ? 1 : deathDelay(world.rand));
        }
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        schedule(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        schedule(world, x, y, z);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || !world.blockExists(x, y, z) || world.getBlock(x, y, z) != this) return;
        int meta = world.getBlockMetadata(x, y, z);
        if (!validMetadata(meta)) throw new IllegalStateException("Invalid saved wall fan metadata");
        // Support and dry-water observation never turn a missing neighbor into absence or load it.
        if (!world.checkChunksExist(
            x - 1,
            Math.max(0, y - 1),
            z - 1,
            x + 1,
            Math.min(world.getActualHeight() - 1, y + 1),
            z + 1)) {
            schedule(world, x, y, z);
            return;
        }
        if (!supported(world, x, y, z, meta)) {
            world.setBlock(x, y, z, wet ? Blocks.water : Blocks.air, 0, 3);
            return;
        }
        if (!wet && (meta & 4) == 0 && !hasWater(world, x, y, z)) {
            world.setBlockMetadataWithNotify(x, y, z, deadMetadata(meta), 2);
        }
        if (wet) RetainedPlantSourceWater.tick(world, x, y, z, random);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean harvest) {
        return world.setBlock(x, y, z, wet ? Blocks.water : Blocks.air, 0, 3);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        if (world.setBlock(x, y, z, wet ? Blocks.water : Blocks.air, 0, 3))
            onBlockDestroyedByExplosion(world, x, y, z, explosion);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block old, int meta) {
        super.breakBlock(world, x, y, z, old, meta);
        if (wet && !world.isRemote && world.getBlock(x, y, z) == Blocks.air)
            world.setBlock(x, y, z, Blocks.water, 0, 3);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return (meta & 4) != 0 ? Item.getItemFromBlock(BiomicAquaticBlocks.CORAL_FAN) : null;
    }

    @Override
    public int damageDropped(int meta) {
        return species + ((meta & 4) != 0 ? 5 : 0);
    }

    @Override
    protected ItemStack createStackedBlock(int meta) {
        return new ItemStack(BiomicAquaticBlocks.CORAL_FAN, 1, damageDropped(meta));
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {/* Original floor-fan sprites, no duplicated assets. */}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return BiomicAquaticBlocks.CORAL_FAN.getPlantIcon(damageDropped(meta));
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void getSubBlocks(Item item, CreativeTabs tab, List stacks) {
        if (!wet) {
            stacks.add(new ItemStack(item, 1, 0));
            stacks.add(new ItemStack(item, 1, 4));
        }
    }
}
