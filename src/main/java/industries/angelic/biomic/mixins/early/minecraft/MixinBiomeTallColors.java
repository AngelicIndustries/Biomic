package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import industries.angelic.biomic.world.TallWorlds;

/**
 * Grass and leaves are tinted by the climate at the height vanilla expects (TallWorlds.climateY): vanilla cools the
 * tint with height, so in a tall world the lowlands were tinted like mountainsides. Angelica's colour cache and every
 * block's colour multiplier call these with world coordinates; colours are only drawn for the client's world.
 */
@Mixin(BiomeGenBase.class)
public abstract class MixinBiomeTallColors {

    @ModifyArg(
        method = { "getBiomeGrassColor", "getBiomeFoliageColor" },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/BiomeGenBase;getFloatTemperature(III)F"),
        index = 1,
        require = 2)
    private int biomic$climateY(int y) {
        return TallWorlds.climateY(Minecraft.getMinecraft().theWorld, y);
    }
}
