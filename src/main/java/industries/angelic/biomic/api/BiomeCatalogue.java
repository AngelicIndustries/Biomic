package industries.angelic.biomic.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.biome.VerticalBiomeCatalogue;
import industries.angelic.biomic.biome.VerticalBiomeDefinition;
import industries.angelic.biomic.compat.biome.BopBiomeSelection;
import industries.angelic.biomic.compat.biome.BopNativeBiomes;
import industries.angelic.biomic.compat.biome.SaltMarshCompat;
import industries.angelic.biomic.config.ExpansionConfig;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.NetherWorldData;
import industries.angelic.biomic.world.modern.ModernBiomeCompatibility;
import industries.angelic.biomic.world.terrain.HostedIslandBiomes;
import industries.angelic.biomic.world.terrain.RegionalHabitatSampler;

/**
 * Every biome key Biomic's worlds report (API version 2), with the tags and the biome the position lookups
 * ({@link BiomicBiomeApi}) give it, read once at startup rather than per position: what a chunk's 3D biome volume can
 * store (the surface marker, caves, both island families) and the surface identities Biomic generates or hosts in
 * the overworld and the Nether. Entries are resolved by the same code as {@link BiomicBiomeApi#getBiomeTags}, so a
 * key's tags here are the tags a cell with that key reports.
 *
 * <p>
 * Complete once Forge has finished loading ({@link #isComplete}): BOP's and Netherlicious's habitats depend on what
 * is installed and enabled. It lists what a new world can store with this installation and its current config (BOP's
 * curated or full selection, the Nether's switches), leaving out keys Biomic registers but never generates. A given
 * world stores a subset: it depends on the world's height, and saved worlds keep the selection they were made with.
 */
public final class BiomeCatalogue {

    /** Where a key lives. */
    public enum Kind {
        /** A surface biome, or the surface marker. */
        SURFACE,
        /** Underground: Origen's, Regions Unexplored's and Alex's Caves' cave biomes. */
        CAVE,
        /** A sky island: Origen's floating profiles, Aeropelago's hosts, and the habitats islands host. */
        FLOATING,
        /** The Tartarus Nether's own biomes and the other mods' biomes it hosts. */
        NETHER
    }

    /**
     * One key: its tags (immutable), the biome it resolves to, null where that is the column's own surface biome
     * ({@code biomic:surface}, Alex's Caves), and where it lives.
     */
    public record Entry(String key, Set<String> tags, BiomeGenBase biome, Kind kind) {}

    private static volatile List<Entry> built;
    private static volatile Map<String, Entry> byKey;

    private BiomeCatalogue() {}

    /** Whether Forge has finished loading, so the catalogue lists every installed mod's habitats. */
    public static boolean isComplete() {
        try {
            return Loader.instance()
                .hasReachedState(LoaderState.AVAILABLE);
        } catch (RuntimeException | LinkageError notLoading) {
            return false;
        }
    }

    /**
     * Every key, in a stable order: the 3D volume's keys as declared, then the island habitats, then the surface and
     * Nether identities, each by key. Built once complete; asked earlier, it lists what is registered so far.
     */
    public static List<Entry> entries() {
        List<Entry> known = built;
        if (known != null) return known;
        List<Entry> entries = build();
        if (isComplete()) {
            Map<String, Entry> keys = new LinkedHashMap<>();
            for (Entry entry : entries) keys.put(entry.key(), entry);
            byKey = Collections.unmodifiableMap(keys);
            built = entries;
        }
        return entries;
    }

    /** The entry of a key, or null when Biomic does not know it. */
    public static Entry get(String key) {
        entries();
        Map<String, Entry> keys = byKey;
        if (keys != null) return keys.get(key);
        for (Entry entry : entries()) if (entry.key()
            .equals(key)) return entry;
        return null;
    }

    /**
     * A key's tags without a world: the set {@link BiomicBiomeApi#getBiomeTags} returns for a cell with that key. The
     * surface marker {@code biomic:surface} has none of its own (a surface cell reports its surface biome's key and
     * tags instead), and an unknown key none at all.
     */
    public static Set<String> getTags(String key) {
        Entry entry = get(key);
        return entry == null ? Collections.<String>emptySet() : entry.tags();
    }

