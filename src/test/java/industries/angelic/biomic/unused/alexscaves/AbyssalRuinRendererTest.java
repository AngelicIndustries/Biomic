package industries.angelic.biomic.unused.alexscaves;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AbyssalRuinRendererTest {

    @Test
    public void boneWormBlockstateUsesExactSourceFacingRotations() {
        assertRotation(0, 180, 0);
        assertRotation(1, 0, 0);
        assertRotation(2, 90, 0);
        assertRotation(3, 90, 180);
        assertRotation(4, 90, 270);
        assertRotation(5, 90, 90);
        assertEquals(
            1,
            AbyssalRuinBlocks.BONE_WORMS.cuboids(1)
                .size());
        var up = AbyssalRuinBlocks.BONE_WORMS.cuboids(1)
            .get(0);
        assertEquals(1D / 16D, up.x0(), 0D);
        assertEquals(0D, up.y0(), 0D);
        assertEquals(15D / 16D, up.x1(), 0D);
        assertEquals(4D / 16D, up.y1(), 0D);
        var planes = AbyssalRuinRenderer.boneWormPlanes();
        assertEquals(8, planes.length);
        int x4 = 0, x12 = 0, z4 = 0, z12 = 0, mirrored = 0;
        for (var plane : planes) {
            if (plane.x0() == plane.x1() && plane.x0() == 4) x4++;
            if (plane.x0() == plane.x1() && plane.x0() == 12) x12++;
            if (plane.z0() == plane.z1() && plane.z0() == 4) z4++;
            if (plane.z0() == plane.z1() && plane.z0() == 12) z12++;
            if (plane.mirror()) mirrored++;
        }
        assertEquals(2, x4);
        assertEquals(2, x12);
        assertEquals(2, z4);
        assertEquals(2, z12);
        assertEquals(4, mirrored);
    }

    private static void assertRotation(int metadata, int x, int y) {
        assertTrue(java.util.Arrays.equals(new int[] { x, y }, AbyssalRuinRenderer.boneWormRotation(metadata)));
    }
}
