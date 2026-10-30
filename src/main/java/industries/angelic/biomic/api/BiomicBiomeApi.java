package industries.angelic.biomic.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.VerticalBiomeCatalogue;
import industries.angelic.biomic.biome.VerticalBiomeDefinition;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.config.DebugConfig;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.storage.BiomeVolumeManager;
import industries.angelic.biomic.world.BiomicVolumeChunkManager;
import industries.angelic.biomic.world.modern.ModernBiomeCompatibility;

/** Position-aware biome lookup surface for JAS and other downstream integrations. */
public final class BiomicBiomeApi {

    private static final String LEGACY_KEY_PREFIX = "legacy:";
    private static final Set<String> REPORTED_UNKNOWN_KEYS = Collections.synchronizedSet(new HashSet<String>());

    private BiomicBiomeApi() {}

    public static BiomeGenBase getBiome(World world, int blockX, int blockY, int blockZ) {
        return find(world, blockX, blockY, blockZ).biome;
    }

    /**
     * A position's biome, key and tags (API version 2): what {@link #getBiome}, {@link #getBiomeKey} and
     * {@link #getBiomeTags} answer, from one lookup. The tag set is shared and immutable.
     */
    public record Lookup(BiomeGenBase biome, String key, Set<String> tags) {}

    /** The biome, key and tags of a position in one lookup (version 2); see {@link #getBiome} for when it loads. */
    public static Lookup lookup(World world, int blockX, int blockY, int blockZ) {
        LookupResult result = find(world, blockX, blockY, blockZ);
        return new Lookup(result.biome, result.key, result.tags);
    }

    /** One key's run in a column: blocks {@code fromY} up to {@code toY} (exclusive). */
    public record KeySpan(String key, int fromY, int toY) {}

    /**
     * The keys of a column from the bottom up (version 2), each with the heights it covers, as {@link #getBiomeKey}
     * reports them (a surface cell under its surface biome's key), at the volume's 4-block resolution. Read-only: an
     * unloaded chunk gives an empty list and is not loaded.
     */
    public static List<KeySpan> columnKeys(World world, int blockX, int blockZ) {
        requireWorld(world);
        if (!world.getChunkProvider()
            .chunkExists(blockX >> 4, blockZ >> 4)) return List.of();
        BiomeGenBase surface = getSurfaceBiome(world, blockX, blockZ);
        String worldType = world.getWorldInfo()
            .getTerrainType() == null ? ""
                : world.getWorldInfo()
                    .getTerrainType()
                    .getWorldTypeName();
        BiomeVolume volume = usesSavedVerticalVolume(
            world.getWorldChunkManager() instanceof BiomicVolumeChunkManager,
            world.provider == null ? Integer.MIN_VALUE : world.provider.dimensionId,
            worldType) ? BiomeVolumeManager.getVolume(world.getChunkFromChunkCoords(blockX >> 4, blockZ >> 4)) : null;
        return spans(surface, volume, Math.floorMod(blockX, 16), Math.floorMod(blockZ, 16), world.getHeight());
    }

    static List<KeySpan> spans(BiomeGenBase surface, BiomeVolume volume, int localX, int localZ, int height) {
        if (surface == null) surface = BiomeGenBase.plains;
        List<KeySpan> spans = new ArrayList<>();
        String key = null;
        int from = 0;
        for (int y = 0; y < height; y += 4) {
            String here = volume == null || !volume.containsBlockY(y) ? surfaceResult(surface).key
                : resolveStoredKey(surface, volume.getBiomeKey(localX >> 2, y >> 2, localZ >> 2)).key;
            if (key != null && !key.equals(here)) {
                spans.add(new KeySpan(key, from, y));
                from = y;
            }
            key = here;
        }
        if (key != null) spans.add(new KeySpan(key, from, height));
        return spans;
    }

    /** Resolve an already captured immutable volume; safe for render workers and never loads chunks. */
    public static BiomeGenBase getSnapshotBiome(BiomeGenBase surface, BiomeVolume volume, int x, int y, int z) {
        if (surface == null) surface = BiomeGenBase.plains;
        if (volume == null || !volume.containsBlockY(y)) return surface;
        String key = volume.getBiomeKey((x & 15) >> 2, y >> 2, (z & 15) >> 2);
        if (BiomeVolume.SURFACE_KEY.equals(key)) return surface;
        String hosted = industries.angelic.biomic.world.terrain.HostedIslandBiomes.modernKey(key);
        if (hosted != null) return ModernBiomeCompatibility.biome(hosted);
        BiomeGenBase biome = BiomicBiomes.getBiome(key);
        if (biome == null) biome = industries.angelic.biomic.biome.AlexCavesBiomeVisuals.colorBiome(key);
        if (biome == null) biome = ModernBiomeCompatibility.biomeForStoredKey(key);
        if (biome == null) {
            OrigenBiomeProfile profile = VerticalBiomeCatalogue.getFloatingIslandProfile(key);
            if (profile != null) biome = BiomicBiomes.getOrigenBiome(profile);
        }
        // Color lookup is a hot render-worker path: do not allocate the full API's result/tag sets here.
        return biome == null ? surface : biome;
    }

