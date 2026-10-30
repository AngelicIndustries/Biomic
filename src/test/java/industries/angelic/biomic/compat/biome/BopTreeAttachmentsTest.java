package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import org.junit.Test;

public class BopTreeAttachmentsTest {

    @Test
    public void everyVineFaceAndCocoaAgeFacingIsEncodedExplicitly() {
        for (int mask = 1; mask < 16; mask++) {
            String state = BopTreeAttachments.vineState(mask);
            String[] names = { "south", "west", "north", "east" };
            for (int side = 0; side < 4; side++)
                assertTrue(state.contains(names[side] + "=" + ((mask & (1 << side)) != 0)));
        }
        for (int age = 0; age < 3; age++) for (int face = 0; face < 4; face++) assertEquals(
            "minecraft:cocoa[age=" + age + ",facing=" + new String[] { "south", "east", "north", "west" }[face] + "]",
            BopTreeAttachments.cocoaState(age * 4 + face));
    }

    @Test
    public void invalidAndCeilingOnlyAttachmentsAreRejected() {
        for (int mask : new int[] { -1, 0, 16, 31 })
            assertThrows(IllegalArgumentException.class, () -> BopTreeAttachments.vineState(mask));
        for (int meta : new int[] { -1, 12, 15, 16 })
            assertThrows(IllegalArgumentException.class, () -> BopTreeAttachments.cocoaState(meta));
    }
}
