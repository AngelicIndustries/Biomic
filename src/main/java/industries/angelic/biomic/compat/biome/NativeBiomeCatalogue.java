package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import industries.angelic.biomic.Biomic;

/**
 * Immutable identity bindings to installed native biomes. Does not instantiate,
 * enable, decorate or select a biome. Registration is not generation ownership.
 */
public final class NativeBiomeCatalogue {

    private static volatile Snapshot snapshot = new Builder().build();

    private NativeBiomeCatalogue() {}

    public static void discoverInstalled() {
        Builder builder = new Builder();
        discover(builder, "saltymod", "saltymod", "darkbum.saltymod.init.ModBiomes", "darkbum.saltymod.");
        discover(builder, "BiomesOPlenty", "biomesoplenty", "biomesoplenty.api.content.BOPCBiomes", "biomesoplenty.");
        discover(
            builder,
            "netherlicious",
            "netherlicious",
            "DelirusCrux.Netherlicious.Biomes.Utility.NetherBiomeManager",
            "DelirusCrux.Netherlicious.");
        discover(
            builder,
            "etfuturum",
            "etfuturum",
            "ganymedes01.etfuturum.world.nether.biome.utils.NetherBiomeManager",
            "ganymedes01.etfuturum.");
        snapshot = builder.build();
        Biomic.LOG.info(
            "Native biome identity catalogue: {} installed bindings; generation remains separately gated",
            snapshot.byKey.size());
    }

    private static void discover(Builder builder, String modId, String namespace, String owner,
        String expectedClassPrefix) {
        ModContainer mod = Loader.instance()
            .getIndexedModList()
            .get(modId);
        if (mod == null) return;
        try {
            Class<?> type = Class.forName(owner, false, NativeBiomeCatalogue.class.getClassLoader());
            List<Field> fields = new ArrayList<>();
            for (Field field : type.getFields())
                if (Modifier.isStatic(field.getModifiers()) && BiomeGenBase.class.isAssignableFrom(field.getType()))
                    fields.add(field);
            fields.sort(Comparator.comparing(Field::getName));
            for (Field field : fields) {
                BiomeGenBase biome = (BiomeGenBase) field.get(null);
                if (biome == null) continue; // Disabled native content is not invented/enabled.
                if (!biome.getClass()
                    .getName()
                    .startsWith(expectedClassPrefix)) {
                    // EFR may expose Netherlicious aliases: do not misattribute/re-register them as EFR.
                    Biomic.LOG.info(
                        "Native biome alias {}.{} belongs to {}; leaving ownership unresolved",
                        owner,
                        field.getName(),
                        biome.getClass()
                            .getName());
                    continue;
                }
                BiomeGenBase[] registry = BiomeGenBase.getBiomeGenArray();
                if (biome.biomeID < 0 || biome.biomeID >= registry.length || registry[biome.biomeID] != biome) {
                    Biomic.LOG.warn(
                        "Native biome {}.{} is not the active registry object; ignoring binding",
                        owner,
                        field.getName());
                    continue;
                }
                Set<String> tags = new LinkedHashSet<>();
                for (BiomeDictionary.Type tag : BiomeDictionary.getTypesForBiome(biome)) tags.add(
                    tag.name()
                        .toLowerCase(Locale.ROOT));
                // BOP 2.1.0.2308 omits Polar Chasm from its BiomeDictionary registrations even though the
                // concrete biome extends BOPNetherBiome and is in BOPBiomeManager.netherBiomes. Retain that
                // authoritative provider classification rather than special-casing a habitat in consumers.
                if (modId.equals("BiomesOPlenty")
                    && hasSuperclass(biome.getClass(), "biomesoplenty.common.biome.BOPNetherBiome")) tags.add("nether");
                String key = namespace + ":" + snakeCase(field.getName());
                builder.add(new Entry(key, mod.getVersion(), biome, tags));
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            // No fake biome fallback. Other providers remain discoverable.
            Biomic.LOG.warn("Native biome API {} is unavailable: {}", owner, error.toString());
        }
    }

    private static boolean hasSuperclass(Class<?> type, String expected) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass()) if (cursor.getName()
            .equals(expected)) return true;
        return false;
    }

    static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
            .toLowerCase(Locale.ROOT);
    }

    public static Entry get(String key) {
        return snapshot.byKey.get(key);
    }

    public static Entry get(BiomeGenBase biome) {
        return snapshot.byBiome.get(biome);
    }

    public static BiomeGenBase biome(String key) {
        Entry entry = get(key);
        return entry == null ? null : entry.biome;
    }

    public static String key(BiomeGenBase biome) {
        Entry entry = get(biome);
        return entry == null ? null : entry.key;
    }

    public static Set<String> tags(String key) {
        Entry entry = get(key);
        return entry == null ? Set.of() : entry.tags;
    }

    public static Set<String> tags(BiomeGenBase biome) {
        Entry entry = get(biome);
        return entry == null ? Set.of() : entry.tags;
    }

    public static final class Entry {

        public final String key, version;
        public final BiomeGenBase biome;
        public final Set<String> tags;

        Entry(String key, String version, BiomeGenBase biome, Set<String> tags) {
            if (key == null || !key.matches("[a-z0-9_]+:[a-z0-9_]+")
                || version == null
                || version.isEmpty()
                || biome == null) throw new IllegalArgumentException("Incomplete native biome binding");
            this.key = key;
            this.version = version;
            this.biome = biome;
            this.tags = Collections.unmodifiableSet(new LinkedHashSet<>(tags));
        }
    }

    static final class Builder {

        private final Map<String, Entry> keys = new LinkedHashMap<>();
        private final Map<BiomeGenBase, Entry> biomes = new IdentityHashMap<>();

        void add(Entry entry) {
            if (keys.containsKey(entry.key) || biomes.containsKey(entry.biome))
                throw new IllegalArgumentException("Duplicate native biome identity: " + entry.key);
            keys.put(entry.key, entry);
            biomes.put(entry.biome, entry);
        }

        Snapshot build() {
            return new Snapshot(keys, biomes);
        }
    }

    static final class Snapshot {

        final Map<String, Entry> byKey;
        final Map<BiomeGenBase, Entry> byBiome;

        Snapshot(Map<String, Entry> keys, Map<BiomeGenBase, Entry> biomes) {
            byKey = Collections.unmodifiableMap(new LinkedHashMap<>(keys));
            byBiome = Collections.unmodifiableMap(new IdentityHashMap<>(biomes));
        }
    }
}
