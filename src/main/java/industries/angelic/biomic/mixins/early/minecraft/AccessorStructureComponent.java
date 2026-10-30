package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.gen.structure.StructureComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StructureComponent.class)
public interface AccessorStructureComponent {

    @Invoker("getXWithOffset")
    int biomic$x(int x, int z);

    @Invoker("getYWithOffset")
    int biomic$y(int y);

    @Invoker("getZWithOffset")
    int biomic$z(int x, int z);
}
