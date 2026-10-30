package industries.angelic.biomic.mixins.late.angelica;

import net.irisshaders.iris.api.v0.item.IrisItemLightProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;

import com.gtnewhorizons.angelica.api.IDynamicLightProducer;

import ganymedes01.etfuturum.items.ItemGlowBerries;

/** Optional client-only item light; matches EFR's placed berry-bearing cave vine (14). */
@Mixin(value = ItemGlowBerries.class, remap = false)
public abstract class MixinGlowBerriesLight implements IDynamicLightProducer, IrisItemLightProvider {

    @Override
    public int getLuminance(ItemStack stack) {
        return 14;
    }

    @Override
    public int getLightEmission(EntityPlayer player, ItemStack stack) {
        // Shader-held light is a separate path from Angelica's moving light sources.
        return getLuminance(stack);
    }
}
