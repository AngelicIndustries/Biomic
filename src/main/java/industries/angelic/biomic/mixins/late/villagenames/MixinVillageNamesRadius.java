package industries.angelic.biomic.mixins.late.villagenames;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import industries.angelic.biomic.world.structure.VillageLayoutRadius;

/** The same road cap for Village Names' own layout code, which repeats vanilla's 112. */
@Pseudo
@Mixin(targets = "astrotibs.villagenames.village.StructureVillageVN", remap = false)
public abstract class MixinVillageNamesRadius {

    @ModifyConstant(
        method = { "getNextVillageStructureComponent", "getRandomVillageRoadComponent", "generateAndAddRoadPiece" },
        constant = @Constant(intValue = 112),
        remap = false,
        require = 1)
    private static int biomic$layoutRadius(int vanilla) {
        return VillageLayoutRadius.get(vanilla);
    }
}
