package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

public class HostedTreeJournalValidationTest {

    private static final String AIR = "minecraft:air", WOOD = "minecraft:oak_log[axis=y]",
        LEAF = "minecraft:oak_leaves";

    @Test
    public void compactPayloadCannotFallBackToLegacyWhenMarkerIsMissingOrWrongType() {
        NBTTagCompound missing = compactTwoLayers();
        missing.removeTag("cell_format");
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(missing));
        NBTTagCompound wrong = compactTwoLayers();
        wrong.setString("cell_format", "2");
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(wrong));
    }

    @Test
    public void legacyRejectsEmptyStatesAndLayerChains() {
        NBTTagCompound emptyBase = legacyTwoLayers();
        emptyBase.getTagList("cells", 10)
            .getCompoundTagAt(0)
            .setString("base", "");
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(emptyBase));
        NBTTagCompound emptyState = legacyTwoLayers();
        emptyState.getTagList("cells", 10)
            .getCompoundTagAt(0)
            .getTagList("layers", 10)
            .getCompoundTagAt(0)
            .setString("state", "");
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(emptyState));
        NBTTagCompound emptyLayers = legacyTwoLayers();
        emptyLayers.getTagList("cells", 10)
            .getCompoundTagAt(0)
            .setTag("layers", new NBTTagList());
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(emptyLayers));
    }

    @Test
    public void legacyRejectsDuplicateCellsAndNonChronologicalLayers() {
        NBTTagCompound treeOrder = legacyTwoLayers();
        NBTTagList originalTrees = treeOrder.getTagList("trees", 10);
        NBTTagList swappedTrees = new NBTTagList();
        swappedTrees.appendTag(
            (NBTTagCompound) originalTrees.getCompoundTagAt(1)
                .copy());
        swappedTrees.appendTag(
            (NBTTagCompound) originalTrees.getCompoundTagAt(0)
                .copy());
        treeOrder.setTag("trees", swappedTrees);
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(treeOrder));
        NBTTagCompound duplicate = legacyTwoLayers();
        NBTTagList cells = duplicate.getTagList("cells", 10);
        cells.appendTag(
            (NBTTagCompound) cells.getCompoundTagAt(0)
                .copy());
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(duplicate));
        NBTTagCompound reversed = legacyTwoLayers();
        NBTTagList layers = reversed.getTagList("cells", 10)
            .getCompoundTagAt(0)
            .getTagList("layers", 10);
        NBTTagCompound first = (NBTTagCompound) layers.getCompoundTagAt(0)
            .copy();
        NBTTagCompound second = (NBTTagCompound) layers.getCompoundTagAt(1)
            .copy();
        NBTTagList swapped = new NBTTagList();
        swapped.appendTag(second);
        swapped.appendTag(first);
        reversed.getTagList("cells", 10)
            .getCompoundTagAt(0)
            .setTag("layers", swapped);
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(reversed));
    }

    @Test
    public void compactRejectsNonChronologicalLayerIndices() {
        NBTTagCompound compact = compactTwoLayers();
        int[] page = compact.getTagList("cell_pages", 11)
            .func_150306_c(0);
        assertEquals(2, page[4]);
        assertEquals(0, page[5]);
        assertEquals(1, page[7]);
        page[5] = 1;
        page[7] = 0;
        assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(compact));
    }

    @Test
    public void invalidNextIdsAndRuntimeOverflowAreRejected() throws Exception {
        for (long next : new long[] { -1, Long.MAX_VALUE }) {
            NBTTagCompound root = new NBTTagCompound();
            root.setLong("next", next);
            assertThrows(IllegalStateException.class, () -> new HostedTreeLedger().load(root));
        }
        HostedTreeLedger ledger = new HostedTreeLedger();
        Field next = HostedTreeLedger.class.getDeclaredField("nextId");
        next.setAccessible(true);
        next.setLong(ledger, Long.MAX_VALUE - 1);
        assertThrows(
            IllegalStateException.class,
            () -> ledger.commit(Map.of(new Pos(0, 64, 0), WOOD), new Grid(), (x, z) -> false));
    }

    private static NBTTagCompound compactTwoLayers() {
        Grid grid = new Grid();
        HostedTreeLedger ledger = new HostedTreeLedger();
        ledger.commit(Map.of(new Pos(0, 64, 0), LEAF), grid, (x, z) -> false);
        ledger.commit(Map.of(new Pos(0, 64, 0), WOOD), grid, (x, z) -> false);
        return ledger.save();
    }

    private static NBTTagCompound legacyTwoLayers() {
        NBTTagCompound root = new NBTTagCompound();
        root.setLong("next", 2);
        NBTTagList trees = new NBTTagList();
        trees.appendTag(tree(1));
        trees.appendTag(tree(2));
        root.setTag("trees", trees);
        NBTTagCompound cell = new NBTTagCompound();
        cell.setIntArray("pos", new int[] { 0, 64, 0 });
        cell.setString("base", AIR);
        NBTTagList layers = new NBTTagList();
        layers.appendTag(layer(1, LEAF));
        layers.appendTag(layer(2, WOOD));
        cell.setTag("layers", layers);
        NBTTagList cells = new NBTTagList();
        cells.appendTag(cell);
        root.setTag("cells", cells);
        return root;
    }

    private static NBTTagCompound tree(long id) {
        NBTTagCompound tree = new NBTTagCompound();
        tree.setLong("id", id);
        tree.setIntArray("bounds", new int[] { -1, 64, -1, 1, 65, 1 });
        tree.setIntArray("pending", new int[] { 0, 0 });
        return tree;
    }

    private static NBTTagCompound layer(long id, String state) {
        NBTTagCompound layer = new NBTTagCompound();
        layer.setLong("id", id);
        layer.setString("state", state);
        return layer;
    }

    private static final class Grid implements HostedTreeLedger.Access {

        private final Map<Pos, String> blocks = new HashMap<>();

        public String read(Pos pos) {
            return blocks.getOrDefault(pos, AIR);
        }

        public void write(Pos pos, String state) {
            blocks.put(pos, state);
        }
    }
}
