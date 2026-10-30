package industries.angelic.biomic.mixins.early.minecraft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import cpw.mods.fml.common.registry.VillagerRegistry;

/**
 * Forge keeps village piece handlers in a HashMap keyed by class, so their order (and the random numbers each one
 * draws) changes with every JVM start: the same seed then lays out a different village. Biomic shapes terrain for a
 * village before it is built, possibly in another session, so the layout must not change. Handlers run in class name
 * order instead.
 */
@Mixin(value = VillagerRegistry.class, remap = false)
public abstract class MixinVillagerRegistryOrder {

    @Redirect(
        method = "addExtraVillageComponents",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"),
        remap = false,
        require = 1)
    private static Collection<VillagerRegistry.IVillageCreationHandler> biomic$byClassName(
        Map<Class<?>, VillagerRegistry.IVillageCreationHandler> handlers) {
        List<Map.Entry<Class<?>, VillagerRegistry.IVillageCreationHandler>> entries = new ArrayList<>(
            handlers.entrySet());
        entries.sort(
            Comparator.comparing(
                entry -> entry.getKey()
                    .getName()));
        List<VillagerRegistry.IVillageCreationHandler> ordered = new ArrayList<>(entries.size());
        for (var entry : entries) ordered.add(entry.getValue());
        return ordered;
    }
}
