package industries.angelic.biomic.world.terrain;

import java.util.List;

import industries.angelic.biomic.compat.biome.BopNativeBiomes;
import industries.angelic.biomic.compat.biome.BopSubBiomePolicy;
import industries.angelic.biomic.world.WorldgenContext;

/** Saved opt-in only. Select a whole ecological province/sky cluster, never slice an existing landmark. */
public final class NativeBopHabitatSelector {

    private NativeBopHabitatSelector() {}

    public static String select(long seed, long anchor, int family, List<String> enabled) {
        if (enabled.isEmpty()) return null;
        if (family == 41 || family == 42 || family == 38 || family == 39) {
            String parent = family == 41 ? "minecraft:deep_ocean" : "minecraft:ocean";
            long hash = WorldgenContext.mix64(seed ^ anchor ^ family * 0x756fc15746764329L ^ 0x424f504841424954L);
            String child = BopSubBiomePolicy.selectExternal(parent, hash, enabled);
            return parent.equals(child) ? null : child;
        }
        List<String> pool = switch (family) {
            case 1, 10, 15, 20 -> List.of(
                BopNativeBiomes.GROVE,
                BopNativeBiomes.CHERRY,
                BopNativeBiomes.DECIDUOUS,
                BopNativeBiomes.WOODLAND,
                BopNativeBiomes.BOREAL,
                BopNativeBiomes.MAPLE,
                BopNativeBiomes.SEASONAL,
                BopNativeBiomes.CONIFEROUS,
                BopNativeBiomes.TEMPERATE_RAINFOREST,
                BopNativeBiomes.DENSE_FOREST,
                BopNativeBiomes.SHIELD,
                BopNativeBiomes.REDWOOD,
                BopNativeBiomes.MYSTIC_GROVE,
                BopNativeBiomes.GARDEN);
            case 16 -> List.of(
                BopNativeBiomes.GROVE,
                BopNativeBiomes.CHERRY,
                BopNativeBiomes.DECIDUOUS,
                BopNativeBiomes.WOODLAND,
                BopNativeBiomes.BOREAL,
                BopNativeBiomes.MAPLE,
                BopNativeBiomes.SEASONAL,
                BopNativeBiomes.CONIFEROUS,
                BopNativeBiomes.TEMPERATE_RAINFOREST,
                BopNativeBiomes.DENSE_FOREST,
                BopNativeBiomes.SHIELD,
                BopNativeBiomes.REDWOOD,
                BopNativeBiomes.FUNGI_FOREST,
                BopNativeBiomes.MYSTIC_GROVE,
                BopNativeBiomes.GARDEN);
            case 2 -> List.of(
                BopNativeBiomes.LAVENDER,
                BopNativeBiomes.PRAIRIE,
                BopNativeBiomes.FLOWER_FIELD,
                BopNativeBiomes.HEATHLAND,
                BopNativeBiomes.SHRUBLAND,
                BopNativeBiomes.THICKET,
                BopNativeBiomes.MEADOW,
                BopNativeBiomes.STEPPE,
                BopNativeBiomes.TUNDRA,
                BopNativeBiomes.CHAPARRAL,
                BopNativeBiomes.GRASSLAND,
                BopNativeBiomes.GARDEN);
            case 14, 17 -> List.of(
                BopNativeBiomes.LAVENDER,
                BopNativeBiomes.PRAIRIE,
                BopNativeBiomes.FLOWER_FIELD,
                BopNativeBiomes.HEATHLAND,
                BopNativeBiomes.SHRUBLAND,
                BopNativeBiomes.THICKET,
                BopNativeBiomes.MEADOW,
                BopNativeBiomes.STEPPE,
                BopNativeBiomes.HIGHLAND,
                BopNativeBiomes.CHAPARRAL,
                BopNativeBiomes.MOUNTAIN,
                BopNativeBiomes.CRAG,
                BopNativeBiomes.JADE_CLIFFS,
                BopNativeBiomes.GARDEN);
            case 32 -> List.of(BopNativeBiomes.BRUSHLAND, BopNativeBiomes.CHAPARRAL);
            case 13, 21 -> List.of(
                BopNativeBiomes.EUCALYPTUS,
                BopNativeBiomes.RAINFOREST,
                BopNativeBiomes.TROPICAL_RAINFOREST,
                BopNativeBiomes.BAMBOO_FOREST,
                BopNativeBiomes.FUNGI_FOREST);
            case 27 -> List.of(
                BopNativeBiomes.EUCALYPTUS,
                BopNativeBiomes.RAINFOREST,
                BopNativeBiomes.TROPICAL_RAINFOREST,
                BopNativeBiomes.FUNGI_FOREST);
            case 23 -> List.of(BopNativeBiomes.BAMBOO_FOREST);
            case 4, 11 -> List.of(
                BopNativeBiomes.BOREAL,
                BopNativeBiomes.MAPLE,
                BopNativeBiomes.CONIFEROUS,
                BopNativeBiomes.TEMPERATE_RAINFOREST,
                BopNativeBiomes.SHIELD,
                BopNativeBiomes.REDWOOD,
                BopNativeBiomes.DEAD_FOREST,
                BopNativeBiomes.OMINOUS_WOODS);
            case 22 -> List.of(BopNativeBiomes.SNOWY_CONIFEROUS, BopNativeBiomes.FROST, BopNativeBiomes.ARCTIC);
            case 30, 31 -> List.of(BopNativeBiomes.SNOWY_CONIFEROUS, BopNativeBiomes.FROST, BopNativeBiomes.ALPS);
            case -1 -> List.of(
                BopNativeBiomes.GROVE,
                BopNativeBiomes.CHERRY,
                BopNativeBiomes.LAVENDER,
                BopNativeBiomes.DECIDUOUS,
                BopNativeBiomes.PRAIRIE,
                BopNativeBiomes.FLOWER_FIELD,
                BopNativeBiomes.WOODLAND,
                BopNativeBiomes.HEATHLAND,
                BopNativeBiomes.SHRUBLAND,
                BopNativeBiomes.BOREAL,
                BopNativeBiomes.MAPLE,
                BopNativeBiomes.SEASONAL,
                BopNativeBiomes.THICKET,
                BopNativeBiomes.CONIFEROUS,
                BopNativeBiomes.SNOWY_CONIFEROUS,
                BopNativeBiomes.FROST,
                BopNativeBiomes.MEADOW,
                BopNativeBiomes.TEMPERATE_RAINFOREST,
                BopNativeBiomes.STEPPE,
                BopNativeBiomes.DENSE_FOREST,
                BopNativeBiomes.SHIELD,
                BopNativeBiomes.HIGHLAND,
                BopNativeBiomes.TUNDRA,
                BopNativeBiomes.ARCTIC,
                BopNativeBiomes.ALPS,
                BopNativeBiomes.BRUSHLAND,
                BopNativeBiomes.CHAPARRAL,
                BopNativeBiomes.GRASSLAND,
                BopNativeBiomes.MOUNTAIN,
                BopNativeBiomes.CRAG,
                BopNativeBiomes.EUCALYPTUS,
                BopNativeBiomes.RAINFOREST,
                BopNativeBiomes.TROPICAL_RAINFOREST,
                BopNativeBiomes.BAMBOO_FOREST,
                BopNativeBiomes.REDWOOD,
                BopNativeBiomes.JADE_CLIFFS,
                BopNativeBiomes.DEAD_FOREST,
                BopNativeBiomes.OMINOUS_WOODS,
                BopNativeBiomes.FUNGI_FOREST,
                BopNativeBiomes.MYSTIC_GROVE,
                BopNativeBiomes.GARDEN);
            default -> List.of();
        };
        // Appended only for newly opted-in saves; old filtered pools retain their exact order.
        if (enabled.contains(BopNativeBiomes.ORIGIN_VALLEY) && java.util.Set.of(-1, 1, 2, 10, 15, 16, 20)
            .contains(family)) {
            var extended = new java.util.ArrayList<>(pool);
            extended.add(BopNativeBiomes.ORIGIN_VALLEY);
            pool = extended;
        }
        var admitted = pool.stream()
            .filter(enabled::contains)
            .toList();
        if (java.util.Set.of(-1, 1, 2, 10, 15, 16, 20, 13, 21, 27)
            .contains(family)) {
            var extended = new java.util.ArrayList<>(admitted);
            if (java.util.Set.of(-1, 1, 2, 10, 15, 16, 20)
                .contains(family) && enabled.contains("biomesoplenty:orchard")) extended.add("biomesoplenty:orchard");
            if (java.util.Set.of(-1, 1, 10, 13, 21, 27)
                .contains(family) && enabled.contains("biomesoplenty:sacred_springs"))
                extended.add("biomesoplenty:sacred_springs");
            admitted = extended;
        }
        if (java.util.Set.of(-1, 3, 12, 32)
            .contains(family)) {
            var extended = new java.util.ArrayList<>(admitted);
            if (enabled.contains(BopNativeBiomes.OUTBACK)) extended.add(BopNativeBiomes.OUTBACK);
            if ((family == -1 || family == 3) && enabled.contains(BopNativeBiomes.XERIC_SHRUBLAND))
                extended.add(BopNativeBiomes.XERIC_SHRUBLAND);
            if (enabled.contains(BopNativeBiomes.LUSH_DESERT)) extended.add(BopNativeBiomes.LUSH_DESERT);
            if (enabled.contains("biomesoplenty:wasteland")) extended.add("biomesoplenty:wasteland");
            admitted = extended;
        }
        if (java.util.Set.of(24, 25, 28)
            .contains(family)) {
            var extended = new java.util.ArrayList<>(admitted);
            if (enabled.contains(BopNativeBiomes.MARSH)) extended.add(BopNativeBiomes.MARSH);
            if (enabled.contains(BopNativeBiomes.DEAD_SWAMP)) extended.add(BopNativeBiomes.DEAD_SWAMP);
            if (enabled.contains(BopNativeBiomes.FEN)) extended.add(BopNativeBiomes.FEN);
            if (enabled.contains(BopNativeBiomes.BOG)) extended.add(BopNativeBiomes.BOG);
            if (enabled.contains(BopNativeBiomes.LUSH_SWAMP)) extended.add(BopNativeBiomes.LUSH_SWAMP);
            if (enabled.contains(BopNativeBiomes.BAYOU)) extended.add(BopNativeBiomes.BAYOU);
            if (enabled.contains(BopNativeBiomes.WETLAND)) extended.add(BopNativeBiomes.WETLAND);
            if (enabled.contains(BopNativeBiomes.SLUDGEPIT)) extended.add(BopNativeBiomes.SLUDGEPIT);
            if (enabled.contains(BopNativeBiomes.LAND_OF_LAKES)) extended.add(BopNativeBiomes.LAND_OF_LAKES);
            admitted = extended;
        }
        if ((family == 14 || family == 17) && enabled.contains(BopNativeBiomes.MOOR)) {
            var extended = new java.util.ArrayList<>(admitted);
            extended.add(BopNativeBiomes.MOOR);
            admitted = extended;
        }
        if (java.util.Set.of(-1, 12, 32)
            .contains(family) && enabled.contains(BopNativeBiomes.CANYON)) {
            var extended = new java.util.ArrayList<>(admitted);
            extended.add(BopNativeBiomes.CANYON);
            admitted = extended;
        }
        if (admitted.isEmpty()) return null;
        long hash = WorldgenContext.mix64(seed ^ anchor ^ family * 0x756fc15746764329L ^ 0x424f504841424954L);
        if (Math.floorMod(hash, 5) != 0) return null;
        String parent = admitted.get((int) Math.floorMod(WorldgenContext.mix64(hash), admitted.size()));
        return BopSubBiomePolicy.select(parent, hash, enabled);
    }
}
