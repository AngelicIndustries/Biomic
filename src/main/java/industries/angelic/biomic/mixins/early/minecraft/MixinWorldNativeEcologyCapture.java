package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Tile mutations stay inside a synchronous cave feature transaction until its bounded writes are accepted. */
@Mixin(World.class)
public abstract class MixinWorldNativeEcologyCapture {

    @Unique
    private NativeFeatureCapture.Frame biomic$ecologyFrame() {
        var frame = NativeFeatureCapture.current((World) (Object) this);
        return frame != null && frame.isolated() ? frame : null;
    }

    @Inject(method = "getTileEntity", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$tile(int x, int y, int z, CallbackInfoReturnable<TileEntity> result) {
        var frame = biomic$ecologyFrame();
        if (frame != null) result.setReturnValue(frame.tileEntity(x, y, z));
    }

    @Inject(method = "setTileEntity", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$setTile(int x, int y, int z, TileEntity tile, CallbackInfo ci) {
        var frame = biomic$ecologyFrame();
        if (frame != null) {
            frame.tileMutation(x, y, z, tile);
            ci.cancel();
        }
    }

    @Inject(method = "removeTileEntity", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$removeTile(int x, int y, int z, CallbackInfo ci) {
        var frame = biomic$ecologyFrame();
        if (frame != null) {
            frame.tileMutation(x, y, z, null);
            ci.cancel();
        }
    }

    @WrapMethod(method = "getSavedLightValue")
    private int biomic$light(EnumSkyBlock kind, int x, int y, int z, Operation<Integer> original) {
        var frame = biomic$ecologyFrame();
        return frame == null ? original.call(kind, x, y, z) : frame.savedLight(kind, x, y, z);
    }

    @Inject(
        method = { "getFullBlockLightValue", "getBlockLightValue" },
        at = @At("HEAD"),
        cancellable = true,
        require = 2)
    private void biomic$surfaceLight(int x, int y, int z, CallbackInfoReturnable<Integer> result) {
        var frame = biomic$ecologyFrame();
        if (frame != null && frame.surface()) result.setReturnValue(frame.fullLight(x, y, z));
    }

    @Inject(method = "canBlockSeeTheSky", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$surfaceSky(int x, int y, int z, CallbackInfoReturnable<Boolean> result) {
        var frame = biomic$ecologyFrame();
        if (frame != null && frame.surface()) result.setReturnValue(frame.seesSky(x, y, z));
    }

    @Inject(method = "spawnEntityInWorld", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$surfaceEntity(net.minecraft.entity.Entity entity, CallbackInfoReturnable<Boolean> result) {
        var frame = biomic$ecologyFrame();
        if (frame != null && frame.entitySpawns()) result.setReturnValue(frame.spawnEntity(entity));
    }

    @Inject(method = "getBiomeGenForCoords", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$surfaceBiome(int x, int z,
        CallbackInfoReturnable<net.minecraft.world.biome.BiomeGenBase> result) {
        var frame = biomic$ecologyFrame();
        if (frame != null && frame.biomeSurface()) result.setReturnValue(frame.biome(x, z));
    }

    @Inject(method = "isBlockFreezable", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$surfaceFreeze(int x, int y, int z, CallbackInfoReturnable<Boolean> result) {
        var frame = biomic$ecologyFrame();
        if (frame != null && frame.biomeSurface()) result.setReturnValue(frame.freezable(x, y, z));
    }

    @Inject(
        method = "isSideSolid(IIILnet/minecraftforge/common/util/ForgeDirection;Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        remap = false)
    private void biomic$solid(int x, int y, int z, ForgeDirection side, boolean fallback,
        CallbackInfoReturnable<Boolean> result) {
        var frame = biomic$ecologyFrame();
        if (frame != null) result.setReturnValue(
            frame.block(x, y, z)
                .isSideSolid((World) (Object) this, x, y, z, side));
    }

    @Inject(method = "markBlockForUpdate", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$update(int x, int y, int z, CallbackInfo ci) {
        if (biomic$ecologyFrame() != null) ci.cancel();
    }

    @Inject(method = "markTileEntityChunkModified", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$dirty(int x, int y, int z, TileEntity tile, CallbackInfo ci) {
        if (biomic$ecologyFrame() != null) ci.cancel();
    }

    @Inject(method = "updateLightByType", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$deferredLight(EnumSkyBlock kind, int x, int y, int z, CallbackInfoReturnable<Boolean> result) {
        if (biomic$ecologyFrame() != null) result.setReturnValue(true);
    }
}
