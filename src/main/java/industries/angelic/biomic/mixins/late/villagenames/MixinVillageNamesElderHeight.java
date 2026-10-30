package industries.angelic.biomic.mixins.late.villagenames;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import astrotibs.villagenames.prismarine.guardian.spawning.SpawnEventListener;

/** Village Names adds elder guardians at fixed heights of a monument at y 39; they follow a raised monument. */
@Pseudo
@Mixin(targets = "astrotibs.villagenames.prismarine.guardian.spawning.SpawnEventListener", remap = false)
public abstract class MixinVillageNamesElderHeight {

    @WrapOperation(
        method = "retroGenElders",
        at = @At(
            value = "INVOKE",
            target = "Lastrotibs/villagenames/prismarine/guardian/spawning/SpawnEventListener;spawnElder(Lnet/minecraft/world/World;Lnet/minecraft/world/gen/structure/StructureBoundingBox;DDD)Z",
            remap = false),
        remap = false,
        require = 3)
    private boolean biomic$elderHeight(SpawnEventListener listener, World world, StructureBoundingBox box, double x,
        double y, double z, Operation<Boolean> original) {
        return original.call(listener, world, box, x, y + box.minY - 39, z);
    }
}
