package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

public class NativeBiomeCatalogueTest {

    @Test
    public void identityIsStableFieldBasedNotDisplayNameOrNumericId() {
        assertEquals("cherry_blossom_grove", NativeBiomeCatalogue.snakeCase("cherryBlossomGrove"));
        assertEquals("salt_marsh", NativeBiomeCatalogue.snakeCase("saltMarsh"));
        assertEquals("soul_sand_valley", NativeBiomeCatalogue.snakeCase("soulSandValley"));
    }

    @Test
    public void bindingsAndSnapshotsCannotBeMutatedOrDuplicated() {
        var builder = new NativeBiomeCatalogue.Builder();
        var tags = new HashSet<>(Set.of("swamp"));
        var entry = new NativeBiomeCatalogue.Entry("fixture:marsh", "1", BiomeGenBase.swampland, tags);
        builder.add(entry);
        var snapshot = builder.build();
        tags.add("corruption");
        assertEquals(Set.of("swamp"), entry.tags);
        assertSame(entry, snapshot.byBiome.get(BiomeGenBase.swampland));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.byKey.clear());
        assertThrows(IllegalArgumentException.class, () -> builder.add(entry));
        assertThrows(
            IllegalArgumentException.class,
            () -> builder.add(new NativeBiomeCatalogue.Entry("fixture:alias", "1", BiomeGenBase.swampland, Set.of())));
        builder.add(new NativeBiomeCatalogue.Entry("fixture:desert", "1", BiomeGenBase.desert, Set.of()));
        assertEquals(1, snapshot.byKey.size());
        assertEquals(2, builder.build().byKey.size());
    }

    @Test
    public void missingProvidersRemainUnknownAndDoNotInventBiomes() {
        assertNull(NativeBiomeCatalogue.get("not_installed:marsh"));
        assertNull(NativeBiomeCatalogue.biome("not_installed:marsh"));
        assertThrows(
            IllegalArgumentException.class,
            () -> new NativeBiomeCatalogue.Entry("Bad Key", "1", BiomeGenBase.plains, Set.of()));
    }
}
