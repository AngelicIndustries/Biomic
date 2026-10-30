package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.gen.structure.StructureMineshaftPieces;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureMineshaftPieces.Corridor.class)
public interface AccessorMineshaftCorridor {

    @Accessor("hasSpiders")
    boolean biomic$hasSpiders();

    @Accessor("spawnerPlaced")
    void biomic$setSpawnerPlaced(boolean placed);

    @Accessor("sectionCount")
    int biomic$sectionCount();
}
