package industries.angelic.biomic.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Immutable worldgen-critical settings captured by the active generator version. */
public final class WorldgenConfigSnapshot {

    public static final int SEA_LEVEL = 63;

    private final int seaLevel;
    private final int temperateLowlandsId;
    private final int temperateArchipelagoId;
    private final int windsweptHighlandsId;
    private final int lushCavesId;
    private final int cloverPlainsId;
    private final int wisteriaGroveId;
    private final int pineSlopesId;
    private final int grassyBeachId;
    private final int chalkCliffsId;
    private final int toweringCliffsId;
    private final int redwoodsId;
    private final int bayouId;
    private final int oldGrowthBayouId;
    private final int joshuaDesertId;
    private final int eucalyptusForestId;
    private final int ruExpansionBaseId;
    private final int skyDonutId;
    private final int aeropelagoId;
    private final int origenBiomeBaseId;
    private final int soulblightForestId;
    private final int whistlingWoodsId;
    private final boolean ruSurfaceBiomesEnabled;
    private final boolean floatingIslandsEnabled;
    private final boolean hostedIslandPreview;
    private final String signature;

    private WorldgenConfigSnapshot(int seaLevel, int temperateLowlandsId, int temperateArchipelagoId,
        int windsweptHighlandsId, int lushCavesId, int cloverPlainsId, int wisteriaGroveId, int pineSlopesId,
        int grassyBeachId, int chalkCliffsId, int toweringCliffsId, int redwoodsId, int bayouId, int oldGrowthBayouId,
        int joshuaDesertId, int eucalyptusForestId, int ruExpansionBaseId, int skyDonutId, int aeropelagoId,
        int origenBiomeBaseId, int soulblightForestId, int whistlingWoodsId, boolean floatingIslandsEnabled,
        boolean hostedIslandPreview) {
        this.seaLevel = seaLevel;
        this.temperateLowlandsId = temperateLowlandsId;
        this.temperateArchipelagoId = temperateArchipelagoId;
        this.windsweptHighlandsId = windsweptHighlandsId;
        this.lushCavesId = lushCavesId;
        this.cloverPlainsId = cloverPlainsId;
        this.wisteriaGroveId = wisteriaGroveId;
        this.pineSlopesId = pineSlopesId;
        this.grassyBeachId = grassyBeachId;
        this.chalkCliffsId = chalkCliffsId;
        this.toweringCliffsId = toweringCliffsId;
        this.redwoodsId = redwoodsId;
        this.bayouId = bayouId;
        this.oldGrowthBayouId = oldGrowthBayouId;
        this.joshuaDesertId = joshuaDesertId;
        this.eucalyptusForestId = eucalyptusForestId;
        this.ruExpansionBaseId = ruExpansionBaseId;
        this.skyDonutId = skyDonutId;
        this.aeropelagoId = aeropelagoId;
        this.origenBiomeBaseId = origenBiomeBaseId;
        this.soulblightForestId = soulblightForestId;
        this.whistlingWoodsId = whistlingWoodsId;
        this.ruSurfaceBiomesEnabled = false;
        this.floatingIslandsEnabled = floatingIslandsEnabled;
        this.hostedIslandPreview = hostedIslandPreview;
        this.signature = sha256(canonicalForm());
    }

    public static WorldgenConfigSnapshot current() {
        return CurrentHolder.INSTANCE;
    }

    /** Tall (Hohen Angst) worlds host RU on uncompressed Origen and add their own sky layer. */
    public static WorldgenConfigSnapshot forWorld(net.minecraft.world.World world) {
        return industries.angelic.biomic.world.TallWorlds.liveTall(world) ? TallMainlandHolder.INSTANCE
            : BlendHolder.INSTANCE;
    }

    private static final class BlendHolder {

        private static final WorldgenConfigSnapshot INSTANCE = copyWithFeatureFlags(true);
    }

    /** Explicit pilot: hosted RU mainland, no implicitly borrowed compressed floating layer. */
    private static final class TallMainlandHolder {

        private static final WorldgenConfigSnapshot INSTANCE = copyWithFeatureFlags(false);
    }

