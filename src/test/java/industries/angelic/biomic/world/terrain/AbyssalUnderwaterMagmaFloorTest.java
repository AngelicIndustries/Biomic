package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import industries.angelic.biomic.world.alexscaves.AbyssalFeaturePlanner;

/** Detached source scan/proposal regression; no natural magma or native flow proof. */
public class AbyssalUnderwaterMagmaFloorTest {

    @Test
    public void originalColumnScanFindsDownwardFloorAndNeverSearchesUpward() {
        for (int depth = 1; depth <= 4; depth++) {
            final int floor = 100 - depth;
            List<Integer> reads = new ArrayList<>();
            int result = HostedAbyssalRuntime.findUnderwaterFloorCells(y -> {
                reads.add(y);
                if (y > 100) throw new AssertionError("Upward floor search");
                return y > floor;
            }, 100, 5, 64, 448);
            assertEquals(floor, result);
            assertEquals(depth + 1, reads.size());
            for (int i = 0; i < reads.size(); i++) assertEquals(
                100 - i,
                reads.get(i)
                    .intValue());
        }
    }

    @Test
    public void rangeFiveStopsAtOriginMinusFourAndDoesNotInventFloorInDeepWater() {
        List<Integer> reads = new ArrayList<>();
        assertEquals(-1, HostedAbyssalRuntime.findUnderwaterFloorCells(y -> {
            reads.add(y);
            return y >= 96;
        }, 100, 5, 64, 448));
        assertEquals(List.of(100, 99, 98, 97, 96), reads);
        reads.clear();
        assertEquals(-1, HostedAbyssalRuntime.findUnderwaterFloorCells(y -> {
            reads.add(y);
            return false;
        }, 100, 5, 64, 448));
        assertEquals(List.of(100), reads);
        assertEquals(-1, HostedAbyssalRuntime.findUnderwaterFloorCells(y -> true, 100, 1, 64, 448));
    }

    @Test
    public void native512GuardsProtectLower64UpperHeadroomAndHaveNoOffDomainReads() {
        List<Integer> reads = new ArrayList<>();
        for (int origin : new int[] { -1, 0, 63, 448, 511, 512 }) {
            assertEquals(-1, HostedAbyssalRuntime.findUnderwaterFloorCells(y -> {
                reads.add(y);
                throw new AssertionError("Out-of-frame read");
            }, origin, 5, 64, 448));
        }
        assertTrue(reads.isEmpty());
        assertEquals(-1, HostedAbyssalRuntime.findUnderwaterFloorCells(y -> {
            reads.add(y);
            return true;
        }, 65, 5, 64, 448));
        assertEquals(List.of(65, 64), reads);
        reads.clear();
        assertEquals(64, HostedAbyssalRuntime.findUnderwaterFloorCells(y -> {
            reads.add(y);
            return y > 64;
        }, 65, 5, 64, 448));
        assertEquals(List.of(65, 64), reads);
        assertThrows(
            IllegalArgumentException.class,
            () -> HostedAbyssalRuntime.findUnderwaterFloorCells(y -> true, 65, 0, 64, 448));
    }

    @Test
    public void placementFilterUsesOriginalOceanFloorAndProposalRngAdvancesXFirst() {
        // Original heightmap189 remains distinct from a newly lowered trench floor100.
        assertTrue(HostedAbyssalRuntime.underwaterMagmaOriginAllowed(180, 189));
        assertTrue(HostedAbyssalRuntime.underwaterMagmaOriginAllowed(187, 189));
        assertFalse(HostedAbyssalRuntime.underwaterMagmaOriginAllowed(188, 189));
        var positions = HostedAbyssalRuntime.underwaterMagmaPositions(-17, 100, -16, 1);
        assertEquals(27, positions.size());
        int index = 0;
        for (int z = -17; z <= -15; z++) for (int y = 99; y <= 101; y++) for (int x = -18; x <= -16; x++)
            assertEquals(new AbyssalFeaturePlanner.Position(x, y, z), positions.get(index++));
        var sourceRandom = new Random(73);
        var actualRandom = new Random(73);
        Map<AbyssalFeaturePlanner.Position, Float> expected = new LinkedHashMap<>();
        for (int z = -17; z <= -15; z++) for (int y = 99; y <= 101; y++) for (int x = -18; x <= -16; x++)
            expected.put(new AbyssalFeaturePlanner.Position(x, y, z), sourceRandom.nextFloat());
        for (var p : positions) assertEquals(expected.get(p), Float.valueOf(actualRandom.nextFloat()));
        assertEquals(sourceRandom.nextLong(), actualRandom.nextLong());
        assertThrows(UnsupportedOperationException.class, () -> positions.clear());
    }
}
