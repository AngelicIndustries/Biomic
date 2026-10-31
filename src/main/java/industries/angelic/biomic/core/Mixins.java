package industries.angelic.biomic.core;

import java.util.List;
import java.util.Set;

import industries.angelic.angellib.core.AngelMixins;
import industries.angelic.angellib.util.MiscUtil;
import industries.angelic.angellib.util.MiscUtil.Side;
import industries.angelic.angellib.util.MixinUtil;

public final class Mixins extends AngelMixins {

    private static final Mixins INSTANCE = new Mixins();

    private Mixins() {}

    @Override
    protected void registerMixins(MixinUtil.Registry registry) {
        registry.mixin("MixinWorldNativeFeatureCapture")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinWorldProviderBiomicNether")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinWorldServerBiomicNether")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinChunkProviderServerNetherGeneration")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinChunkProviderServerForeignGeneration")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinChunkProviderServerDeferredGeneration")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinChunkProviderServerGenerating")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinGameRegistryForeignGenerators")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinChunkIOExecutorDeferredDrop")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinWorldDeferredActiveChunks")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinWorldNetherForeignBlocks")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinWetMangroveSourceWater")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinRetainedPlantWaterDecay")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinWorldNativeEcologyCapture")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinNativeCaveVineRandom")
            .modid("etfuturum")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinBopWaspHiveRandom")
            .modid("biomesoplenty")
            .extraModid("BiomesOPlenty")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinBopWeightedRandom")
            .modid("biomesoplenty")
            .extraModid("BiomesOPlenty")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinBopWeightedOrder")
            .modid("biomesoplenty")
            .extraModid("BiomesOPlenty")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinVillageNamesRadius")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinStructureVillagePiecesRadius")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinVillagerRegistryOrder")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinWorldVillageGround")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinStructureStartVillage")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinStructureComponentSpacing")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinVillagePathSpacing")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinVillageNamesGround")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinVillageNamesStyle")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinVillageNamesSpacing")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinVillageNamesMonumentStart")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinVillageNamesMonumentBuilding")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinVillageNamesMonumentWater")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinVillageNamesElderHeight")
            .modid("villagenames")
            .extraModid("VillageNames")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinNetherliciousPortalBlocks")
            .modid("netherlicious")
            .phase(MixinUtil.Phase.LATE)
            .build();
        // Biomic's Nether ambience takes over the Tartarus Nether's sounds from Netherlicious and Et Futurum.
        registry.mixin("MixinNetherliciousAmbience")
            .modid("netherlicious")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinNetherliciousMood")
            .modid("netherlicious")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinNetherliciousMusic")
            .modid("netherlicious")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinEtFuturumNetherAmbience")
            .modid("etfuturum")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinPmpNativeTerminalRandom")
            .modid("plantmegapack")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinMarshReedsBottomPass")
            .modid("saltymod")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinMarshReedsTopPass")
            .modid("saltymod")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinMarshReedsRenderer")
            .modid("saltymod")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinNativeCoralPass")
            .modid("biomesoplenty")
            .extraModid("BiomesOPlenty")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("AccessorChunk")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("AccessorNBTTagList")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("AccessorMineshaftCorridor")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("AccessorStructureComponent")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("AccessorScatteredFeature")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        registry.mixin("MixinChunk")
            .phase(MixinUtil.Phase.EARLY)
            .build();
        // Command classes also execute in the integrated server on physical clients.
        registry.mixin("MixinBlockGrassAlphaRendering")
            .phase(MixinUtil.Phase.EARLY)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinChunkCacheBiomes")
            .phase(MixinUtil.Phase.EARLY)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinVanillaBiomeColors")
            .phase(MixinUtil.Phase.EARLY)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinClonedSectionBiomes")
            .modid("angelica")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinWorldSliceBiomes")
            .modid("angelica")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinSmoothBiomeColors")
            .modid("angelica")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinMCPatcherBiomeColors")
            .modid("angelica")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinRuShaderMaterials")
            .modid("angelica")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinAquaticRenderPass")
            .modid("angelica")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();
        registry.mixin("MixinGlowBerriesLight")
            .modid("angelica")
            .phase(MixinUtil.Phase.LATE)
            .side(Side.CLIENT)
            .build();

        registry.mixin("MixinEtFuturumWorldGenerator")
            .modid("etfuturum")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinEtFuturumLateWorldGenerator")
            .modid("etfuturum")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinWorldEventHandler")
            .modid("etfuturum")
            .phase(MixinUtil.Phase.LATE)
            .build();
        registry.mixin("MixinBlockPinkPetalsRenderer")
            .modid("etfuturum")
            .phase(MixinUtil.Phase.LATE)
            .side(MiscUtil.Side.CLIENT)
            .build();
    }

    public static List<String> getEarlyMixinsForLoader() {
        return INSTANCE.getEarlyMixins();
    }

    public static List<String> getLateMixinsForLoader(Set<String> loadedMods) {
        List<String> selected = INSTANCE.getLateMixins(loadedMods);
        if (loadedMods.contains("BiomesOPlenty")) {
            var mod = cpw.mods.fml.common.Loader.instance()
                .getIndexedModList()
                .get("BiomesOPlenty");
            if (mod == null || !"2.1.0".equals(mod.getVersion()))
                selected.removeIf(name -> name.startsWith("biomesoplenty."));
        }
        if (loadedMods.contains("plantmegapack")) {
            var mods = cpw.mods.fml.common.Loader.instance()
                .getIndexedModList();
            var pmp = mods.get("plantmegapack");
            var fix = mods.get("plantmegapackfix");
            if (pmp == null || fix == null || !"4.33".equals(pmp.getVersion()) || !"1333d4e".equals(fix.getVersion()))
                selected.removeIf(name -> name.startsWith("plantmegapack."));
        }
        if (loadedMods.contains("saltymod")) {
            cpw.mods.fml.common.ModContainer mod = cpw.mods.fml.common.Loader.instance()
                .getIndexedModList()
                .get("saltymod");
            // Modrinth release 2.0.8 reports this build version internally. Other builds
            // may have fixed the SideOnly initializers and must not receive this patch.
            if (mod == null || !"2.0.0-master.150+9a0954aa6f".equals(mod.getVersion()))
                selected.removeIf(name -> name.startsWith("saltymod."));
        }
        String naturaVersion = null;
        if (loadedMods.contains("Natura") || loadedMods.contains("natura")) {
            var mod = cpw.mods.fml.common.Loader.instance()
                .getIndexedModList()
                .get(loadedMods.contains("Natura") ? "Natura" : "natura");
            if (mod != null) naturaVersion = mod.getVersion();
        }
        industries.angelic.biomic.compat.flora.NaturaMixinVersions.filter(selected, loadedMods, naturaVersion);
        return selected;
    }
}
