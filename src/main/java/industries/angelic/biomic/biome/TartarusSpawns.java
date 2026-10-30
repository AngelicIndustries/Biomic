package industries.angelic.biomic.biome;

import java.util.List;
import java.util.Map;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenBase.SpawnListEntry;

/**
 * Each Tartarus biome spawns the modern Nether mobs of the vanilla biome it borrows its ambience from. Mods add their
 * mobs to every NETHER-tagged biome alike; this re-weights the modern roles per biome and drops the ones that do not
 * belong there. Mobs without a vanilla role (Natura's imps, Netherlicious' embers and zoglins) stay as their mods
 * registered them, and each mob keeps the category its mod chose. Skeletons are left out: in a 1.7.10 Nether most of
 * them become wither skeletons, which belong to fortresses. Idempotent; run after every mod has registered spawns.
 */
public final class TartarusSpawns {

    private enum Role {

        GHAST(EnumCreatureType.monster, "net.minecraft.entity.monster.EntityGhast"),
        ZOMBIFIED_PIGLIN(EnumCreatureType.monster, "net.minecraft.entity.monster.EntityPigZombie",
            "DelirusCrux.Netherlicious.Common.Entities.Monster.EntityZombiePiglin"),
        MAGMA_CUBE(EnumCreatureType.monster, "net.minecraft.entity.monster.EntityMagmaCube"),
        ENDERMAN(EnumCreatureType.monster, "net.minecraft.entity.monster.EntityEnderman"),
        PIGLIN(EnumCreatureType.creature, "DelirusCrux.Netherlicious.Common.Entities.Passive.EntityPiglin"),
        HOGLIN(EnumCreatureType.creature, "DelirusCrux.Netherlicious.Common.Entities.Passive.EntityHoglin"),
        STRIDER(EnumCreatureType.creature, "DelirusCrux.Netherlicious.Common.Entities.Passive.EntityStrider");

        /** Where the role's mod registers it, for roles a biome is missing. */
        final EnumCreatureType category;
        final List<String> classes;

        Role(EnumCreatureType category, String... classes) {
            this.category = category;
            this.classes = List.of(classes);
        }

        /** The role's first class, if its mod is installed. */
        Class<? extends EntityLiving> primary() {
            try {
                return Class.forName(classes.get(0), false, Role.class.getClassLoader())
                    .asSubclass(EntityLiving.class);
            } catch (ClassNotFoundException absent) {
                return null;
            }
        }

        static Role of(Class<?> type) {
            for (Role role : values()) if (role.classes.contains(type.getName())) return role;
            return null;
        }
    }

    private record Weight(int weight, int min, int max) {}

    /**
     * 1.20 spawn settings per vanilla biome. Netherlicious registers piglins and hoglins as creatures, where they
     * compete with striders; their weights are scaled by ten so they keep the ground (striders only spawn in lava).
     */
    private static final Map<TartarusBiomes.Effects, Map<Role, Weight>> PROFILES = Map.of(
        TartarusBiomes.Effects.NETHER_WASTES,
        Map.of(
            Role.GHAST,
            new Weight(50, 4, 4),
            Role.ZOMBIFIED_PIGLIN,
            new Weight(100, 4, 4),
            Role.MAGMA_CUBE,
            new Weight(2, 4, 4),
            Role.ENDERMAN,
            new Weight(1, 4, 4),
            Role.PIGLIN,
            new Weight(150, 4, 4),
            Role.STRIDER,
            new Weight(60, 1, 2)),
        TartarusBiomes.Effects.CRIMSON_FOREST,
        Map.of(
            Role.ZOMBIFIED_PIGLIN,
            new Weight(1, 2, 4),
            Role.HOGLIN,
            new Weight(90, 3, 4),
            Role.PIGLIN,
            new Weight(50, 3, 4),
            Role.STRIDER,
            new Weight(60, 1, 2)),
        TartarusBiomes.Effects.WARPED_FOREST,
        Map.of(Role.ENDERMAN, new Weight(1, 4, 4), Role.STRIDER, new Weight(60, 1, 2)),
        TartarusBiomes.Effects.SOUL_SAND_VALLEY,
        Map.of(
            Role.GHAST,
            new Weight(50, 4, 4),
            Role.ENDERMAN,
            new Weight(1, 4, 4),
            Role.STRIDER,
            new Weight(60, 1, 2)),
        TartarusBiomes.Effects.BASALT_DELTAS,
        Map.of(
            Role.GHAST,
            new Weight(40, 1, 1),
            Role.MAGMA_CUBE,
            new Weight(100, 2, 5),
            Role.STRIDER,
            new Weight(60, 1, 2)));

    private TartarusSpawns() {}

    public static void apply() {
        for (TartarusBiomes tartarus : TartarusBiomes.values()) {
            BiomeGenBase biome = BiomicBiomes.getTartarusBiome(tartarus);
            Map<Role, Weight> profile = PROFILES.get(tartarus.effects);
            for (EnumCreatureType type : new EnumCreatureType[] { EnumCreatureType.monster,
                EnumCreatureType.creature }) {
                @SuppressWarnings("unchecked")
                List<SpawnListEntry> entries = biome.getSpawnableList(type);
                entries.removeIf(entry -> {
                    Role role = Role.of(entry.entityClass);
                    return role != null && !profile.containsKey(role);
                });
                java.util.Set<Role> present = java.util.EnumSet.noneOf(Role.class);
                for (SpawnListEntry entry : entries) {
                    Role role = Role.of(entry.entityClass);
                    if (role == null) continue;
                    present.add(role);
                    Weight weight = profile.get(role);
                    // A second mod's variant of a role (Netherlicious' zombie piglin) keeps the share its mod gives it:
                    // 30 next to vanilla's 100.
                    entry.itemWeight = entry.entityClass.getName()
                        .equals(role.classes.get(0)) ? weight.weight : Math.max(1, Math.round(weight.weight * .3F));
                    entry.minGroupCount = weight.min;
                    entry.maxGroupCount = weight.max;
                }
                for (var role : profile.entrySet()) {
                    if (role.getKey().category != type || present.contains(role.getKey())) continue;
                    if (absentElsewhere(biome, role.getKey(), type) && role.getKey()
                        .primary() != null) {
                        Weight weight = role.getValue();
                        entries.add(
                            new SpawnListEntry(
                                role.getKey()
                                    .primary(),
                                weight.weight,
                                weight.min,
                                weight.max));
                    }
                }
            }
        }
    }

    /**
     * A role some mod already put in the biome's other category keeps that category. Creature types added through
     * Forge's EnumHelper have no list in vanilla's biome (null).
     */
    private static boolean absentElsewhere(BiomeGenBase biome, Role role, EnumCreatureType type) {
        for (EnumCreatureType other : EnumCreatureType.values()) {
            if (other == type) continue;
            List<?> entries = biome.getSpawnableList(other);
            if (entries == null) continue;
            for (Object entry : entries) if (Role.of(((SpawnListEntry) entry).entityClass) == role) return false;
        }
        return true;
    }
}
