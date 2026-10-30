package industries.angelic.biomic.mixins.early.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's temples and witch huts: the ground height a piece found, set by Biomic from its plan instead. */
@Mixin(targets = "net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces$Feature")
public interface AccessorScatteredFeature {

    @Accessor("field_74936_d")
    void biomic$setGround(int ground);
}
