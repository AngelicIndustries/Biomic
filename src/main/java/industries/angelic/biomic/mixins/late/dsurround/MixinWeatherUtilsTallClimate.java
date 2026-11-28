package industries.angelic.biomic.mixins.late.dsurround;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import industries.angelic.biomic.world.TallWorlds;

/**
 * Dynamic Surroundings replaces vanilla's rain and snow drawing and reads the climate here, for its storms, splashes
 * and frost breath: give it the height vanilla's climate expects too (TallWorlds.climateY). Its callers all pass the
 * client's world.
 */
@Pseudo
@Mixin(targets = "org.blockartistry.mod.DynSurround.client.WeatherUtils", remap = false)
public abstract class MixinWeatherUtilsTallClimate {

    @ModifyVariable(
        method = "getTemperature(Lnet/minecraft/world/World;Lnet/minecraft/world/biome/BiomeGenBase;III)F",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true,
        remap = false,
        require = 1)
    private static int biomic$temperatureY(int y) {
        return TallWorlds.climateY(Minecraft.getMinecraft().theWorld, y);
    }

    /** The height it samples the biome at, and the precipitation height it hands the chunk manager. */
    @ModifyVariable(
        method = "getPrecipitationTemperature(Lnet/minecraft/world/World;Lnet/minecraft/world/biome/BiomeGenBase;IIII)F",
        at = @At("HEAD"),
        ordinal = 1,
        argsOnly = true,
        remap = false,
        require = 1)
    private static int biomic$renderY(int y) {
        return TallWorlds.climateY(Minecraft.getMinecraft().theWorld, y);
    }

    @ModifyVariable(
        method = "getPrecipitationTemperature(Lnet/minecraft/world/World;Lnet/minecraft/world/biome/BiomeGenBase;IIII)F",
        at = @At("HEAD"),
        ordinal = 3,
        argsOnly = true,
        remap = false,
        require = 1)
    private static int biomic$precipitationY(int y) {
        return TallWorlds.climateY(Minecraft.getMinecraft().theWorld, y);
    }
}
