package industries.angelic.biomic.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Test;

public class BiomicLegacyStatesTest {

    @Test
    public void plainBlocksRoundTripWithTheirMetadata() {
        Block block = new Block(Material.rock) {};
        BiomicLegacyStates.register("test_legacy_state", block);
        assertEquals("biomic:test_legacy_state[legacy_meta=7]", BiomicLegacyStates.encode(block, 7));
        BiomicLegacyStates.Resolved resolved = BiomicLegacyStates.resolve("biomic:test_legacy_state[legacy_meta=7]");
        assertSame(block, resolved.block());
        assertEquals(7, resolved.metadata());
        assertNull(BiomicLegacyStates.resolve("biomic:test_legacy_state[legacy_meta=16]"));
        assertNull(BiomicLegacyStates.resolve("biomic:unknown_block"));
        assertNull(BiomicLegacyStates.encode(block, 16));
    }
}
