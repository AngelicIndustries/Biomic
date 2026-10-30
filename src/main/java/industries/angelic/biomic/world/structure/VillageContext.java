package industries.angelic.biomic.world.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;

/**
 * What vanilla's and Village Names' village code sees while Biomic builds or populates one of its villages. Building a
 * layout, the biome everywhere is the village's own 3D biome (an island's habitat for a sky village), and Village
 * Names' biome mapping holds just that biome with the village's style. Populating, ground is looked for only in the
 * village's own layer: below any sky terrain for a ground village, on the island for a sky village. Outside these
 * calls nothing changes.
 */
public final class VillageContext {

    /** {@code support}: where pieces may stand (a sky village's island), or null for anywhere. */
    private record Build(VillageStyle style, boolean villageNames, Predicate<StructureBoundingBox> support) {}

    /** What a piece runs into where the ground cannot carry it: callers only test for null. */
    private static final StructureComponent NO_GROUND = new StructureComponent() {

        @Override
        protected void func_143012_a(net.minecraft.nbt.NBTTagCompound tag) {}

        @Override
        protected void func_143011_b(net.minecraft.nbt.NBTTagCompound tag) {}

        @Override
        public boolean addComponentParts(World world, Random random, StructureBoundingBox box) {
            return true;
        }
    };

    private static final class Population {

        final World world;
        final BiomicVillagePlacement placement;
        final VillageTerrain terrain;
        BiomicVillagePlacement.Plan plan;
        int pathFloor = Integer.MIN_VALUE;

        Population(World world, BiomicVillagePlacement placement, VillageTerrain terrain) {
            this.world = world;
            this.placement = placement;
            this.terrain = terrain;
        }
    }

    /** How far above and below its pads a village looks for ground (roads between houses, lamps). */
    private static final int BAND = 32;

    private static final ThreadLocal<Build> BUILD = new ThreadLocal<>();
    private static final ThreadLocal<Population> POPULATION = new ThreadLocal<>();

    private VillageContext() {}

    static <T> T build(VillageStyle style, boolean villageNames, Predicate<StructureBoundingBox> support,
        Supplier<T> layout) {
        Build previous = BUILD.get();
        BUILD.set(new Build(style, villageNames, support));
        try {
            return layout.get();
        } finally {
            if (previous == null) BUILD.remove();
            else BUILD.set(previous);
        }
    }

    static boolean populate(World world, BiomicVillagePlacement placement, VillageTerrain terrain,
        BooleanSupplier population) {
        Population previous = POPULATION.get();
        POPULATION.set(new Population(world, placement, terrain));
        try {
            return population.getAsBoolean();
        } finally {
            if (previous == null) POPULATION.remove();
            else POPULATION.set(previous);
        }
    }

    /**
     * The biome a village layout being built sees everywhere, or null outside a build. Vanilla's layouts only tell
     * deserts apart, by vanilla's own desert biome, so a desert-styled vanilla village is built in it.
     */
    public static BiomeGenBase layoutBiome() {
        Build build = BUILD.get();
        if (build == null) return null;
        return !build.villageNames() && build.style()
            .desert() ? BiomeGenBase.desert
                : build.style()
                    .biome();
    }

    /** Whether a Biomic village layout is being built on this thread. */
    public static boolean building() {
        return BUILD.get() != null;
    }

    /**
     * The piece a new piece of a village layout would collide with, or null. Besides overlapping one, a piece that is
     * not a road ({@code spaced}) may not touch any piece but a road, diagonals included: no lamp post or wall is
     * glued to a house, and every piece has its apron of ground (see VillageTerrain). Roads run along anything.
     * Where the build has a support (a sky village's island), a piece the ground cannot carry collides too, so the
     * layout grows only over the island: roads stop short of the edge, houses stand on land.
     */
    public static StructureComponent collision(List<StructureComponent> pieces, StructureBoundingBox box,
        boolean spaced) {
        Build build = BUILD.get();
        if (build != null && build.support() != null
            && !build.support()
                .test(box))
            return NO_GROUND;
        for (StructureComponent piece : pieces) {
            StructureBoundingBox other = piece.getBoundingBox();
            if (other == null || other.maxY < box.minY || other.minY > box.maxY) continue;
            int gap = spaced && !(piece instanceof StructureVillagePieces.Road) ? 1 : 0;
            if (other.maxX >= box.minX - gap && other.minX <= box.maxX + gap
                && other.maxZ >= box.minZ - gap
                && other.minZ <= box.maxZ + gap) return piece;
        }
        return null;
    }

