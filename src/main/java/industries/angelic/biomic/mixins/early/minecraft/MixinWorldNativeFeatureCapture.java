package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/**
 * Anchors the native feature capture on World. Its hooks are not injections: once every mixin on World applied, the
 * early config's plugin puts plain calls at the head of World's methods (core.WorldCaptureTransform), which build
 * nothing while no capture runs.
 */
@Mixin(World.class)
public abstract class MixinWorldNativeFeatureCapture {

    @Unique
    private static final boolean biomic$nativeCaptureInstalled = NativeFeatureCapture.install();
}
