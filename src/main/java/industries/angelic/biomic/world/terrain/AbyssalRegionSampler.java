package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;

import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.alexscaves.OceanTrenchPlanner;

/** Save-versioned sparse 3D ownership for source-grid Ocean Trench starts, admitted only below real ocean water. */
public final class AbyssalRegionSampler {

    private static final int REGION_CHUNKS = 32, OWNERSHIP_RADIUS = OceanTrenchPlanner.MAX_REALIZED_RADIUS + 16;
    private final long seed;
    private final IntBinaryOperator floor;
    private final ThreadLocal<FloorSample> lastFloor = new ThreadLocal<>();

    public AbyssalRegionSampler(long seed, IntBinaryOperator floor) {
        this.seed = seed;
        this.floor = java.util.Objects.requireNonNull(floor, "floor");
    }

    public HostedAbyssalRuntime.Start start(int regionX, int regionZ) {
        long mixed = WorldgenContext
            .mix64(seed ^ 0x4142595353414C31L ^ (long) regionX * 341873128712L ^ (long) regionZ * 132897987541L);
        int evenX = Math.floorMod((int) mixed, REGION_CHUNKS / 2) * 2,
            evenZ = Math.floorMod((int) (mixed >>> 32), REGION_CHUNKS / 2) * 2;
        return new HostedAbyssalRuntime.Start(regionX * REGION_CHUNKS + evenX, regionZ * REGION_CHUNKS + evenZ);
    }

    public HostedAbyssalRuntime.Identity identity(int x, int sourceY, int z) {
        if (sourceY < OceanTrenchPlanner.SOURCE_MIN_Y - 48 || sourceY > OceanTrenchPlanner.SOURCE_SEA_LEVEL)
            return compatibleOcean(x, z) ? HostedAbyssalRuntime.Identity.COMPATIBLE_OCEAN
                : HostedAbyssalRuntime.Identity.INCOMPATIBLE;
        int rx = Math.floorDiv(x >> 4, REGION_CHUNKS), rz = Math.floorDiv(z >> 4, REGION_CHUNKS);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            HostedAbyssalRuntime.Start start = start(rx + dx, rz + dz);
            long ox = (long) x - (start.x() + 8L), oz = (long) z - (start.z() + 8L);
            if (Math.abs(ox) <= OWNERSHIP_RADIUS && Math.abs(oz) <= OWNERSHIP_RADIUS && compatibleOcean(x, z))
                return HostedAbyssalRuntime.Identity.ABYSSAL;
        }
        return compatibleOcean(x, z) ? HostedAbyssalRuntime.Identity.COMPATIBLE_OCEAN
            : HostedAbyssalRuntime.Identity.INCOMPATIBLE;
    }

    public List<HostedAbyssalRuntime.Start> candidates(int chunkX, int chunkZ) {
        int rx = Math.floorDiv(chunkX, REGION_CHUNKS), rz = Math.floorDiv(chunkZ, REGION_CHUNKS);
        List<HostedAbyssalRuntime.Start> out = new ArrayList<>(9);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            HostedAbyssalRuntime.Start start = start(rx + dx, rz + dz);
            int reach = OceanTrenchPlanner.MAX_REALIZED_RADIUS + 1, minX = chunkX << 4, minZ = chunkZ << 4;
            if (start.x() + reach >= minX && start.x() - reach <= minX + 15
                && start.z() + reach >= minZ
                && start.z() - reach <= minZ + 15
                && identity(start.x() + 8, OceanTrenchPlanner.SOURCE_CENTER_Y, start.z() + 8)
                    == HostedAbyssalRuntime.Identity.ABYSSAL)
                out.add(start);
        }
        return out;
    }

    private boolean compatibleOcean(int x, int z) {
        FloorSample known = lastFloor.get();
        if (known == null || known.x() != x || known.z() != z) {
            known = new FloorSample(x, z, floor.applyAsInt(x, z));
            lastFloor.set(known);
        }
        return known.y() <= ModernOceanFloor.MAX_ABYSSAL_FLOOR;
    }

    private record FloorSample(int x, int z, int y) {}

    /** Named threshold is part of VERSION, not a physical-world ceiling. */
    private static final class ModernOceanFloor {

        static final int MAX_ABYSSAL_FLOOR = 55;
    }
}