    private static WorldgenConfigSnapshot copyWithFeatureFlags(boolean floatingIslandsEnabled) {
        WorldgenConfigSnapshot current = current();
        return new WorldgenConfigSnapshot(
            current.seaLevel,
            current.temperateLowlandsId,
            current.temperateArchipelagoId,
            current.windsweptHighlandsId,
            current.lushCavesId,
            current.cloverPlainsId,
            current.wisteriaGroveId,
            current.pineSlopesId,
            current.grassyBeachId,
            current.chalkCliffsId,
            current.toweringCliffsId,
            current.redwoodsId,
            current.bayouId,
            current.oldGrowthBayouId,
            current.joshuaDesertId,
            current.eucalyptusForestId,
            current.ruExpansionBaseId,
            current.skyDonutId,
            current.aeropelagoId,
            current.origenBiomeBaseId,
            current.soulblightForestId,
            current.whistlingWoodsId,
            floatingIslandsEnabled,
            true);
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public int getTemperateLowlandsId() {
        return temperateLowlandsId;
    }

    public int getTemperateArchipelagoId() {
        return temperateArchipelagoId;
    }

    public int getWindsweptHighlandsId() {
        return windsweptHighlandsId;
    }

    public int getLushCavesId() {
        return lushCavesId;
    }

    public int getCloverPlainsId() {
        return cloverPlainsId;
    }

    public int getWisteriaGroveId() {
        return wisteriaGroveId;
    }

    public int getPineSlopesId() {
        return pineSlopesId;
    }

    public int getGrassyBeachId() {
        return grassyBeachId;
    }

    public int getChalkCliffsId() {
        return chalkCliffsId;
    }

    public int getToweringCliffsId() {
        return toweringCliffsId;
    }

    public int getRedwoodsId() {
        return redwoodsId;
    }

    public int getBayouId() {
        return bayouId;
    }

    public int getOldGrowthBayouId() {
        return oldGrowthBayouId;
    }

    public int getJoshuaDesertId() {
        return joshuaDesertId;
    }

    public int getEucalyptusForestId() {
        return eucalyptusForestId;
    }

    public int getRuExpansionBaseId() {
        return ruExpansionBaseId;
    }

    public int getSkyDonutId() {
        return skyDonutId;
    }

    public int getAeropelagoId() {
        return aeropelagoId;
    }

    public int getOrigenBiomeBaseId() {
        return origenBiomeBaseId;
    }

    public int getSoulblightForestId() {
        return soulblightForestId;
    }

    public int getWhistlingWoodsId() {
        return whistlingWoodsId;
    }

    public boolean isFloatingIslandsEnabled() {
        return floatingIslandsEnabled;
    }

    /**
     * True while the generator is being validated as an unmodified Origen baseline.
     *
     * <p>
     * Biomic-owned carvers, RU overlays, floating terrain, and the full-world structure pass must not leak into this
     * mode. Keeping the predicate in the frozen snapshot also makes the choice part of the world's compatibility
     * signature.
     * </p>
     */
    public boolean isStrictOrigenBaseline() {
        return !ruSurfaceBiomesEnabled && !floatingIslandsEnabled;
    }

    public String getSignature() {
        return signature;
    }

    public String canonicalForm() {
        return "seaLevel=" + seaLevel
            + ";temperateLowlandsId="
            + temperateLowlandsId
            + ";temperateArchipelagoId="
            + temperateArchipelagoId
            + ";windsweptHighlandsId="
            + windsweptHighlandsId
            + ";lushCavesId="
            + lushCavesId
            + ";cloverPlainsId="
            + cloverPlainsId
            + ";wisteriaGroveId="
            + wisteriaGroveId
            + ";pineSlopesId="
            + pineSlopesId
            + ";grassyBeachId="
            + grassyBeachId
            + ";chalkCliffsId="
            + chalkCliffsId
            + ";toweringCliffsId="
            + toweringCliffsId
            + ";redwoodsId="
            + redwoodsId
            + ";bayouId="
            + bayouId
            + ";oldGrowthBayouId="
            + oldGrowthBayouId
            + ";joshuaDesertId="
            + joshuaDesertId
            + ";eucalyptusForestId="
            + eucalyptusForestId
            + ";ruExpansionBaseId="
            + ruExpansionBaseId
            + ";skyDonutId="
            + skyDonutId
            + ";aeropelagoId="
            + aeropelagoId
            + ";origenBiomeBaseId="
            + origenBiomeBaseId
            + ";ruSurfaceBiomesEnabled="
            + ruSurfaceBiomesEnabled
            + ";floatingIslandsEnabled="
            + floatingIslandsEnabled
            + (hostedIslandPreview
                ? ";hostedIslandRecipe=2;mainlandHabitatRecipe=8;sinkholeRecipe=1;hostedCaveRecipe=2;mineshaftHostRecipe=1"
                : "");
    }

    private static final class CurrentHolder {

        private static final WorldgenConfigSnapshot INSTANCE = new WorldgenConfigSnapshot(
            SEA_LEVEL,
            BiomeConfig.temperateLowlandsId,
            BiomeConfig.temperateArchipelagoId,
            BiomeConfig.windsweptHighlandsId,
            BiomeConfig.lushCavesId,
            BiomeConfig.cloverPlainsId,
            BiomeConfig.wisteriaGroveId,
            BiomeConfig.pineSlopesId,
            BiomeConfig.grassyBeachId,
            BiomeConfig.chalkCliffsId,
            BiomeConfig.toweringCliffsId,
            BiomeConfig.redwoodsId,
            BiomeConfig.bayouId,
            BiomeConfig.oldGrowthBayouId,
            BiomeConfig.joshuaDesertId,
            BiomeConfig.eucalyptusForestId,
            BiomeConfig.ruExpansionBaseId,
            BiomeConfig.skyDonutId,
            BiomeConfig.aeropelagoId,
            BiomeConfig.origenBiomeBaseId,
            BiomeConfig.soulblightForestId,
            BiomeConfig.whistlingWoodsId,
            false,
            false);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder encoded = new StringBuilder(digest.length * 2);
            for (byte element : digest) {
                encoded.append(String.format("%02x", element & 0xFF));
            }
            return encoded.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The JVM does not provide SHA-256", exception);
        }
    }
}
