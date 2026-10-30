package industries.angelic.biomic.bootstrap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import com.falsepattern.endlessids.config.GeneralConfig;
import com.falsepattern.endlessids.constants.ExtendedConstants;
import com.falsepattern.endlessids.mixin.helpers.ChunkBiomeHook;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.versioning.ArtifactVersion;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.configuration.configs.ConfigExperiments;
import ganymedes01.etfuturum.configuration.configs.ConfigWorld;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.storage.BiomeVolumeChunk;

/** Verifies assumptions that would otherwise corrupt or misinterpret generated chunks. */
public final class BootstrapValidator {

    private static final List<DependencyRequirement> REQUIRED_DEPENDENCIES = Arrays.asList(
        new DependencyRequirement("angellib", Biomic.ANGELLIB_MIN_VERSION),
        new DependencyRequirement("etfuturum", Biomic.EFR_MIN_VERSION),
        new DependencyRequirement("endlessids", Biomic.ENDLESS_IDS_MIN_VERSION),
        new DependencyRequirement("chunkapi", Biomic.CHUNK_API_MIN_VERSION),
        new DependencyRequirement("gtnhlib", Biomic.GTNH_LIB_MIN_VERSION));

    private BootstrapValidator() {}

    public static void validate() {
        validateDependencies(loadedVersions());
        validateEndlessIdsBiomeStorage();
        validateEfrLushCaveContent(
            ModBlocks.MOSS_BLOCK.isEnabled() && ModBlocks.MOSS_CARPET.isEnabled() && ModBlocks.AZALEA.isEnabled(),
            ModBlocks.CAVE_VINE.isEnabled() && ModBlocks.CAVE_VINE_PLANT.isEnabled());
        validateEfrTerrestrialDetailContent(
            ModBlocks.BEETROOTS.isEnabled(),
            ModBlocks.CORNFLOWER.isEnabled(),
            ModBlocks.LILY_OF_THE_VALLEY.isEnabled(),
            ModBlocks.SMOOTH_STONE.isEnabled(),
            ModBlocks.STONE_SLAB.isEnabled());
        validateEfrMangroveContent(
            ConfigExperiments.enableMangroveBlocks && ModBlocks.MANGROVE_LOG.isEnabled()
                && ModBlocks.MANGROVE_ROOTS.isEnabled()
                && ModBlocks.MUDDY_MANGROVE_ROOTS.isEnabled()
                && ModBlocks.LEAVES.isEnabled()
                && ModBlocks.SAPLING.isEnabled());
        validateEfrDripstoneContent(
            ConfigExperiments.enableDripstone && ModBlocks.DRIPSTONE_BLOCK.isEnabled()
                && ModBlocks.POINTED_DRIPSTONE.isEnabled());
        validateEfrCopperContent(ModBlocks.COPPER_ORE.isEnabled());
        validateEfrShroomlightContent(
            ModBlocks.SHROOMLIGHT.isEnabled(),
            net.minecraft.block.Block.getIdFromBlock(ModBlocks.SHROOMLIGHT.get()) > 0);
        validateEfrSculkContent(
            ConfigExperiments.enableSculk,
            exactRegistered(ModBlocks.SCULK),
            exactRegistered(ModBlocks.SCULK_CATALYST));
        validateEfrVolcanicContent(ModBlocks.BASALT.isEnabled(), ModBlocks.MAGMA.isEnabled());
        validateEfrLateUndergroundContent(
            ModBlocks.DEEPSLATE.isEnabled(),
            ModBlocks.TUFF.isEnabled(),
            ModBlocks.STONE.isEnabled(),
            ModBlocks.DEEPSLATE_COAL_ORE.isEnabled() && ModBlocks.DEEPSLATE_IRON_ORE.isEnabled()
                && ModBlocks.DEEPSLATE_GOLD_ORE.isEnabled()
                && ModBlocks.DEEPSLATE_REDSTONE_ORE.isEnabled()
                && ModBlocks.DEEPSLATE_LAPIS_ORE.isEnabled()
                && ModBlocks.DEEPSLATE_DIAMOND_ORE.isEnabled(),
            ConfigWorld.deepslateReplacesStones);
        Biomic.LOG.info(
            "Validated {} hard dependencies and EndlessIDs {}-bit surface-biome storage",
            REQUIRED_DEPENDENCIES.size(),
            ExtendedConstants.bitsPerBiome);
    }

    static void validateDependencies(Map<String, String> installedVersions) {
        for (DependencyRequirement requirement : REQUIRED_DEPENDENCIES) {
            String installedVersion = installedVersions.get(requirement.modId);
            if (installedVersion == null) {
                fail("required mod '" + requirement.modId + "' is not loaded");
            }
            if (!requirement.accepts(installedVersion)) {
                fail(
                    "required mod '" + requirement.modId
                        + "' is version "
                        + installedVersion
                        + ", but Biomic requires "
                        + requirement.minimumVersion
                        + " or newer");
            }
        }
    }

    static void validateEfrLushCaveContent(boolean mossAndAzaleaEnabled, boolean caveVinesEnabled) {
        if (!mossAndAzaleaEnabled) {
            fail(
                "EFR moss and azalea content is disabled; set B:enableMossAzalea=true in config/etfuturum/experiments.cfg");
        }
        if (!caveVinesEnabled) {
            fail("EFR cave vines are disabled; set B:enableGlowBerries=true in config/etfuturum/blocksitems.cfg");
        }
    }

    static void validateEfrShroomlightContent(boolean enabled, boolean registered) {
        if (!enabled || !registered) {
            fail(
                "EFR shroomlight is disabled or unregistered; Biomic requires enableWarpedBlocks=true in "
                    + "config/etfuturum/experiments.cfg before EFR registers its blocks");
        }
    }

