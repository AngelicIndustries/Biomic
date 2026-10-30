package industries.angelic.biomic.world;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import cpw.mods.fml.common.Loader;
import industries.angelic.biomic.api.ChunkVerticalLayout;

/** Validated live storage envelope. This does not select or stretch a terrain generator. */
public final class LiveChunkLayout {

    private LiveChunkLayout() {}

    private static final class Providers {

        static final boolean HOHENANGST = Loader.isModLoaded("hohenangst");
    }

    public static ChunkVerticalLayout of(World world) {
        if (world == null) throw new IllegalArgumentException("Missing live world");
        return select(world.getHeight(), Providers.HOHENANGST);
    }

    public static ChunkVerticalLayout of(Chunk chunk) {
        if (chunk == null) throw new IllegalArgumentException("Missing live chunk");
        ChunkVerticalLayout layout = of(chunk.worldObj);
        validateSections(layout, chunk.getBlockStorageArray().length);
        return layout;
    }

    static ChunkVerticalLayout select(int height, boolean companion) {
        if (height == 256) return ChunkVerticalLayout.LEGACY_256;
        if (height == 512 && companion) return ChunkVerticalLayout.EXTENDED_512;
        throw new IllegalStateException(
            "Unsupported Biomic live storage height " + height
                + "; 512 requires Hohenangst on both peers. Authored terrain height is selected independently by the save.");
    }

    static void validateSections(ChunkVerticalLayout layout, int sections) {
        if (sections != layout.sectionCount()) throw new IllegalStateException(
            "Biomic world/chunk height mismatch: " + layout.height() + " blocks but " + sections + " storage sections");
    }
}
