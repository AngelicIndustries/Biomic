package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import industries.angelic.biomic.content.BiomicWetSculkBlocks;

/** Exact source pickup/fill preserves all six faces and never deletes the occupying vein. */
public final class WetSculkVeinBucket {

    public enum Action {
        NONE,
        DRAIN,
        FILL
    }

    static Action action(boolean empty, boolean filled, boolean wet, boolean dry, int metadata, int faces) {
        if (metadata < 0 || metadata > 15 || faces <= 0 || faces > 63) return Action.NONE;
        return empty && wet ? Action.DRAIN : filled && dry ? Action.FILL : Action.NONE;
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
        boolean wet = BiomicWetSculkBlocks.wet(block), dry = BiomicWetSculkBlocks.dry(block);
        int metadata = world.getBlockMetadata(x, y, z);
        int faces = block instanceof BlockSculkVein vein ? vein.faceMask(metadata) : 0;
        Action action = action(item == Items.bucket, item == Items.water_bucket, wet, dry, metadata, faces);
        if (action == Action.NONE && (item == Items.water_bucket || item == Items.lava_bucket)
            && hit.sideHit >= 0
            && hit.sideHit < 6) {
            var side = ForgeDirection.getOrientation(hit.sideHit);
            int dx = x + side.offsetX, dy = y + side.offsetY, dz = z + side.offsetZ;
            if (world.blockExists(dx, dy, dz) && BiomicWetSculkBlocks.wet(world.getBlock(dx, dy, dz))) {
                event.setCanceled(true);
                return;
            }
        }
        if (!wet && !dry) return;
        if (wet && item == Items.bucket) event.setCanceled(true);
        if (action == Action.NONE) return;
        event.setCanceled(true);
        if (action == Action.FILL && !allowFill(world.provider.isHellWorld)) return;
        if (!world.canMineBlock(event.entityPlayer, x, y, z)
            || !event.entityPlayer.canPlayerEdit(x, y, z, hit.sideHit, event.current)
            || world.isRemote) return;
        var vein = (BlockSculkVein) block;
        Block replacement = action == Action.DRAIN ? BiomicWetSculkBlocks.dryCounterpart((BlockWetSculkVein) vein)
            : BiomicWetSculkBlocks.wetVein(vein.hasUp(), vein.hasDown());
        if (!world.setBlock(x, y, z, replacement, metadata, 3)) return;
        event.result = new ItemStack(action == Action.DRAIN ? Items.water_bucket : Items.bucket);
        event.setCanceled(false);
        event.setResult(Event.Result.ALLOW);
    }
}
