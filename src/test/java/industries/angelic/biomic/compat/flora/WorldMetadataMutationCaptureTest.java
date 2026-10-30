package industries.angelic.biomic.compat.flora;

import static org.junit.Assert.*;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import org.junit.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.mixins.early.minecraft.MixinWorldNativeFeatureCapture;

/** Exercises the exact handler without claiming a transformed Forge runtime. */
public class WorldMetadataMutationCaptureTest {

    private static void invoke(MixinWorldNativeFeatureCapture fixture, CallbackInfoReturnable<Boolean> result) {
        try {
            var method = MixinWorldNativeFeatureCapture.class.getDeclaredMethod(
                "biomic$capturedMetadataWrite",
                int.class,
                int.class,
                int.class,
                int.class,
                int.class,
                CallbackInfoReturnable.class);
            method.setAccessible(true);
            method.invoke(fixture, -17, 120, 31, 7, 2, result);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @Test
    public void virtualMetadataUsesCurrentVirtualBlockAndNeverFallsThrough() {
        var fixture = new MixinWorldNativeFeatureCapture() {

            @Override
            protected World biomic$metadataMutationWorld() {
                return null;
            }
        };
        int[] calls = { 0 };
        NativeFeatureCapture.scoped(null, new NativeFeatureCapture.Access() {

            public Block block(int x, int y, int z) {
                assertNull(NativeFeatureCapture.current(null));
                calls[0]++;
                return null;
            }

            public int metadata(int x, int y, int z) {
                throw new AssertionError("Old metadata is not a write prerequisite");
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                assertNull(block);
                assertEquals(-17, x);
                assertEquals(120, y);
                assertEquals(31, z);
                assertEquals(7, metadata);
                assertNull(NativeFeatureCapture.current(null));
                calls[0]++;
                return true;
            }
        }, () -> {
            var result = new CallbackInfoReturnable<Boolean>("metadata", true, false);
            invoke(fixture, result);
            assertTrue(result.isCancelled());
            assertTrue(result.getReturnValue());
            return null;
        });
        assertEquals(2, calls[0]);
        assertNull(NativeFeatureCapture.current(null));
    }

    @Test
    public void noCapturePreservesOrdinaryMetadataExecution() {
        var fixture = new MixinWorldNativeFeatureCapture() {

            @Override
            protected World biomic$metadataMutationWorld() {
                return null;
            }
        };
        var result = new CallbackInfoReturnable<Boolean>("metadata", true, false);
        invoke(fixture, result);
        assertFalse(result.isCancelled());
    }
}
