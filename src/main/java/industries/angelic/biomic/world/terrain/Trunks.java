package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;

/** Tree trunks rising from a habitat's ground in a chunk: what bee nests, fruit vines and epiphytes hang on. */
final class Trunks {

    /** Facing (ForgeDirection ordinal) by horizontal step: north, south, west, east. */
    static final int[][] SIDES = { { 0, -1, 2 }, { 0, 1, 3 }, { -1, 0, 4 }, { 1, 0, 5 } };

    record Trunk(int x, int z, int ground, int top, Block log) {}

    private Trunks() {}

    /** Trunks at least four logs high, in the order of {@code order} (a hash of the column). */
    static List<Trunk> find(OrigenFeatureRuntime.PhysicalPopulation physical, List<FloraLedger.Column> columns,
        ToLongFunction<FloraLedger.Column> order) {
        List<Trunk> trunks = new ArrayList<>();
        List<Long> keys = new ArrayList<>();
        for (FloraLedger.Column column : columns) {
            int x = column.x(), z = column.z(), ground = column.floorY();
            Block log = physical.block(x, ground + 1, z);
            if (log.getMaterial() != Material.wood || !(log instanceof BlockLog)) continue;
            int top = ground + 1;
            while (top < ground + 24 && physical.block(x, top + 1, z) == log) top++;
            if (top - ground < 4) continue;
            trunks.add(new Trunk(x, z, ground, top, log));
            keys.add(order.applyAsLong(column));
        }
        List<Integer> index = new ArrayList<>();
        for (int i = 0; i < trunks.size(); i++) index.add(i);
        index.sort((a, b) -> Long.compare(keys.get(a), keys.get(b)));
        List<Trunk> sorted = new ArrayList<>(trunks.size());
        for (int i : index) sorted.add(trunks.get(i));
        return sorted;
    }
}
