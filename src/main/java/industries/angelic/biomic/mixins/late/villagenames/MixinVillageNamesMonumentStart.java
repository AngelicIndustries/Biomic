package industries.angelic.biomic.mixins.late.villagenames;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.world.structure.VillageNamesMonuments;

/** Tells the monument's building piece which world it is laid out in (see VillageNamesMonuments). */
@Pseudo
@Mixin(targets = "astrotibs.villagenames.prismarine.monument.StructureOceanMonument$StartMonument", remap = false)
public abstract class MixinVillageNamesMonumentStart {

    @Inject(method = "func_175789_b", at = @At("HEAD"), remap = false, require = 1)
    private void biomic$begin(World world, Random random, int chunkX, int chunkZ, CallbackInfo info) {
        VillageNamesMonuments.begin(world);
    }

    @Inject(method = "func_175789_b", at = @At("RETURN"), remap = false, require = 1)
    private void biomic$end(World world, Random random, int chunkX, int chunkZ, CallbackInfo info) {
        VillageNamesMonuments.end();
    }
}
