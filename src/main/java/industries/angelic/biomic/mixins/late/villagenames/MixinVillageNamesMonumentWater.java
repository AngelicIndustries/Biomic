package industries.angelic.biomic.mixins.late.villagenames;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import com.llamalad7.mixinextras.sugar.Local;

import industries.angelic.biomic.world.structure.VillageNamesMonuments;

/** Monument pieces fill water below sea level 63 and air above it; the sea is raised in a 512 Biomic world. */
@Pseudo
@Mixin(targets = "astrotibs.villagenames.prismarine.monument.StructureOceanMonumentPieces$Piece", remap = false)
public abstract class MixinVillageNamesMonumentWater {

    @ModifyConstant(method = "generateWaterBox", constant = @Constant(intValue = 63), remap = false, require = 1)
    private int biomic$seaLevel(int y, @Local(argsOnly = true) World world) {
        return y + VillageNamesMonuments.lift(world);
    }
}
