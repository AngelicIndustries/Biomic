package industries.angelic.biomic.browser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.browser.BrowserCatalogue.Entry;
import industries.angelic.biomic.browser.BrowserCatalogue.Place;
import industries.angelic.biomic.compat.biome.NativeHabitatSettings;

public class BrowserCatalogueTest {

    @BeforeClass
    public static void registerBiomes() {
        BiomicBiomes.register();
    }

    private static Entry find(List<Entry> entries, String source, String habitat) {
        for (Entry entry : entries) if (entry.source()
            .equals(source)
            && entry.habitat()
                .equals(habitat))
            return entry;
        throw new AssertionError("No " + source + " entry for " + habitat);
    }

    /** The browser's never-generated biomes are the shared list's registered ones, not a second list. */
    @Test
    public void neverGeneratedFollowsTheSharedList() {
        assertTrue(BiomicBiomes.NOT_GENERATED.containsAll(BrowserCatalogue.NEVER_GENERATED.keySet()));
    }

    @Test
    public void listsEachFamilyWhereItLives() {
        List<Entry> entries = BrowserCatalogue.build(List.of(), NativeHabitatSettings.OFF);
        Entry redwoods = find(entries, "Regions Unexplored", "regions_unexplored:redwoods");
        assertTrue(
            redwoods.places()
                .contains(Place.SURFACE));
        assertTrue(
            redwoods.places()
                .contains(Place.SKY));
        assertFalse(
            redwoods.plants()
                .isEmpty());
        Entry deepDark = find(entries, "Origen", "origen:DEEP_DARK");
        assertTrue(
            deepDark.places()
                .contains(Place.CAVES));
        Entry crimson = null;
        for (Entry entry : entries) if (entry.source()
            .equals("Tartarus")
            && entry.key()
                .endsWith("crimson_forest"))
            crimson = entry;
        assertTrue(
            crimson != null && crimson.places()
                .equals(java.util.Set.of(Place.NETHER)));
        Entry estuary = find(entries, "Biomic", "biomic:estuary");
        assertTrue(
            estuary.places()
                .isEmpty());
    }

    @Test
    public void survivesTheNetwork() {
        List<Entry> entries = BrowserCatalogue.build(List.of(), NativeHabitatSettings.OFF);
        assertEquals(entries, BrowserCatalogue.read(BrowserCatalogue.write(entries)));
    }
}
