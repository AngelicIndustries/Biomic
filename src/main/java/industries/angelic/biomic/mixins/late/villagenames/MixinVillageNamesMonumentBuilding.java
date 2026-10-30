package industries.angelic.biomic.mixins.late.villagenames;

import net.minecraft.world.gen.structure.StructureComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import industries.angelic.biomic.world.structure.VillageNamesMonuments;

/**
 * The monument building stands at y 39..61 and fills water up to sea level 64 (Village Names' Math.max(63, 64));
 * raised in a 512 Biomic world. Its rooms are placed from the building's own height and follow.
 */
@Pseudo
@Mixin(
    targets = "astrotibs.villagenames.prismarine.monument.StructureOceanMonumentPieces$MonumentBuilding",
    remap = false)
public abstract class MixinVillageNamesMonumentBuilding extends StructureComponent {

    @ModifyConstant(
        method = "<init>(Ljava/util/Random;IILastrotibs/villagenames/prismarine/minecraft/EnumFacing189;)V",
        constant = { @Constant(intValue = 39), @Constant(intValue = 61) },
        remap = false,
        require = 2)
    private int biomic$raise(int y) {
        return y + VillageNamesMonuments.buildingLift();
    }

    @ModifyConstant(
        method = { "addComponentParts", "func_74875_a" },
        constant = @Constant(intValue = 64),
        remap = false,
        require = 1)
    private int biomic$seaLevel(int y) {
        return y + boundingBox.minY - 39;
    }
}
