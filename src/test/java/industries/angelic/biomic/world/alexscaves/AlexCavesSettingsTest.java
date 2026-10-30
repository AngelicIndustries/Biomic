package industries.angelic.biomic.world.alexscaves;

import static org.junit.Assert.*;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class AlexCavesSettingsTest {

    @Test
    public void saveRoundTripPinsProviderAndRejectsDrift() {
        for (var settings : new AlexCavesSettings[] { AlexCavesSettings.OFF,
            new AlexCavesSettings(AlexCavesSettings.FORLORN_V1, AlexCavesSettings.NONE, AlexCavesSettings.NONE),
            new AlexCavesSettings(AlexCavesSettings.NONE, AlexCavesSettings.PRIMORDIAL_V1, AlexCavesSettings.NONE),
            new AlexCavesSettings(AlexCavesSettings.NONE, AlexCavesSettings.NONE, AlexCavesSettings.ABYSSAL_V1),
            new AlexCavesSettings(
                AlexCavesSettings.NONE,
                AlexCavesSettings.PRIMORDIAL_V2,
                AlexCavesSettings.ABYSSAL_V2),
            new AlexCavesSettings(
                AlexCavesSettings.NONE,
                AlexCavesSettings.PRIMORDIAL_V3,
                AlexCavesSettings.ABYSSAL_V2),
            new AlexCavesSettings(
                AlexCavesSettings.NONE,
                AlexCavesSettings.NONE,
                AlexCavesSettings.ABYSSAL_NATURAL_V3) }) {
            NBTTagCompound nbt = new NBTTagCompound();
            settings.write(nbt);
            assertEquals(settings, AlexCavesSettings.read(nbt));
        }
        assertThrows(IllegalArgumentException.class, () -> AlexCavesSettings.read(new NBTTagCompound()));
        assertThrows(
            IllegalArgumentException.class,
            () -> new AlexCavesSettings("alexscaves:future", AlexCavesSettings.NONE));
        NBTTagCompound future = new NBTTagCompound();
        future.setInteger("schema", 4);
        future.setString("forlornProvider", AlexCavesSettings.FORLORN_V1);
        future.setString("primordialProvider", AlexCavesSettings.NONE);
        assertThrows(IllegalArgumentException.class, () -> AlexCavesSettings.read(future));
    }

    @Test
    public void legacyManifestsRemainReadable() {
        NBTTagCompound legacy = new NBTTagCompound();
        legacy.setInteger("schema", 1);
        legacy.setString("forlornProvider", AlexCavesSettings.FORLORN_V1);
        AlexCavesSettings imported = AlexCavesSettings.read(legacy);
        assertTrue(imported.forlornEnabled());
        assertFalse(imported.primordialEnabled());
        assertFalse(imported.abyssalEnabled());
        NBTTagCompound old = new NBTTagCompound();
        old.setInteger("schema", 2);
        old.setString("forlornProvider", AlexCavesSettings.NONE);
        old.setString("primordialProvider", AlexCavesSettings.PRIMORDIAL_V1);
        imported = AlexCavesSettings.read(old);
        assertTrue(imported.primordialEnabled());
        assertFalse(imported.abyssalEnabled());
    }

    @Test
    public void freshWorldsSelectOnlyTheOptedInChunkTrench() {
        for (boolean fresh : new boolean[] { false, true }) for (boolean blend : new boolean[] { false, true })
            for (boolean abyssal : new boolean[] { false, true }) for (boolean tall : new boolean[] { false, true }) {
                var selected = AlexCavesSettings.select(fresh, blend, abyssal, tall);
                assertFalse(selected.forlornEnabled());
                assertFalse(selected.primordialEnabled());
                if (!(fresh && blend && abyssal)) assertEquals(AlexCavesSettings.OFF, selected);
                else assertEquals(
                    tall ? AlexCavesSettings.ABYSSAL_SHIFTED512 : AlexCavesSettings.ABYSSAL_CHUNK,
                    selected.abyssalProvider());
            }
        assertThrows(
            IllegalArgumentException.class,
            () -> new AlexCavesSettings(
                AlexCavesSettings.NONE,
                AlexCavesSettings.PRIMORDIAL_V3,
                AlexCavesSettings.ABYSSAL_NATURAL_V3));
    }
}
