package industries.angelic.biomic;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.client.VanillaTextures;
import industries.angelic.biomic.client.VerticalBiomeDebugOverlay;
import industries.angelic.biomic.client.render.AquaticPlantRenderer;
import industries.angelic.biomic.client.render.ChalkGrassRenderer;
import industries.angelic.biomic.client.render.DripleafRenderer;
import industries.angelic.biomic.client.render.EucalyptusBranchRenderer;
import industries.angelic.biomic.client.render.EucalyptusLogRenderer;
import industries.angelic.biomic.client.render.JoshuaBeardRenderer;
import industries.angelic.biomic.client.render.JoshuaCrownRenderer;
import industries.angelic.biomic.client.render.LushPlantRenderer;
import industries.angelic.biomic.client.render.PalmBeardRenderer;
import industries.angelic.biomic.client.render.RuAquaticRenderer;
import industries.angelic.biomic.client.render.RuBranchRenderer;
import industries.angelic.biomic.client.render.RuDoublePlantRenderer;
import industries.angelic.biomic.client.render.SculkRenderer;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit() {
        super.preInit();
        VanillaTextures.inject();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        BiomicRenderIds.DRIPLEAF = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new DripleafRenderer(BiomicRenderIds.DRIPLEAF));
        BiomicRenderIds.LUSH_PLANTS = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new LushPlantRenderer(BiomicRenderIds.LUSH_PLANTS));
        BiomicRenderIds.AQUATIC_PLANTS = RenderingRegistry.getNextAvailableRenderId();
        BiomicRenderIds.WET_MANGROVE_ROOTS = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.SkylandsWetMangroveRootsRenderer(
                BiomicRenderIds.WET_MANGROVE_ROOTS));
        BiomicRenderIds.WET_GLOW_LICHEN = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.SkylandsWetGlowLichenRenderer(BiomicRenderIds.WET_GLOW_LICHEN));
        BiomicRenderIds.FROZEN_GRASS = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.FrozenGrassRenderer(BiomicRenderIds.FROZEN_GRASS));
        RenderingRegistry.registerBlockHandler(new AquaticPlantRenderer(BiomicRenderIds.AQUATIC_PLANTS));
        BiomicRenderIds.GROUND_SHELL = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.GroundShellRenderer(BiomicRenderIds.GROUND_SHELL));
        BiomicRenderIds.CORAL_WALL_FANS = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.CoralWallFanRenderer(BiomicRenderIds.CORAL_WALL_FANS));
        BiomicRenderIds.AQUATIC_SPRITE = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new AquaticPlantRenderer(BiomicRenderIds.AQUATIC_SPRITE, false));
        BiomicRenderIds.NATIVE_AQUATIC = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.NativeAquaticPlantRenderer(BiomicRenderIds.NATIVE_AQUATIC));
        BiomicRenderIds.SAGUARO = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry
            .registerBlockHandler(new industries.angelic.biomic.client.render.SaguaroRenderer(BiomicRenderIds.SAGUARO));
        BiomicRenderIds.SMALL_OAK_LOG = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.SmallOakLogRenderer(BiomicRenderIds.SMALL_OAK_LOG));
        BiomicRenderIds.SCULK = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new SculkRenderer(BiomicRenderIds.SCULK));
        BiomicRenderIds.RU_AQUATIC = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new RuAquaticRenderer(BiomicRenderIds.RU_AQUATIC));
        BiomicRenderIds.RU_BRANCH = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new RuBranchRenderer(BiomicRenderIds.RU_BRANCH));
        BiomicRenderIds.RU_LEAF_LITTER = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.RuLeafLitterRenderer(BiomicRenderIds.RU_LEAF_LITTER));
        BiomicRenderIds.RU_FLOWERING_LEAVES = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.RuFloweringLeavesRenderer(BiomicRenderIds.RU_FLOWERING_LEAVES));
        BiomicRenderIds.RU_CONEFLOWER = RenderingRegistry.getNextAvailableRenderId();
        BiomicRenderIds.RU_CAVE_MODEL = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.RuCaveModelRenderer(BiomicRenderIds.RU_CAVE_MODEL));
        BiomicRenderIds.RU_MODEL_SPRITE = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.RuCaveModelRenderer(BiomicRenderIds.RU_MODEL_SPRITE, false));
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.RuConeflowerRenderer(BiomicRenderIds.RU_CONEFLOWER));
        BiomicRenderIds.PALM_BEARD = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new PalmBeardRenderer(BiomicRenderIds.PALM_BEARD));
        BiomicRenderIds.JOSHUA_CROWN = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new JoshuaCrownRenderer(BiomicRenderIds.JOSHUA_CROWN));
        BiomicRenderIds.JOSHUA_BEARD = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new JoshuaBeardRenderer(BiomicRenderIds.JOSHUA_BEARD));
        BiomicRenderIds.RU_DOUBLE_PLANT = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new RuDoublePlantRenderer(BiomicRenderIds.RU_DOUBLE_PLANT));
        BiomicRenderIds.CHALK_GRASS = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new ChalkGrassRenderer(BiomicRenderIds.CHALK_GRASS));
        BiomicRenderIds.EUCALYPTUS_LOG = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new EucalyptusLogRenderer(BiomicRenderIds.EUCALYPTUS_LOG));
        BiomicRenderIds.EUCALYPTUS_BRANCH = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(new EucalyptusBranchRenderer(BiomicRenderIds.EUCALYPTUS_BRANCH));
        BiomicRenderIds.GARDENS_SOUL_SPORE = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.GardensSoulSporeRenderer(BiomicRenderIds.GARDENS_SOUL_SPORE));
        BiomicRenderIds.GARDENS_WHISTLECANE = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.GardensWhistlecaneRenderer(
                BiomicRenderIds.GARDENS_WHISTLECANE));
        BiomicRenderIds.ABYSSAL_MODEL = RenderingRegistry.getNextAvailableRenderId();
        BiomicRenderIds.ABYSSAL_GENERATED_ITEM = RenderingRegistry.getNextAvailableRenderId();
        industries.angelic.biomic.content.alexscaves.AbyssalClientRendering
            .bindRenderIds(BiomicRenderIds.ABYSSAL_MODEL, BiomicRenderIds.ABYSSAL_GENERATED_ITEM);
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.AbyssalBlockRenderer(BiomicRenderIds.ABYSSAL_MODEL));
        RenderingRegistry.registerBlockHandler(
            new industries.angelic.biomic.client.render.AbyssalBlockRenderer(
                BiomicRenderIds.ABYSSAL_GENERATED_ITEM,
                false));
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(industries.angelic.biomic.client.GardensWhistlecaneEffects.INSTANCE);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(industries.angelic.biomic.client.AbyssalVentEffects.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new VerticalBiomeDebugOverlay());
        // Sounds and fog on the Forge bus, the loop and particles on FML's.
        MinecraftForge.EVENT_BUS.register(industries.angelic.biomic.client.NetherAmbience.INSTANCE);
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(industries.angelic.biomic.client.NetherAmbience.INSTANCE);
        industries.angelic.biomic.client.FrameTimeLog.registerIfEnabled();
        net.minecraftforge.client.ClientCommandHandler.instance
            .registerCommand(new industries.angelic.biomic.client.CopyPosCommand());
        industries.angelic.biomic.client.browser.BrowserKeys.register();
    }

    @Override
    public void gardensWhistle(int x, int y, int z) {
        industries.angelic.biomic.client.GardensWhistlecaneEffects.add(x, y, z);
    }

    /** The world type has no world, so the client's own decides: Origen's modern 192, raised 128 at 512. */
    @Override
    public float cloudHeight() {
        return industries.angelic.biomic.world.TallWorlds
            .liveTall(net.minecraft.client.Minecraft.getMinecraft().theWorld) ? 320 : 128;
    }
}
