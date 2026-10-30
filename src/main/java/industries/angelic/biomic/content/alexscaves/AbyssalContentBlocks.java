package industries.angelic.biomic.content.alexscaves;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Dormant standalone registry for non-progression Abyssal terrain and ecology. */
public final class AbyssalContentBlocks {

    public static final Block ABYSSMARINE = new BlockAbyssalSolid("abyssmarine", 1.5F, 6F);
    public static final Block MUCK = new BlockAbyssalMuck();
    public static final BlockAbyssalAquatic VENT = new BlockAbyssalAquatic(
        "geothermal_vent",
        BlockAbyssalAquatic.Kind.VENT_FULL,
        "geothermal_vent",
        "geothermal_vent_top");
    public static final BlockAbyssalAquatic VENT_MEDIUM = aquatic(
        "geothermal_vent_medium",
        BlockAbyssalAquatic.Kind.VENT_MEDIUM,
        "geothermal_vent_medium",
        "geothermal_vent_medium_top");
    public static final BlockAbyssalAquatic VENT_THIN = aquatic(
        "geothermal_vent_thin",
        BlockAbyssalAquatic.Kind.VENT_THIN,
        "geothermal_vent_thin",
        "geothermal_vent_medium_top");
    public static final BlockAbyssalAquatic TUBE_WORM = aquatic(
        "tube_worm",
        BlockAbyssalAquatic.Kind.TUBE_WORM,
        "tube_worm",
        "tube_worm_turn",
        "tube_worm_elbow");
    public static final BlockAbyssalAquatic[] MUSSELS = { aquaticMussel(1), aquaticMussel(2), aquaticMussel(3),
        aquaticMussel(4), aquaticMussel(5) };
    public static final BlockAbyssalAquatic DRY_VENT_MEDIUM = new BlockAbyssalDryAquatic(
        "geothermal_vent_medium",
        BlockAbyssalAquatic.Kind.VENT_MEDIUM,
        "geothermal_vent_medium",
        "geothermal_vent_medium_top");
    public static final BlockAbyssalAquatic DRY_VENT_THIN = new BlockAbyssalDryAquatic(
        "geothermal_vent_thin",
        BlockAbyssalAquatic.Kind.VENT_THIN,
        "geothermal_vent_thin",
        "geothermal_vent_medium_top");
    public static final BlockAbyssalAquatic[] DRY_MUSSELS = { dryMussel(1), dryMussel(2), dryMussel(3), dryMussel(4),
        dryMussel(5) };
    public static final BlockAbyssalAquatic SPONGE = aquatic(
        "ping_pong_sponge",
        BlockAbyssalAquatic.Kind.SPONGE,
        "ping_pong_sponge",
        "ping_pong_sponge_top");
    public static final BlockAbyssalAquatic DUSK = aquatic(
        "dusk_anemone",
        BlockAbyssalAquatic.Kind.DUSK,
        "dusk_anemone_base",
        "dusk_anemone_bottom",
        "dusk_anemone_top",
        "dusk_anemone_tentacles");
    public static final BlockAbyssalAquatic TWILIGHT = aquatic(
        "twilight_anemone",
        BlockAbyssalAquatic.Kind.TWILIGHT,
        "twilight_anemone_base",
        "twilight_anemone_bottom",
        "twilight_anemone_top",
        "twilight_anemone_tentacles");
    public static final BlockAbyssalAquatic MIDNIGHT = aquatic(
        "midnight_anemone",
        BlockAbyssalAquatic.Kind.MIDNIGHT,
        "midnight_anemone_base",
        "midnight_anemone_bottom",
        "midnight_anemone_top",
        "midnight_anemone_tentacles");
    private static final Map<String, Block> BLOCKS = blocks();
    private static boolean registered;

    private AbyssalContentBlocks() {}

