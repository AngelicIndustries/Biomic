/*
 * Source liquid/null height semantics from YUNG Better Caves 2.0.4 (LGPL-3.0),
 * e71f869808fc139773b75125919a65925a983a77. Basin sealing is an explicit Biomic adaptation.
 */
package industries.angelic.biomic.world.cave;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import industries.angelic.biomic.world.cave.CaveComposition.Material;
import industries.angelic.biomic.world.cave.YungWaterRegionSelector.Kind;

/**
 * Immutable source256 disposition of an ALREADY admitted whole-clearance mask.
 * Never floods original air or fluids. Unsafe wet cells remain original rock, not air.
 * Local deterministic sealing is not a claim of full source carver/global cave parity.
 */
public final class YungCaveLiquidPlan {

    @FunctionalInterface
    public interface Excavations {

        boolean at(int globalX, int sourceY, int globalZ);
    }

    public record Settings(int liquidAltitude) {

        public Settings {
            if (liquidAltitude < 0 || liquidAltitude > 254)
                throw new IllegalArgumentException("Liquid plan requires source256 plus64 frame");
        }

        public static Settings sourceDefaults() {
            return new Settings(10);
        }
    }

    private YungCaveLiquidPlan() {}

    /**
     * The cells of the mask that stay rock: wet cells whose liquid would not be sealed in.
     * Neighbor callback returns immutable PRE-liquid masks, never a recursively requested final plan.
     * Guard reads are at most one cell below/horizontally adjacent to the subject chunk.
     */
    public static BitSet retainedRock(int cx, int cz, BitSet sourceMask, CaveComposition.Host host,
        Excavations excavations, YungWaterRegionSelector selector, Settings settings) {
        Objects.requireNonNull(sourceMask);
        Objects.requireNonNull(host);
        Objects.requireNonNull(excavations);
        Objects.requireNonNull(selector);
        Objects.requireNonNull(settings);
        if (sourceMask.length() > 65536) throw new IllegalArgumentException("Excessive source cave mask");
        int ox = Math.multiplyExact(cx, 16), oz = Math.multiplyExact(cz, 16);
        Math.subtractExact(ox, 1);
        Math.addExact(ox, 16);
        Math.subtractExact(oz, 1);
        Math.addExact(oz, 16);
        var selections = new HashMap<Long, YungWaterRegionSelector.Selection>();
        BitSet retained = new BitSet(65536);
        for (int i = sourceMask.nextSetBit(0); i >= 0; i = sourceMask.nextSetBit(i + 1)) {
            int x = ox + i / 4096, z = oz + (i / 256) % 16, y = i % 256;
            if (host.at(x, y, z) != Material.ROCK || !excavations.at(x, y, z))
                throw new IllegalArgumentException("Liquid plan mask differs from immutable admitted rock excavation");
            if (y > settings.liquidAltitude()) continue;
            Kind kind = kind(selector, selections, x, z);
            if (kind == Kind.NONE || !sealed(x, y, z, kind, host, excavations, selector, selections, settings))
                retained.set(i);
        }
        return retained;
    }

    private static boolean sealed(int x, int y, int z, Kind kind, CaveComposition.Host host, Excavations excavations,
        YungWaterRegionSelector selector, Map<Long, YungWaterRegionSelector.Selection> selections, Settings settings) {
        return barrier(x, y - 1, z, kind, host, excavations, selector, selections, settings)
            && barrier(x - 1, y, z, kind, host, excavations, selector, selections, settings)
            && barrier(x + 1, y, z, kind, host, excavations, selector, selections, settings)
            && barrier(x, y, z - 1, kind, host, excavations, selector, selections, settings)
            && barrier(x, y, z + 1, kind, host, excavations, selector, selections, settings);
    }

    private static boolean barrier(int x, int y, int z, Kind kind, CaveComposition.Host host, Excavations excavations,
        YungWaterRegionSelector selector, Map<Long, YungWaterRegionSelector.Selection> selections, Settings settings) {
        if (y < 0 || y > 255) return false;
        Material before = host.at(x, y, z);
        if (before != Material.ROCK && before != Material.SOLID) return false;
        if (!excavations.at(x, y, z)) return true;
        // Only ROCK is legally excavated. A bad neighboring callback cannot authorize a fluid edge.
        if (before != Material.ROCK || y > settings.liquidAltitude()) return false;
        Kind neighbor = kind(selector, selections, x, z);
        // NONE stays original rock. Same-fluid neighbors become that fluid OR safely retained rock.
        // Opposite fluid edges are retained on BOTH sides, independently of evaluation order.
        return neighbor == Kind.NONE || neighbor == kind;
    }

    private static Kind kind(YungWaterRegionSelector selector, Map<Long, YungWaterRegionSelector.Selection> selections,
        int x, int z) {
        int cx = Math.floorDiv(x, 16), cz = Math.floorDiv(z, 16);
        long key = ((long) cx << 32) | (cz & 0xffffffffL);
        return selections.computeIfAbsent(key, k -> selector.chunk(cx, cz))
            .at(Math.floorMod(x, 16), Math.floorMod(z, 16));
    }
}
