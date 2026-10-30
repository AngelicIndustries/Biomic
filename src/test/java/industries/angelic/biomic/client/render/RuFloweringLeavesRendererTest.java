package industries.angelic.biomic.client.render;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.security.MessageDigest;

import javax.imageio.ImageIO;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;

import org.junit.Test;

import sun.misc.Unsafe;

/** Detached geometry/state/asset contracts; not a client pixel-parity claim. */
public class RuFloweringLeavesRendererTest {

    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    @Test
    public void inventoryFlowerLayerNeverReceivesBaseTintAndPlanesAreSeparated() {
        for (int tint : new int[] { 0, 0x48B518, 0x71934C, 0xFF00FF, 0xFFFFFF }) {
            assertEquals(tint, RuFloweringLeavesRenderer.layerColor(tint, false));
            assertEquals(0xFFFFFF, RuFloweringLeavesRenderer.layerColor(tint, true));
        }
        assertTrue(RuFloweringLeavesRenderer.FLOWER_OFFSET > 0);
        assertTrue(RuFloweringLeavesRenderer.FLOWER_OFFSET < 1D / 256);
    }

    @Test
    public void rendererStateRestoresBoundsOverrideAoAndCallerLockEvenAfterFailedLayer() throws Exception {
        var renderer = (RenderBlocks) unsafe().allocateInstance(RenderBlocks.class);
        renderer.renderMinX = .1;
        renderer.renderMinY = .2;
        renderer.renderMinZ = .3;
        renderer.renderMaxX = .7;
        renderer.renderMaxY = .8;
        renderer.renderMaxZ = .9;
        renderer.enableAO = true;
        renderer.partialRenderBounds = true;
        renderer.lockBlockBounds = true;
        var texture = (IIcon) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] { IIcon.class },
            (proxy, method, args) -> null);
        renderer.overrideBlockTexture = texture;
        var state = new RuFloweringLeavesRenderer.RenderState(renderer);
        try {
            renderer.renderMinX = -1;
            renderer.renderMinY = -1;
            renderer.renderMinZ = -1;
            renderer.renderMaxX = 2;
            renderer.renderMaxY = 2;
            renderer.renderMaxZ = 2;
            renderer.overrideBlockTexture = null;
            renderer.enableAO = false;
            renderer.partialRenderBounds = false;
            renderer.lockBlockBounds = false;
            throw new IllegalStateException("Synthetic second-layer failure");
        } catch (IllegalStateException expected) {
            assertEquals("Synthetic second-layer failure", expected.getMessage());
        } finally {
            state.restore(renderer);
        }
        assertArrayEquals(
            new double[] { .1, .2, .3, .7, .8, .9 },
            new double[] { renderer.renderMinX, renderer.renderMinY, renderer.renderMinZ, renderer.renderMaxX,
                renderer.renderMaxY, renderer.renderMaxZ },
            0);
        assertSame(texture, renderer.overrideBlockTexture);
        assertTrue(renderer.enableAO);
        assertTrue(renderer.partialRenderBounds);
        assertTrue(renderer.lockBlockBounds);
    }

    @Test
    public void originalUntintedFlowerAssetIsByteExactAndLeavesBackgroundTransparent() throws Exception {
        String path = "/assets/biomic/textures/blocks/ru/flowering_leaves_flower.png";
        byte[] bytes;
        try (var input = getClass().getResourceAsStream(path)) {
            assertNotNull(input);
            bytes = input.readAllBytes();
        }
        var hash = MessageDigest.getInstance("SHA-256")
            .digest(bytes);
        var hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b & 255));
        assertEquals("e16f2d137cbbbf5d7f63e4018eb22164c75a91fe48bfc33c1ffc09fa5e077624", hex.toString());
        var image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        int transparent = 0, flowers = 0;
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            if ((image.getRGB(x, y) >>> 24) == 0) transparent++;
            else flowers++;
        }
        assertTrue("Leaf background comes from oak, not an opaque recolored asset", transparent > 128);
        assertTrue("Original flower pixels remain", flowers > 0);
    }
}
