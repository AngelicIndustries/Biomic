package industries.angelic.biomic.mixins.late.saltymod;

import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import industries.angelic.biomic.content.block.WaterloggedRenderPlant;

/** Native lower reed contains water, but its plant mesh belongs in the cutout pass. */
@Pseudo
@Mixin(targets = "darkbum.saltymod.block.BlockMarshReeds$BlockMarshReedsBottom", remap = false)
public abstract class MixinMarshReedsBottomPass extends BlockBush implements WaterloggedRenderPlant {

    protected MixinMarshReedsBottomPass(Material material) {
        super(material);
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }
}
