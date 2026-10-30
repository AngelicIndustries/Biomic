package industries.angelic.biomic.content.block;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.junit.Assert;
import org.junit.Test;

import sun.misc.Unsafe;

public class SurfaceReviewBlockTest {

    @Test
    public void particleTexturesPreserveTheirSourceNamespace() {
        Assert.assertEquals("minecraft:blackstone", BlockRuCaveModel.particleTexture("minecraft:blackstone"));
        Assert
            .assertEquals("biomic:ru_decor/hyacinth_base", BlockRuCaveModel.particleTexture("ru_decor/hyacinth_base"));
        Assert.assertEquals(
            "biomic:ru_decor/hyacinth_base",
            BlockRuCaveModel.particleTexture("biomic:ru_decor/hyacinth_base"));
    }

    @Test
    public void icicleItemUsesUpTipButWorldKeepsAllTenShapes() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        BlockIcicle block = (BlockIcicle) ((Unsafe) field.get(null)).allocateInstance(BlockIcicle.class);
        block.setBlockTextureName("biomic:ru_decor/icicle");
        block.registerBlockIcons(
            name -> (IIcon) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IIcon.class },
                (proxy, method, args) -> method.getName()
                    .equals("getIconName") ? name : null));
        Assert.assertNull("Do not inherit EFR's unrelated item texture", block.getItemIconName());
        String[] shapes = { "tip", "frustum", "middle", "base", "tip_merge" };
        for (int meta = 0; meta < 10; meta++) {
            final int value = meta;
            IBlockAccess world = (IBlockAccess) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { IBlockAccess.class },
                (proxy, method, args) -> method.getName()
                    .equals("getBlockMetadata") ? value : null);
            Assert.assertEquals(
                "biomic:ru_decor/icicle_up_tip",
                block.getIcon(0, meta)
                    .getIconName());
            Assert.assertEquals(
                "biomic:ru_decor/icicle_" + (meta < 5 ? "down_" : "up_") + shapes[meta % 5],
                block.getIcon(world, 0, 70, 0, 0)
                    .getIconName());
            Assert.assertFalse(block.isSideSolid(world, 0, 70, 0, net.minecraftforge.common.util.ForgeDirection.UP));
            Assert.assertFalse(industries.angelic.biomic.world.modern.ModernBlockSemantics.legacyFullFace(block));
        }
    }

    @Test
    public void ashenDirtKeepsItsSmoulderingBit() {
        Assert.assertFalse(BlockRuAshenDirt.isSmouldering(0));
        Assert.assertTrue(BlockRuAshenDirt.isSmouldering(1));
    }
}
