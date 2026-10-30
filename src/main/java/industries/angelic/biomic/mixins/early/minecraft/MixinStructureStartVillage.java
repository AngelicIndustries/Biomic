package industries.angelic.biomic.mixins.early.minecraft;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureStart;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.world.structure.VillageContext;

/** Tells a Biomic village population which village's pieces are being placed. */
@Mixin(StructureStart.class)
public abstract class MixinStructureStartVillage {

    @Inject(method = "generateStructure", at = @At("HEAD"), require = 1)
    private void biomic$enterVillage(World world, Random random, StructureBoundingBox box, CallbackInfo info) {
        VillageContext.enterStart((StructureStart) (Object) this);
    }

    @Inject(method = "generateStructure", at = @At("RETURN"), require = 1)
    private void biomic$exitVillage(World world, Random random, StructureBoundingBox box, CallbackInfo info) {
        VillageContext.exitStart();
    }
}
