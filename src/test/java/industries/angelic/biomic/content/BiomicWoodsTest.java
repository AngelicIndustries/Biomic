package industries.angelic.biomic.content;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.junit.Test;

public class BiomicWoodsTest {

    @Test
    public void everyWoodHasItsArtAndNames() throws Exception {
        Properties english = new Properties();
        try (Reader reader = new InputStreamReader(
            getClass().getResourceAsStream("/assets/biomic/lang/en_US.lang"),
            StandardCharsets.UTF_8)) {
            english.load(reader);
        }
        for (BiomicWoods.Wood wood : BiomicWoods.Wood.values()) {
            assertNotNull(
                wood.id + " planks art",
                getClass().getResource("/assets/biomic/textures/blocks/" + wood.texture + ".png"));
            for (String part : new String[] { "planks", "slab", "stairs" }) {
                String key = "tile.biomic." + wood.id + "_" + part + ".name";
                assertTrue("no English name " + key, english.containsKey(key));
            }
        }
    }
}
