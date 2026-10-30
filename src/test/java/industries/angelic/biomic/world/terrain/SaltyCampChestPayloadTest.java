package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.Test;

import industries.angelic.biomic.compat.biome.SaltyBrickmakerCamp;

public class SaltyCampChestPayloadTest {

    @Test
    public void exactChestPayloadSurvivesPendingCodecAndIsDefensivelyCopied() {
        NBTTagCompound source = chest(10, 70, -4, 3);
        var block = new OrigenFeatureRuntime.PendingBlock("minecraft:chest[facing=north]", false, null, source);
        source.setInteger("x", 999);
        assertEquals(
            10,
            block.chestNbt()
                .getInteger("x"));
        NBTTagCompound encoded = new NBTTagCompound();
        OrigenFeatureRuntime.writePendingBlock(encoded, block);
        var decoded = OrigenFeatureRuntime.readPendingBlock(encoded);
        assertEquals(
            "Chest",
            decoded.chestNbt()
                .getString("id"));
        assertEquals(
            3,
            decoded.chestNbt()
                .getTagList("Items", 10)
                .getCompoundTagAt(0)
                .getByte("Slot"));
        decoded.chestNbt()
            .setInteger("x", 123);
        assertEquals(
            10,
            decoded.chestNbt()
                .getInteger("x"));
    }

    @Test
    public void malformedOrMismatchedChestPayloadsAreRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> OrigenFeatureRuntime.validateChestPayload("minecraft:chest", chest(1, 2, 3, 0), 9, 2, 3));
        assertThrows(
            IllegalArgumentException.class,
            () -> OrigenFeatureRuntime.validateChestPayload("minecraft:trapped_chest", chest(1, 2, 3, 0), 1, 2, 3));
        NBTTagCompound duplicate = chest(1, 2, 3, 0);
        duplicate.getTagList("Items", 10)
            .appendTag(item(0));
        assertThrows(
            IllegalArgumentException.class,
            () -> OrigenFeatureRuntime.validateChestPayload("minecraft:chest", duplicate, 1, 2, 3));
    }

    @Test
    public void wholeCampValidationRunsBeforeAnyPhysicalWrite() {
        var writes = new LinkedHashMap<SaltyBrickmakerCamp.Position, SaltyBrickmakerCamp.Placement>();
        writes.put(
            new SaltyBrickmakerCamp.Position(0, 64, 0),
            new SaltyBrickmakerCamp.Placement("minecraft:wool[color=white]", null));
        writes.put(
            new SaltyBrickmakerCamp.Position(1, 64, 0),
            new SaltyBrickmakerCamp.Placement("minecraft:chest", chest(99, 64, 0, 0)));
        int[] committed = { 0 };
        OrigenFeatureRuntime.PhysicalPopulation physical = new OrigenFeatureRuntime.PhysicalPopulation() {

            public boolean isMaterialized(int x, int y, int z) {
                return true;
            }

            public String blockState(int x, int y, int z) {
                return "minecraft:air";
            }

            public byte rawMaterial(int x, int y, int z) {
                return 0;
            }

            public void setBlock(int x, int y, int z, String state) {
                committed[0]++;
            }

            public void setChest(int x, int y, int z, String state, NBTTagCompound nbt) {
                committed[0]++;
            }
        };
        assertThrows(
            IllegalArgumentException.class,
            () -> physical.commitSaltyCamp(new SaltyBrickmakerCamp.Plan(writes)));
        assertEquals(0, committed[0]);
    }

    private static NBTTagCompound chest(int x, int y, int z, int slot) {
        NBTTagCompound result = new NBTTagCompound();
        result.setString("id", "Chest");
        result.setInteger("x", x);
        result.setInteger("y", y);
        result.setInteger("z", z);
        NBTTagList items = new NBTTagList();
        items.appendTag(item(slot));
        result.setTag("Items", items);
        return result;
    }

    private static NBTTagCompound item(int slot) {
        NBTTagCompound item = new NBTTagCompound();
        item.setByte("Slot", (byte) slot);
        return item;
    }
}