    private static boolean exactRegistered(ModBlocks entry) {
        if (!entry.isEnabled()) return false;
        Block block = entry.get();
        if (block == null) return false;
        Object name = Block.blockRegistry.getNameForObject(block);
        return name != null && Block.blockRegistry.getObject(name) == block;
    }

    static void validateEfrSculkContent(boolean enabled, boolean sculkRegistered, boolean catalystRegistered) {
        if (!enabled || !sculkRegistered || !catalystRegistered) fail(
            "EFR sculk or sculk catalyst is disabled or unregistered; Biomic requires "
                + "B:enableSculk=true in config/etfuturum/experiments.cfg before EFR registers its blocks");
    }

    static void validateEfrCopperContent(boolean copperOreEnabled) {
        if (!copperOreEnabled) {
            fail(
                "EFR copper ore is disabled; set B:enableCopper=true in config/etfuturum/blocksitems.cfg and "
                    + "B:disableCopperOreAndIngotOnly=false in config/etfuturum/modcompat.cfg");
        }
    }

    static void validateEfrTerrestrialDetailContent(boolean beetrootsEnabled, boolean cornflowersEnabled,
        boolean liliesEnabled, boolean smoothStoneEnabled, boolean extraSlabsEnabled) {
        if (!beetrootsEnabled || !cornflowersEnabled || !liliesEnabled) {
            fail(
                "EFR terrestrial flora is disabled; enable beetroots, cornflowers, and lilies of the valley in "
                    + "config/etfuturum/blocksitems.cfg");
        }
        if (!smoothStoneEnabled || !extraSlabsEnabled) {
            fail(
                "EFR terrain detail blocks are disabled; set B:enableSmoothStone=true and "
                    + "B:enableExtraVanillaSlabs=true in config/etfuturum/blocksitems.cfg");
        }
    }

    static void validateEfrMangroveContent(boolean mangroveContentEnabled) {
        if (!mangroveContentEnabled) {
            fail(
                "EFR mangrove content is disabled; set B:enableMangroveBlocks=true in "
                    + "config/etfuturum/experiments.cfg");
        }
    }

    static void validateEfrDripstoneContent(boolean dripstoneContentEnabled) {
        if (!dripstoneContentEnabled) {
            fail(
                "EFR dripstone content is disabled; set B:enableDripstone=true in "
                    + "config/etfuturum/experiments.cfg");
        }
    }

    static void validateEfrVolcanicContent(boolean basaltEnabled, boolean magmaEnabled) {
        if (!basaltEnabled || !magmaEnabled) {
            fail(
                "EFR basalt or magma content is disabled; set B:enableBasalt=true and B:enableMagmaBlock=true in "
                    + "config/etfuturum/blocksitems.cfg");
        }
    }

    static void validateEfrLateUndergroundContent(boolean deepslateEnabled, boolean tuffEnabled,
        boolean stoneVariantsEnabled, boolean deepslateOresEnabled, boolean deepslateReplacesStones) {
        if (!deepslateEnabled || !tuffEnabled || !stoneVariantsEnabled || !deepslateOresEnabled) {
            fail(
                "EFR late underground content is disabled; enable deepslate, deepslate ores, tuff, and stones in "
                    + "config/etfuturum/blocksitems.cfg");
        }
        if (!deepslateReplacesStones) {
            fail(
                "EFR deepslateReplacesStones is disabled; set B:deepslateReplacesStones=true in "
                    + "config/etfuturum/world.cfg");
        }
    }

    private static Map<String, String> loadedVersions() {
        Map<String, String> loadedVersions = new LinkedHashMap<>();
        for (Map.Entry<String, ModContainer> entry : Loader.instance()
            .getIndexedModList()
            .entrySet()) {
            loadedVersions.put(
                entry.getKey(),
                entry.getValue()
                    .getVersion());
        }
        return loadedVersions;
    }

    private static void validateEndlessIdsBiomeStorage() {
        if (!GeneralConfig.extendBiome) {
            fail("EndlessIDs biome extension is disabled; set B:extendBiome=true in config/endlessids.cfg");
        }

        int registrySize = BiomeGenBase.getBiomeGenArray().length;
        if (registrySize != ExtendedConstants.biomeIDCount) {
            fail(
                "EndlessIDs biome registry has " + registrySize + " slots; expected " + ExtendedConstants.biomeIDCount);
        }

        if (!ChunkBiomeHook.class.isAssignableFrom(Chunk.class)) {
            fail("EndlessIDs did not install ChunkBiomeHook on net.minecraft.world.chunk.Chunk");
        }
        if (!BiomeVolumeChunk.class.isAssignableFrom(Chunk.class)) {
            fail("Biomic did not install BiomeVolumeChunk on net.minecraft.world.chunk.Chunk");
        }
    }

    private static void fail(String reason) {
        String message = "Biomic bootstrap validation failed: " + reason;
        Biomic.LOG.fatal(message);
        throw new IllegalStateException(message);
    }

    private static final class DependencyRequirement {

        private final String modId;
        private final String minimumVersion;
        private final ArtifactVersion versionRange;

        private DependencyRequirement(String modId, String minimumVersion) {
            this.modId = modId;
            this.minimumVersion = minimumVersion;
            this.versionRange = VersionParser.parseVersionReference(modId + "@[" + minimumVersion + ",)");
        }

        private boolean accepts(String installedVersion) {
            return versionRange.containsVersion(new DefaultArtifactVersion(modId, installedVersion));
        }
    }
}
