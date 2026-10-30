package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import org.junit.Test;

public class AbyssalDeepOnePaletteTest {

    @Test
    public void vanillaArchitecturalStatesUseExactEfrHolderKeysWithoutInitializingForge() {
        var dark = OrigenBlockStateResolver.abyssalArchitectureKey("minecraft:dark_prismarine");
        assertEquals("PRISMARINE_BLOCK", dark.holder());
        assertEquals(2, dark.metadata());
        var lantern = OrigenBlockStateResolver.abyssalArchitectureKey("minecraft:sea_lantern");
        assertEquals("SEA_LANTERN", lantern.holder());
        assertEquals(0, lantern.metadata());
        assertEquals(
            "PRISMARINE_STAIRS_DARK",
            OrigenBlockStateResolver.abyssalArchitectureKey("minecraft:dark_prismarine_stairs")
                .holder());
    }
}
