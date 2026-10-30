package industries.angelic.biomic.client.render;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.content.block.BlockAquaticPlant;
import sun.misc.Unsafe;

public class AquaticRenderSemanticsTest {

    @Test
    public void cutoutPlantPassNeverUsesShaderFluidVertexSemantics() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Block plant = (Block) ((Unsafe) f.get(null)).allocateInstance(BlockAquaticPlant.class);
        Assert.assertSame(Material.plants, AquaticRenderSemantics.material(plant, Material.water, 0));
        Assert.assertSame(Material.water, AquaticRenderSemantics.material(plant, Material.water, 1));
        Assert.assertSame(Material.water, AquaticRenderSemantics.material(null, Material.water, 0));
        Assert.assertSame(Material.lava, AquaticRenderSemantics.material(null, Material.lava, 1));
    }
}
