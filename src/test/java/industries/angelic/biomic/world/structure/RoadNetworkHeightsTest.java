package industries.angelic.biomic.world.structure;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class RoadNetworkHeightsTest {

    /** A road of cross-sections three cells wide, with a one-section ridge in what the ground wants. */
    @Test
    public void aRidgeOneCrossSectionLongIsLevelled() {
        double[] wants = { 195.2, 195.3, 195.1, 195.4, 195.6, 196.4 };
        int[] heights = solveRoad(wants, 2);
        for (int height : heights) assertEquals(195, height);
    }

    @Test
    public void aSlopeTheGroundHoldsIsKeptInSingleSteps() {
        double[] wants = new double[12];
        for (int k = 0; k < wants.length; k++) wants[k] = 190 + 0.8 * k;
        int[] heights = solveRoad(wants, 2);
        for (int k = 0; k < heights.length; k++) assertTrue(Math.abs(heights[k] - wants[k]) <= 2);
        for (int k = 1; k < heights.length; k++)
            assertTrue(heights[k] - heights[k - 1] == 0 || heights[k] - heights[k - 1] == 1);
    }

    @Test
    public void stepsNeverExceedOneBlockEvenAgainstTheGround() {
        int[] heights = solveRoad(new double[] { 190, 190, 200, 200 }, 0);
        for (int k = 1; k < heights.length; k++) assertTrue(Math.abs(heights[k] - heights[k - 1]) <= 1);
    }

    @Test
    public void floorsHold() {
        int[] start = { 0, 1, 2 };
        int[] heights = RoadNetworkHeights.solve(
            start,
            new double[] { 60, 60 },
            new int[] { 64, Integer.MIN_VALUE },
            new int[] { 0 },
            new int[] { 1 },
            new int[] { 1 },
            0);
        assertArrayEquals(new int[] { 64, 63 }, heights);
    }

    /** Small random networks against every labelling. */
    @Test
    public void matchesBruteForce() {
        Random random = new Random(7);
        for (int trial = 0; trial < 300; trial++) {
            int parts = 2 + random.nextInt(4);
            int[] start = new int[parts + 1];
            List<Double> wants = new ArrayList<>();
            int[] floor = new int[parts];
            for (int p = 0; p < parts; p++) {
                int cells = 1 + random.nextInt(3);
                for (int c = 0; c < cells; c++) wants.add(100 + random.nextDouble() * 4);
                start[p + 1] = wants.size();
                floor[p] = random.nextInt(4) == 0 ? 101 + random.nextInt(3) : Integer.MIN_VALUE;
            }
            List<int[]> pairs = new ArrayList<>();
            for (int p = 0; p < parts; p++) for (int q = p + 1; q < parts; q++)
                if (q == p + 1 || random.nextInt(3) == 0) pairs.add(new int[] { p, q, random.nextInt(3) });
            int[] a = pairs.stream()
                .mapToInt(e -> e[0])
                .toArray(),
                b = pairs.stream()
                    .mapToInt(e -> e[1])
                    .toArray();
            int[] edge = pairs.stream()
                .mapToInt(e -> e[2])
                .toArray();
            double[] w = wants.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();
            double smoothness = random.nextDouble() * 3;
            int[] solved = RoadNetworkHeights.solve(start, w, floor, a, b, edge, smoothness);
            double best = Double.POSITIVE_INFINITY;
            int[] labels = new int[parts];
            for (int code = 0; code < Math.pow(6, parts); code++) {
                int rest = code;
                for (int p = 0; p < parts; p++) {
                    labels[p] = 99 + rest % 6;
                    rest /= 6;
                }
                best = Math.min(best, cost(labels, start, w, floor, a, b, edge, smoothness));
            }
            assertEquals("trial " + trial, best, cost(solved, start, w, floor, a, b, edge, smoothness), 0.01);
        }
    }

    private static double cost(int[] h, int[] start, double[] wants, int[] floor, int[] a, int[] b, int[] edge,
        double smoothness) {
        double total = 0;
        for (int p = 0; p < h.length; p++) {
            if (h[p] < floor[p]) return Double.POSITIVE_INFINITY;
            for (int k = start[p]; k < start[p + 1]; k++) total += Math.abs(h[p] - wants[k]);
        }
        for (int k = 0; k < a.length; k++) {
            int change = Math.abs(h[a[k]] - h[b[k]]);
            if (change > 1) return Double.POSITIVE_INFINITY;
            total += smoothness * edge[k] * change;
        }
        return total;
    }

    private static int[] solveRoad(double[] sectionWants, double smoothness) {
        int sections = sectionWants.length;
        int[] start = new int[sections + 1];
        double[] wants = new double[sections * 3];
        for (int k = 0; k < sections; k++) {
            start[k + 1] = 3 * (k + 1);
            for (int c = 0; c < 3; c++) wants[3 * k + c] = sectionWants[k];
        }
        int[] a = new int[sections - 1], b = new int[sections - 1], edge = new int[sections - 1];
        for (int k = 0; k + 1 < sections; k++) {
            a[k] = k;
            b[k] = k + 1;
            edge[k] = 3;
        }
        int[] floor = new int[sections];
        java.util.Arrays.fill(floor, Integer.MIN_VALUE);
        return RoadNetworkHeights.solve(start, wants, floor, a, b, edge, smoothness);
    }
}
