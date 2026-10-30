package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class BopNativeNetherDecoratorTest {

    @Test
    public void pendingWaspSpawnerPayloadSurvivesNbtAndRejectsWrongTargets() {
        var pending = new OrigenFeatureRuntime.PendingBlock("minecraft:spawner", false, "BiomesOPlenty.Wasp");
        NBTTagCompound nbt = new NBTTagCompound();
        OrigenFeatureRuntime.writePendingBlock(nbt, pending);
        assertEquals(pending, OrigenFeatureRuntime.readPendingBlock(nbt));

        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setString("state", "minecraft:netherrack");
        assertNull(
            OrigenFeatureRuntime.readPendingBlock(legacy)
                .spawnerEntity());
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrigenFeatureRuntime.PendingBlock("minecraft:netherrack", false, "BiomesOPlenty.Wasp"));
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrigenFeatureRuntime.PendingBlock("minecraft:spawner", false, "minecraft:blaze"));
    }
}
