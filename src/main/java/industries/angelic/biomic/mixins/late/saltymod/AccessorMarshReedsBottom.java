package industries.angelic.biomic.mixins.late.saltymod;

import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The lower reed's four textures, which Salty registers when its diverseMarshReeds option is on. */
@Pseudo
@Mixin(targets = "darkbum.saltymod.block.BlockMarshReeds$BlockMarshReedsBottom", remap = false)
public interface AccessorMarshReedsBottom {

    @Accessor(value = "iconsBottom", remap = false)
    IIcon[] biomic$variants();
}
