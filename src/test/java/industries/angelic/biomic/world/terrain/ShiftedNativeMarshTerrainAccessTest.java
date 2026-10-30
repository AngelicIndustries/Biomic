package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import industries.angelic.biomic.api.ChunkVerticalLayout;

public class ShiftedNativeMarshTerrainAccessTest {

    @Test
    public void shiftedMarshPreservesLowerGeologyAndReturnsIndependentChunks() {
        var base = new Foundation();
        var marsh = HostedMarshTerrain.shifted512(42L, (x, z) -> true);
        var access = new ShiftedNativeMarshTerrainAccess(base, marsh);
        byte[] first = access.createMaterialArray(0, 0);
        assertEquals(BiomicTerrainSampler.BEDROCK, first[index(0, 0, 0)]);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 64; y++)
            assertEquals("protected lower geology", base.payload[index(x, y, z)], first[index(x, y, z)]);
        assertFalse("reviewed wetland must lower the source-Y80 plateau", Arrays.equals(base.payload, first));
        first[index(0, 191, 0)] = 99;
        assertNotEquals(99, access.createMaterialArray(0, 0)[index(0, 191, 0)]);
        assertTrue(access.sampleCarvedGroundSurfaceHeight(0, 0) >= 64);
    }

    @Test
    public void authoredViewReconcilesOnlyTheChangedSourceColumn() {
        var access = new ShiftedNativeMarshTerrainAccess(
            new Foundation(),
            HostedMarshTerrain.shifted512(7L, (x, z) -> true));
        byte[] authored = new byte[384];
        Arrays.fill(authored, BiomicTerrainSampler.STONE);
        byte[] result = access.authoredView(authored, 0, 0);
        assertNotSame(authored, result);
        assertArrayEquals("caller input remains immutable", filled(384, BiomicTerrainSampler.STONE), authored);
    }

    private static byte[] filled(int size, byte value) {
        byte[] out = new byte[size];
        Arrays.fill(out, value);
        return out;
    }

    private static int index(int x, int y, int z) {
        return ChunkVerticalLayout.EXTENDED_512.index(x, y, z);
    }

    private static final class Foundation implements HostedTerrainAccess {

        final byte[] payload = new byte[ChunkVerticalLayout.EXTENDED_512.blockCount()];

        Foundation() {
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                payload[index(x, 0, z)] = BiomicTerrainSampler.BEDROCK;
                for (int y = 1; y <= 208; y++) payload[index(x, y, z)] = BiomicTerrainSampler.STONE;
            }
        }

        public ChunkVerticalLayout layout() {
            return ChunkVerticalLayout.EXTENDED_512;
        }

        public int minimumEcologyY() {
            return 64;
        }

        public AuthoredVerticalProjection projection() {
            return AuthoredVerticalProjections.shifted512();
        }

        public byte[] createMaterialArray(int x, int z) {
            return payload.clone();
        }

        public int sampleCarvedGroundSurfaceHeight(int x, int z) {
            return 208;
        }

        public FloatingIslandArchetype floatingArchetypeAt(int x, int y, int z) {
            return null;
        }

        public String floatingHabitatAt(int x, int y, int z) {
            return null;
        }
    }
}
