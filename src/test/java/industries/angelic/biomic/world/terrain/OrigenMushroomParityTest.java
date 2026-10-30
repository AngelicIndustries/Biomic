package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import org.junit.Test;

import industries.angelic.biomic.client.VanillaTextures;
import industries.angelic.biomic.content.BiomicMushroomBlocks;

public class OrigenMushroomParityTest {

    @Test
    public void explicitLegacyMushroomsPreserveEveryNativeFaceAndRejectMixedFormats() {
        var resolver = new OrigenBlockStateResolver();
        for (String name : new String[] { "red", "brown" }) for (int meta = 0; meta < 16; meta++) assertResolved(
            resolver,
            "minecraft:" + name + "_mushroom_block[legacy_meta=" + meta + "]",
            name.equals("red") ? Blocks.red_mushroom_block : Blocks.brown_mushroom_block,
            meta);
        for (String props : new String[] { "legacy_meta=-1", "legacy_meta=16", "legacy_meta=nope",
            "legacy_meta=3,up=false" }) {
            boolean rejected = false;
            try {
                resolver.resolve("minecraft:red_mushroom_block[" + props + "]");
            } catch (IllegalArgumentException expected) {
                rejected = true;
            }
            assertTrue("Accepted invalid legacy mushroom " + props, rejected);
        }
    }

    @Test
    public void modernCapPoreAndStemStatesKeepDistinct17Models() {
        OrigenBlockStateResolver resolver = new OrigenBlockStateResolver();

        assertResolved(resolver, "minecraft:red_mushroom_block", Blocks.red_mushroom_block, 14);
        assertResolved(
            resolver,
            "minecraft:red_mushroom_block[up=false,down=false,north=false,east=false,south=false,west=false]",
            Blocks.red_mushroom_block,
            0);
        assertResolved(resolver, "minecraft:mushroom_stem", Blocks.brown_mushroom_block, 10);

        assertResolved(resolver, "minecraft:brown_mushroom_block", Blocks.brown_mushroom_block, 14);
        assertResolved(
            resolver,
            "minecraft:brown_mushroom_block[up=false,down=false,north=false,east=false,south=false,west=false]",
            Blocks.brown_mushroom_block,
            0);
        assertResolved(resolver, "minecraft:brown_mushroom_block[down=false]", Blocks.brown_mushroom_block, 14);

        assertResolved(resolver, "biomic:mushroom_flesh", BiomicMushroomBlocks.MUSHROOM_FLESH, 0);
        assertEquals("biomic:mushroom_flesh", resolver.canonicalId(BiomicMushroomBlocks.MUSHROOM_FLESH, 0));
    }

    @Test
    public void decorativeSpotsAndExposedStemTopsUseTheModernAllSidedBlock() {
        String source = "str stemBlock = \"minecraft:mushroom_stem\";\n"
            + "str spotBlock = \"minecraft:mushroom_stem\";\n"
            + "block(x+warpX, h, z+warpZ, \"minecraft:mushroom_stem\");";
        String translated = OrigenFeatureRuntime.translateMushroomFlesh("large_mixed_mushroom_procedural", source);

        assertTrue(translated.contains("str stemBlock = \"minecraft:mushroom_stem\";"));
        assertTrue(translated.contains("str spotBlock = \"biomic:mushroom_flesh\";"));
        assertTrue(translated.contains("if (h == stemHeight - 1)"));
        assertTrue(translated.contains("block(x+warpX, h, z+warpZ, \"biomic:mushroom_flesh\");"));
        assertTrue(translated.contains("block(x+warpX, h, z+warpZ, \"minecraft:mushroom_stem\");"));
        assertEquals(source, OrigenFeatureRuntime.translateMushroomFlesh("unrelated_tree", source));
    }

    @Test
    public void modernMushroomStemTextureIsDownloaded() {
        assertTrue(VanillaTextures.downloads("textures/blocks/mushroom_stem.png"));
    }

    @Test
    public void packedTreeCoordinatesRoundTripAcrossNegativeWorldQuadrant() {
        assertPackedPosition(-6_294, 133, -10_778);
        assertPackedPosition(-30_000_000, -64, 30_000_000);
        assertPackedPosition(30_000_000, 319, -30_000_000);
    }

    private static OrigenBlockStateResolver resolver() {
        return new OrigenBlockStateResolver();
    }

    private static void assertResolved(OrigenBlockStateResolver resolver, String state, Block expectedBlock,
        int expectedMetadata) {
        OrigenBlockStateResolver.ResolvedBlock resolved = resolver.resolve(state);
        assertSame(expectedBlock, resolved.block());
        assertEquals(expectedMetadata, resolved.metadata());
        assertFalse(resolved.compatibilitySubstitute());
    }

    private static void assertPackedPosition(int x, int y, int z) {
        long packed = OrigenFeatureRuntime.blockKey(x, y, z);
        assertEquals(x, OrigenFeatureRuntime.blockKeyX(packed));
        assertEquals(y, OrigenFeatureRuntime.blockKeyY(packed));
        assertEquals(z, OrigenFeatureRuntime.blockKeyZ(packed));
    }
}
