package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.IIcon;

import org.junit.Test;

import industries.angelic.biomic.content.BiomicExpansionBlocks;

public class TerraPackFroglightTest {

    @Test
    public void pillarAxesTexturesLightAndDropsRemainIndependentOfLogs() {
        var block = BiomicExpansionBlocks.OCHRE_FROGLIGHT;
        var resolver = new OrigenBlockStateResolver();
        Map<String, IIcon> icons = new HashMap<>();
        block.registerBlockIcons(
            name -> icons.computeIfAbsent(
                name,
                ignored -> (IIcon) Proxy.newProxyInstance(
                    IIcon.class.getClassLoader(),
                    new Class<?>[] { IIcon.class },
                    (proxy, method, args) -> null)));
        for (String axis : new String[] { "x", "y", "z" }) {
            var state = resolver.resolve("minecraft:ochre_froglight[axis=" + axis + "]");
            int metadata = axis.equals("x") ? 4 : axis.equals("z") ? 8 : 0;
            assertSame(block, state.block());
            assertEquals(metadata, state.metadata());
            assertFalse(state.compatibilitySubstitute());
            assertEquals("minecraft:ochre_froglight", resolver.canonicalId(block, metadata));
            for (int face = 0; face < 6; face++) {
                boolean end = axis.equals("y") ? face < 2 : axis.equals("z") ? face == 2 || face == 3 : face > 3;
                assertSame(
                    icons.get("biomic:ochre_froglight_" + (end ? "top" : "side")),
                    block.getIcon(face, metadata));
            }
            assertEquals(0, block.damageDropped(metadata));
        }
        assertEquals(15, block.getLightValue());
        assertTrue(block.isOpaqueCube());
        assertFalse(block.isWood(null, 0, 0, 0));
        for (int face = 0; face < 6; face++)
            assertEquals(face < 2 ? 0 : face < 4 ? 8 : 4, block.onBlockPlaced(null, 0, 0, 0, face, .5F, .5F, .5F, 0));
    }
}
