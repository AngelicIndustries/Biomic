package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;

import org.junit.Test;

public class SaltyCampStateResolverTest {

    private final OrigenBlockStateResolver resolver = new OrigenBlockStateResolver();

    @Test
    public void resolvesEveryMetadataBearingNativeCampStateExactly() {
        assertMetadata("minecraft:wool", 0, 7, 8, 12, 15);
        assertMetadata("minecraft:bed", 0, 1, 2, 3, 8, 9, 10, 11);
        assertMetadata("minecraft:chest", 2, 3, 4, 5);
        assertMetadata("minecraft:fence_gate", 0, 1, 2, 3);
        assertMetadata("minecraft:hay_block", 0, 4, 8);
        assertMetadata("minecraft:wooden_slab", 0);
    }

    @Test
    public void rejectsMalformedOrOutOfPaletteCampStates() {
        for (String state : List.of(
            "minecraft:wool",
            "minecraft:wool[legacy_meta=1]",
            "minecraft:wool[legacy_meta=16]",
            "minecraft:wool[legacy_meta=nope]",
            "minecraft:bed[legacy_meta=4]",
            "minecraft:bed[legacy_meta=0,facing=north]",
            "minecraft:fence_gate[legacy_meta=4]",
            "minecraft:hay_block[legacy_meta=1]",
            "minecraft:wooden_slab[legacy_meta=8]"))
            assertThrows(IllegalArgumentException.class, () -> resolver.resolve(state));
    }

    private void assertMetadata(String id, int... values) {
        for (int metadata : values) assertEquals(
            metadata,
            resolver.resolve(id + "[legacy_meta=" + metadata + "]")
                .metadata());
    }
}
