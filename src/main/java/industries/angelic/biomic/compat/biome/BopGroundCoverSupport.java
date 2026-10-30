package industries.angelic.biomic.compat.biome;

import java.util.HashMap;
import java.util.Map;

/** Soil-only subset of native predicates for the admitted BOP batch; no World/registry/light queries. */
public final class BopGroundCoverSupport {

    private static final Map<String, Integer> KINDS = kinds();

    private BopGroundCoverSupport() {}

    private static Map<String, Integer> kinds() {
        var result = new HashMap<String, Integer>();
        for (int meta : new int[] { 1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 13, 14, 15 }) result.put(state("foliage", meta), 0);
        for (int meta : new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }) result.put(state("flowers", meta), 0);
        for (int meta = 0; meta < 3; meta++) result.put(state("lilybop", meta), 1);
        result.put(state("flowers2", 3), 0);
        result.put(state("flowers2", 4), 0);
        result.put(state("flowers2", 5), 0);
        result.put(state("flowers2", 7), 0);
        result.put(state("flowers2", 8), 0);
        result.put(state("plants", 1), 12);
        result.put(state("plants", 2), 9);
        result.put(state("plants", 3), 9);
        result.put(state("plants", 12), 10);
        result.put(state("flowers", 12), 11);
        result.put(state("foliage", 0), 1);
        result.put(state("foliage", 6), 2);
        result.put(state("plants", 14), 1);
        result.put(state("plants", 5), 5);
        result.put(state("plants", 11), 0);
        result.put(state("plants", 6), 0); // Native wild rice is a ground plant, not a submerged reed.
        result.put(state("plants", 8), 6);
        for (int age = 0; age < 16; age++) result.put(state("bamboo", age), 7);
        for (int meta = 0; meta < 3; meta++) result.put(state("mushrooms", meta), meta == 0 ? 4 : 3);
        result.put(state("mushrooms", 3), 8);
        return Map.copyOf(result);
    }

    private static String state(String block, int metadata) {
        return "biomesoplenty:" + block + "[legacy_meta=" + metadata + "]";
    }

    /** New habitat only: older loamy profiles keep their saved placement behavior. */
    public static Boolean located(String habitat, String plant, String below) {
        if ((BopNativeBiomes.SLUDGEPIT.equals(habitat) || BopNativeBiomes.WETLAND.equals(habitat)
            || BopNativeBiomes.DEAD_SWAMP.equals(habitat)
            || BopNativeBiomes.MOOR.equals(habitat))
            && (BopNativeBiomes.LOAMY_GRASS.equals(below) || BopNativeBiomes.LOAMY_DIRT.equals(below))) {
            String name = plant == null ? "" : plant.split("\\[", 2)[0];
            if (java.util.Set
                .of(
                    "minecraft:short_grass",
                    "minecraft:grass",
                    "minecraft:tall_grass",
                    "minecraft:fern",
                    "minecraft:large_fern")
                .contains(name)) return !plant.contains("half=upper");
        }
        return test(plant, below);
    }

    public static Boolean test(String plant, String below) {
        if (plant == null) return null;
        if (BopNativeBiomes.ORIGIN_GRASS.equals(below) && plant.equals("minecraft:dandelion")) return true;
        // Garden's native block universally sustains plants, but transport settlement
        // must admit only the vanilla flora actually audited for that profile. An
        // upper double-plant half still needs its partner, never the soil directly.
        if (BopNativeBiomes.LONG_GRASS.equals(below)) {
            String name = plant.split("\\[", 2)[0];
            if (java.util.Set
                .of(
                    "minecraft:poppy",
                    "minecraft:sunflower",
                    "minecraft:short_grass",
                    "minecraft:grass",
                    "minecraft:tall_grass")
                .contains(name)) return !plant.contains("half=upper");
        }
        Integer kind = KINDS.get(plant);
        if (kind == null) return null;
        if (below == null) return false;
        if (kind == 2) return below.equals(state("foliage", 3));
        if (kind == 6 && below.equals(state("plants", 8))) return true;
        if (kind == 7 && Integer.valueOf(7)
            .equals(KINDS.get(below))) return true;
        String ground = below.split("\\[", 2)[0];
        if (kind == 1) return ground.equals("minecraft:water");
        if (kind == 12) return ground.equals("minecraft:terracotta");
        if (kind == 9 || kind == 10 || kind == 11)
            return ground.equals("minecraft:sand") || ground.equals("minecraft:red_sand")
                || kind != 9 && ground.equals("minecraft:terracotta")
                || kind == 10 && ground.equals("minecraft:soul_sand")
                || kind == 11 && below.equals("biomesoplenty:harddirt[legacy_meta=0]");
        if (kind == 5 && ground.equals("minecraft:soul_sand")) return true;
        if (kind == 3 || kind == 4 || kind == 8)
            return ground.equals("minecraft:grass_block") || ground.equals("minecraft:dirt")
                || ground.equals("minecraft:mycelium")
                || (kind == 4 || kind == 8) && ground.equals("minecraft:netherrack")
                || kind == 8 && ground.equals("biomesoplenty:overgrownnetherrack")
                || kind == 8 && ground.equals("minecraft:stone");
        // Native Plains-plant support. Mushrooms deliberately do not use canSustainPlant upstream.
        if (below.equals(BopNativeBiomes.LONG_GRASS)) return true;
        if (below.equals(BopNativeBiomes.ORIGIN_GRASS)) return true;
        if (below.equals(BopNativeBiomes.LOAMY_GRASS) || below.equals(BopNativeBiomes.LOAMY_DIRT)) return true;
        return ground.equals("minecraft:grass_block") || ground.equals("minecraft:dirt")
            || ground.equals("minecraft:farmland")
            || ground.equals("minecraft:coarse_dirt")
            || ground.equals("minecraft:podzol");
    }
}