    public static synchronized void register() {
        if (registered) return;
        for (Map.Entry<String, Block> entry : BLOCKS.entrySet()) {
            if (isNaturalBlock(entry.getValue())) entry.getValue()
                .setCreativeTab(BiomicBlocks.CREATIVE_TAB);
            if (itemFamily(entry.getValue()))
                GameRegistry.registerBlock(entry.getValue(), ItemBlockAbyssalAquatic.class, entry.getKey());
            else GameRegistry.registerBlock(entry.getValue(), entry.getKey());
        }
        registered = true;
        Biomic.LOG.info("Registered {} intrinsic Abyssal blocks", BLOCKS.size());
    }

    /** Supported fresh-generation/creative scope; all other registered IDs remain for saved content. */
    public static boolean isNaturalBlock(Block block) {
        if (block == ABYSSMARINE || block == MUCK
            || block == VENT
            || block == VENT_MEDIUM
            || block == VENT_THIN
            || block == TUBE_WORM
            || block == SPONGE
            || block == DUSK
            || block == TWILIGHT
            || block == MIDNIGHT) return true;
        for (Block mussel : MUSSELS) if (block == mussel) return true;
        return false;
    }

    private static BlockAbyssalAquatic aquaticMussel(int count) {
        return aquatic("mussel_" + count, BlockAbyssalAquatic.Kind.MUSSEL, "mussels");
    }

    private static BlockAbyssalAquatic dryMussel(int count) {
        return new BlockAbyssalDryAquatic("mussel_" + count, BlockAbyssalAquatic.Kind.MUSSEL, "mussels");
    }

    public static int musselCount(Block block) {
        for (int i = 0; i < MUSSELS.length; i++) if (block == MUSSELS[i] || block == DRY_MUSSELS[i]) return i + 1;
        return 0;
    }

    public static BlockAbyssalAquatic mussel(int count, boolean wet) {
        if (count < 1 || count > 5) throw new IllegalArgumentException("Invalid mussel count");
        return (wet ? MUSSELS : DRY_MUSSELS)[count - 1];
    }

    public static Block visibleItemBlock(Block block) {
        if (block == DRY_VENT_MEDIUM) return VENT_MEDIUM;
        if (block == DRY_VENT_THIN) return VENT_THIN;
        int count = musselCount(block);
        return count > 0 ? MUSSELS[count - 1] : block;
    }

    private static boolean itemFamily(Block block) {
        return block == VENT_MEDIUM || block == VENT_THIN
            || block == DRY_VENT_MEDIUM
            || block == DRY_VENT_THIN
            || musselCount(block) > 0;
    }

    private static BlockAbyssalAquatic aquatic(String name, BlockAbyssalAquatic.Kind kind, String... textures) {
        return new BlockAbyssalWaterlogged(name, kind, textures);
    }

    private static Map<String, Block> blocks() {
        Map<String, Block> out = new LinkedHashMap<>();
        out.put("alexs_abyssal_abyssmarine", ABYSSMARINE);
        out.put("alexs_abyssal_muck", MUCK);
        out.put("alexs_abyssal_geothermal_vent", VENT);
        out.put("alexs_abyssal_geothermal_vent_medium", VENT_MEDIUM);
        out.put("alexs_abyssal_geothermal_vent_thin", VENT_THIN);
        out.put("alexs_abyssal_tube_worm", TUBE_WORM);
        for (int i = 0; i < MUSSELS.length; i++) out.put("alexs_abyssal_mussel_" + (i + 1), MUSSELS[i]);
        out.put("alexs_abyssal_ping_pong_sponge", SPONGE);
        out.put("alexs_abyssal_geothermal_vent_medium_dry", DRY_VENT_MEDIUM);
        out.put("alexs_abyssal_geothermal_vent_thin_dry", DRY_VENT_THIN);
        for (int i = 0; i < DRY_MUSSELS.length; i++)
            out.put("alexs_abyssal_mussel_" + (i + 1) + "_dry", DRY_MUSSELS[i]);
        out.put("alexs_abyssal_dusk_anemone", DUSK);
        out.put("alexs_abyssal_twilight_anemone", TWILIGHT);
        out.put("alexs_abyssal_midnight_anemone", MIDNIGHT);
        return out;
    }
}
