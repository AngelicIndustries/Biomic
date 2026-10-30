package industries.angelic.biomic.mixins.early.minecraft;

import java.util.List;

import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.world.structure.VillageContext;

/** A road still runs right along the pieces beside it: its box only may not overlap them. */
@Mixin(StructureVillagePieces.Path.class)
public abstract class MixinVillagePathSpacing {

    @Redirect(
        method = "func_74933_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/gen/structure/StructureComponent;findIntersecting(Ljava/util/List;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)Lnet/minecraft/world/gen/structure/StructureComponent;"),
        require = 1)
    private static StructureComponent biomic$roadOverlap(List<StructureComponent> pieces, StructureBoundingBox box) {
        return VillageContext.collision(pieces, box, false);
    }
}
