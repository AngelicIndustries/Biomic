package industries.angelic.biomic.mixins.late.saltymod;

import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The upper reed's four textures, which Salty registers when its diverseMarshReeds option is on. */
@Pseudo
@Mixin(targets = "darkbum.saltymod.block.BlockMarshReeds$BlockMarshReedsTop", remap = false)
public interface AccessorMarshReedsTop {

    @Accessor(value = "iconsTop", remap = false)
    IIcon[] biomic$variants();
}
