package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class BopFinalSpongeCodecTest {

    // Block identity/roundtrips require FML and are checked in BopFinalSourceAudit.
    @Test
    public void requestMetadataIsDistinctAndUnknownStatesAreRejected() {
        assertEquals(0, OrigenBlockStateResolver.spongeMetadata("minecraft:sponge"));
        assertEquals(1, OrigenBlockStateResolver.spongeMetadata("minecraft:wet_sponge"));
        assertThrows(IllegalArgumentException.class, () -> OrigenBlockStateResolver.spongeMetadata("etfuturum:sponge"));
    }
}
