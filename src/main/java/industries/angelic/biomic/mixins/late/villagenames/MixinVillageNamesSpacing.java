package industries.angelic.biomic.mixins.late.villagenames;

import java.util.List;

import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.world.structure.VillageContext;

/** Village Names' roads, like vanilla's, still run right along the pieces beside them (see MixinVillagePathSpacing). */
@Pseudo
@Mixin(targets = "astrotibs.villagenames.village.StructureVillageVN", remap = false)
public abstract class MixinVillageNamesSpacing {

    @Redirect(
        method = "findPieceBox",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/structure/StructureComponent;findIntersecting(Ljava/util/List;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)Lnet/minecraft/world/gen/structure/StructureComponent;",
            remap = true),
        remap = false,
        require = 1)
    private static StructureComponent biomic$roadOverlap(List<StructureComponent> pieces, StructureBoundingBox box) {
        return VillageContext.collision(pieces, box, false);
    }
}
