package industries.angelic.biomic.world.structure;

import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.WorldgenPhase;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;

/** Pure Biomic placement predicate for ordinary underground mineshafts. */
final class BiomicMineshaftPlacement {

    static final int REFERENCE_DEPTH_BELOW_SEA = 28;
    static final int REQUIRED_ROCK_COVER = 16;

    private static final long CHANCE_SALT = 0xD1B54A32D192ED03L;
    private static final int[][] HOST_SAMPLE_OFFSETS = { { 0, 0 }, { -8, -8 }, { -8, 8 }, { 8, -8 }, { 8, 8 } };

    private final WorldgenContext context;
    private final StructureTerrain terrain;
    private final industries.angelic.biomic.world.terrain.CavePockets pockets;
    private final int referenceY;
    /** Pure per chunk, but vanilla re-asks it for every chunk within eight of each generated chunk. */
    private final java.util.Map<Long, Boolean> spawnable = new java.util.LinkedHashMap<Long, Boolean>(
        1024,
        .75F,
        true) {

        @Override
        protected boolean removeEldestEntry(java.util.Map.Entry<Long, Boolean> entry) {
            return size() > 4096;
        }
    };

    BiomicMineshaftPlacement(WorldgenContext context, BiomicTerrainSampler terrainSampler) {
        this(context, StructureTerrain.legacy(context, terrainSampler));
    }

    BiomicMineshaftPlacement(WorldgenContext context, StructureTerrain terrain) {
        if (context == null || terrain == null) {
            throw new IllegalArgumentException("Worldgen context and structure terrain cannot be null");
        }
        this.context = context;
        this.terrain = terrain;
        this.pockets = new industries.angelic.biomic.world.terrain.CavePockets(
            context.getSettings()
                .getWorldSeed());
        this.referenceY = Math.max(8, terrain.seaLevel() - terrain.verticalOffset() - REFERENCE_DEPTH_BELOW_SEA)
            + terrain.verticalOffset();
    }

    int verticalOffset() {
        return terrain.verticalOffset();
    }

    boolean canSpawnAt(int chunkX, int chunkZ) {
        long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        synchronized (spawnable) {
            Boolean known = spawnable.get(key);
            if (known != null) return known;
        }
        boolean result = evaluateSpawnAt(chunkX, chunkZ);
        synchronized (spawnable) {
            spawnable.put(key, result);
        }
        return result;
    }

    /**
     * A mineshaft starts only at the centre of a rock pocket (CavePockets), in three pockets in four: the caves leave
     * the pocket solid, so the whole shaft runs through rock (review: "most of the time the mineshaft stops in the
     * middle of nothing"; mineshafts used to start anywhere in the swiss cheese).
     */
    private boolean evaluateSpawnAt(int chunkX, int chunkZ) {
        long structureSeed = context.seedForChunk(WorldgenPhase.STRUCTURES, chunkX, chunkZ);
        var settings = industries.angelic.biomic.config.StructureConfig.get().mineshafts;
        if (!settings.enabled()) return false;
        var pocket = pockets.containing((chunkX << 4) + 8, (chunkZ << 4) + 8);
        if (pocket == null || pocket.x() >> 4 != chunkX || pocket.z() >> 4 != chunkZ) return false;
        return toUnitInterval(WorldgenContext.mix64(structureSeed ^ CHANCE_SALT)) < POCKET_CHANCE
            && hasHostRock(chunkX, chunkZ);
    }

    private static final double POCKET_CHANCE = 0.75D;

    /** How far to move a raised start so its middle stands at its pocket's middle. */
    int pocketShift(net.minecraft.world.gen.structure.StructureStart start) {
        var box = start.getBoundingBox();
        var pocket = pockets.containing((box.minX + box.maxX) / 2, (box.minZ + box.maxZ) / 2);
        if (pocket == null) return 0;
        // The start room is the first piece; its chunk holds the pocket's centre.
        var room = ((net.minecraft.world.gen.structure.StructureComponent) start.getComponents()
            .get(0)).getBoundingBox();
        return terrain.physicalY(pocket.y()) - (room.minY + room.maxY) / 2;
    }

