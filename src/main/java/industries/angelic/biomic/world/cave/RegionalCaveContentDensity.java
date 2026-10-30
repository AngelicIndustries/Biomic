package industries.angelic.biomic.world.cave;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Biomic smooth regional adaptation: source-faithful kernels, not upstream YUNG region-controller parity.
 * One convex density composition enters one immutable-host whole-clearance plan. The hosted expansion fills the
 * opened cells at and below YUNG's liquid altitude.
 */
public final class RegionalCaveContentDensity {

    public static final long YUNG_REGION_SALT = 0x59554E475245474EL, LIQUID_REGION_SALT = 0x59554E474C495155L;

    public record Settings(RegionalCaveDensity.Settings base, YungFlooredCavernDensity.Settings floored,
        YungFlooredCavernDensity.Settings liquid, GregCavernRegionSampler.Settings yungRegions,
        GregCavernRegionSampler.Settings liquidRegions) {

        public Settings {
            Objects.requireNonNull(base);
            Objects.requireNonNull(floored);
            Objects.requireNonNull(liquid);
            Objects.requireNonNull(yungRegions);
            Objects.requireNonNull(liquidRegions);
        }
    }

    public interface Column {

        double openDensity(int sourceY);
    }

    private final Settings settings;
    private final RegionalCaveDensity base;
    private final YungFlooredCavernDensity floored;
    private final YungLiquidCavernDensity liquid;
    private final GregCavernRegionSampler yungRegions, liquidRegions;

    public static Settings defaults() {
        return new Settings(
            new RegionalCaveDensity.Settings(
                new UltimateWorleyField.Settings(-.25f, 8f, 1.5f, 1f, 1, 128),
                true,
                GregOpenDensity.Policy.REGIONAL_CHAMBER_V1,
                new GregCavernRegionSampler.Settings(256, 1, 5, 48, 176),
                new CaveComposition.Limits(8, 120, 10, 4, 4)),
            new YungFlooredCavernDensity.Settings(8, 122, 10, .65f, .55f, .6f, 1, .3f, .028f, 2),
            new YungFlooredCavernDensity.Settings(8, 122, 10, .65f, .55f, .6f, 1, .3f, .03f, 2),
            new GregCavernRegionSampler.Settings(384, 1, 3, 64, 224),
            new GregCavernRegionSampler.Settings(256, 1, 2, 32, 160));
    }

    public RegionalCaveContentDensity(long seed, Settings settings) {
        this.settings = Objects.requireNonNull(settings);
        base = new RegionalCaveDensity(seed, settings.base());
        floored = new YungFlooredCavernDensity(seed, settings.floored());
        liquid = new YungLiquidCavernDensity(seed, settings.liquid());
        yungRegions = new GregCavernRegionSampler(seed ^ YUNG_REGION_SALT, settings.yungRegions());
        liquidRegions = new GregCavernRegionSampler(seed ^ LIQUID_REGION_SALT, settings.liquidRegions());
    }

    public Settings settings() {
        return settings;
    }

    private Column column(int x, int z, CaveComposition.Density planningBase) {
        var floorColumn = floored.column(x, z);
        var liquidColumn = liquid.column(x, z);
        double u = checkedWeight(yungRegions.weight(x, z));
        double l = checkedWeight(liquidRegions.weight(x, z));
        return new Column() {

            public double openDensity(int y) {
                if (y < 0 || y > 255) throw new IllegalArgumentException("Source Y must be within0..255");
                double core = u == 1 ? 0 : checked(planningBase.at(x, y, z));
                if (u == 0) return core;
                double f = l == 1 ? 0 : checked(floorColumn.openDensity(y));
                double q = l == 0 ? 0 : checked(liquidColumn.openDensity(y, 1));
                double chamber = l == 0 ? f : l == 1 ? q : CaveComposition.blend(f, q, l);
                return u == 1 ? chamber : CaveComposition.blend(core, chamber, u);
            }
        };
    }

    /** As above, with the Worley chunk fields shared across plans (the caller's cache of this seed's field). */
    public CaveComposition.Density createPlanningDensity(RegionalCaveDensity.ChunkFields worleyFields) {
        return createPlanningDensity(base.createPlanningDensity(worleyFields));
    }

    /** Shifted512 rows 0..255 are authored -64..191: Worley and clearance reach the same authored band as 256. */
    public static Settings shifted512Defaults() {
        Settings legacy = defaults();
        var b = legacy.base();
        return new Settings(
            new RegionalCaveDensity.Settings(
                new UltimateWorleyField.Settings(-.25f, 8f, 1.5f, 1f, 1, 190),
                b.continuousWorley(),
                b.gregPolicy(),
                b.regions(),
                new CaveComposition.Limits(8, 180, 10, 4, 4)),
            legacy.floored(),
            legacy.liquid(),
            legacy.yungRegions(),
            legacy.liquidRegions());
    }

    private CaveComposition.Density createPlanningDensity(CaveComposition.Density planningBase) {
        Map<Long, Column> columns = new HashMap<>();
        return (x, y, z) -> columns
            .computeIfAbsent(((long) x << 32) | (z & 0xffffffffL), ignored -> column(x, z, planningBase))
            .openDensity(y);
    }

    private static double checked(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Nonfinite content cave density");
        return value;
    }

    private static double checkedWeight(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Invalid regional influence");
        return value;
    }
}
