package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import org.junit.Test;

public class HostedIslandBiomesTest {

    @Test
    public void anyHabitatRoundTripsThroughItsStoredKey() {
        for (String habitat : new String[] { HostedIslandBiomes.REDWOODS, "minecraft:forest",
            "regions_unexplored:tundra", "minecraft:snowy_plains", "minecraft:cherry_grove" }) {
            String stored = HostedIslandBiomes.storedKey(habitat);
            assertTrue(stored.startsWith("biomic:hosted/"));
            assertEquals(habitat, HostedIslandBiomes.modernKey(stored));
        }
    }

    @Test
    public void otherKeysAreNotHosted() {
        assertNull(HostedIslandBiomes.modernKey("regions_unexplored:redwoods"));
        assertNull(HostedIslandBiomes.modernKey("biomic:hosted/broken"));
        assertNull(HostedIslandBiomes.modernKey("biomic:hosted/a/b/c"));
    }

    @Test
    public void islandKindsFollowTheHabitat() {
        assertEquals(IslandKind.GREEN, IslandKind.of("regions_unexplored:maple_forest"));
        assertEquals(IslandKind.LUSH, IslandKind.of("regions_unexplored:wisteria_grove"));
        assertEquals(IslandKind.SNOWY, IslandKind.of("minecraft:snowy_slopes"));
        assertEquals(IslandKind.SNOWY, IslandKind.of("minecraft:grove"));
        assertEquals(IslandKind.DRY, IslandKind.of("minecraft:desert"));
        assertEquals(IslandKind.DRY, IslandKind.of("minecraft:stony_peaks"));
        assertEquals(IslandKind.GREEN, IslandKind.of("regions_unexplored:highland_fields"));
    }
}
