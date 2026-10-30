package industries.angelic.biomic.client;

import static industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle.ASH;
import static industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle.CRIMSON_SPORE;
import static industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle.SNOW;
import static industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle.SOULBLIGHT_SPORE;
import static industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle.WARPED_SPORE;
import static industries.angelic.biomic.biome.TartarusBiomes.Effects.BASALT_DELTAS;
import static industries.angelic.biomic.biome.TartarusBiomes.Effects.CRIMSON_FOREST;
import static industries.angelic.biomic.biome.TartarusBiomes.Effects.NETHER_WASTES;
import static industries.angelic.biomic.biome.TartarusBiomes.Effects.SOUL_SAND_VALLEY;
import static industries.angelic.biomic.biome.TartarusBiomes.Effects.WARPED_FOREST;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import net.minecraft.world.biome.BiomeGenBase;

import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle;
import industries.angelic.biomic.biome.TartarusBiomes.Effects;
import industries.angelic.biomic.world.terrain.gardens.GardensProfiles;

/**
 * The modern ambience of each Tartarus Nether biome (decided 2026-10-21). Tartarus biomes take the whole set of
 * their pack's {@code vanilla:} key; the neighbours take the closest vanilla set, or their own mod's choice.
 */
public final class NetherAmbienceProfiles {

    /** Leaves the fog as the other handlers made it. */
    public static final int NO_FOG = -1;

    /**
     * What a biome plays and shows. {@code loop} plays with its additions (null: none). {@code mood} replaces the
     * cave sound (null: the cave sound stays). {@code music} replaces C418's Nether tracks (null: they stay), unless
     * the biome is {@code silent} or has music of its own ({@code ownMusic}, a Biomic sound event).
     */
    public record Profile(Effects loop, Effects mood, Effects music, boolean silent, AmbientParticle particle,
        float particleChance, int fog, String ownMusic) {

        Profile(Effects loop, Effects mood, Effects music, boolean silent, AmbientParticle particle,
            float particleChance, int fog) {
            this(loop, mood, music, silent, particle, particleChance, fog, null);
        }

        Profile withOwnMusic(String event) {
            return new Profile(loop, mood, music, silent, particle, particleChance, fog, event);
        }
    }

    /** Biomes outside the table: nether_wastes sounds, no particles and the fog left alone. */
    public static final Profile DEFAULT = of(NETHER_WASTES, null, 0, NO_FOG);

    private static final Map<String, Profile> NEIGHBOURS = Map.ofEntries(
        // BOP has no ambience of its own; Polar Chasm keeps its own dense fog.
        Map.entry("biomesoplenty:undergarden", of(CRIMSON_FOREST, CRIMSON_SPORE, .01F, CRIMSON_FOREST.fogColor)),
        Map.entry("biomesoplenty:polar_chasm", of(SOUL_SAND_VALLEY, SNOW, .03F, NO_FOG)),
        Map.entry(
            "biomesoplenty:boneyard",
            of(SOUL_SAND_VALLEY, ASH, SOUL_SAND_VALLEY.particleChance, NETHER_WASTES.fogColor)),
        // Netherlicious keeps its own particles and fog; Biomic only plays its sounds, as Netherlicious picks them.
        Map.entry("netherlicious:foxfire_swamp", of(CRIMSON_FOREST, null, 0, NO_FOG)),
        Map.entry("netherlicious:abyssal_shadowland", new Profile(null, null, null, true, null, 0, NO_FOG)),
        Map.entry("netherlicious:ruptured_chasm", of(BASALT_DELTAS, null, 0, NO_FOG)),
        Map.entry("netherlicious:glow_jungle", of(WARPED_FOREST, WARPED_SPORE, WARPED_FOREST.particleChance, NO_FOG)),
        Map.entry("netherlicious:burning_desert", of(NETHER_WASTES, null, 0, NO_FOG)),
        Map.entry(
            "netherlicious:tainted_shroom_cave",
            new Profile(WARPED_FOREST, WARPED_FOREST, CRIMSON_FOREST, false, null, 0, NO_FOG)),
        // Gardens of the Dead: upstream's particles, fog and music (its own track or C418's, as upstream weighs
        // them), crimson sounds.
        Map.entry(
            GardensProfiles.SOULBLIGHT_FOREST,
            of(
                CRIMSON_FOREST,
                SOULBLIGHT_SPORE,
                .01F,
                GardensProfiles.profile(GardensProfiles.SOULBLIGHT_FOREST)
                    .fogColor()).withOwnMusic("biomic:music.gardens.soulblight_forest")),
        Map.entry(
            GardensProfiles.WHISTLING_WOODS,
            of(
                CRIMSON_FOREST,
                CRIMSON_SPORE,
                .01F,
                GardensProfiles.profile(GardensProfiles.WHISTLING_WOODS)
                    .fogColor()).withOwnMusic("biomic:music.gardens.whistling_woods")));