    /** Village Names' biome mapping for the village being built or populated, or null. */
    public static Map<String, ArrayList<String>> villageNamesMapping() {
        Build build = BUILD.get();
        if (build != null) return build.style()
            .villageNamesMapping();
        Population population = POPULATION.get();
        return population != null && population.plan != null ? population.plan.style()
            .villageNamesMapping() : null;
    }

    /** A structure start begins placing its pieces in the chunk being populated. */
    public static void enterStart(StructureStart start) {
        Population population = POPULATION.get();
        if (population != null) population.plan = population.placement
            .startingAt(start.func_143019_e(), start.func_143018_f(), population.world);
    }

    /**
     * Where the village being populated is named from (its start chunk's centre at its lowest pad), the same for every
     * piece and box; null when no Biomic village is being populated.
     */
    public static int[] anchor() {
        Population population = POPULATION.get();
        if (population == null || population.plan == null) return null;
        var plan = population.plan;
        return new int[] { (plan.chunkX() << 4) + 8, plan.minPad(), (plan.chunkZ() << 4) + 8 };
    }

    public static void exitStart() {
        Population population = POPULATION.get();
        if (population != null) population.plan = null;
    }

    /**
     * The top of the terrain a path of the village being populated is laid on at a column, as shaping left it; -1
     * where the village's layer has none (off an island's edge). {@link Integer#MIN_VALUE} when no Biomic village
     * is being populated. Also sets how deep Village Names' path search may look below it ({@link #pathFloor}).
     */
    public static int pathSurface(int x, int z) {
        Population population = POPULATION.get();
        if (population == null || population.plan == null) return Integer.MIN_VALUE;
        int surface = population.terrain.surface(population.world, x, z, population.plan.layer());
        population.pathFloor = surface < 0 ? Integer.MIN_VALUE : surface - 3;
        return surface;
    }

    /**
     * The lowest Y Village Names' path search may reach for the column last asked about ({@link #pathSurface}), or
     * the given default: its own floor is sea level 63, a hundred blocks into the rock at 512.
     */
    public static int pathFloor(int fallback) {
        Population population = POPULATION.get();
        return population == null || population.plan == null || population.pathFloor == Integer.MIN_VALUE ? fallback
            : population.pathFloor;
    }

    /**
     * The Y just above the ground at a column, in the layer of the village being populated, by vanilla's rules
     * ({@code getTopSolidOrLiquidBlock}) or Village Names' (which count water as ground); -1 where the layer has no
     * ground there (off an island's edge). {@link Integer#MIN_VALUE} when no Biomic village is being populated: the
     * caller's own answer stands.
     *
     * <p>
     * The answer comes from the village's shaped terrain, never from the world. Village Names measures a building's
     * median ground over its whole footprint, beyond the chunks being populated; reading the world there loaded and
     * generated those chunks mid-population (cascading worldgen), and the answer depended on what neighbours had
     * already placed (trees, other pieces).
     */
    public static int groundAbove(World world, int x, int z, boolean villageNamesRules) {
        Population population = POPULATION.get();
        if (population == null || population.plan == null) return Integer.MIN_VALUE;
        BiomicVillagePlacement.Plan plan = population.plan;
        int ceiling = Math.min(plan.maxPad() + BAND, world.getHeight() - 1), floor = Math.max(1, plan.minPad() - BAND);
        int surface = population.terrain.surface(world, x, z, plan.layer());
        if (surface < 0) return -1;
        if (villageNamesRules && plan.layer() == BiomicVillagePlacement.Layer.GROUND) surface = Math.max(
            surface,
            population.placement.terrain()
                .seaLevel());
        if (surface > ceiling) return ceiling + 1;
        return surface < floor ? -1 : surface + 1;
    }
}
