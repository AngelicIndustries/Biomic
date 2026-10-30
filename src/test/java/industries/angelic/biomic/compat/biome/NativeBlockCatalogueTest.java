package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Test;

public class NativeBlockCatalogueTest {

    @Test
    public void metadataIsLosslessAndNotSilentlyIgnored() {
        for (int meta = 0; meta < 16; meta++) {
            String state = NativeBlockCatalogue.state("fixture:plant", meta);
            assertEquals(
                meta,
                NativeBlockCatalogue.parse(state)
                    .metadata());
            assertEquals(
                "fixture:plant",
                NativeBlockCatalogue.parse(state)
                    .key());
        }
        for (String invalid : new String[] { "fixture:plant", "fixture:plant[legacy_meta=16]",
            "fixture:plant[legacy_meta=-1]", "fixture:plant[legacy_meta=01]", "fixture:plant[legacy_meta=1,foo=bar]",
            "fixture:plant[legacy_meta=+1]" })
            assertThrows(invalid, IllegalArgumentException.class, () -> NativeBlockCatalogue.parse(invalid));
    }

    @Test
    public void caseFoldingCannotLoseNativeRegistryIdentity() {
        var builder = new NativeBlockCatalogue.Builder();
        Block log = new Block(Material.wood) {}, other = new Block(Material.wood) {};
        builder.add("Fixture:SmallLog", log);
        var snapshot = builder.build();
        assertSame(log, snapshot.keys.get("fixture:smalllog"));
        assertEquals("fixture:smalllog", snapshot.blocks.get(log));
        assertThrows(IllegalArgumentException.class, () -> builder.add("Fixture:smalllog", other));
        assertThrows(IllegalArgumentException.class, () -> builder.add("Fixture:alias", log));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.keys.clear());
    }
}
