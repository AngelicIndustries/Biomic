package industries.angelic.biomic.world;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import industries.angelic.biomic.api.ChunkVerticalLayout;

/** Shared full-height chunk construction; it does not resolve terrain or select a world policy. */
public final class TallTerrainMaterializer {

    private TallTerrainMaterializer() {}

    public static Chunk materialize(World world, int chunkX, int chunkZ, Block[] blocks, byte[] metadata) {
        if (world == null || blocks == null || metadata == null) {
            throw new IllegalArgumentException("Tall materialization inputs are required");
        }
        ChunkVerticalLayout layout = ChunkVerticalLayout.EXTENDED_512;
        if (blocks.length != layout.blockCount() || metadata.length != layout.blockCount()) {
            throw new IllegalArgumentException("Tall terrain payload has the wrong physical size");
        }
        Chunk chunk = new Chunk(world, chunkX, chunkZ);
        if (LiveChunkLayout.of(chunk)
            .height() != layout.height()) {
            throw new IllegalStateException("Constructed chunk does not expose the required 512-block layout");
        }
        ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
        for (int localX = 0; localX < 16; localX++) for (int localZ = 0; localZ < 16; localZ++) {
            for (int y = 0; y < layout.height(); y++) {
                int index = layout.index(localX, y, localZ);
                Block block = blocks[index];
                if (block == null) continue;
                int sectionIndex = y >> 4;
                ExtendedBlockStorage section = sections[sectionIndex];
                if (section == null) {
                    section = new ExtendedBlockStorage(sectionIndex << 4, !world.provider.hasNoSky);
                    sections[sectionIndex] = section;
                }
                section.func_150818_a(localX, y & 15, localZ, block);
                section.setExtBlockMetadata(localX, y & 15, localZ, metadata[index] & 15);
            }
        }
        return chunk;
    }
}
