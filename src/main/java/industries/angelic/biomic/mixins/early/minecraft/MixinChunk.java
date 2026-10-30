package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.api.BlockSubstitutes;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.storage.BiomeVolumeChunk;
import industries.angelic.biomic.world.ChunkProviderBiomic;
import industries.angelic.biomic.world.ChunkProviderBiomicTall;

@Mixin(Chunk.class)
public abstract class MixinChunk implements BiomeVolumeChunk {

    @Unique
    private BiomeVolume biomic$biomeVolume;

    @Unique
    private boolean biomic$populationSettled;

    @Override
    public BiomeVolume biomic$getBiomeVolume() {
        return biomic$biomeVolume;
    }

    @Override
    public void biomic$setBiomeVolume(BiomeVolume volume) {
        biomic$biomeVolume = volume;
    }

    /**
     * A box-confined structure write runs the block's onBlockAdded itself, reading the box (StructurePopulationBox).
     */
    @com.llamalad7.mixinextras.injector.WrapWithCondition(
        method = "func_150807_a",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onBlockAdded(Lnet/minecraft/world/World;III)V"),
        require = 1)
    private boolean biomic$deferBlockAdded(net.minecraft.block.Block block, net.minecraft.world.World world, int x,
        int y, int z) {
        return !industries.angelic.biomic.world.structure.StructurePopulationBox.defersBlockAdded();
    }

    /**
     * Blocks swapped in Biomic's worlds (api.BlockSubstitutes), whoever writes them. Biomic's own features write past
     * this and swap them themselves (OrigenFeatureRuntime#placeInChunk).
     */
    @ModifyVariable(method = "func_150807_a", at = @At("HEAD"), argsOnly = true, require = 1)
    private Block biomic$substitute(Block block, int localX, int y, int localZ, Block written, int metadata) {
        boolean generating = industries.angelic.biomic.world.GenerationScope.active();
        if (!BlockSubstitutes.watched(block, generating)) return block;
        Chunk chunk = (Chunk) (Object) this;
        if (!(chunk.worldObj instanceof WorldServer world)) return block;
        IChunkProvider provider = world.theChunkProviderServer == null ? null
            : world.theChunkProviderServer.currentChunkProvider;
        if (!(provider instanceof ChunkProviderBiomic || provider instanceof ChunkProviderBiomicTall)) return block;
        return BlockSubstitutes.substitute(block, chunk, localX, localZ, generating);
    }

    /**
     * While a Biomic chunk generates, a log or leaf block that a write replaces does not flag the leaves around it for
     * decay: the flag landed on a tree only if it already stood when a structure or another mod's generator cut
     * through it, so it depended on the order chunks were generated in. Biomic's own population never calls this.
     */
    @com.llamalad7.mixinextras.injector.WrapWithCondition(
        method = "func_150807_a",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;breakBlock(Lnet/minecraft/world/World;IIILnet/minecraft/block/Block;I)V"),
        require = 1)
    private boolean biomic$noDecayNoticeInGeneration(Block old, net.minecraft.world.World world, int x, int y, int z,
        Block same, int metadata) {
        if (!(old instanceof net.minecraft.block.BlockLeaves || old instanceof net.minecraft.block.BlockLog)
            || !industries.angelic.biomic.world.GenerationScope.active()
            || !(world instanceof WorldServer server)) return true;
        IChunkProvider provider = server.theChunkProviderServer == null ? null
            : server.theChunkProviderServer.currentChunkProvider;
        return !(provider instanceof ChunkProviderBiomic || provider instanceof ChunkProviderBiomicTall);
    }

    /** {@code -Dbiomic.traceCell}: every write to a traced cell, with its caller (CellTrace). */
    @Inject(method = "func_150807_a", at = @At("HEAD"))
    private void biomic$traceCell(int localX, int y, int localZ, Block block, int metadata,
        org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callback) {
        if (!industries.angelic.biomic.world.terrain.CellTrace.ON) return;
        Chunk chunk = (Chunk) (Object) this;
        int x = (chunk.xPosition << 4) + localX, z = (chunk.zPosition << 4) + localZ;
        if (industries.angelic.biomic.world.terrain.CellTrace.traced(x, y, z))
            industries.angelic.biomic.world.terrain.CellTrace.log(
                x,
                y,
                z,
                "chunk write {}:{} over {}:{} {}",
                Block.blockRegistry.getNameForObject(block),
                metadata,
                Block.blockRegistry.getNameForObject(chunk.getBlock(localX, y, localZ)),
                chunk.getBlockMetadata(localX, y, localZ),
                industries.angelic.biomic.world.terrain.CellTrace.caller());
    }

    @Shadow
    public boolean field_150815_m;

    /**
     * Send readiness. Vanilla sends a chunk once {@code func_150802_k} holds, and Hodgepodge's fast loading overwrites
     * that check to read only {@code field_150815_m}, which this chunk tick sets, so chunks go out before they are
     * even populated. Biomic population writes features into all eight neighbours; every write into an already-sent
     * chunk reaches the client as a block change that it relights (at 512 height most of a second per burst).
     * Keep the latch closed until the whole 3x3 neighbourhood is populated; after that no population reaches it.
     */
    @Inject(method = "func_150804_b", at = @At("RETURN"))
    private void biomic$sendOnlyOncePopulationSettled(boolean skipRecheckGaps, CallbackInfo callback) {
        if (!field_150815_m || biomic$populationSettled) return;
        Chunk chunk = (Chunk) (Object) this;
        if (!(chunk.worldObj instanceof WorldServer world)) return;
        ChunkProviderServer server = world.theChunkProviderServer;
        if (server == null || !(server.currentChunkProvider instanceof ChunkProviderBiomic
            || server.currentChunkProvider instanceof ChunkProviderBiomicTall)) return;
        boolean settled = chunk.isTerrainPopulated;
        for (int dx = -1; settled && dx <= 1; dx++) for (int dz = -1; settled && dz <= 1; dz++) {
            if (dx == 0 && dz == 0) continue;
            Chunk neighbour = (Chunk) server.loadedChunkHashMap
                .getValueByKey(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition + dx, chunk.zPosition + dz));
            settled = neighbour != null && neighbour.isTerrainPopulated;
        }
        if (settled) biomic$populationSettled = true;
        else field_150815_m = false;
    }
}