    boolean hasHostRock(int chunkX, int chunkZ) {
        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;
        for (int[] offset : HOST_SAMPLE_OFFSETS) {
            int blockX = centerX + offset[0];
            int blockZ = centerZ + offset[1];
            if (terrain.surfaceHeight(blockX, blockZ) - referenceY < REQUIRED_ROCK_COVER
                || !terrain.solidAt(blockX, referenceY, blockZ)) {
                return false;
            }
        }
        return true;
    }

    /** Ground over every piece's ceiling, at least, once the whole mineshaft is sunk; and room above the floor. */
    static final int BURIAL_COVER = 12, FLOOR_MARGIN = 8;

    /**
     * How far to sink a whole mineshaft (zero or less) so that every piece lies at least BURIAL_COVER under the ground
     * of each column it spans (sampled at its corners and every 8 blocks), or null where that would take it into the
     * floor. Vanilla puts a mineshaft's top at most ten blocks under the sea, so valleys and canyon floors laid it
     * open; sinking it whole keeps every piece instead of dropping the exposed ones.
     */
    Integer burial(net.minecraft.world.gen.structure.StructureStart start) {
        int shift = 0, lowest = Integer.MAX_VALUE;
        for (Object entry : start.getComponents()) {
            var box = ((net.minecraft.world.gen.structure.StructureComponent) entry).getBoundingBox();
            lowest = Math.min(lowest, box.minY);
            for (int x = box.minX;; x = Math.min(x + 8, box.maxX)) {
                for (int z = box.minZ;; z = Math.min(z + 8, box.maxZ)) {
                    shift = Math.min(shift, terrain.carvedGroundHeight(x, z) - BURIAL_COVER - box.maxY);
                    if (z == box.maxZ) break;
                }
                if (x == box.maxX) break;
            }
        }
        if (lowest == Integer.MAX_VALUE || lowest + shift < terrain.minY() + FLOOR_MARGIN) return null;
        return shift;
    }

    boolean hasHostRockForLayout(net.minecraft.world.gen.structure.StructureStart start) {
        if (!terrain.exactMaterials()) return hasColumnCoverForLayout(start);
        // Terrain-only queries cannot load/populate live chunks recursively. Cache only this admission's chunks.
        java.util.Map<Long, byte[]> chunks = new java.util.HashMap<>();
        MineshaftRockEnvelope.Rock rock = (x, y, z) -> {
            int cx = x >> 4, cz = z >> 4;
            long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
            byte[] materials = chunks.computeIfAbsent(key, ignored -> terrain.materials(cx, cz));
            byte material = materials[terrain.layout()
                .index(x & 15, y, z & 15)];
            return material != BiomicTerrainSampler.AIR && material != BiomicTerrainSampler.WATER
                && material != BiomicTerrainSampler.LAVA
                && material != BiomicTerrainSampler.POWDER_SNOW
                && material != BiomicTerrainSampler.SAND
                && material != BiomicTerrainSampler.RED_SAND
                && material != BiomicTerrainSampler.GRAVEL
                && material != BiomicTerrainSampler.WHITE_CONCRETE_POWDER;
        };
        for (Object entry : start.getComponents()) {
            var box = ((net.minecraft.world.gen.structure.StructureComponent) entry).getBoundingBox();
            if (!MineshaftRockEnvelope.contains(
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ,
                terrain.minY(),
                terrain.maxY(),
                rock)) {
                return false;
            }
        }
        return !start.getComponents()
            .isEmpty();
    }

    /** Every piece stays under rock cover at its corners and centre; open caves are left to the cave-biome check. */
    private boolean hasColumnCoverForLayout(net.minecraft.world.gen.structure.StructureStart start) {
        for (Object entry : start.getComponents()) {
            var box = ((net.minecraft.world.gen.structure.StructureComponent) entry).getBoundingBox();
            if (box.minY < terrain.minY() + 2 || box.maxY > terrain.maxY() - 4) return false;
            int[][] columns = { { box.minX, box.minZ }, { box.minX, box.maxZ }, { box.maxX, box.minZ },
                { box.maxX, box.maxZ }, { (box.minX + box.maxX) >> 1, (box.minZ + box.maxZ) >> 1 } };
            for (int[] column : columns) {
                if (terrain.carvedGroundHeight(column[0], column[1]) - box.maxY < REQUIRED_ROCK_COVER / 2) return false;
            }
        }
        return !start.getComponents()
            .isEmpty();
    }

    private static double toUnitInterval(long bits) {
        return (bits >>> 11) * 0x1.0p-53;
    }
}
