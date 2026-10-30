package industries.angelic.biomic.mixins.early.minecraft;

import java.util.List;

import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.world.structure.VillageContext;

/** In a Biomic village layout, pieces other than roads keep a block apart (see VillageContext#collision). */
@Mixin(StructureComponent.class)
public abstract class MixinStructureComponentSpacing {

    @Inject(method = "findIntersecting", at = @At("HEAD"), cancellable = true, require = 1)
    private static void biomic$spacedVillagePieces(List<StructureComponent> pieces, StructureBoundingBox box,
        CallbackInfoReturnable<StructureComponent> result) {
        if (VillageContext.building()) result.setReturnValue(VillageContext.collision(pieces, box, true));
    }
}
