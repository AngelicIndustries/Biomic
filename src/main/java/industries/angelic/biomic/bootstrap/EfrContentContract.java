package industries.angelic.biomic.bootstrap;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import ganymedes01.etfuturum.configuration.ConfigBase;
import ganymedes01.etfuturum.configuration.configs.ConfigBlocksItems;
import ganymedes01.etfuturum.configuration.configs.ConfigExperiments;
import ganymedes01.etfuturum.configuration.configs.ConfigModCompat;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import industries.angelic.biomic.Biomic;

/** Makes the EFR content claimed by Biomic part of the hard-dependency contract. */
public final class EfrContentContract {

    private EfrContentContract() {}

    /**
     * Enforces and persists every EFR option required by Biomic before EFR registers its blocks in pre-initialization.
     */
    public static void enforce() {
        int changes = 0;
        changes += enforceExperiments(ConfigBase.EXPERIMENTS);
        changes += enforceBlocksAndItems();
        changes += enforceModCompatibility();
        changes += enforceWorldOptions();

        // EFR loaded these values while its early-mixin plugin was selecting mixins. Keep the already-loaded
        // fields in lockstep with the persisted properties; EFR's block enum reads them during its later pre-init.
        ConfigExperiments.enableDripstone = true;
        ConfigExperiments.enableMangroveBlocks = true;
        ConfigExperiments.enableMossAzalea = true;
        // EFR gates shroomlight registration behind either Nether wood family. RU's Overworld
        // bioshrooms use it too; a disabled ModBlocks entry still returns an unregistered object.
        ConfigExperiments.enableWarpedBlocks = true;
        // Gardens' Whistling Woods and authored Tartarus features require crimson nylium/fungi/vines.
        // This enables blocks only, never EFR's global provider or biome selection.
        ConfigExperiments.enableCrimsonBlocks = true;
        // Origen's authored vocabulary requires exact sculk and catalyst, not obsidian fallbacks.
        // This must be set before EFR's pre-init block registration, never during world replay.
        ConfigExperiments.enableSculk = true;

        ConfigBlocksItems.enableBasalt = true;
        ConfigBlocksItems.enableBeetroot = true;
        ConfigBlocksItems.enableCopper = true;
        ConfigBlocksItems.enableCornflower = true;
        ConfigBlocksItems.enableDeepslate = true;
        ConfigBlocksItems.enableDeepslateOres = true;
        ConfigBlocksItems.enableExtraVanillaSlabs = true;
        ConfigBlocksItems.enableGlowBerries = true;
        ConfigBlocksItems.enableLilyOfTheValley = true;
        ConfigBlocksItems.enableMagmaBlock = true;
        ConfigBlocksItems.enableSmoothStone = true;
        ConfigBlocksItems.enableStones = true;
        ConfigBlocksItems.enableTuff = true;

        ConfigModCompat.disableCopperOreAndIngotOnly = false;
        ConfigWorld.deepslateReplacesStones = true;

        Biomic.LOG.info("Enforced Biomic's EFR content contract ({} persisted option changes)", changes);
    }

    static int enforceExperiments(Configuration configuration) {
        return enforceAll(
            configuration,
            required("enableDripstone", true),
            required("enableMangroveBlocks", true),
            required("enableMossAzalea", true),
            required("enableCrimsonBlocks", true),
            required("enableWarpedBlocks", true),
            required("enableSculk", true));
    }

    private static int enforceBlocksAndItems() {
        return enforceAll(
            ConfigBase.BLOCKS_ITEMS,
            required("enableBasalt", true),
            required("enableBeetroot", true),
            required("enableCopper", true),
            required("enableCornflower", true),
            required("enableDeepslate", true),
            required("enableDeepslateOres", true),
            required("enableExtraVanillaSlabs", true),
            required("enableGlowBerries", true),
            required("enableLilyOfTheValley", true),
            required("enableMagmaBlock", true),
            required("enableSmoothStone", true),
            required("enableStones", true),
            required("enableTuff", true));
    }

    private static int enforceModCompatibility() {
        return enforceAll(ConfigBase.MOD_COMPAT, required("disableCopperOreAndIngotOnly", false));
    }

    private static int enforceWorldOptions() {
        return enforceAll(ConfigBase.WORLD, required("deepslateReplacesStones", true));
    }

    static int enforceAll(Configuration configuration, RequiredBoolean... requirements) {
        int changes = 0;
        for (RequiredBoolean requirement : requirements) {
            Property property = find(configuration, requirement.key);
            if (property == null) {
                throw new IllegalStateException(
                    "EFR no longer exposes required config option '" + requirement.key + "'");
            }
            if (property.getBoolean() != requirement.value) {
                property.set(requirement.value);
                changes++;
            }
        }
        if (changes > 0) {
            configuration.save();
        }
        return changes;
    }

    private static Property find(Configuration configuration, String key) {
        for (String categoryName : configuration.getCategoryNames()) {
            ConfigCategory category = configuration.getCategory(categoryName);
            if (category.containsKey(key)) {
                return category.get(key);
            }
        }
        return null;
    }

    static RequiredBoolean required(String key, boolean value) {
        return new RequiredBoolean(key, value);
    }

    static final class RequiredBoolean {

        private final String key;
        private final boolean value;

        private RequiredBoolean(String key, boolean value) {
            this.key = key;
            this.value = value;
        }
    }
}
