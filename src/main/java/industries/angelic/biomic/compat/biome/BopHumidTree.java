package industries.angelic.biomic.compat.biome;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

/** A complete native tree, including its vines/cocoa. Earlier tree profiles retain their old path. */
final class BopHumidTree {

    private record Cell(Block block, int metadata) {}

    private BopHumidTree() {}

    static boolean place(World world, NativeFeatureCapture.Access physical, Random random, WorldGenAbstractTree tree,
        int x, int y, int z) {
        return place(world, physical, random, tree, x, y, z, Blocks.vine, false, false);
    }

    static boolean placeFinal(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenAbstractTree tree, int x, int y, int z) {
        return place(world, physical, random, tree, x, y, z, Blocks.vine, false, true);
    }

    static boolean place(World world, NativeFeatureCapture.Access physical, Random random, WorldGenAbstractTree tree,
        int x, int y, int z, Block vines) {
        return place(world, physical, random, tree, x, y, z, vines, false, false);
    }

    static boolean placeKeepingCanopy(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenAbstractTree tree, int x, int y, int z, Block vines) {
        return place(world, physical, random, tree, x, y, z, vines, true, false);
    }

    private static boolean place(World world, NativeFeatureCapture.Access physical, Random random,
        WorldGenAbstractTree tree, int x, int y, int z, Block vines, boolean preserveExistingCanopy,
        boolean finalGroundScan) {
        Map<Pos, Cell> proposal = new LinkedHashMap<>();
        var buffered = new NativeFeatureCapture.Access() {

            public Block block(int px, int py, int pz) {
                Cell cell = proposal.get(new Pos(px, py, pz));
                return cell == null ? physical.block(px, py, pz) : cell.block;
            }

            public int metadata(int px, int py, int pz) {
                Cell cell = proposal.get(new Pos(px, py, pz));
                return cell == null ? physical.metadata(px, py, pz) : cell.metadata;
            }

            public int heightValue(int px, int pz) {
                return physical.heightValue(px, pz);
            }

            public int minimumY() {
                return physical.minimumY();
            }

            public int maximumYExclusive() {
                return physical.maximumYExclusive();
            }

            public boolean set(int px, int py, int pz, Block block, int meta) {
                proposal.put(new Pos(px, py, pz), new Cell(block, meta));
                return true;
            }
        };
        // Native swamp trees descend through water before rejecting depths >1.
        // Allow that read over a12-deep admitted bed, without granting deeper writes.
        boolean placed = finalGroundScan ? BopWholeTree.placeFinal(world, buffered, random, tree, x, y, z)
            : BopWholeTree.place(world, buffered, random, tree, x, y, z, preserveExistingCanopy ? 16 : 8);
        if (!placed) return false;
        for (var iterator = proposal.entrySet()
            .iterator(); iterator.hasNext();) {
            var entry = iterator.next();
            Pos p = entry.getKey();
            Cell cell = entry.getValue();
            if (cell.block == vines) {
                // An ideal source tree can propose ivy where another tree already has
                // leaves. Replacing that canopy may strand the older tree's vines.
                // New wetland profiles reject the complete tree, never clip its ivy.
                if (preserveExistingCanopy && physical.block(p.x, p.y, p.z) != Blocks.air) return false;
                // The source bulb tree can leave detached canopy vines. Keep all real tree
                // geometry and valid faces; do not port vines that disappear on the next update.
                int faces = BopTreeAttachments.supportedVineFaces(buffered, p.x, p.y, p.z, cell.metadata, vines);
                if (faces == 0) iterator.remove();
                else if (faces != cell.metadata) entry.setValue(new Cell(vines, faces));
            }
            if (cell.block == Blocks.cocoa
                && !BopTreeAttachments.cocoaSupported(buffered, p.x, p.y, p.z, cell.metadata)) return false;
        }
        // The caller still owns the final habitat/ceiling transaction and must discard it on failure.
        for (var entry : proposal.entrySet()) {
            Pos p = entry.getKey();
            Cell cell = entry.getValue();
            if (!physical.set(p.x, p.y, p.z, cell.block, cell.metadata)) return false;
        }
        return true;
    }
}
