package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import industries.angelic.biomic.world.TallWorlds;

/**
 * Rain is drawn as rain or snow, and splashes, by the height vanilla's climate expects (TallWorlds.climateY). Dynamic
 * Surroundings overwrites both methods with its own renderer (priority 1001; mixins.late.dsurround patches that one):
 * applied after it, these find nothing there, so they are optional.
 */
@Mixin(value = EntityRenderer.class, priority = 1100)
public abstract class MixinEntityRendererTallClimate {

    @Shadow
    private Minecraft mc;

    @ModifyArg(
        method = { "addRainParticles", "renderRainSnow" },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/BiomeGenBase;getFloatTemperature(III)F"),
        index = 1,
        require = 0)
    private int biomic$climateY(int y) {
        return TallWorlds.climateY(mc.theWorld, y);
    }
}
