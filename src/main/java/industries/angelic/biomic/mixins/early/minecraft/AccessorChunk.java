package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(Chunk.class)
public interface AccessorChunk {
}
