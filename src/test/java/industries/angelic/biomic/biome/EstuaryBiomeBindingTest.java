package industries.angelic.biomic.biome;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.config.BiomeConfig;
import industries.angelic.biomic.config.WorldgenConfigSnapshot;

/** Detached identity/climate contract only; actual native registry delivery remains a sandbox gate. */
public class EstuaryBiomeBindingTest {

    @Test
    public void extraNativeIdDoesNotRewriteHistoricalGenerator77Signature() {
        int original = BiomeConfig.estuaryId;
        String before = WorldgenConfigSnapshot.current()
            .getSignature();
        try {
            BiomeConfig.estuaryId = original + 1;
            assertEquals(
                before,
                WorldgenConfigSnapshot.current()
                    .getSignature());
        } finally {
            BiomeConfig.estuaryId = original;
        }
    }
}
