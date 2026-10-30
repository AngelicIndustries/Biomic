package industries.angelic.biomic.world.terrain.gardens;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.content.gardens.GardensStateCodec;
import industries.angelic.biomic.content.gardens.GardensStateCodec.State;
import industries.angelic.biomic.world.modern.noise.ModernPerlinSimplexNoise;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.modern.noise.ModernWorldgenRandom;

/**
 * Gardens of the Dead's feature schedule (its placement modifiers and seeds) for its two biomes in the Tartarus
 * Nether, run with the terrain on a worker. The source is a 128-tall Nether: layers are counted over 0..255 here,
 * and the one full-range slot keeps its density over twice the height.
 */
public final class GardensNetherDecorator {

    /** The terrain one chunk's decoration reads and writes. */
    public interface Region {

        Block blockAt(int x, int y, int z);

        int metadata(int x, int y, int z);

        void set(int x, int y, int z, String state);

        /** The Gardens biome key of a column, or null. */
        String profile(int x, int z);
    }

    private static final int VEGETAL_DECORATION_STEP = 9, TOP = GardensProfiles.SOURCE_MAX_Y;
    private static final ModernPerlinSimplexNoise BIOME_INFO_NOISE = new ModernPerlinSimplexNoise(2345L, 0);
    private static final List<String> ORDER = List.of(
        "soulblight_fungi",
        "soulblight_forest_vegetation",
        "short_standing_soul_spore_patch",
        "long_standing_soul_spore_patch",
        "short_hanging_soul_spore_patch",
        "long_hanging_soul_spore_patch",
        "nether_wart_block_pile",
        "dense_weeping_vines",
        "noisy_crimson_fungi",
        "whistlecane_column",
        "tall_blistercrown_patch",
        "whistling_woods_vegetation");

    private GardensNetherDecorator() {}

    /** Decorates the anchors of one chunk; writes may reach into its neighbours. */
    public static void decorate(Region region, long seed, int chunkX, int chunkZ) {
        int startX = chunkX << 4, startZ = chunkZ << 4;
        boolean gardens = false;
        for (int z = 0; z < 16 && !gardens; z++)
            for (int x = 0; x < 16 && !gardens; x++) gardens = region.profile(startX + x, startZ + z) != null;
        if (!gardens) return;
        long decorationSeed = new ModernWorldgenRandom().setDecorationSeed(seed, startX, startZ);
        for (int index = 0; index < ORDER.size(); index++) {
            String feature = ORDER.get(index);
            String profile = index < 6 ? GardensProfiles.SOULBLIGHT_FOREST : GardensProfiles.WHISTLING_WOODS;
            ModernWorldgenRandom random = new ModernWorldgenRandom();
            random.setFeatureSeed(decorationSeed, index, VEGETAL_DECORATION_STEP);
            switch (feature) {
                case "dense_weeping_vines" -> {
                    for (int i = 0; i < 80; i++) {
                        int x = startX + random.nextInt(16), z = startZ + random.nextInt(16), y = random.nextInt(256);
                        attempt(region, profile, feature, random, x, y, z);
                    }
                }
                case "noisy_crimson_fungi" -> {
                    int repetitions = GardensProfiles
                        .noisyCrimsonFungusAnchors(BIOME_INFO_NOISE.value(startX / 180D, startZ / 180D));
                    for (int i = 0; i < repetitions; i++)
                        everyLayer(region, profile, feature, 1, false, false, random, startX, startZ);
                }
                case "nether_wart_block_pile" -> everyLayer(
                    region,
                    profile,
                    feature,
                    1,
                    false,
                    true,
                    random,
                    startX,
                    startZ);
                case "short_hanging_soul_spore_patch" -> everyLayer(
                    region,
                    profile,
                    feature,
                    4,
                    true,
                    false,
                    random,
                    startX,
                    startZ);
                case "long_hanging_soul_spore_patch" -> everyLayer(
                    region,
                    profile,
                    feature,
                    8,
                    true,
                    false,
                    random,
                    startX,
                    startZ);
                default -> everyLayer(
                    region,
                    profile,
                    feature,
                    GardensProfiles.profile(profile)
                        .intrinsic()
                        .stream()
                        .filter(
                            slot -> slot.key()
                                .equals(feature))
                        .findFirst()
                        .orElseThrow()
                        .attempts(),
                    false,
                    false,
                    random,
                    startX,
                    startZ);
            }
        }
    }

