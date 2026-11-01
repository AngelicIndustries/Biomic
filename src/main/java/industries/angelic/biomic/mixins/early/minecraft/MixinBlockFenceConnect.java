package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fences of one material join each other and every gate, as in modern Minecraft: 1.7.10 joins a fence only to its own
 * block and vanilla's gate, so Biomic's woods' fences stood apart from vanilla's and each other's. Injected rather
 * than overwritten, so it sits beside Et Futurum Requiem's overwrite of the same method.
 */
@Mixin(BlockFence.class)
public abstract class MixinBlockFenceConnect {

    @Inject(method = "canConnectFenceTo", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$joinFences(IBlockAccess world, int x, int y, int z, CallbackInfoReturnable<Boolean> result) {
        Block block = world.getBlock(x, y, z);
        if (block instanceof BlockFenceGate
            || block instanceof BlockFence && block.getMaterial() == ((Block) (Object) this).getMaterial())
            result.setReturnValue(true);
    }
}