    /** Vanilla samples about 1334 spots a tick for ambient particles; Biomic samples fewer, nearer the camera. */
    static final int VANILLA_SAMPLES = 1334, SAMPLES = 400;

    private NetherAmbienceProfiles() {}

    /** A whole vanilla set, as a Tartarus biome takes it. */
    public static Profile of(Effects set) {
        return of(set, set.particle, set.particleChance, set.fogColor);
    }

    static Profile of(Effects set, AmbientParticle particle, float particleChance, int fog) {
        return new Profile(set, set, hasMusic(set) ? set : null, false, particle, particleChance, fog);
    }

    /** Warped forests keep C418's tracks: modern Minecraft gives them no music. */
    static boolean hasMusic(Effects set) {
        return set != WARPED_FOREST;
    }

    /** The profile of each biome id, built from the registry; null for biomes outside the table. */
    static Profile[] byBiomeId(BiomeGenBase[] registry, Function<String, BiomeGenBase> biomes) {
        Profile[] profiles = new Profile[registry.length];
        for (int id = 0; id < registry.length; id++) {
            TartarusBiomes tartarus = registry[id] == null ? null : TartarusBiomes.fromBiomeId(id);
            if (tartarus != null) profiles[id] = of(tartarus.effects);
        }
        for (Map.Entry<String, Profile> entry : NEIGHBOURS.entrySet()) {
            BiomeGenBase biome = biomes.apply(entry.getKey());
            if (biome != null && biome.biomeID >= 0 && biome.biomeID < profiles.length)
                profiles[biome.biomeID] = entry.getValue();
        }
        return profiles;
    }

    /**
     * The chance one sampled spot spawns a particle: vanilla's, raised for the smaller sample, then scaled by the
     * video settings' particle level (All, Decreased, Minimal) and the config.
     */
    static float spotChance(float vanillaChance, int particleSetting, float scale) {
        float level = particleSetting == 0 ? 1F : particleSetting == 1 ? .4F : 0F;
        return Math.min(1F, vanillaChance * VANILLA_SAMPLES / SAMPLES * level * scale);
    }

    /** Holds a value until a new one has been seen for some ticks in a row, so ambience doesn't flicker at borders. */
    static final class Settled<T> {

        private final int ticks;
        private T value, candidate;
        private int seen;

        Settled(int ticks) {
            this.ticks = ticks;
        }

        T update(T next) {
            if (value == null || Objects.equals(next, value)) {
                value = next;
                candidate = null;
                seen = 0;
                return value;
            }
            if (!Objects.equals(next, candidate)) {
                candidate = next;
                seen = 0;
            }
            if (++seen >= ticks) {
                value = next;
                candidate = null;
                seen = 0;
            }
            return value;
        }

        T value() {
            return value;
        }

        void reset() {
            value = candidate = null;
            seen = 0;
        }
    }
}
