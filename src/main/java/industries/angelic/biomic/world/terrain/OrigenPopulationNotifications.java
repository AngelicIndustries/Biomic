package industries.angelic.biomic.world.terrain;

import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.WorldServer;

import industries.angelic.biomic.api.ChunkVerticalLayout;

/**
 * Height-aware transport for Origen population notifications. Recording is detached and cannot load chunks or send
 * packets; {@link #deliver(WorldServer, PlayerManager, int, int)} is an explicit publication boundary.
 */
final class OrigenPopulationNotifications {

    interface Sink {

        void markBlock(int blockX, int blockY, int blockZ);

        void deferSections(int sectionMask);
    }

    private final ChunkVerticalLayout layout;
    private final int clumpingThreshold;
    private long[] retained;
    private int retainedCount;
    private boolean sectionRefresh;
    private int sectionMask;
    private long[] tileEntities = new long[2];
    private int tileEntityCount;
    private boolean lightingChanged;

    OrigenPopulationNotifications(ChunkVerticalLayout layout, int clumpingThreshold) {
        if (layout == null) throw new IllegalArgumentException("Missing population layout");
        if (clumpingThreshold < 1) throw new IllegalArgumentException("Clumping threshold must be positive");
        this.layout = layout;
        this.clumpingThreshold = clumpingThreshold;
        retained = new long[Math.min(64, clumpingThreshold)];
    }

    void record(int x, int y, int z, boolean tileEntity, boolean blockLightingChanged) {
        long position = packLocalPosition(layout, x, y, z);
        sectionMask |= 1 << (y >> 4);
        lightingChanged |= blockLightingChanged;
        if (tileEntity) tileEntityCount = addUnique(tileEntities, tileEntityCount, position);
        if (sectionRefresh || contains(retained, retainedCount, position)) return;
        if (retainedCount == retained.length) retained = grow(retained, clumpingThreshold);
        retained[retainedCount++] = position;
        sectionRefresh = retainedCount >= clumpingThreshold;
    }

    void deliver(WorldServer world, PlayerManager manager, int chunkX, int chunkZ) {
        if (world == null || manager == null) throw new IllegalArgumentException("Missing population delivery target");
        deliver(chunkX, chunkZ, new Sink() {

            @Override
            public void markBlock(int blockX, int blockY, int blockZ) {
                manager.markBlockForUpdate(blockX, blockY, blockZ);
            }

            @Override
            public void deferSections(int mask) {
                // Retained blocks were already marked individually; whole-section refreshes are not batched.
            }
        });
    }

    void deliver(int chunkX, int chunkZ, Sink sink) {
        if (sink == null) throw new IllegalArgumentException("Missing population notification sink");
        int representedLegacySections = 0;
        for (int index = 0; index < retainedCount; index++) {
            long position = retained[index];
            int y = localY(position);
            if (y < 256) representedLegacySections |= 1 << (y >> 4);
            sink.markBlock((chunkX << 4) + localX(position), y, (chunkZ << 4) + localZ(position));
        }
        if (!sectionRefresh) return;

        int upperSections = sectionMask & 0xFFFF0000;
        if (upperSections != 0) {
            // Upper marks bypass Forge's packed-short clumping counter. A mixed prefix therefore cannot reliably
            // trigger the lower section snapshot either; publish every touched section through the authoritative
            // full-height batch. Its existing follow-up also covers all tile entities in those sections.
            sink.deferSections(sectionMask);
            return;
        }

        // Preserve Forge's exact lower-256 threshold path. Upper marks bypass vanilla's packed-short list in the
        // height companion, but this branch is reached only when every touched section is representable by vanilla.
        int missingLegacySections = (sectionMask & 0xFFFF) & ~representedLegacySections;
        for (int section = 0; section < 16; section++) {
            if ((missingLegacySections & 1 << section) != 0) {
                sink.markBlock(chunkX << 4, section << 4, chunkZ << 4);
            }
        }
        // Lower S21 refreshes still need Forge's explicit tile-description follow-up.
        for (int index = 0; index < tileEntityCount; index++) {
            long position = tileEntities[index];
            if (!contains(retained, retainedCount, position)) {
                sink.markBlock((chunkX << 4) + localX(position), localY(position), (chunkZ << 4) + localZ(position));
            }
        }
    }

    boolean lightingChanged() {
        return lightingChanged;
    }

    static long packLocalPosition(ChunkVerticalLayout layout, int x, int y, int z) {
        if (layout == null || !layout.containsY(y)) {
            throw new IllegalArgumentException("Population Y outside local column: " + y);
        }
        return (long) (x & 15) << 36 | (long) (z & 15) << 32 | y & 0xFFFF_FFFFL;
    }

    static int localX(long packed) {
        return (int) (packed >> 36 & 15);
    }

    static int localY(long packed) {
        return (int) packed;
    }

    static int localZ(long packed) {
        return (int) (packed >> 32 & 15);
    }

    private int addUnique(long[] values, int count, long value) {
        if (contains(values, count, value)) return count;
        if (count == values.length) {
            long[] grown = grow(values, layout.blockCount());
            tileEntities = grown;
            values = grown;
        }
        values[count] = value;
        return count + 1;
    }

    private static long[] grow(long[] values, int maximum) {
        int next = Math.min(maximum, Math.max(values.length + 1, values.length << 1));
        long[] grown = new long[next];
        System.arraycopy(values, 0, grown, 0, values.length);
        return grown;
    }

    private static boolean contains(long[] values, int count, long target) {
        for (int index = 0; index < count; index++) {
            if (values[index] == target) return true;
        }
        return false;
    }
}
