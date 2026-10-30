package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.world.NetherForeignGeneration;

/** Refuses other mods' duplicate Nether blocks while their generators run over the Tartarus Nether. */
@Mixin(World.class)
public abstract class MixinWorldNetherForeignBlocks {

    @Inject(method = "setBlock(IIILnet/minecraft/block/Block;II)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$refuseDuplicate(int x, int y, int z, Block block, int metadata, int flags,
        CallbackInfoReturnable<Boolean> result) {
        if (NetherForeignGeneration.refuses(block)) result.setReturnValue(false);
    }
}