    private static void everyLayer(Region region, String profile, String feature, int count, boolean ceiling,
        boolean rarityTwo, ModernRandom random, int startX, int startZ) {
        int layer = 0;
        boolean found;
        do {
            found = false;
            for (int i = 0; i < count; i++) {
                int x = startX + random.nextInt(16), z = startZ + random.nextInt(16);
                int y = ceiling ? ceilingForLayer(region, x, z, layer) : floorForLayer(region, x, z, layer);
                if (y == Integer.MAX_VALUE) continue;
                found = true;
                if (!rarityTwo || random.nextFloat() < .5F) attempt(region, profile, feature, random, x, y, z);
            }
            layer++;
        } while (found);
    }

    private static void attempt(Region region, String profile, String feature, ModernRandom random, int x, int y,
        int z) {
        if (!profile.equals(region.profile(x, z))) return;
        var result = GardensDecorationRuntime
            .plan(profile, feature, random, x, y, z, new GardensDecorationRuntime.Access() {

                @Override
                public State state(int px, int py, int pz) {
                    return read(region, px, py, pz);
                }

                @Override
                public boolean habitat(String expected, int px, int py, int pz) {
                    return expected.equals(region.profile(px, pz));
                }

                @Override
                public String writeRejection(int px, int py, int pz, State before, State after) {
                    return rejection(region, px, py, pz, after);
                }
            });
        if (!result.accepted()) return;
        for (var entry : result.proposal()
            .writes()
            .entrySet()) {
            var p = entry.getKey();
            region.set(
                p.x(),
                p.y(),
                p.z(),
                entry.getValue()
                    .canonical());
        }
    }

    private static String rejection(Region region, int x, int y, int z, State after) {
        if (y < 1 || y >= TOP) return "source-y-bounds";
        if (after == null || after.block()
            .hasTileEntity(after.metadata())) return "unsupported-output-state";
        return region.blockAt(x, y, z) == Blocks.bedrock ? "bedrock" : null;
    }

    private static int floorForLayer(Region region, int x, int z, int wanted) {
        int layer = 0;
        boolean previous = empty(region.blockAt(x, TOP, z));
        for (int y = TOP; y >= 1; y--) {
            Block below = region.blockAt(x, y - 1, z);
            boolean belowEmpty = empty(below);
            if (!belowEmpty && previous && below != Blocks.bedrock) {
                if (layer == wanted) return y;
                layer++;
            }
            previous = belowEmpty;
        }
        return Integer.MAX_VALUE;
    }

    private static int ceilingForLayer(Region region, int x, int z, int wanted) {
        int layer = 0;
        Block previous = region.blockAt(x, TOP, z);
        for (int y = TOP; y >= 1; y--) {
            Block below = region.blockAt(x, y - 1, z);
            if (!empty(previous) && empty(below) && previous != Blocks.bedrock) {
                if (layer == wanted) return y - 1;
                layer++;
            }
            previous = below;
        }
        return Integer.MAX_VALUE;
    }

    private static State read(Region region, int x, int y, int z) {
        Block block = region.blockAt(x, y, z);
        int metadata = region.metadata(x, y, z);
        String canonical;
        try {
            canonical = GardensStateCodec.encode(block, metadata);
        } catch (RuntimeException unknown) {
            canonical = null;
        }
        return new State(block, metadata, canonical == null ? "unresolved:physical" : canonical);
    }

    private static boolean empty(Block block) {
        Material material = block.getMaterial();
        return material == Material.air || material.isLiquid();
    }
}
