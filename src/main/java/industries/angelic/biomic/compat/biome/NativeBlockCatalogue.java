package industries.angelic.biomic.compat.biome;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;

import cpw.mods.fml.common.Loader;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;

/**
 * Lossless native block+metadata transport for installed biome providers. This is
 * a codec, not a feature/placement allow-list: adapters still authorize writes.
 * Never substitutes a copied texture/block or drops unknown state properties.
 */
public final class NativeBlockCatalogue {

    private static final java.util.List<String> PROVIDERS = java.util.List.of(
        "saltymod",
        "BiomesOPlenty",
        "netherlicious",
        "plantmegapack",
        "etfuturum",
        "harvestcraft",
        "harvestthenether");
    private static volatile Snapshot snapshot = new Builder().build();

    private NativeBlockCatalogue() {}

    public static void discoverInstalled() {
        Builder result = new Builder();
        for (String provider : PROVIDERS) {
            if (!Loader.isModLoaded(provider)) continue;
            // A provider with conflicting case-folded keys must not leave a partial
            // table. Forge itself uses case-sensitive legacy registry identifiers.
            Builder local = new Builder();
            try {
                for (Object object : Block.blockRegistry) {
                    Block block = (Block) object;
                    String registry = Block.blockRegistry.getNameForObject(block);
                    if (registry == null || !registry.startsWith(provider + ":")) continue;
                    // Registry ownership, not implementation class: mods legitimately
                    // register ordinary vanilla Block subclasses with their own textures.
                    local.add(registry, block);
                }
                local.keys.forEach(result::add);
            } catch (RuntimeException error) {
                Biomic.LOG.warn("Native block codec unavailable for {}: {}", provider, error.toString());
            }
        }
        snapshot = result.build();
        Biomic.LOG.info(
            "Native biome block transport: {} installed bindings (generation separately gated)",
            snapshot.keys.size());
    }

    public static boolean handles(String state) {
        if (state == null) return false;
        int split = state.indexOf('[');
        String key = split < 0 ? state : state.substring(0, split);
        // Preserve existing EFR aliases expressed using modern properties (e.g. etfuturum:bone_block).
        // EFR opts into this codec only through the explicit native-metadata representation.
        if (key.startsWith("etfuturum:") && !state.startsWith(key + "[legacy_meta=")) return false;
        return snapshot.keys.containsKey(key);
    }

    public static String encode(Block block, int metadata) {
        String key = snapshot.blocks.get(block);
        return key == null ? null : state(key, metadata);
    }

    public static OrigenBlockStateResolver.ResolvedBlock resolve(String state) {
        Parsed parsed = parse(state);
        Block block = snapshot.keys.get(parsed.key);
        if (block == null) throw new IllegalArgumentException("Unavailable native biome state: " + state);
        return OrigenBlockStateResolver.imported(state, block, parsed.metadata);
    }

    /** Generation-only encoder. Inspecting an existing tile block above remains legal. */
    public static String encodeTerrainWrite(Block block, int metadata) {
        String state = encode(block, metadata);
        if (state == null) throw new IllegalArgumentException("Unregistered native biome block write");
        if (block.hasTileEntity(metadata))
            throw new IllegalArgumentException("Native tile state requires a separate adapter: " + state);
        return state;
    }

    static String state(String key, int metadata) {
        if (metadata < 0 || metadata > 15) throw new IllegalArgumentException("Invalid native block metadata");
        return key + "[legacy_meta=" + metadata + "]";
    }

    static Parsed parse(String value) {
        int split = value.indexOf("[legacy_meta=");
        if (split < 1 || !value.endsWith("]"))
            throw new IllegalArgumentException("Native metadata must be explicit: " + value);
        String key = value.substring(0, split);
        int metadata;
        try {
            metadata = Integer.parseInt(value.substring(split + 13, value.length() - 1));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Malformed native metadata", error);
        }
        if (!state(key, metadata).equals(value))
            throw new IllegalArgumentException("Noncanonical native state: " + value);
        return new Parsed(key, metadata);
    }

    record Parsed(String key, int metadata) {}

    static final class Builder {

        private final Map<String, Block> keys = new LinkedHashMap<>();
        private final Map<Block, String> blocks = new IdentityHashMap<>();

        void add(String registry, Block block) {
            String key = registry.toLowerCase(Locale.ROOT);
            if (block == null || !key.matches("[a-z0-9_]+:[a-z0-9_./-]+")
                || keys.containsKey(key)
                || blocks.containsKey(block))
                throw new IllegalArgumentException("Ambiguous native block identity: " + registry);
            keys.put(key, block);
            blocks.put(block, key);
        }

        Snapshot build() {
            return new Snapshot(keys, blocks);
        }
    }

    static final class Snapshot {

        final Map<String, Block> keys;
        final Map<Block, String> blocks;

        Snapshot(Map<String, Block> keys, Map<Block, String> blocks) {
            this.keys = Collections.unmodifiableMap(new java.util.TreeMap<>(keys));
            this.blocks = Collections.unmodifiableMap(new IdentityHashMap<>(blocks));
        }
    }
}
