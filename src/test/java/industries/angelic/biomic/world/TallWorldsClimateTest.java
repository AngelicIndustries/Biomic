package industries.angelic.biomic.world;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TallWorldsClimateTest {

    @Test
    public void tallLowlandsReadAsVanillaLowlands() {
        assertEquals(128, TallWorlds.SHIFT);
        assertEquals(62, TallWorlds.climateY(true, 190));
        assertEquals(78, TallWorlds.climateY(true, 206));
    }

    @Test
    public void ordinaryWorldsKeepTheirHeight() {
        assertEquals(206, TallWorlds.climateY(false, 206));
    }
}
