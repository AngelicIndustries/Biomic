package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import ganymedes01.etfuturum.tileentities.TileEntityGlowLichen;
import industries.angelic.biomic.content.BiomicBlocks;

/** Source-water drain/fill keeps every original EFR face and full tile payload; no lava/water side overwrite. */
public final class SkylandsWetGlowLichenBucket {

    public enum Action {
        NONE,
        DRAIN,
        FILL
    }

    static Action action(boolean empty, boolean water, boolean wet, boolean dry, int mask) {
        if (mask < 1 || mask > 63) return Action.NONE;
        return empty && wet ? Action.DRAIN : water && dry ? Action.FILL : Action.NONE;
    }

    static boolean allowFill(boolean hell) {
        return !hell;
    }

    @SubscribeEvent
    public void fill(FillBucketEvent event) {
        var hit = event.target;
        var world = event.world;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || event.current == null)
            return;
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        if (!world.blockExists(x, y, z)) return;
        Item item = event.current.getItem();
        Block block = world.getBlock(x, y, z);
        boolean wet = block == BiomicBlocks.SKYLANDS_WET_GLOW_LICHEN,
            dry = block == ganymedes01.etfuturum.ModBlocks.GLOW_LICHEN.get();
        var tile = world.getTileEntity(x, y, z);
        int mask = tile != null && tile.getClass() == TileEntityGlowLichen.class
            ? ((TileEntityGlowLichen) tile).getSideMap()
            : 0;
        Action action = action(item == Items.bucket, item == Items.water_bucket, wet, dry, mask);
        if (wet && world.getBlockMetadata(x, y, z) != 0) action = Action.NONE;
        boolean filled = item == Items.water_bucket || item == Items.lava_bucket;
        if (action == Action.NONE && filled && hit.sideHit >= 0 && hit.sideHit < 6) {
            var side = ForgeDirection.getOrientation(hit.sideHit);
            int dx = x + side.offsetX, dy = y + side.offsetY, dz = z + side.offsetZ;
            if (world.blockExists(dx, dy, dz) && world.getBlock(dx, dy, dz) == BiomicBlocks.SKYLANDS_WET_GLOW_LICHEN) {
                event.setCanceled(true);
                return;
            }
        }
        if (!wet && !dry) return;
        if (wet && (item == Items.bucket || filled)) event.setCanceled(true);
        if (action == Action.NONE) return;
        event.setCanceled(true);
        if ((action == Action.FILL && !allowFill(world.provider.isHellWorld)) || world.isRemote) return;
        if (!world.canMineBlock(event.entityPlayer, x, y, z)
            || !event.entityPlayer.canPlayerEdit(x, y, z, hit.sideHit, event.current)) return;
        Block replacement = action == Action.DRAIN ? BlockSkylandsWetGlowLichen.dryLichen()
            : BiomicBlocks.SKYLANDS_WET_GLOW_LICHEN;
        if (!transition(world, x, y, z, block, replacement)) return;
        event.result = new ItemStack(action == Action.DRAIN ? Items.water_bucket : Items.bucket);
        event.setCanceled(false);
        event.setResult(Event.Result.ALLOW);
    }

    private static boolean transition(World world, int x, int y, int z, Block old, Block replacement) {
        var tile = world.getTileEntity(x, y, z);
        if (tile == null || tile.getClass() != TileEntityGlowLichen.class) return false;
        var saved = new NBTTagCompound();
        tile.writeToNBT(saved);
        int oldMetadata = world.getBlockMetadata(x, y, z);
        return transition(old, oldMetadata, replacement, saved, new Transition() {

            public boolean replace(Block block, int metadata) {
                return world.setBlock(x, y, z, block, metadata, 2);
            }

            public void tile(NBTTagCompound payload) {
                var next = world.getTileEntity(x, y, z);
                if (next == null || next.getClass() != TileEntityGlowLichen.class)
                    throw new IllegalStateException("Wet/dry lichen tile missing");
                next.readFromNBT(payload);
                next.markDirty();
            }

            public void notifyChange(Block block) {
                world.markBlockForUpdate(x, y, z);
                world.notifyBlocksOfNeighborChange(x, y, z, block);
            }
        });
    }

    interface Transition {

        boolean replace(Block block, int metadata);

        void tile(NBTTagCompound payload);

        void notifyChange(Block block);
    }

    /** Small bucket-only fault-injection seam; actual World implementation above retains the exact EFR tile. */
    static boolean transition(Block old, int oldMetadata, Block replacement, NBTTagCompound saved, Transition cells) {
        try {
            if (!cells.replace(replacement, 0)) return false;
            cells.tile((NBTTagCompound) saved.copy());
            cells.notifyChange(replacement);
            return true;
        } catch (RuntimeException | Error failure) {
            try {
                if (!cells.replace(old, oldMetadata)) throw new IllegalStateException("Lichen bucket rollback refused");
                cells.tile((NBTTagCompound) saved.copy());
                cells.notifyChange(old);
            } catch (RuntimeException | Error rollback) {
                failure.addSuppressed(rollback);
            }
            throw failure;
        }
    }
}
