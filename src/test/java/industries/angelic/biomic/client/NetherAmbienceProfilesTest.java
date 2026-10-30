package industries.angelic.biomic.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.Test;

import industries.angelic.biomic.biome.TartarusBiomes;
import industries.angelic.biomic.biome.TartarusBiomes.AmbientParticle;
import industries.angelic.biomic.biome.TartarusBiomes.Effects;
import industries.angelic.biomic.client.NetherAmbienceProfiles.Profile;
import industries.angelic.biomic.config.BiomeConfig;
import industries.angelic.biomic.world.terrain.gardens.GardensProfiles;

public class NetherAmbienceProfilesTest {

    /** A neighbour biome's profile, as the registry scan finds it. */
    private static Profile neighbour(String key) {
        BiomeGenBase[] registry = new BiomeGenBase[BiomeGenBase.hell.biomeID + 1];
        registry[BiomeGenBase.hell.biomeID] = BiomeGenBase.hell;
        return NetherAmbienceProfiles
            .byBiomeId(registry, k -> k.equals(key) ? BiomeGenBase.hell : null)[BiomeGenBase.hell.biomeID];
    }

    @Test
    public void tartarusBiomesTakeTheirVanillaKeysWholeSet() {
        for (TartarusBiomes biome : TartarusBiomes.values()) {
            Profile profile = NetherAmbienceProfiles.of(biome.effects);
            assertSame(biome.effects, profile.loop());
            assertSame(biome.effects, profile.mood());
            assertSame(biome.effects.particle, profile.particle());
            assertEquals(biome.effects.particleChance, profile.particleChance(), 0);
            assertEquals(biome.effects.fogColor, profile.fog());
            assertFalse(profile.silent());
        }
        assertSame(Effects.BASALT_DELTAS, TartarusBiomes.SERRATED_MOUNTAINS.effects);
        assertSame(
            AmbientParticle.WHITE_ASH,
            NetherAmbienceProfiles.of(Effects.BASALT_DELTAS)
                .particle());
        assertEquals(
            .118F,
            NetherAmbienceProfiles.of(Effects.BASALT_DELTAS)
                .particleChance(),
            0);
        assertNull(
            NetherAmbienceProfiles.of(Effects.NETHER_WASTES)
                .particle());
    }

    @Test
    public void warpedBiomesKeepC418WhileTheOthersPlayTheirOwnMusic() {
        for (Effects set : Effects.values()) {
            Profile profile = NetherAmbienceProfiles.of(set);
            if (set == Effects.WARPED_FOREST) assertNull(profile.music());
            else assertSame(set, profile.music());
        }
        assertNull(
            NetherAmbienceProfiles.of(TartarusBiomes.CRYSTALLINE_VELDTS.effects)
                .music());
        assertNull(neighbour("netherlicious:glow_jungle").music());
    }

    @Test
    public void neighboursFollowTheirOwnModsChoices() {
        Profile abyssal = neighbour("netherlicious:abyssal_shadowland");
        assertNull(abyssal.loop());
        assertNull(abyssal.mood());
        assertTrue(abyssal.silent());

        Profile shrooms = neighbour("netherlicious:tainted_shroom_cave");
        assertSame(Effects.WARPED_FOREST, shrooms.loop());
        assertSame(Effects.CRIMSON_FOREST, shrooms.music());

        Profile polar = neighbour("biomesoplenty:polar_chasm");
        assertSame(Effects.SOUL_SAND_VALLEY, polar.loop());
        assertSame(AmbientParticle.SNOW, polar.particle());
        assertEquals(NetherAmbienceProfiles.NO_FOG, polar.fog());

        assertEquals(Effects.CRIMSON_FOREST.fogColor, neighbour("biomesoplenty:undergarden").fog());
        assertEquals(NetherAmbienceProfiles.NO_FOG, neighbour("netherlicious:foxfire_swamp").fog());
        assertNull(neighbour("netherlicious:burning_desert").particle());
    }

