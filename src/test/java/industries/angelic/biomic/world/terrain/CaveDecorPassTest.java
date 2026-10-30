package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CaveDecorPassTest {

    @Test
    public void lichenStatesNameTheirAttachedFaces() {
        assertEquals(
            "minecraft:glow_lichen[down=true,up=false,north=false,south=false,west=false,east=false]",
            CaveDecorPass.lichenState(1));
        assertEquals(
            "minecraft:glow_lichen[down=false,up=true,north=true,south=false,west=false,east=true]",
            CaveDecorPass.lichenState(2 | 4 | 32));
    }
}