    /** Y-aware render color identity without changing the general gameplay/API biome facade. */
    public static BiomeGenBase getColorBiome(World world, int blockX, int blockY, int blockZ) {
        LookupResult result = find(world, blockX, blockY, blockZ);
        BiomeGenBase colors = industries.angelic.biomic.biome.AlexCavesBiomeVisuals.colorBiome(result.key);
        return colors == null ? result.biome : colors;
    }

    /**
     * Returns an authored Biomic key, an unknown stored key unchanged, or {@code legacy:<numeric-id>} for an ordinary
     * unregistered 1.7 surface biome.
     */
    public static String getBiomeKey(World world, int blockX, int blockY, int blockZ) {
        return find(world, blockX, blockY, blockZ).key;
    }

    public static Set<String> getBiomeTags(World world, int blockX, int blockY, int blockZ) {
        return find(world, blockX, blockY, blockZ).tags;
    }

    public static BiomeGenBase getSurfaceBiome(World world, int blockX, int blockZ) {
        requireWorld(world);
        BiomeGenBase surface = world.getBiomeGenForCoords(blockX, blockZ);
        return surface == null ? BiomeGenBase.plains : surface;
    }

    private static LookupResult find(World world, int blockX, int blockY, int blockZ) {
        requireWorld(world);
        BiomeGenBase surface = getSurfaceBiome(world, blockX, blockZ);
        String worldType = world.getWorldInfo()
            .getTerrainType() == null ? ""
                : world.getWorldInfo()
                    .getTerrainType()
                    .getWorldTypeName();
        if (!usesSavedVerticalVolume(
            world.getWorldChunkManager() instanceof BiomicVolumeChunkManager,
            world.provider == null ? Integer.MIN_VALUE : world.provider.dimensionId,
            worldType) || blockY < 0 || !world.blockExists(blockX, 0, blockZ)) {
            return surfaceResult(surface);
        }

        // The pinned modern target exposes BiomeManager#getBiome, which fuzzily zooms raw
        // quart-resolution noise biomes. A direct volume-cell lookup produces visible 4x4 edges
        // and is especially wrong where rivers or surface and cave biomes meet.
        // Remote WorldClient does not receive the world seed. Predicting there would use seed zero;
        // clients must use the authoritative synchronized volume below, not invent another habitat.

        Chunk chunk = world.getChunkFromChunkCoords(blockX >> 4, blockZ >> 4);
        BiomeVolume volume = BiomeVolumeManager.getVolume(chunk);
        // Coverage belongs to the stored volume, not the historical schema-1 constant. A legacy
        // volume still falls back above 255; a future negotiated schema-2 volume may cover 0..511.
        LookupResult result = resolve(surface, volume, Math.floorMod(blockX, 16), blockY, Math.floorMod(blockZ, 16));
        if (result.unknownStoredKey && REPORTED_UNKNOWN_KEYS.add(result.key)) {
            Biomic.LOG.warn("Unknown stored Biomic biome key '{}'; using the surface biome at runtime", result.key);
        }
        if (DebugConfig.traceVerticalBiomeStorage) {
            Biomic.LOG.info(
                "Resolved biome [{}, {}, {}] to key={} id={} vertical={}",
                blockX,
                blockY,
                blockZ,
                result.key,
                result.biome.biomeID,
                volume != null);
        }
        return result;
    }

    /**
     * The composed Biomic Nether deliberately retains its native climate/spawn chunk manager. Both the dedicated
     * server provider and a remote {@code ChunkProviderClient} still carry authoritative loaded BiomeVolume data,
     * so eligibility is world-type/dimension based and the ordinary loaded-only guard above remains mandatory.
     */
    static boolean usesSavedVerticalVolume(boolean markedManager, int dimension, String worldType) {
        // Eligibility only: worlds without a saved volume still fall back to their native surface biome.
        // The HA demo's explicitly saved Nether opt-in uses the same native chunk manager as legacy composition.
        return markedManager || dimension == -1 && "biomic_blend".equals(worldType);
    }

    static LookupResult resolve(BiomeGenBase surface, BiomeVolume volume, int localBlockX, int blockY,
        int localBlockZ) {
        if (surface == null) {
            surface = BiomeGenBase.plains;
        }
        if (volume == null || !volume.containsBlockY(blockY)) {
            return surfaceResult(surface);
        }
        if (localBlockX < 0 || localBlockX >= 16 || localBlockZ < 0 || localBlockZ >= 16) {
            throw new IndexOutOfBoundsException("Chunk-local biome lookup coordinates must be in [0, 15]");
        }

        String storedKey = volume.getBiomeKey(localBlockX >> 2, blockY >> 2, localBlockZ >> 2);
        return resolveStoredKey(surface, storedKey);
    }

