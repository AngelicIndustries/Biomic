package industries.angelic.biomic.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.BeforeClass;
import org.junit.Test;

import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.storage.BiomeVolume;

public class BiomeCatalogueTest {

    @BeforeClass
    public static void registerBiomes() {
        BiomicBiomes.register();
    }

    @Test
    public void listsEveryFamilyWithTheLookupsTags() {
        List<BiomeCatalogue.Entry> entries = BiomeCatalogue.entries();
        Set<String> keys = new HashSet<>();
        for (BiomeCatalogue.Entry entry : entries) {
            assertTrue("duplicate " + entry.key(), keys.add(entry.key()));
            if (!BiomeVolume.SURFACE_KEY.equals(entry.key()) && entry.kind() != BiomeCatalogue.Kind.SURFACE
                && entry.kind() != BiomeCatalogue.Kind.NETHER)
                assertEquals(
                    entry.key(),
                    BiomicBiomeApi.resolution(entry.key())
                        .tags(),
                    entry.tags());
            assertEquals(entry.tags(), BiomeCatalogue.getTags(entry.key()));
        }
        assertEquals(entries, BiomeCatalogue.entries());

        // Registered but never generated: not listed.
        for (String key : industries.angelic.biomic.biome.BiomicBiomes.NOT_GENERATED)
            assertNull(key, BiomeCatalogue.get(key));

        BiomeCatalogue.Entry surface = BiomeCatalogue.get(BiomeVolume.SURFACE_KEY);
        assertNull(surface.biome());
        assertTrue(
            surface.tags()
                .isEmpty());

        BiomeCatalogue.Entry donut = BiomeCatalogue.get(BiomeKeys.SKY_DONUT);
        assertEquals(BiomeCatalogue.Kind.FLOATING, donut.kind());
        assertTrue(
            donut.tags()
                .contains(BiomeTags.FLOATING_ISLAND));

        BiomeCatalogue.Entry host = BiomeCatalogue.get("biomic:aeropelago/origen/lush_loops");
        assertEquals(BiomeCatalogue.Kind.FLOATING, host.kind());
        assertTrue(
            host.tags()
                .contains(BiomeTags.ARCHIPELAGO));
        assertNotNull(host.biome());

        BiomeCatalogue.Entry island = BiomeCatalogue.get("biomic:hosted/regions_unexplored/redwoods");
        assertEquals(BiomeCatalogue.Kind.FLOATING, island.kind());
        assertTrue(
            island.tags()
                .contains(BiomeTags.FLOATING_ISLAND));
        assertEquals(BiomicBiomes.getBiome("biomic:redwoods"), island.biome());

        BiomeCatalogue.Entry deepDark = BiomeCatalogue.get("biomic:origen/deep_dark");
        assertEquals(BiomeCatalogue.Kind.CAVE, deepDark.kind());
        assertTrue(
            deepDark.tags()
                .contains(BiomeTags.CAVE));

        BiomeCatalogue.Entry redwoods = BiomeCatalogue.get("biomic:redwoods");
        assertEquals(BiomeCatalogue.Kind.SURFACE, redwoods.kind());
        assertEquals(BiomicBiomes.getTags(redwoods.biome()), redwoods.tags());

        // Origen's floating profiles have keys of their own, apart from the same biome on the ground.
        BiomeCatalogue.Entry lushLoops = BiomeCatalogue.get("biomic:origen/lush_loops");
        assertEquals(BiomeCatalogue.Kind.SURFACE, lushLoops.kind());
        assertEquals(BiomicBiomes.getTags(lushLoops.biome()), lushLoops.tags());
        BiomeCatalogue.Entry floatingLushLoops = BiomeCatalogue.get("biomic:floating/origen/lush_loops");
        assertEquals(BiomeCatalogue.Kind.FLOATING, floatingLushLoops.kind());
        assertEquals(lushLoops.biome(), floatingLushLoops.biome());
        assertTrue(
            floatingLushLoops.tags()
                .contains(BiomeTags.FLOATING_ISLAND));
        assertTrue(
            floatingLushLoops.tags()
                .containsAll(lushLoops.tags()));

        BiomeCatalogue.Entry crimson = BiomeCatalogue.get("biomic:crimson_forest");
        assertEquals(BiomeCatalogue.Kind.NETHER, crimson.kind());

        assertTrue(
            BiomeCatalogue.getTags("biomic:no_such_biome")
                .isEmpty());
    }

    @Test
    public void columnSpansFollowTheVolume() {
        String deepDark = "biomic:origen/deep_dark";
        short[] cells = new short[BiomeVolume.CELL_COUNT];
        // Deep dark in the bottom ten quarts of the column at local 5, 9 (quart 1, 2); the rest is surface.
        for (int qy = 0; qy < 10; qy++) cells[(qy * 4 + 2) * 4 + 1] = 1;
        BiomeVolume volume = BiomeVolume.create(1, List.of(BiomeVolume.SURFACE_KEY, deepDark), cells);
        BiomeGenBase surface = BiomicBiomes.getBiome("biomic:redwoods");
        List<BiomicBiomeApi.KeySpan> spans = BiomicBiomeApi.spans(surface, volume, 5, 9, 256);
        assertEquals(
            List.of(
                new BiomicBiomeApi.KeySpan(deepDark, 0, 40),
                new BiomicBiomeApi.KeySpan("biomic:redwoods", 40, 256)),
            spans);
        // Another column of the same chunk is all surface.
        assertEquals(
            List.of(new BiomicBiomeApi.KeySpan("biomic:redwoods", 0, 256)),
            BiomicBiomeApi.spans(surface, volume, 0, 0, 256));
    }
}