    @Test
    public void gardensFogColoursAreLive() {
        Profile soulblight = neighbour(GardensProfiles.SOULBLIGHT_FOREST);
        assertEquals(0x593A21, soulblight.fog());
        assertSame(AmbientParticle.SOULBLIGHT_SPORE, soulblight.particle());
        assertEquals(0x5E1118, neighbour(GardensProfiles.WHISTLING_WOODS).fog());
        assertSame(Effects.CRIMSON_FOREST, soulblight.loop());
        // Upstream's own music, weighed against C418's in Biomic's sounds.json.
        assertEquals("biomic:music.gardens.soulblight_forest", soulblight.ownMusic());
        assertEquals("biomic:music.gardens.whistling_woods", neighbour(GardensProfiles.WHISTLING_WOODS).ownMusic());
        assertNull(neighbour("biomesoplenty:undergarden").ownMusic());
    }

    @Test
    public void biomeIdsMapToTartarusAndInstalledNeighbourProfiles() {
        int base = BiomeConfig.tartarusBiomeBaseId;
        BiomeGenBase[] registry = new BiomeGenBase[base + 64];
        for (int i = 0; i < TartarusBiomes.values().length; i++) registry[base + i] = BiomeGenBase.hell;
        registry[BiomeGenBase.hell.biomeID] = BiomeGenBase.hell;
        Map<String, BiomeGenBase> installed = Map.of("biomesoplenty:boneyard", BiomeGenBase.hell);
        Profile[] profiles = NetherAmbienceProfiles.byBiomeId(registry, installed::get);

        assertEquals(NetherAmbienceProfiles.of(Effects.BASALT_DELTAS), profiles[base]);
        assertEquals(
            NetherAmbienceProfiles.of(Effects.WARPED_FOREST),
            profiles[base + TartarusBiomes.WARPED_FOREST.ordinal()]);
        assertSame(neighbour("biomesoplenty:boneyard"), profiles[BiomeGenBase.hell.biomeID]);
        // Absent mods and empty ids have no profile.
        assertNull(profiles[base + TartarusBiomes.values().length]);
        assertNull(profiles[BiomeGenBase.plains.biomeID]);
    }

    @Test
    public void spotChanceKeepsVanillasCountAndFollowsTheParticleSetting() {
        float all = NetherAmbienceProfiles.spotChance(.025F, 0, 1F);
        assertEquals(.025F * 1334 / 400, all, 1E-6);
        assertEquals(all * .4F, NetherAmbienceProfiles.spotChance(.025F, 1, 1F), 1E-6);
        assertEquals(0, NetherAmbienceProfiles.spotChance(.025F, 2, 1F), 0);
        assertEquals(all / 2, NetherAmbienceProfiles.spotChance(.025F, 0, .5F), 1E-6);
        assertEquals(1F, NetherAmbienceProfiles.spotChance(.118F, 0, 4F), 0);
        assertEquals(0, NetherAmbienceProfiles.spotChance(.118F, 0, 0F), 0);
    }

    @Test
    public void settledValueWaitsForANewBiomeToHold() {
        NetherAmbienceProfiles.Settled<String> settled = new NetherAmbienceProfiles.Settled<>(3);
        assertEquals("crimson", settled.update("crimson"));
        assertEquals("crimson", settled.update("warped"));
        assertEquals("crimson", settled.update("warped"));
        assertEquals("warped", settled.update("warped"));
        // A flicker back and forth never switches.
        for (int i = 0; i < 10; i++) {
            assertEquals("warped", settled.update("basalt"));
            assertEquals("warped", settled.update("warped"));
        }
        // An interrupted streak starts over.
        settled.update("basalt");
        settled.update("basalt");
        settled.update("soul");
        assertEquals("warped", settled.update("basalt"));
        assertEquals("warped", settled.update("basalt"));
        assertEquals("basalt", settled.update("basalt"));
        settled.reset();
        assertNull(settled.value());
        assertEquals("soul", settled.update("soul"));
    }
}