    private static LookupResult resolveStoredKey(BiomeGenBase surface, String storedKey) {
        if (BiomeVolume.SURFACE_KEY.equals(storedKey)) {
            return surfaceResult(surface);
        }
        Resolution resolution = resolution(storedKey);
        return new LookupResult(
            resolution.biome() == null ? surface : resolution.biome(),
            storedKey,
            resolution.tags(),
            resolution.unknown());
    }

    /** What a stored key (not the surface marker) resolves to: its biome (null: the column's surface) and tags. */
    record Resolution(BiomeGenBase biome, Set<String> tags, boolean unknown) {}

    /** Each stored key's resolution, once everything is registered (BiomeCatalogue#isComplete): no set per lookup. */
    private static final java.util.concurrent.ConcurrentHashMap<String, Resolution> RESOLVED = new java.util.concurrent.ConcurrentHashMap<>();

    static Resolution resolution(String storedKey) {
        Resolution known = RESOLVED.get(storedKey);
        if (known != null) return known;
        Resolution resolved = resolve(storedKey);
        if (BiomeCatalogue.isComplete()) RESOLVED.putIfAbsent(storedKey, resolved);
        return resolved;
    }

    private static Resolution resolve(String storedKey) {
        String hosted = industries.angelic.biomic.world.terrain.HostedIslandBiomes.modernKey(storedKey);
        if (hosted != null) {
            BiomeGenBase biome = ModernBiomeCompatibility.biome(hosted);
            if (biome == null) return new Resolution(null, Collections.<String>emptySet(), true);
            LinkedHashSet<String> tags = new LinkedHashSet<>(BiomicBiomes.getTags(biome));
            tags.add(BiomeTags.FLOATING_ISLAND);
            return new Resolution(biome, Collections.unmodifiableSet(tags), false);
        }
        BiomeGenBase storedBiome = BiomicBiomes.getBiome(storedKey);
        if (storedBiome == null && industries.angelic.biomic.biome.AlexCavesBiomeVisuals.profile(storedKey) != null)
            return new Resolution(null, verticalTags(storedKey, Collections.<String>emptySet()), false);
        if (storedBiome == null) storedBiome = ModernBiomeCompatibility.biomeForStoredKey(storedKey);
        if (storedBiome == null) {
            OrigenBiomeProfile hostedProfile = VerticalBiomeCatalogue.getFloatingIslandProfile(storedKey);
            if (hostedProfile != null) {
                storedBiome = BiomicBiomes.getOrigenBiome(hostedProfile);
                if (storedBiome != null) return new Resolution(
                    storedBiome,
                    verticalTags(storedKey, BiomicBiomes.getTags(storedBiome)),
                    false);
            }
            return new Resolution(null, Collections.<String>emptySet(), true);
        }
        return new Resolution(storedBiome, verticalTags(storedKey, BiomicBiomes.getTags(storedKey)), false);
    }

    private static Set<String> verticalTags(String storedKey, Set<String> baseTags) {
        VerticalBiomeDefinition definition = VerticalBiomeCatalogue.get(storedKey);
        if (definition == null) return baseTags;
        LinkedHashSet<String> tags = new LinkedHashSet<>(baseTags);
        if (definition.hasTrait(VerticalBiomeDefinition.Trait.CAVE)) tags.add(BiomeTags.CAVE);
        if (definition.hasTrait(VerticalBiomeDefinition.Trait.FLOATING_ISLAND)) tags.add(BiomeTags.FLOATING_ISLAND);
        if (VerticalBiomeCatalogue.isAeropelagoHostedKey(storedKey)) tags.add(BiomeTags.ARCHIPELAGO);
        return Collections.unmodifiableSet(tags);
    }

    static LookupResult surfaceResult(BiomeGenBase surface) {
        String key = BiomicBiomes.getKey(surface);
        if (key == null) {
            return new LookupResult(
                surface,
                LEGACY_KEY_PREFIX + surface.biomeID,
                Collections.<String>emptySet(),
                false);
        }
        return new LookupResult(surface, key, BiomicBiomes.getTags(surface), false);
    }

    private static void requireWorld(World world) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
    }

    static final class LookupResult {

        final BiomeGenBase biome;
        final String key;
        final Set<String> tags;
        final boolean unknownStoredKey;

        private LookupResult(BiomeGenBase biome, String key, Set<String> tags, boolean unknownStoredKey) {
            this.biome = biome;
            this.key = key;
            this.tags = tags;
            this.unknownStoredKey = unknownStoredKey;
        }

    }
}
