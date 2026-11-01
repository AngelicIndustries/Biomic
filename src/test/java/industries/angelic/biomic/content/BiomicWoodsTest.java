package industries.angelic.biomic.content;

import static org.junit.Assert.assertEquals;
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
            if (wood.kit == BiomicWoods.Kit.NONE) continue;
            for (String art : new String[] { "blocks/ru/" + wood.id + "_door_upper",
                "blocks/ru/" + wood.id + "_door_lower", "blocks/ru/" + wood.id + "_trapdoor",
                "items/ru/" + wood.id + "_door", "items/ru/" + wood.id + "_sign", "entity/signs/" + wood.id })
                assertArt(wood, art);
            for (String part : new String[] { "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate",
                "sign" }) {
                String key = "tile.biomic." + wood.id + "_" + part + ".name";
                assertTrue("no English name " + key, english.containsKey(key));
            }
            boolean boat = wood.kit == BiomicWoods.Kit.FULL;
            assertEquals(
                wood.id + " boat art matches its kit",
                boat,
                getClass().getResource("/assets/biomic/textures/entity/boat/" + wood.id + ".png") != null);
            if (!boat) continue;
            for (String art : new String[] { "items/ru/" + wood.id + "_boat", "items/ru/" + wood.id + "_chest_boat" })
                assertArt(wood, art);
            for (String part : new String[] { "boat", "chest_boat" }) {
                String key = "item.biomic." + wood.id + "_" + part + ".name";
                assertTrue("no English name " + key, english.containsKey(key));
            }
        }
    }

    private void assertArt(BiomicWoods.Wood wood, String art) {
        assertNotNull(wood.id + " art " + art, getClass().getResource("/assets/biomic/textures/" + art + ".png"));
    }
}
