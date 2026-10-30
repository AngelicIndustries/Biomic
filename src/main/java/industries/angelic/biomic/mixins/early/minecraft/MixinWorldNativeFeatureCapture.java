package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Dormant except while Biomic synchronously invokes an audited optional native feature. */
@Mixin(World.class)
public abstract class MixinWorldNativeFeatureCapture {

    @Unique
    private static final boolean biomic$nativeCaptureInstalled = NativeFeatureCapture.install();

    @Inject(method = "getBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$capturedBlock(int x, int y, int z, CallbackInfoReturnable<Block> result) {
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current((World) (Object) this);
        if (frame != null) result.setReturnValue(frame.block(x, y, z));
    }

    @Inject(method = "getBlockMetadata", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$capturedMetadata(int x, int y, int z, CallbackInfoReturnable<Integer> result) {
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current((World) (Object) this);
        if (frame != null) result.setReturnValue(frame.metadata(x, y, z));
    }

    @Inject(method = "setBlock(IIILnet/minecraft/block/Block;II)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$capturedWrite(int x, int y, int z, Block block, int metadata, int flags,
        CallbackInfoReturnable<Boolean> result) {
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current((World) (Object) this);
        if (frame != null) {
            result.setReturnValue(frame.set(x, y, z, block, metadata));
        }
    }

    @Inject(method = "setBlockMetadataWithNotify(IIIII)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$capturedMetadataWrite(int x, int y, int z, int metadata, int flags,
        CallbackInfoReturnable<Boolean> result) {
        World world = biomic$metadataMutationWorld();
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current(world);
        if (frame != null) result.setReturnValue(frame.set(x, y, z, frame.block(x, y, z), metadata));
    }

    /** Native cast boundary is overrideable by an untransformed synthetic mixin fixture only. */
    @Unique
    protected World biomic$metadataMutationWorld() {
        return (World) (Object) this;
    }

    @Inject(method = "getHeightValue", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$capturedHeight(int x, int z, CallbackInfoReturnable<Integer> result) {
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current((World) (Object) this);
        if (frame != null) result.setReturnValue(frame.heightValue(x, z));
    }

    @Inject(method = "getTileEntity", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$boxedTile(int x, int y, int z,
        CallbackInfoReturnable<net.minecraft.tileentity.TileEntity> result) {
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current((World) (Object) this);
        if (frame != null && frame.boxed()) result.setReturnValue(frame.boxedTile(x, y, z));
    }

    @Inject(method = "spawnEntityInWorld", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$boxedSpawn(net.minecraft.entity.Entity entity, CallbackInfoReturnable<Boolean> result) {
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current((World) (Object) this);
        if (frame != null && frame.boxed()) result.setReturnValue(frame.boxedSpawn(entity));
    }

    /**
     * A box-confined generator (structures, other mods' generators) gets a column's biome from the biome map, never
     * from a loaded chunk: whether the column's chunk was loaded yet depended on timing.
     */
    @Inject(method = "getBiomeGenForCoords", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$boxedBiome(int x, int z,
        CallbackInfoReturnable<net.minecraft.world.biome.BiomeGenBase> result) {
        World world = (World) (Object) this;
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current(world);
        if (frame != null && frame.boxed()) result.setReturnValue(
            world.getWorldChunkManager()
                .getBiomeGenAt(x, z));
    }

    @Inject(method = "getTopSolidOrLiquidBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$capturedTopSolidOrLiquid(int x, int z, CallbackInfoReturnable<Integer> result) {
        NativeFeatureCapture.Frame frame = NativeFeatureCapture.current((World) (Object) this);
        if (frame != null) result.setReturnValue(frame.topSolidOrLiquid(x, z));
    }
}
