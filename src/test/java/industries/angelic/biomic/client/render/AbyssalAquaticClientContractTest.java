package industries.angelic.biomic.client.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.imageio.ImageIO;

import net.minecraft.block.material.Material;

import org.junit.Test;

import industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks;
import industries.angelic.biomic.content.alexscaves.BlockAbyssalAquatic;
import industries.angelic.biomic.content.block.WaterloggedRenderPlant;

public class AbyssalAquaticClientContractTest {

    @Test
    public void waterloggedModelsKeepCutoutGeometrySeparateFromRealWaterPass() {
        assertFalse(AbyssalContentBlocks.VENT instanceof WaterloggedRenderPlant);
        assertEquals(0, AbyssalContentBlocks.VENT.getRenderBlockPass());
        assertTrue(AbyssalContentBlocks.VENT.canRenderInPass(0));
        assertFalse(AbyssalContentBlocks.VENT.canRenderInPass(1));
        for (BlockAbyssalAquatic block : waterlogged()) {
            assertTrue(
                block.kind()
                    .name(),
                block instanceof WaterloggedRenderPlant);
            assertSame(Material.water, block.getMaterial());
            assertTrue(block.canRenderInPass(0));
            assertTrue(block.canRenderInPass(1));
            assertSame(Material.plants, AquaticRenderSemantics.material(block, block.getMaterial(), 0));
            assertSame(Material.water, AquaticRenderSemantics.material(block, block.getMaterial(), 1));
        }
    }

    @Test
    public void sourceGeneratedInventoryRolesUsePinnedSprites() throws Exception {
        assertTrue(AbyssalContentBlocks.MUSSELS[0].generatedItemKind());
        assertTrue(AbyssalContentBlocks.SPONGE.generatedItemKind());
        assertFalse(AbyssalContentBlocks.TUBE_WORM.generatedItemKind());
        assertFalse(
            "generated source items need the flat-inventory render transform",
            new AbyssalBlockRenderer(81, false).shouldRender3DInInventory(81));
        assertTrue(new AbyssalBlockRenderer(82).shouldRender3DInInventory(82));
        assertEquals(
            "681a5f20ce94cb19437ca4de322c6ad96c745f9d2ea518f502e444a5cc2c093b",
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(bytes("/assets/biomic/textures/blocks/alexscaves/mussel_item.png"))));
        assertEquals(
            "0333663e4178144dcf4172d1b65037e5313ba94965ee3b8f8643800650f89b32",
            HexFormat.of()
                .formatHex(
                    MessageDigest.getInstance("SHA-256")
                        .digest(bytes("/assets/biomic/textures/particles/alexscaves/tube_worm.png"))));
    }

    @Test
    public void spongePickAndDropsAlwaysUseTheSourceTopItem() {
        assertEquals(1, AbyssalContentBlocks.SPONGE.damageDropped(0));
        assertEquals(1, AbyssalContentBlocks.SPONGE.damageDropped(1));
    }

    @Test
    public void muckRendersAndSupportsAsAFullBlockButKeepsItsSourceCollisionShelf() {
        assertEquals(1D, AbyssalContentBlocks.MUCK.getBlockBoundsMaxY(), 0D);
        assertEquals(13D / 16D, AbyssalContentBlocks.MUCK.getCollisionBoundingBoxFromPool(null, 0, 0, 0).maxY, 0D);
    }

    @Test
    public void sourceCutoutTexturesDoNotContainTranslucentFringes() throws Exception {
        for (String name : new String[] { "mussels.png", "ping_pong_sponge.png", "ping_pong_sponge_top.png",
            "tube_worm.png", "dusk_anemone_base.png", "dusk_anemone_tentacles.png" }) assertBinaryAlpha(name);
    }

    private static BlockAbyssalAquatic[] waterlogged() {
        return new BlockAbyssalAquatic[] { AbyssalContentBlocks.VENT_MEDIUM, AbyssalContentBlocks.VENT_THIN,
            AbyssalContentBlocks.TUBE_WORM, AbyssalContentBlocks.MUSSELS[0], AbyssalContentBlocks.SPONGE,
            AbyssalContentBlocks.DUSK, AbyssalContentBlocks.TWILIGHT, AbyssalContentBlocks.MIDNIGHT };
    }

    private static void assertBinaryAlpha(String name) throws Exception {
        BufferedImage image = image("/assets/biomic/textures/blocks/alexscaves/" + name);
        boolean clear = false, solid = false;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int alpha = image.getRGB(x, y) >>> 24;
            if (alpha == 0) clear = true;
            else if (alpha == 255) solid = true;
            else throw new AssertionError(name + " has translucent alpha " + alpha);
        }
        assertTrue(name, clear && solid);
    }

    private static BufferedImage image(String path) throws Exception {
        try (InputStream stream = AbyssalAquaticClientContractTest.class.getResourceAsStream(path)) {
            assertNotNull(path, stream);
            return ImageIO.read(stream);
        }
    }

    private static byte[] bytes(String path) throws Exception {
        try (InputStream stream = AbyssalAquaticClientContractTest.class.getResourceAsStream(path)) {
            assertNotNull(path, stream);
            return stream.readAllBytes();
        }
    }

    private static String text(String path) throws Exception {
        return new String(bytes(path), StandardCharsets.UTF_8);
    }
}
