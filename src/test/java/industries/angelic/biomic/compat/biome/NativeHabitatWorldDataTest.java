package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class NativeHabitatWorldDataTest {

    @Test
    public void reloadNeverReselects() {
        assertEquals(
            NativeHabitatSettings.OFF,
            NativeHabitatWorldData.initialSettings(true, false, true, true, false, List.of(BopNativeBiomes.GARDEN)));
    }

    @Test
    public void tallWorldsPinTheShiftedRevision() {
        var selected = NativeHabitatWorldData.initialSettings(true, true, true, false, false, List.of());
        assertTrue(selected.shifted512());
        assertTrue(selected.saltMarsh());
        assertFalse(selected.bop());
    }

    @Test
    public void absentOptionalProvidersProduceAnExplicitOffManifest() {
        var selected = NativeHabitatWorldData.initialSettings(false, true, false, true, false, List.of());
        assertFalse(selected.shifted512());
        assertFalse(selected.saltMarsh());
        assertFalse(selected.bop());
    }
}
