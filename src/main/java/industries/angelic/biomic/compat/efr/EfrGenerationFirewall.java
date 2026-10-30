package industries.angelic.biomic.compat.efr;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.WorldInfo;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.config.DebugConfig;

/** Ownership boundary around EFR generation paths that would otherwise compete with Biomic. */
public final class EfrGenerationFirewall {

    public static final int OVERWORLD_DIMENSION_ID = 0;
    public static final String BIOMIC_WORLD_TYPE_ID = Biomic.MODID;

    private EfrGenerationFirewall() {}

    public static EfrFirewallAction observe(EfrGeneratorStage stage, int chunkX, int chunkZ, World world) {
        int dimensionId = dimensionId(world);
        String worldTypeId = worldTypeId(world);
        EfrFirewallAction action = classify(world, dimensionId, worldTypeId);

        if (DebugConfig.traceEfrGenerationFirewall) {
            Biomic.LOG.info(
                "EFR generation firewall: stage={}, target={}, weight={}, chunk=[{}, {}], dimension={}, worldType={}, action={}",
                stage,
                stage.getTargetClassName(),
                stage.getRegistrationWeight(),
                chunkX,
                chunkZ,
                dimensionId,
                worldTypeId,
                action);
        }
        return action;
    }

    public static EfrFirewallAction observeTreePostProcess(int blockX, int blockZ, World world) {
        int dimensionId = dimensionId(world);
        String worldTypeId = worldTypeId(world);
        EfrFirewallAction action = classify(world, dimensionId, worldTypeId);

        if (DebugConfig.traceEfrGenerationFirewall) {
            Biomic.LOG.info(
                "EFR generation firewall: stage=TREE_POST_PROCESS, target=WorldEventHandler.onTreeGenerated, "
                    + "block=[{}, {}], dimension={}, worldType={}, action={}",
                blockX,
                blockZ,
                dimensionId,
                worldTypeId,
                action);
        }
        return action;
    }

    /**
     * The Tartarus Nether already places EFR's Nether blocks; EFR's own pass would double them and add overworld ores.
     */
    private static EfrFirewallAction classify(World world, int dimensionId, String worldTypeId) {
        if (industries.angelic.biomic.world.ChunkProviderBiomicNether.generates(world))
            return EfrFirewallAction.SUPPRESS;
        return classify(dimensionId, worldTypeId);
    }

    static EfrFirewallAction classify(int dimensionId, String worldTypeId) {
        if (dimensionId == OVERWORLD_DIMENSION_ID && (BIOMIC_WORLD_TYPE_ID.equals(worldTypeId)
            || industries.angelic.biomic.world.WorldTypeBiomicBlend.ID.equals(worldTypeId))) {
            return EfrFirewallAction.SUPPRESS;
        }
        return EfrFirewallAction.PASS_THROUGH;
    }

    private static int dimensionId(World world) {
        return world == null || world.provider == null ? Integer.MIN_VALUE : world.provider.dimensionId;
    }

    private static String worldTypeId(World world) {
        if (world == null) {
            return "<missing>";
        }
        WorldInfo worldInfo = world.getWorldInfo();
        if (worldInfo == null) {
            return "<missing>";
        }
        WorldType worldType = worldInfo.getTerrainType();
        return worldType == null ? "<missing>" : worldType.getWorldTypeName();
    }
}
