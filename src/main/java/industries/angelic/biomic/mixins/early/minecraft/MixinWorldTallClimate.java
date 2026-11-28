package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import industries.angelic.biomic.world.TallWorlds;

/**
 * Snow settles and water freezes by the height vanilla's climate expects, the lowlands near Y=64 (TallWorlds.climateY).
 */
@Mixin(World.class)
public abstract class MixinWorldTallClimate {

    // Forge's method names, unmapped; the vanilla call they make is mapped.
    @ModifyArg(
        method = { "canSnowAtBody", "canBlockFreezeBody" },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/biome/BiomeGenBase;getFloatTemperature(III)F",
            remap = true),
        index = 1,
        remap = false,
        require = 2)
    private int biomic$climateY(int y) {
        return TallWorlds.climateY((World) (Object) this, y);
    }
}
