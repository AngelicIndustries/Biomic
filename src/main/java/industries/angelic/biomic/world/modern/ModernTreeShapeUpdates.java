package industries.angelic.biomic.world.modern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;

/** TreeFeature's bounded leaf-distance shape and StructureTemplate's ordered outside faces. */
final class ModernTreeShapeUpdates {

    private static final int[][] OFFSETS = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 },
        { 1, 0, 0 } };
    private static final String[] FACES = { "down", "up", "north", "south", "west", "east" };

    private ModernTreeShapeUpdates() {}

    /**
     * RU small-oak writes use flag 2 (not TreeFeature's known-shape flag 19).
     * Their surrounding foliage immediately updates BranchBlock's support check.
     * Unlike an end-of-feature cleanup this permits later foliage to fill the vacated cell.
     */
    static void smallOakWrite(ModernDecorationEngine engine, World world, Pos p, String state) {
        if (!world.setBlock(p.x, p.y, p.z, state) || !world.immediateShapeUpdates()) return;
        for (int face = 0; face < 6; face++) {
            int[] d = OFFSETS[face];
            Pos q = p.offset(d[0], d[1], d[2]);
            String before = world.blockState(q.x, q.y, q.z);
            if (!ModernDecorationEngine.blockName(before)
                .endsWith("_branch")) continue;
            String after = update(engine, world, q, before, face ^ 1, state);
            if (!before.equals(after)) world.setBlock(q.x, q.y, q.z, after);
        }
    }

    static void finish(ModernDecorationEngine engine, World world, Set<Pos> logs, Set<Pos> leaves, Set<Pos> roots,
        Set<Pos> decorators) {
        Set<Pos> bounds = new HashSet<>(logs);
        bounds.addAll(leaves);
        bounds.addAll(roots);
        bounds.addAll(decorators);
        if (bounds.isEmpty()) return;
        int[] lo = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
        int[] hi = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
        for (Pos p : bounds) for (int axis = 0; axis < 3; axis++) {
            lo[axis] = Math.min(lo[axis], coordinate(p, axis));
            hi[axis] = Math.max(hi[axis], coordinate(p, axis));
        }
        Set<Pos> shape = new HashSet<>(roots);
        shape.addAll(decorators);
        List<Set<Pos>> pending = new ArrayList<>();
        for (int d = 0; d < 7; d++) pending.add(new HashSet<>());
        pending.get(0)
            .addAll(logs);
        // TreeFeature takes each level's first element by a new iterator. One that lives until its level gains an
        // element
        // yields the same elements in the same order without scanning the emptied front of the table every time.
        @SuppressWarnings("unchecked")
        java.util.Iterator<Pos>[] live = new java.util.Iterator[7];
        while (true) {
            int distance = 0;
            while (distance < 7 && pending.get(distance)
                .isEmpty()) distance++;
            if (distance == 7) break;
            if (live[distance] == null) live[distance] = pending.get(distance)
                .iterator();
            var iterator = live[distance];
            Pos p = iterator.next();
            iterator.remove();
            if (!inside(p, lo, hi)) continue;
            if (distance != 0) world.setBlock(
                p.x,
                p.y,
                p.z,
                ModernFeatureEngine.property(world.blockState(p.x, p.y, p.z), "distance", Integer.toString(distance)));
            shape.add(p);
            for (int[] delta : OFFSETS) {
                Pos next = p.offset(delta[0], delta[1], delta[2]);
                if (!inside(next, lo, hi) || shape.contains(next)) continue;
                String state = world.blockState(next.x, next.y, next.z);
                int existing = engine.isBlockInTag("minecraft:logs", state) ? 0 : distance(state);
                if (existing < 0) continue;
                int candidate = Math.min(existing, distance + 1);
                if (candidate < 7 && pending.get(candidate)
                    .add(next)) live[candidate] = null;
            }
        }
        for (Edge edge : edges(shape)) {
            Pos p = edge.position;
            int[] delta = OFFSETS[edge.face];
            Pos q = p.offset(delta[0], delta[1], delta[2]);
            String before = world.blockState(p.x, p.y, p.z), neighbor = world.blockState(q.x, q.y, q.z);
            String after = update(engine, world, p, before, edge.face, neighbor);
            if (!before.equals(after)) world.setBlock(p.x, p.y, p.z, after);
            String next = update(engine, world, q, neighbor, edge.face ^ 1, after);
            if (!neighbor.equals(next)) world.setBlock(q.x, q.y, q.z, next);
        }
    }

    static List<Edge> edges(Set<Pos> shape) {
        List<Edge> result = new ArrayList<>();
        // DiscreteVoxelShape: NONE, FORWARD, BACKWARD => Z faces, Y faces, X faces.
        for (int axis : new int[] { 2, 1, 0 }) {
            List<Edge> faces = new ArrayList<>();
            int negative = axis == 2 ? 2 : axis == 1 ? 0 : 4;
            for (Pos p : shape) for (int face = negative; face <= negative + 1; face++) {
                int[] d = OFFSETS[face];
                if (!shape.contains(p.offset(d[0], d[1], d[2]))) faces.add(new Edge(p, face));
            }
            int outer = axis == 2 ? 0 : axis == 1 ? 2 : 1;
            int inner = axis == 2 ? 1 : axis == 1 ? 0 : 2;
            faces.sort((a, b) -> {
                int c = Integer.compare(coordinate(a.position, outer), coordinate(b.position, outer));
                if (c == 0) c = Integer.compare(coordinate(a.position, inner), coordinate(b.position, inner));
                if (c == 0) c = Integer
                    .compare(coordinate(a.position, axis) + (a.face & 1), coordinate(b.position, axis) + (b.face & 1));
                return c;
            });
            result.addAll(faces);
        }
        return result;
    }

    private static String update(ModernDecorationEngine engine, World world, Pos p, String state, int changedFace,
        String neighbor) {
        String block = ModernDecorationEngine.blockName(state);
        if (block.equals("minecraft:vine") && changedFace != 0) {
            String result = state;
            for (int face = 1; face < 6; face++) {
                if (!result.contains(FACES[face] + "=true")) continue;
                int[] d = OFFSETS[face];
                boolean supported = world.hasVineSupportFace(p.x + d[0], p.y + d[1], p.z + d[2], FACES[face ^ 1]);
                if (!supported && face != 1) {
                    String above = world.blockState(p.x, p.y + 1, p.z);
                    supported = ModernDecorationEngine.blockName(above)
                        .equals("minecraft:vine") && above.contains(FACES[face] + "=true");
                }
                if (!supported) result = ModernFeatureEngine.property(result, FACES[face], "false");
            }
            return result.contains("=true") ? result : "minecraft:air";
        }
        if (block.equals("regions_unexplored:kapok_vines_plant") && changedFace == 0
            && !ModernDecorationEngine.blockName(neighbor)
                .startsWith("regions_unexplored:kapok_vines"))
            return "regions_unexplored:kapok_vines[age=" + world.growingPlantHeadAge(p) + ']';
        if (block.equals("regions_unexplored:kapok_vines") && changedFace == 0
            && ModernDecorationEngine.blockName(neighbor)
                .startsWith("regions_unexplored:kapok_vines"))
            return "regions_unexplored:kapok_vines_plant";
        if (block.startsWith("regions_unexplored:") && block.endsWith("_branch")) {
            for (int face = 2; face < 6; face++) if (state.contains("facing=" + FACES[face])) {
                int[] d = OFFSETS[face ^ 1];
                String support = world.blockState(p.x + d[0], p.y, p.z + d[2]);
                if (!engine.isBlockInTag("regions_unexplored:supports/branches", support)
                    // Both source blocks have a narrow collision shape, despite their wood material.
                    || ModernDecorationEngine.blockName(support)
                        .equals("regions_unexplored:small_oak_log")
                    || ModernDecorationEngine.blockName(support)
                        .equals("regions_unexplored:bamboo_log")
                    || !world.hasSturdyFace(p.x + d[0], p.y, p.z + d[2], FACES[face])) return "minecraft:air";
            }
        }
        return state;
    }

    private static int distance(String state) {
        int at = state.indexOf("distance=");
        if (at < 0) return -1;
        int end = at + 9;
        while (end < state.length() && Character.isDigit(state.charAt(end))) end++;
        return Integer.parseInt(state.substring(at + 9, end));
    }

    private static boolean inside(Pos p, int[] lo, int[] hi) {
        return p.x >= lo[0] && p.y >= lo[1] && p.z >= lo[2] && p.x <= hi[0] && p.y <= hi[1] && p.z <= hi[2];
    }

    private static int coordinate(Pos p, int axis) {
        return axis == 0 ? p.x : axis == 1 ? p.y : p.z;
    }

    static final class Edge {

        final Pos position;
        final int face;

        Edge(Pos position, int face) {
            this.position = position;
            this.face = face;
        }
    }
}
