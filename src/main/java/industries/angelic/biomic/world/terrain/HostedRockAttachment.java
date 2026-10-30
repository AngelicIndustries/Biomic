package industries.angelic.biomic.world.terrain;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

/** Checks a buffered rock against pre-decoration terrain, never against its own staged blocks. */
public final class HostedRockAttachment {

    private static final int[][] FACES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private HostedRockAttachment() {}

    /** Every face-connected component needs terrain contact. Corner/edge contact is not attachment. */
    public static boolean attached(Set<Pos> footprint, Predicate<Pos> terrainSupport) {
        Set<Pos> remaining = new HashSet<>(footprint);
        ArrayDeque<Pos> queue = new ArrayDeque<>();
        while (!remaining.isEmpty()) {
            Pos start = remaining.iterator()
                .next();
            remaining.remove(start);
            queue.add(start);
            boolean anchored = false;
            while (!queue.isEmpty()) {
                Pos p = queue.removeFirst();
                // A rock embedded in the soil may replace its anchor, so inspect the original
                // terrain at the write itself too. The predicate must not read buffered writes.
                anchored |= terrainSupport.test(p);
                for (int[] face : FACES) {
                    Pos neighbor = p.offset(face[0], face[1], face[2]);
                    if (remaining.remove(neighbor)) queue.addLast(neighbor);
                    if (!footprint.contains(neighbor)) anchored |= terrainSupport.test(neighbor);
                }
            }
            if (!anchored) return false;
        }
        return true;
    }

    public static boolean solidTerrain(byte material) {
        return material != BiomicTerrainSampler.AIR && material != BiomicTerrainSampler.WATER
            && material != BiomicTerrainSampler.LAVA
            && material != BiomicTerrainSampler.POWDER_SNOW;
    }
}
