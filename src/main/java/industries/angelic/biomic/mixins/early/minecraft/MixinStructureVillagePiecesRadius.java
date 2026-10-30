package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.gen.structure.StructureVillagePieces;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import industries.angelic.biomic.world.structure.VillageLayoutRadius;

/** Lets a small Biomic village cap how far vanilla's roads grow from its start (vanilla: 112 blocks). */
@Mixin(StructureVillagePieces.class)
public abstract class MixinStructureVillagePiecesRadius {

    @ModifyConstant(
        method = { "getNextVillageStructureComponent", "getNextComponentVillagePath" },
        constant = @Constant(intValue = 112),
        require = 1)
    private static int biomic$layoutRadius(int vanilla) {
        return VillageLayoutRadius.get(vanilla);
    }
}
