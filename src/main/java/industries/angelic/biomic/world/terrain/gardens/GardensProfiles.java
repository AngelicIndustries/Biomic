package industries.angelic.biomic.world.terrain.gardens;

import java.util.List;
import java.util.Map;

/** Audited source data for the two Gardens of the Dead Nether habitats. Mob tables are descriptive only. */
public final class GardensProfiles {

    public static final String SOULBLIGHT_FOREST = "biomic:soulblight_forest";
    public static final String WHISTLING_WOODS = "biomic:whistling_woods";
    /** The Tartarus Nether's height: features keep below the bedrock roof. */
    public static final int SOURCE_MAX_Y = 255;

    public record Slot(String key, int attempts) {}

    public record Profile(int fogColor, List<Slot> intrinsic) {}

    private static final Profile SOULBLIGHT = new Profile(
        0x593A21,
        List.of(
            new Slot("soulblight_fungi", 3),
            new Slot("soulblight_forest_vegetation", 2),
            new Slot("short_standing_soul_spore_patch", 4),
            new Slot("long_standing_soul_spore_patch", 12),
            new Slot("short_hanging_soul_spore_patch", 4),
            new Slot("long_hanging_soul_spore_patch", 8)));
    private static final Profile WHISTLING = new Profile(
        0x5E1118,
        List.of(
            new Slot("nether_wart_block_pile", 1),
            new Slot("dense_weeping_vines", 40),
            new Slot("noisy_crimson_fungi", 1),
            new Slot("whistlecane_column", 20),
            new Slot("tall_blistercrown_patch", 1),
            new Slot("whistling_woods_vegetation", 10)));
    private static final Map<String, Profile> PROFILES = Map
        .of(SOULBLIGHT_FOREST, SOULBLIGHT, WHISTLING_WOODS, WHISTLING);

    private GardensProfiles() {}

    public static Profile profile(String key) {
        return key == null ? null : PROFILES.get(key);
    }

    /** NoiseBasedCountPlacement.of(6, 180, 0); caller samples the vanilla biome-info noise at x/180,z/180. */
    public static int noisyCrimsonFungusAnchors(double biomeInfoNoise) {
        return Math.max(0, (int) Math.ceil(biomeInfoNoise * 6D));
    }

}
