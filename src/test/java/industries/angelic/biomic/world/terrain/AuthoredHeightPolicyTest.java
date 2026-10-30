package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import org.junit.Test;

public class AuthoredHeightPolicyTest {

    @Test
    public void missingRevisionAlwaysSelectsLegacyWithoutInferringFromStorageOrCapability() {
        var at256 = AuthoredHeightPolicy.select(null, 256, false);
        var capable512 = AuthoredHeightPolicy.select(null, 512, true);
        assertSame(at256, capable512);
        assertEquals(AuthoredHeightPolicy.LEGACY_COMPRESSED_REVISION, at256.revision());
        assertEquals(256, at256.requiredPhysicalStorage());
        assertEquals(1, at256.biomeVolumeSchema());
        assertFalse(at256.shifted512());
    }

    @Test
    public void shiftedRevisionRequiresBothExplicitStorageAndSchemaCapability() {
        assertThrows(
            IllegalStateException.class,
            () -> AuthoredHeightPolicy.select(AuthoredHeightPolicy.SHIFTED_512_REVISION, 256, true));
        assertThrows(
            IllegalStateException.class,
            () -> AuthoredHeightPolicy.select(AuthoredHeightPolicy.SHIFTED_512_REVISION, 512, false));
        var shifted = AuthoredHeightPolicy.select(AuthoredHeightPolicy.SHIFTED_512_REVISION, 512, true);
        assertTrue(shifted.shifted512());
        assertEquals(512, shifted.requiredPhysicalStorage());
        assertEquals(2, shifted.biomeVolumeSchema());
    }

    @Test
    public void explicitUnknownRevisionAndUnknownStorageFailWithoutFallback() {
        assertThrows(IllegalArgumentException.class, () -> AuthoredHeightPolicy.select("0.0.1", 512, true));
        assertThrows(IllegalArgumentException.class, () -> AuthoredHeightPolicy.select("", 512, true));
        assertThrows(IllegalArgumentException.class, () -> AuthoredHeightPolicy.select(null, 384, false));
    }
}