    private static List<Entry> build() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        // The 3D volume's own keys, as Biomic declares them: the surface marker, caves, Origen's floating profiles,
        // Sky Donut, Aeropelago and Aeropelago's hosts.
        for (VerticalBiomeDefinition definition : VerticalBiomeCatalogue.all()) {
            String key = definition.getKey();
            if (BiomicBiomes.NOT_GENERATED.contains(key)) continue;
            if (BiomeVolume.SURFACE_KEY.equals(key)) {
                entries.put(key, new Entry(key, Collections.<String>emptySet(), null, Kind.SURFACE));
                continue;
            }
            BiomicBiomeApi.Resolution resolution = BiomicBiomeApi.resolution(key);
            if (resolution.unknown()) continue;
            Set<String> tags = resolution.tags();
            Kind kind = tags.contains(BiomeTags.CAVE) ? Kind.CAVE
                : tags.contains(BiomeTags.FLOATING_ISLAND) ? Kind.FLOATING : Kind.SURFACE;
            entries.put(key, new Entry(key, tags, resolution.biome(), kind));
        }
        // The habitats sky islands host, stored as biomic:hosted/<namespace>/<biome>.
        Set<String> bop = bopHabitats();
        List<Entry> islands = new ArrayList<>();
        for (String habitat : HostedIslandBiomes.islandHabitats()) {
            if (installed(habitat) == null || BopNativeBiomes.handles(habitat) && !bop.contains(habitat)) continue;
            String key = HostedIslandBiomes.storedKey(habitat);
            BiomicBiomeApi.Resolution resolution = BiomicBiomeApi.resolution(key);
            if (!resolution.unknown() && !entries.containsKey(key))
                islands.add(new Entry(key, resolution.tags(), resolution.biome(), Kind.FLOATING));
        }
        islands.sort(Comparator.comparing(Entry::key));
        for (Entry island : islands) entries.put(island.key(), island);
        // The surface and Nether identities: Biomic's registrations, the habitats it hosts, Tartarus and its guests.
        Set<BiomeGenBase> surface = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<BiomeGenBase> nether = Collections.newSetFromMap(new IdentityHashMap<>());
        surface.addAll(BiomicBiomes.all());
        List<String> hosted = new ArrayList<>(RegionalHabitatSampler.habitats());
        hosted.removeIf(habitat -> BopNativeBiomes.handles(habitat) && !bop.contains(habitat));
        hosted.addAll(bop);
        if (ExpansionConfig.saltyMarsh && SaltMarshCompat.available()) hosted.add(SaltMarshCompat.BIOME);
        for (String habitat : hosted) {
            BiomeGenBase biome = installed(habitat);
            if (biome != null) surface.add(biome);
        }
        for (TartarusBiomes biome : TartarusBiomes.values()) {
            BiomeGenBase registered = BiomicBiomes.getTartarusBiome(biome);
            if (registered != null) nether.add(registered);
        }
        for (BiomeGenBase biome : NetherWorldData.availableNeighbourBiomes()) if (biome != null) nether.add(biome);
        List<Entry> identities = new ArrayList<>();
        for (BiomeGenBase biome : surface)
            identity(biome, nether.contains(biome) || netherTagged(biome), entries, identities);
        for (BiomeGenBase biome : nether) identity(biome, true, entries, identities);
        identities.sort(Comparator.comparing(Entry::key));
        for (Entry identity : identities) entries.putIfAbsent(identity.key(), identity);
        return Collections.unmodifiableList(new ArrayList<>(entries.values()));
    }

    /** BOP's habitats new worlds host: the curated selection, or every supported one with bopFullCatalogue. */
    private static Set<String> bopHabitats() {
        if (!ExpansionConfig.bopHabitats) return Collections.emptySet();
        return new HashSet<>(
            BopBiomeSelection.surface(BopNativeBiomes.enabledKeys(), ExpansionConfig.bopFullCatalogue));
    }

    private static void identity(BiomeGenBase biome, boolean nether, Map<String, Entry> entries, List<Entry> out) {
        String key = BiomicBiomes.getKey(biome);
        if (key == null || entries.containsKey(key) || BiomicBiomes.NOT_GENERATED.contains(key)) return;
        for (Entry entry : out) if (entry.key()
            .equals(key)) return;
        BiomicBiomeApi.LookupResult result = BiomicBiomeApi.surfaceResult(biome);
        out.add(new Entry(key, result.tags, biome, nether ? Kind.NETHER : Kind.SURFACE));
    }

    private static boolean netherTagged(BiomeGenBase biome) {
        return TartarusBiomes.fromBiomeId(biome.biomeID) != null || BiomicBiomes.getTags(biome)
            .contains(BiomeTags.NETHER);
    }

    /** A habitat's biome when its mod is installed, else null. */
    private static BiomeGenBase installed(String habitat) {
        try {
            return ModernBiomeCompatibility.biome(habitat);
        } catch (RuntimeException absent) {
            return null;
        }
    }
}
