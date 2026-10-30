package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.Random;

import net.minecraft.block.Block;

import org.junit.Test;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

public class BopSurfaceCaptureHostedAccessTest {

    private static NativeFeatureCapture.Access plain() {
        return new NativeFeatureCapture.Access() {

            public Block block(int x, int y, int z) {
                return null;
            }

            public int metadata(int x, int y, int z) {
                return 0;
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                return true;
            }
        };
    }

    @Test
    public void realSurfaceWrapperForwardsHostedCarrierProofAndReason() {
        int[] calls = { 0 };
        BopNativePond.HostedAccess hosted = new BopNativePond.HostedAccess() {

            public Block block(int x, int y, int z) {
                return null;
            }

            public int metadata(int x, int y, int z) {
                return 0;
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                return true;
            }

            public boolean immutableCarrier(int x, int y, int z, Block block, int metadata) {
                calls[0]++;
                return x == 7 && y == 61 && z == -9 && metadata == 1;
            }

            public String immutableCarrierRejection(int x, int y, int z, Block block, int metadata) {
                return immutableCarrier(x, y, z, block, metadata) ? null : "delegate-specific-reason";
            }
        };
        var wrapped = BopSurfaceCapture.hostedAware(hosted, new Random(0));
        assertTrue(wrapped instanceof BopNativePond.HostedAccess);
        var proof = (BopNativePond.HostedAccess) wrapped;
        assertTrue(proof.immutableCarrier(7, 61, -9, null, 1));
        assertNull(proof.immutableCarrierRejection(7, 61, -9, null, 1));
        assertEquals("delegate-specific-reason", proof.immutableCarrierRejection(8, 61, -9, null, 1));
        assertEquals(3, calls[0]);
    }

    @Test
    public void realSurfaceWrapperFailsClosedForOrdinaryDelegate() {
        var wrapped = BopSurfaceCapture.hostedAware(plain(), new Random(0));
        assertFalse(wrapped instanceof BopNativePond.HostedAccess);
    }
}
