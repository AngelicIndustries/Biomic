package industries.angelic.biomic.compat.biome;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** New native tree families: retain their complete source geometry or reject the whole proposal. */
public final class BopWholeTree {

    private record Pos(int x, int y, int z) {}

    private record Cell(Block block, int metadata) {}

    /** A tree that reaches outside its audited view is rejected whole; it never crashes population. */
    private BopWholeTree() {}

    static boolean visibleIdealCanopy(Block block) {
        return block.getMaterial() == Material.leaves;
    }

    /** The caller must buffer physical.set writes and discard them if placement returns false or throws. */
    public static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenAbstractTree tree, int x, int y, int z) {
        return place(world, physical, random, tree, x, y, z, 8);
    }

    /** New final-profile trees only: reject a source ground scan that cannot fit this bounded view. */
    public static boolean placeFinal(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenAbstractTree tree, int x, int y, int z) {
        return place(world, physical, random, tree, x, y, z, 8);
    }

    static boolean place(World world, NativeFeatureCapture.Access physical, Random random, WorldGenAbstractTree tree,
        int x, int y, int z, int belowReadLimit) {
        if (belowReadLimit != 8 && belowReadLimit != 16)
            throw new IllegalArgumentException("Unaudited native tree read depth");
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var ideal = new NativeFeatureCapture.Access() {

            private void check(int px, int py, int pz, boolean read) {
                if (Math.abs((long) px - x) > 32 || Math.abs((long) pz - z) > 32
                    || py < y - (long) belowReadLimit
                    || py > y + 64L) {
                    // Reads past the view are ground and water scans (shrubs on steep hosts, swamp trees in deep
                    // pools); a write past it is a tree bigger than the view. Either way the tree goes whole.
                    throw NativeFeatureCapture.outside(
                        "native tree " + tree.getClass()
                            .getName() + (read ? " read" : " wrote") + " outside its audited view");
                }
            }

            public Block block(int px, int py, int pz) {
                check(px, py, pz, true);
                Cell cell = proposal.get(new Pos(px, py, pz));
                if (cell != null) return cell.block;
                Block existing = physical.block(px, py, pz);
                // Native bog trees explicitly admit existing leaves in their clearance
                // scan, then test real air before starting each hanging-vine chain. Keep
                // those leaves visible to the proposal so a canopy collision suppresses
                // only the vine chain that native BOP would not have emitted. Other host
                // obstacles remain idealized and are checked against the whole footprint.
                return py >= y && !visibleIdealCanopy(existing) ? Blocks.air : existing;
            }

            public int metadata(int px, int py, int pz) {
                check(px, py, pz, true);
                Cell cell = proposal.get(new Pos(px, py, pz));
                if (cell != null) return cell.metadata;
                Block existing = physical.block(px, py, pz);
                return py >= y && !visibleIdealCanopy(existing) ? 0 : physical.metadata(px, py, pz);
            }

            public boolean set(int px, int py, int pz, Block block, int metadata) {
                check(px, py, pz, false);
                proposal.put(new Pos(px, py, pz), new Cell(block, metadata));
                return true;
            }

            public int heightValue(int px, int pz) {
                check(px, y, pz, true);
                return y;
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }
        };
        tree.setScale(1, 1, 1);
        var capture = tree.getClass()
            .getName()
            .equals("biomesoplenty.common.world.features.trees.WorldGenSacredOak")
                ? BopSurfaceCapture.sacredOak(ideal, random)
                : new BopSurfaceCapture(ideal, random);
        boolean generated;
        try {
            generated = NativeFeatureCapture.run(world, capture, () -> tree.generate(world, random, x, y, z));
        } catch (NativeFeatureCapture.OutsideEnvelope outside) {
            return false;
        }
        if (!generated || proposal.isEmpty()) return false;
        // Height is deliberately the current native-generator budget, not a new 512-height claim.
        // Actual per-surface ceilings, habitat boundaries and mushroom ownership are enforced by
        // the caller's transaction when applying the proposal. Never commit a clipped prefix.
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            if (!NativeFeatureCapture.containsFeatureY(physical, p.y)) return false;
            if (p.y >= y) {
                Block existing = physical.block(p.x, p.y, p.z);
                if (existing != Blocks.air && existing.getMaterial() != Material.leaves) return false;
            }
        }
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell c = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, c.block, c.metadata)) return false;
        }
        return true;
    }
}
