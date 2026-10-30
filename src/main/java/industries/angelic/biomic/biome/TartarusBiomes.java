package industries.angelic.biomic.biome;

import industries.angelic.biomic.Biomic;

/**
 * Tartarus' twelve Nether biomes, in pack order, with the pack's map colours. THE_BOUNDARY is an extrusion above the
 * bedrock roof (y 256..285) and never generates in a 256-tall Nether.
 */
public enum TartarusBiomes {

    BASALTIC_DELTAS("Basaltic Deltas", 0x4a3e3d, Effects.BASALT_DELTAS),
    BLOODLESS_UNDERGROWTH("Bloodless Undergrowth", 0xd15f56, Effects.WARPED_FOREST),
    BLOODY_REMNANTS("Bloody Remnants", 0xf21200, Effects.CRIMSON_FOREST),
    CORRUPTED_WASTES("Corrupted Wastes", 0xf22451, Effects.NETHER_WASTES),
    CRIMSON_FOREST("Crimson Forest", 0xf24a24, Effects.CRIMSON_FOREST),
    CRYSTALLINE_VELDTS("Crystalline Veldts", 0xde4ede, Effects.WARPED_FOREST),
    FLAMING_HELLSCAPES("Flaming Hellscapes", 0xdea43a, Effects.NETHER_WASTES),
    LIFELESS_SPIRES("Lifeless Spires", 0x291f1d, Effects.BASALT_DELTAS),
    SERRATED_MOUNTAINS("Serrated Mountains", 0x121111, Effects.BASALT_DELTAS),
    SOUL_VALLEY("Soul Valley", 0x5e4429, Effects.SOUL_SAND_VALLEY),
    TORRID_CRAGS("Torrid Crags", 0xd1b06f, Effects.NETHER_WASTES),
    WARPED_FOREST("Warped Forest", 0x4d73bd, Effects.WARPED_FOREST);

    /** The modern vanilla biome whose ambience a Tartarus biome borrows (the pack's {@code vanilla:} key). */
    public enum Effects {

        NETHER_WASTES(0x330808, null, 0),
        CRIMSON_FOREST(0x330303, AmbientParticle.CRIMSON_SPORE, .025F),
        WARPED_FOREST(0x1a051a, AmbientParticle.WARPED_SPORE, .01428F),
        SOUL_SAND_VALLEY(0x1b4745, AmbientParticle.ASH, .00625F),
        BASALT_DELTAS(0x685f70, AmbientParticle.WHITE_ASH, .118F);

        public final int fogColor;
        /** Vanilla's ambient particle (null: none) and the chance it spawns at each sampled spot. */
        public final AmbientParticle particle;
        public final float particleChance;

        Effects(int fogColor, AmbientParticle particle, float particleChance) {
            this.fogColor = fogColor;
            this.particle = particle;
            this.particleChance = particleChance;
        }

        /** Its modern sound set: ambient.KEY.loop, .mood and .additions, and music.nether.KEY. */
        public String key() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** Modern Nether ambient particles. Snow and soulblight spores are tints for neighbour biomes. */
    public enum AmbientParticle {
        CRIMSON_SPORE,
        WARPED_SPORE,
        SOULBLIGHT_SPORE,
        ASH,
        WHITE_ASH,
        SNOW
    }

    public final String displayName;
    public final int color;
    public final Effects effects;

    TartarusBiomes(String displayName, int color, Effects effects) {
        this.displayName = displayName;
        this.color = color;
        this.effects = effects;
    }

    public String key() {
        return Biomic.MODID + ":" + name().toLowerCase(java.util.Locale.ROOT);
    }

    /** The Tartarus biome a registered biome id stands for, or null. */
    public static TartarusBiomes fromBiomeId(int id) {
        int index = id - industries.angelic.biomic.config.BiomeConfig.tartarusBiomeBaseId;
        return index >= 0 && index < VALUES.length ? VALUES[index] : null;
    }

    private static final TartarusBiomes[] VALUES = values();
}
