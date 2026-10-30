package industries.angelic.biomic.content.block;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import industries.angelic.biomic.content.BiomicCoralWallFans;

public final class CoralWallFanBucket {

    enum Action {
        NONE,
        DRAIN,
        FILL
    }

    static Action action(boolean empty, boolean waterFilled, boolean wet, int metadata) {
        if (!BlockCoralWallFan.validMetadata(metadata)) return Action.NONE;
        return empty && wet ? Action.DRAIN : waterFilled && !wet ? Action.FILL : Action.NONE;
    }

    @SubscribeEvent
    public void fill(FillBucketEvent event) {
        var hit = event.target;
        var world = event.world;
        if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || event.current == null)
            return;
        int x = hit.blockX, y = hit.blockY, z = hit.blockZ;
        if (!world.blockExists(x, y, z)) return;
        var block = world.getBlock(x, y, z);
        var item = event.current.getItem();
        boolean filled = item == Items.water_bucket || item == Items.lava_bucket;
        boolean directFill = block instanceof BlockCoralWallFan direct && !direct.isWaterlogged()
            && item == Items.water_bucket;
        if (filled && !directFill && hit.sideHit >= 0 && hit.sideHit < 6) {
            var side = ForgeDirection.getOrientation(hit.sideHit);
            int ax = x + side.offsetX, ay = y + side.offsetY, az = z + side.offsetZ;
            if (world.blockExists(ax, ay, az) && world.getBlock(ax, ay, az) instanceof BlockCoralWallFan target
                && target.isWaterlogged()) {
                event.setCanceled(true);
                return;
            }
        }
        if (!(block instanceof BlockCoralWallFan fan)
            || BiomicCoralWallFans.get(fan.species(), fan.isWaterlogged()) != fan) return;
        if (filled && fan.isWaterlogged()) {
            event.setCanceled(true);
            return;
        }
        Action action = action(
            item == Items.bucket,
            item == Items.water_bucket,
            fan.isWaterlogged(),
            world.getBlockMetadata(x, y, z));
        boolean drain = action == Action.DRAIN, fill = action == Action.FILL;
        if (!drain && !fill) return;
        event.setCanceled(true);
        if (fill && world.provider.isHellWorld || world.isRemote
            || !world.canMineBlock(event.entityPlayer, x, y, z)
            || !event.entityPlayer.canPlayerEdit(x, y, z, hit.sideHit, event.current)) return;
        int meta = world.getBlockMetadata(x, y, z);
        if (!BlockCoralWallFan.validMetadata(meta)) return;
        var replacement = BiomicCoralWallFans.get(fan.species(), fill);
        if (!replacement.supported(world, x, y, z, meta) || !world.setBlock(x, y, z, replacement, meta, 3)) return;
        event.result = new ItemStack(drain ? Items.water_bucket : Items.bucket);
        event.setCanceled(false);
        event.setResult(Event.Result.ALLOW);
    }
}
