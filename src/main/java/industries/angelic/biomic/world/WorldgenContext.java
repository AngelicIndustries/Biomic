package industries.angelic.biomic.world;

import net.minecraft.world.World;

import industries.angelic.biomic.config.WorldgenConfigSnapshot;

/** Immutable seed and frozen-settings context shared by every generation stage. */
public final class WorldgenContext {

    private static final long CHUNK_X_MULTIPLIER = 341873128712L;
    private static final long CHUNK_Z_MULTIPLIER = 132897987541L;
    // Generator versions are save-compatibility boundaries, not implicit random salts. Increment this only for an
    // intentional global reseed of every named worldgen phase.
    private static final long SEED_LAYOUT_VERSION = 8L;

    private final BiomicWorldSettings settings;
    private final WorldgenConfigSnapshot config;
    private final industries.angelic.biomic.world.cave.CaveExpansionSettings caveExpansion;
    private final industries.angelic.biomic.compat.biome.NativeHabitatSettings nativeHabitats;
    private final industries.angelic.biomic.world.alexscaves.AlexCavesSettings alexCaves;

    private WorldgenContext(BiomicWorldSettings settings, WorldgenConfigSnapshot config,
        industries.angelic.biomic.world.cave.CaveExpansionSettings caveExpansion,
        industries.angelic.biomic.compat.biome.NativeHabitatSettings nativeHabitats,
        industries.angelic.biomic.world.alexscaves.AlexCavesSettings alexCaves) {
        this.settings = settings;
        this.config = config;
        this.caveExpansion = caveExpansion;
        this.nativeHabitats = nativeHabitats;
        this.alexCaves = alexCaves;
    }

    public static WorldgenContext forWorld(World world) {
        return new WorldgenContext(
            BiomicWorldSettings.loadOrCreate(world),
            WorldgenConfigSnapshot.forWorld(world),
            industries.angelic.biomic.world.cave.CaveExpansionWorldData.peek(world),
            industries.angelic.biomic.compat.biome.NativeHabitatWorldData.peek(world),
            industries.angelic.biomic.world.alexscaves.AlexCavesWorldData.peek(world));
    }

    public long seedForPhase(WorldgenPhase phase) {
        if (phase == null) {
            throw new IllegalArgumentException("Worldgen phase cannot be null");
        }
        return mix64(settings.getWorldSeed() ^ phase.getSalt() ^ SEED_LAYOUT_VERSION);
    }

    public long seedForChunk(WorldgenPhase phase, int chunkX, int chunkZ) {
        return mix64(seedForPhase(phase) ^ chunkX * CHUNK_X_MULTIPLIER ^ chunkZ * CHUNK_Z_MULTIPLIER);
    }

    public int getGeneratorVersion() {
        return settings.getGeneratorVersion();
    }

    public int getSeaLevel() {
        return settings.getSeaLevel();
    }

    public WorldgenConfigSnapshot getConfig() {
        return config;
    }

    public industries.angelic.biomic.world.cave.CaveExpansionSettings getCaveExpansion() {
        return caveExpansion;
    }

    public industries.angelic.biomic.compat.biome.NativeHabitatSettings getNativeHabitats() {
        return nativeHabitats;
    }

    public industries.angelic.biomic.world.alexscaves.AlexCavesSettings getAlexCaves() {
        return alexCaves;
    }

    public BiomicWorldSettings getSettings() {
        return settings;
    }

    public static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }
}
