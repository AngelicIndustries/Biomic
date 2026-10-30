package industries.angelic.biomic.world.structure;

import java.util.Arrays;

/**
 * Integer heights for the parts of a village's road network (each cross-section of a road, each piece at a road with
 * its apron), solved together:
 * <ul>
 * <li>each part as close as it can be to the heights its cells want (the sum of their distances);</li>
 * <li>every change of height between neighbouring parts paying {@code smoothness} per block of the edge they
 * share;</li>
 * <li>no two neighbouring parts (diagonals included) more than a block apart, and no part below its floor.</li>
 * </ul>
 * A ridge or a notch one part long costs more than it fits, so none is left; a slope the ground holds over many parts
 * is kept, in steps of one block. The solution is exact: a minimum cut of Ishikawa's layered graph (a layer per
 * height), found with Dinic's algorithm.
 */
final class RoadNetworkHeights {

    private static final long SCALE = 1024, INFINITE = Long.MAX_VALUE / 4;

    private RoadNetworkHeights() {}

    /**
     * @param start      part {@code p}'s cells want {@code wants[start[p] .. start[p + 1])}
     * @param floor      the lowest height of each part
     * @param a          with {@code b}: pairs of neighbouring parts
     * @param edge       the length of the edge each pair shares (0 where they only touch diagonally)
     * @param smoothness what a block of height change costs per block of shared edge, against a block of misfit per
     *                   cell
     */
    static int[] solve(int[] start, double[] wants, int[] floor, int[] a, int[] b, int[] edge, double smoothness) {
        int parts = start.length - 1;
        int[] heights = new int[parts];
        if (parts == 0) return heights;
        // Heights between the lowest want and the highest want or floor: clamping to that range never costs more.
        double least = Double.POSITIVE_INFINITY, most = Double.NEGATIVE_INFINITY;
        for (double want : wants) {
            least = Math.min(least, want);
            most = Math.max(most, want);
        }
        int low = (int) Math.floor(least), high = (int) Math.ceil(most);
        for (int p = 0; p < parts; p++) high = Math.max(high, floor[p]);
        int layers = high - low;
        if (layers == 0) {
            Arrays.fill(heights, low);
            return heights;
        }
        // Vertex (p, t) on the source side means part p is at least t (t in low+1..high).
        int source = parts * layers, sink = source + 1;
        Network network = new Network(sink + 1, parts * layers * 3 + a.length * layers * 4);
        for (int p = 0; p < parts; p++) for (int t = low + 1; t <= high; t++) {
            int vertex = p * layers + t - low - 1;
            if (t > low + 1) network.add(vertex, vertex - 1, INFINITE, 0); // At least t: at least t - 1.
            if (t <= floor[p]) network.add(source, vertex, INFINITE, 0);
            double rise = 0; // What being at least t instead of t - 1 costs the part's cells.
            for (int k = start[p]; k < start[p + 1]; k++) rise += Math.abs(t - wants[k]) - Math.abs(t - 1 - wants[k]);
            long cost = Math.round(rise * SCALE);
            if (cost > 0) network.add(vertex, sink, cost, 0);
            else if (cost < 0) network.add(source, vertex, -cost, 0);
        }
        for (int k = 0; k < a.length; k++) {
            long change = Math.round(smoothness * edge[k] * SCALE);
            for (int t = low + 1; t <= high; t++) {
                int u = a[k] * layers + t - low - 1, v = b[k] * layers + t - low - 1;
                if (change > 0) network.add(u, v, change, change);
                if (t > low + 1) { // A part at least t keeps its neighbour at least t - 1.
                    network.add(u, v - 1, INFINITE, 0);
                    network.add(v, u - 1, INFINITE, 0);
                }
            }
        }
        network.maxFlow(source, sink);
        for (int p = 0; p < parts; p++) {
            heights[p] = low;
            for (int t = low + 1; t <= high; t++) if (network.level[p * layers + t - low - 1] >= 0) heights[p] = t;
        }
        return heights;
    }

    /**
     * A flow network; after {@link #maxFlow}, {@code level} is non-negative exactly on the source side of a minimum
     * cut.
     */
    private static final class Network {

        final int[] head, next, to, level, current, path, queue;
        final long[] capacity;
        int arcs;

        Network(int vertices, int maxArcs) {
            head = new int[vertices];
            Arrays.fill(head, -1);
            next = new int[2 * maxArcs];
            to = new int[2 * maxArcs];
            capacity = new long[2 * maxArcs];
            level = new int[vertices];
            current = new int[vertices];
            path = new int[vertices];
            queue = new int[vertices];
        }

        void add(int from, int into, long forward, long backward) {
            to[arcs] = into;
            capacity[arcs] = forward;
            next[arcs] = head[from];
            head[from] = arcs++;
            to[arcs] = from;
            capacity[arcs] = backward;
            next[arcs] = head[into];
            head[into] = arcs++;
        }

        void maxFlow(int source, int sink) {
            while (levels(source, sink)) {
                System.arraycopy(head, 0, current, 0, head.length);
                int depth = 0, vertex = source;
                while (true) {
                    if (vertex == sink) {
                        long push = INFINITE;
                        for (int k = 0; k < depth; k++) push = Math.min(push, capacity[path[k]]);
                        int saturated = depth;
                        for (int k = depth - 1; k >= 0; k--) {
                            capacity[path[k]] -= push;
                            capacity[path[k] ^ 1] += push;
                            if (capacity[path[k]] == 0) saturated = k;
                        }
                        depth = saturated;
                        vertex = depth == 0 ? source : to[path[depth - 1]];
                        continue;
                    }
                    int arc = current[vertex];
                    while (arc >= 0 && (capacity[arc] == 0 || level[to[arc]] != level[vertex] + 1)) arc = next[arc];
                    current[vertex] = arc;
                    if (arc >= 0) {
                        path[depth++] = arc;
                        vertex = to[arc];
                    } else if (vertex == source) break;
                    else {
                        level[vertex] = -1; // A dead end in this phase.
                        depth--;
                        vertex = depth == 0 ? source : to[path[depth - 1]];
                    }
                }
            }
        }

        /** Breadth-first levels from the source over arcs with room left; whether the sink is reached. */
        private boolean levels(int source, int sink) {
            Arrays.fill(level, -1);
            int first = 0, last = 0;
            level[source] = 0;
            queue[last++] = source;
            while (first < last) {
                int vertex = queue[first++];
                for (int arc = head[vertex]; arc >= 0; arc = next[arc]) {
                    if (capacity[arc] == 0 || level[to[arc]] >= 0) continue;
                    level[to[arc]] = level[vertex] + 1;
                    queue[last++] = to[arc];
                }
            }
            return level[sink] >= 0;
        }
    }
}
