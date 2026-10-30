package industries.angelic.biomic.world.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.IntBinaryOperator;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraft.world.gen.structure.StructureVillagePieces;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.config.StructureConfig;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.terrain.BiomicTerrainSampler;
import industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler;
import industries.angelic.biomic.world.terrain.MacroTerrainSample;
import industries.angelic.biomic.world.terrain.MacroTerrainSampler;
import industries.angelic.biomic.world.terrain.OrigenProvinceSample;

/**
 * Where villages go and what they are, decided once per place and shared by placement and terrain shaping. Ground
 * villages are planned per village cell, sky villages per island cluster (clusters lie hundreds of blocks apart, at
 * most one per cluster region). Each searches its area for the flattest ground of its layer, then gets a standard
 * village where the ground allows one, a small one where only that fits, or none. Its look comes from its own 3D biome
 * ({@link VillageStyle}); its layout from vanilla's pieces or, when installed and enabled, Village Names'. Each house
 * records the ground its pad sits on (the average under it, as vanilla measures). A sky village keeps only the pieces
 * its island carries. See docs/structure-study.md.
 */
final class BiomicVillagePlacement {

    enum Size {
        STANDARD,
        SMALL
    }

    enum Layer {
        GROUND,
        SKY
    }

    /** A house (any piece that is not a road) and the top solid Y of the ground it stands on. */
    record House(StructureBoundingBox box, int ground) {}

    /**
     * A planned village: its start chunk, layer, size, look, houses, roads, the thin pieces standing at a road (lamp
     * posts, arches over it), overall bounds and the range of its pads.
     */
    record Plan(int chunkX, int chunkZ, Layer layer, Size size, VillageStyle style, List<House> houses,
        List<StructureBoundingBox> roads, List<StructureBoundingBox> accents, StructureBoundingBox bounds, int minPad,
        int maxPad) {}

    static final int MIN_HEIGHT_ABOVE_SEA = 2;
    static final double MAX_RIVER_STRENGTH = 0.38D;
    /** Candidate spots per cell side, flatness probe radius, standard and small check radii (blocks). */
    private static final int SEARCH = 6, PROBE_RADIUS = 48, STANDARD_RADIUS = 64, SMALL_RADIUS = 40;
    /**
     * The same for sky villages. Islands and the bands of ring islands are 50 to 150 blocks across: a 24-block grid
     * over the cluster finds every one wide enough for a village, and the checks fit their size (pieces the island
     * cannot carry are dropped afterwards).
     */
    private static final int SKY_STEP = 24, SKY_PROBE_RADIUS = 24, SKY_STANDARD_RADIUS = 48, SKY_SMALL_RADIUS = 24;
    /** Grid hits refined, and refined candidates confirmed on the exact island layer, per cluster. */
    private static final int SKY_REFINED_HITS = 6, SKY_CONFIRMED = 8;
    /** How far a sky village's start can lie from its cluster region's centre (jitter plus cluster reach). */
    private static final int SKY_REACH = 600;
    private static final int REGION = FloatingIslandDensitySampler.CLUSTER_REGION;
    /** Ground tolerance of the checks, ±blocks on 256's relief (Improved Village Placement uses 10). */
    private static final int TOLERANCE = 10;
    /**
     * How far from its well a village may start roads and houses; vanilla's is 112, though its house limits usually
     * stop it within about 50 (Village Names' do not). Pieces reach about 15 past their start, so these fit the
     * standard and small site checks.
     */
    static final int STANDARD_LAYOUT_RADIUS = 56, SMALL_LAYOUT_RADIUS = 20;
    /** How far beyond its footprint a house's pad averages the ground (VillageTerrain smooths roads as far). */
    static final int PAD_SPREAD = 6;
    /** Fewest houses a sky village keeps on its island. */
    private static final int MIN_SKY_HOUSES = 3;
    private static final long CHANCE_SALT = 0x5EED0F7111A6E5L, SKY_SALT = 0x51C1A5D0FF1A7E5L;
    private static final int PLAN_CACHE_SIZE = 2048;
    private static final int NO_GROUND = Integer.MIN_VALUE;

    static final Plan NONE = new Plan(0, 0, null, null, null, List.of(), List.of(), List.of(), null, 0, 0);

    private final WorldgenContext context;
    private final long worldSeed;
    private final int seaLevel, tolerance;
    private final MacroTerrainSampler macroSampler;
    private final StructureTerrain terrain;
    private final StructureConfig.Villages settings = StructureConfig.get().villages;
    private final LinkedHashMap<Long, FutureTask<Plan>> plans = new LinkedHashMap<>(64, .75F, true);

    BiomicVillagePlacement(WorldgenContext context, MacroTerrainSampler macroSampler,
        BiomicTerrainSampler terrainSampler) {
        this(context, macroSampler, StructureTerrain.legacy(context, terrainSampler));
    }

    BiomicVillagePlacement(WorldgenContext context, MacroTerrainSampler macroSampler, StructureTerrain terrain) {
        if (context == null || macroSampler == null || terrain == null) {
            throw new IllegalArgumentException("Worldgen context and terrain samplers cannot be null");
        }
        this.context = context;
        this.worldSeed = context.getSettings()
            .getWorldSeed();
        this.seaLevel = terrain.seaLevel();
        this.tolerance = (int) Math.round(TOLERANCE * terrain.reliefScale());
        this.macroSampler = macroSampler;
        this.terrain = terrain;
    }

    StructureTerrain terrain() {
        return terrain;
    }

    /** The plan whose village starts in this chunk, or null. */
    Plan startingAt(int chunkX, int chunkZ, World world) {
        Plan ground = groundStartingAt(chunkX, chunkZ, world);
        if (ground != null || !settings.skyVillages()) return ground;
        int x = chunkX << 4, z = chunkZ << 4;
        for (int rx = Math.floorDiv(x - SKY_REACH, REGION); rx <= Math.floorDiv(x + SKY_REACH, REGION); rx++)
            for (int rz = Math.floorDiv(z - SKY_REACH, REGION); rz <= Math.floorDiv(z + SKY_REACH, REGION); rz++) {
                int[] range = startRange(rx, rz, Layer.SKY);
                if (range == null || chunkX < range[0] || chunkZ < range[1] || chunkX > range[2] || chunkZ > range[3])
                    continue;
                Plan plan = plan(rx, rz, Layer.SKY, world);
                if (plan != NONE && plan.chunkX == chunkX && plan.chunkZ == chunkZ) return plan;
            }
        return null;
    }

    private Plan groundStartingAt(int chunkX, int chunkZ, World world) {
        int cellX = Math.floorDiv(chunkX, settings.spacingChunks()),
            cellZ = Math.floorDiv(chunkZ, settings.spacingChunks());
        int[] range = startRange(cellX, cellZ, Layer.GROUND);
        if (chunkX < range[0] || chunkZ < range[1] || chunkX > range[2] || chunkZ > range[3]) return null;
        Plan plan = plan(cellX, cellZ, Layer.GROUND, world);
        return plan != NONE && plan.chunkX == chunkX && plan.chunkZ == chunkZ ? plan : null;
    }

    /** The village plans, of both layers, whose bounds come within {@code margin} blocks of a chunk. */
    List<Plan> near(int chunkX, int chunkZ, int margin, World world) {
        int minX = chunkX << 4, minZ = chunkZ << 4;
        List<Plan> result = new ArrayList<>(2);
        for (int[] cell : cellsNear(chunkX, chunkZ, margin)) {
            Plan plan = plan(cell[1], cell[2], Layer.values()[cell[0]], world);
            if (plan == NONE) continue;
            StructureBoundingBox b = plan.bounds;
            if (b.maxX + margin >= minX && b.minX - margin <= minX + 15
                && b.maxZ + margin >= minZ
                && b.minZ - margin <= minZ + 15) result.add(plan);
        }
        return result;
    }

    /**
     * The village cells, {layer ordinal, x, z}, whose villages can come within {@code margin} blocks of a chunk from
     * where they can start; their plans may still find no village or one farther away.
     */
    List<int[]> cellsNear(int chunkX, int chunkZ, int margin) {
        List<int[]> result = candidateCells(chunkX, chunkZ, margin);
        result.removeIf(cell -> !mayReach(cell, chunkX, chunkZ, margin));
        return result;
    }

    /**
     * The cells near a chunk whose villages {@link #mayReach} it, before that check: in the sky the check places the
     * cluster, which samples the ground under it, so callers in a hurry hand it to a worker.
     */
    List<int[]> candidateCells(int chunkX, int chunkZ, int margin) {
        int minX = chunkX << 4, minZ = chunkZ << 4, reach = reach(margin);
        List<int[]> result = new ArrayList<>(8);
        for (Layer layer : Layer.values()) {
            if (layer == Layer.SKY && !settings.skyVillages()) continue;
            int size = layer == Layer.SKY ? REGION : settings.spacingChunks() << 4;
            int extra = layer == Layer.SKY ? SKY_REACH : 0;
            for (int cellX = Math.floorDiv(minX - reach - extra, size); cellX
                <= Math.floorDiv(minX + 15 + reach + extra, size); cellX++)
                for (int cellZ = Math.floorDiv(minZ - reach - extra, size); cellZ
                    <= Math.floorDiv(minZ + 15 + reach + extra, size); cellZ++)
                    result.add(new int[] { layer.ordinal(), cellX, cellZ });
        }
        return result;
    }

    /** Whether a cell's village can come within {@code margin} blocks of a chunk from where it can start. */
    boolean mayReach(int[] cell, int chunkX, int chunkZ, int margin) {
        int minX = chunkX << 4, minZ = chunkZ << 4, reach = reach(margin);
        int[] range = startRange(cell[1], cell[2], Layer.values()[cell[0]]);
        return range != null && minX + 15 + reach >= range[0] << 4
            && minX - reach <= (range[2] << 4) + 15
            && minZ + 15 + reach >= range[1] << 4
            && minZ - reach <= (range[3] << 4) + 15;
    }

    /** A road may start STANDARD_LAYOUT_RADIUS from the well and run 35 blocks further. */
    private static int reach(int margin) {
        return margin + STANDARD_LAYOUT_RADIUS + 48;
    }

    /**
     * The chunks a cell's village can start in, {min x, min z, max x, max z}, or null where it has none: the cell's
     * search range on the ground; its cluster's disc, with the refinement around it, in the sky. Known without
     * planning, so a chunk plans only the villages that can reach it.
     */
    private int[] startRange(int cellX, int cellZ, Layer layer) {
        if (layer == Layer.GROUND) {
            int span = (settings.spacingChunks() - settings.separationChunks()) << 4;
            int originX = cellX * settings.spacingChunks() << 4, originZ = cellZ * settings.spacingChunks() << 4;
            return new int[] { originX + 8 >> 4, originZ + 8 >> 4, originX + span - 8 >> 4, originZ + span - 8 >> 4 };
        }
        FloatingIslandDensitySampler.Cluster cluster = terrain.skyCluster(cellX, cellZ);
        if (cluster == null) return null;
        int r = cluster.radius() + SKY_STEP / 2;
        return new int[] { cluster.centerX() - r >> 4, cluster.centerZ() - r >> 4, cluster.centerX() + r >> 4,
            cluster.centerZ() + r >> 4 };
    }

    /** Builds a planned village's start exactly as planning saw it. */
    StructureStart build(World world, Random random, Plan plan) {
        return build(world, random, plan.chunkX, plan.chunkZ, plan.size, plan.layer, plan.style);
    }

    private StructureStart build(World world, Random random, int chunkX, int chunkZ, Size size, Layer layer,
        VillageStyle style) {
        boolean small = size == Size.SMALL, villageNames = VillageNamesLayouts.active(settings);
        StructureStart start = VillageContext.build(
            style,
            villageNames,
            layer == Layer.SKY ? this::supported : null,
            () -> VillageLayoutRadius.with(
                small ? SMALL_LAYOUT_RADIUS : STANDARD_LAYOUT_RADIUS,
                () -> villageNames ? VillageNamesLayouts.start(world, random, chunkX, chunkZ, small)
                    : new MapGenVillage.Start(world, random, chunkX, chunkZ, 0)));
        keepApart(start);
        if (layer == Layer.SKY) {
            keepOnIsland(start);
            trimRoads(start);
        }
        return start;
    }

    /**
     * Pieces other than roads keep a block apart (see VillageContext#collision). Village Names checks the room for a
     * road accent before moving it onto the street, so one can still land on a house: it goes.
     */
    private static void keepApart(StructureStart start) {
        List<StructureBoundingBox> kept = new ArrayList<>();
        for (Iterator<?> pieces = start.getComponents()
            .iterator(); pieces.hasNext();) {
            StructureComponent piece = (StructureComponent) pieces.next();
            if (piece instanceof StructureVillagePieces.Road) continue;
            StructureBoundingBox box = piece.getBoundingBox();
            boolean clear = true;
            for (var other : kept) clear &= !touches(box, other);
            if (clear) kept.add(box);
            else pieces.remove();
        }
    }

    /**
     * A sky village keeps the pieces its island carries (the layout already grew only over the island; this catches
     * what a layout places without asking). The well always stays.
     */
    private void keepOnIsland(StructureStart start) {
        Iterator<?> pieces = start.getComponents()
            .iterator();
        if (pieces.hasNext()) pieces.next();
        while (pieces.hasNext()) {
            StructureComponent piece = (StructureComponent) pieces.next();
            if (!(piece instanceof StructureVillagePieces.Road) && !supported(piece.getBoundingBox())) pieces.remove();
        }
    }

    /**
     * A sky village's roads serve its houses. A piece beside a road's length serves it; one touching an end only joins
     * it there (the well a road leaves from, the road it continues). A road serving nothing that joins at most one
     * other
     * piece goes, and the rest end at their last house or junction: on an island, a road past its last house only runs
     * to the cliff. Repeats until nothing changes, since a removed road can leave another one serving nothing.
     */
    static void trimRoads(StructureStart start) {
        List<StructureComponent> pieces = start.getComponents();
        List<StructureComponent> roads = new ArrayList<>(), others = new ArrayList<>();
        for (StructureComponent piece : pieces)
            (piece instanceof StructureVillagePieces.Road && piece != pieces.get(0) ? roads : others).add(piece);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Iterator<StructureComponent> it = roads.iterator(); it.hasNext();) {
                StructureComponent road = it.next();
                StructureBoundingBox box = road.getBoundingBox();
                boolean alongX = box.getXSize() >= box.getZSize();
                int min = alongX ? box.minX : box.minZ, max = alongX ? box.maxX : box.maxZ;
                int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE, served = 0, joined = 0;
                for (StructureComponent other : pieces) {
                    StructureBoundingBox o = other.getBoundingBox();
                    if (other == road || !beside(box, o)) continue;
                    int oMin = alongX ? o.minX : o.minZ, oMax = alongX ? o.maxX : o.maxZ;
                    int from = Math.max(min, oMin), to = Math.min(max, oMax);
                    if (from > to) {
                        from = to = oMin > max ? max : min;
                        joined++;
                    } else if (roads.contains(other)) joined++;
                    else served++;
                    lo = Math.min(lo, from);
                    hi = Math.max(hi, to);
                }
                if (served == 0 && joined <= 1) {
                    it.remove();
                    pieces.remove(road);
                    changed = true;
                    continue;
                }
                if (lo > min || hi < max) {
                    if (alongX) {
                        box.minX = lo;
                        box.maxX = hi;
                    } else {
                        box.minZ = lo;
                        box.maxZ = hi;
                    }
                    changed = true;
                }
            }
        }
    }

    /** Whether two boxes overlap or touch side by side (heights aside: roads keep a nominal height). */
    private static boolean beside(StructureBoundingBox a, StructureBoundingBox b) {
        return a.maxX >= b.minX - 1 && a.minX <= b.maxX + 1 && a.maxZ >= b.minZ - 1 && a.minZ <= b.maxZ + 1;
    }

    private boolean supported(StructureBoundingBox box) {
        for (int x = box.minX; x <= box.maxX; x++)
            for (int z = box.minZ; z <= box.maxZ; z++) if (terrain.skySpan(x, z) < 0) return false;
        return true;
    }

    Plan plan(int cellX, int cellZ, Layer layer, World world) {
        long key = ((long) cellX << 32 ^ cellZ & 0xFFFF_FFFFL) * 2 + layer.ordinal();
        FutureTask<Plan> task;
        boolean owner = false;
        synchronized (plans) {
            task = plans.get(key);
            if (task == null) {
                plans.put(key, task = new FutureTask<>(() -> compute(cellX, cellZ, layer, world)));
                owner = true;
                if (plans.size() > PLAN_CACHE_SIZE) {
                    var eldest = plans.keySet()
                        .iterator();
                    eldest.next();
                    eldest.remove();
                }
            }
        }
        if (owner) task.run();
        try {
            return task.get();
        } catch (ExecutionException failure) {
            synchronized (plans) {
                plans.remove(key, task);
            }
            throw new IllegalStateException(
                "Village planning failed for cell " + cellX + "," + cellZ + " " + layer,
                failure.getCause());
        } catch (InterruptedException interrupted) {
            Thread.currentThread()
                .interrupt();
            throw new IllegalStateException("Interrupted while planning a village", interrupted);
        }
    }

    /** Plans a ground village cell, or a sky village cluster region. */
    private Plan compute(int cellX, int cellZ, Layer layer, World world) {
        if (!settings.enabled() || layer == Layer.SKY && !settings.skyVillages()) return NONE;
        boolean sky = layer == Layer.SKY;
        FloatingIslandDensitySampler.Cluster cluster = sky ? terrain.skyCluster(cellX, cellZ) : null;
        if (sky && cluster == null) return NONE;
        long cellSeed = WorldgenContext
            .mix64(worldSeed ^ cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL ^ (sky ? SKY_SALT : 0));
        if ((WorldgenContext.mix64(cellSeed ^ CHANCE_SALT) >>> 11) * 0x1.0p-53 >= settings.chance()) return NONE;
        IntBinaryOperator height = sky ? (SkyHeight) this::skyHeight : this::groundHeight;
        int bestX, bestZ;
        Size size;
        if (sky) {
            int[] site = skySite(cluster, (SkyHeight) (x, z) -> {
                int span = terrain.skyColumn(x, z);
                return span < 0 ? NO_GROUND : StructureTerrain.spanTop(span);
            });
            if (site == null) return NONE;
            bestX = site[0];
            bestZ = site[1];
            size = Size.values()[site[2]];
        } else {
            // The flattest land among SEARCH x SEARCH spots spread over the cell's range.
            int best = Integer.MAX_VALUE;
            bestX = bestZ = 0;
            int span = (settings.spacingChunks() - settings.separationChunks()) << 4;
            int originX = cellX * settings.spacingChunks() << 4, originZ = cellZ * settings.spacingChunks() << 4;
            for (int i = 0; i < SEARCH; i++) for (int j = 0; j < SEARCH; j++) {
                int x = originX + 8 + i * (span - 16) / (SEARCH - 1), z = originZ + 8 + j * (span - 16) / (SEARCH - 1);
                if (!hasSuitableCenter(x, z)) continue;
                int relief = relief(height, x, z, PROBE_RADIUS, 16, best);
                if (relief < best) {
                    best = relief;
                    bestX = x;
                    bestZ = z;
                }
            }
            if (best == Integer.MAX_VALUE) return NONE;
            size = size(height, bestX, bestZ, false);
            if (size == null) return NONE;
        }
        int chunkX = bestX >> 4, chunkZ = bestZ >> 4;
        // One start per chunk: a sky village never shares its chunk with a ground village.
        if (sky && groundStartingAt(chunkX, chunkZ, world) != null) return NONE;
        int startY = height.applyAsInt(bestX, bestZ);
        VillageStyle style = VillageStyle
            .at(context, terrain, biome(layer, bestX, startY, bestZ, world), bestX, startY, bestZ);
        StructureStart start = build(world, structureRandom(chunkX, chunkZ), chunkX, chunkZ, size, layer, style);
        if (!start.isSizeableStructure()) return NONE;
        List<House> houses = new ArrayList<>();
        List<StructureBoundingBox> roads = new ArrayList<>(), thin = new ArrayList<>(), accents = new ArrayList<>();
        int minPad = startY, maxPad = startY;
        for (Object component : start.getComponents()) {
            StructureBoundingBox box = ((StructureComponent) component).getBoundingBox();
            if (component instanceof StructureVillagePieces.Road) {
                roads.add(new StructureBoundingBox(box));
                continue;
            }
            // Lamp posts and other single-column decor follow the ground as it is.
            if (box.getXSize() < 3 || box.getZSize() < 3) {
                thin.add(new StructureBoundingBox(box));
                continue;
            }
            int pad = averageHeight(height, box);
            if (pad == NO_GROUND) continue;
            houses.add(new House(box, pad));
            minPad = Math.min(minPad, pad);
            maxPad = Math.max(maxPad, pad);
        }
        if (sky && (houses.size() < MIN_SKY_HOUSES || !supported(
            ((StructureComponent) start.getComponents()
                .get(0)).getBoundingBox())))
            return NONE;
        for (var piece : thin) for (var road : roads) if (touches(piece, road)) {
            accents.add(piece);
            break;
        }
        return new Plan(
            chunkX,
            chunkZ,
            layer,
            size,
            style,
            List.copyOf(houses),
            List.copyOf(roads),
            List.copyOf(accents),
            start.getBoundingBox(),
            minPad,
            maxPad);
    }

    /** The size the ground at a site allows, or null. */
    private Size size(IntBinaryOperator height, int x, int z, boolean sky) {
        if (flat(height, x, z, sky ? SKY_STANDARD_RADIUS : STANDARD_RADIUS, 16)) return Size.STANDARD;
        return settings.smallVillages() && flat(height, x, z, sky ? SKY_SMALL_RADIUS : SMALL_RADIUS, sky ? 12 : 20)
            ? Size.SMALL
            : null;
    }

    /**
     * The best village site in an island cluster: {x, z, size ordinal}, or null. Searched on cheap estimates of the
     * island tops (a 24-block grid scored by how much of a village disc stays on the island, then refined around the
     * best hits), and confirmed on the exact island layer.
     */
    private int[] skySite(FloatingIslandDensitySampler.Cluster cluster, IntBinaryOperator exact) {
        java.util.HashMap<Long, Integer> known = new java.util.HashMap<>();
        IntBinaryOperator estimate = (SkyHeight) (x, z) -> known
            .computeIfAbsent((long) x << 32 ^ z & 0xFFFF_FFFFL, key -> {
                int top = terrain.skyTopEstimate(x, z);
                return top < 0 ? NO_GROUND : top;
            });
        List<int[]> hits = new ArrayList<>(); // {off-island probe points, relief, x, z}
        int r = cluster.radius();
        for (int dx = -r; dx <= r; dx += SKY_STEP) for (int dz = -r; dz <= r; dz += SKY_STEP) {
            if (dx * dx + dz * dz > r * r) continue;
            int x = cluster.centerX() + dx, z = cluster.centerZ() + dz;
            if (estimate.applyAsInt(x, z) != NO_GROUND) hits.add(probe(estimate, x, z));
        }
        hits.sort(
            java.util.Comparator.<int[]>comparingInt(hit -> hit[0])
                .thenComparingInt(hit -> hit[1]));
        // Narrow islands (a ring's band) fit a village only near their middle: look around the best hits.
        List<int[]> candidates = new ArrayList<>();
        for (int[] hit : hits.subList(0, Math.min(SKY_REFINED_HITS, hits.size())))
            for (int ox = -SKY_STEP / 2; ox <= SKY_STEP / 2; ox += 4)
                for (int oz = -SKY_STEP / 2; oz <= SKY_STEP / 2; oz += 4) {
                    int[] candidate = probe(estimate, hit[2] + ox, hit[3] + oz);
                    if (candidate[0] == 0) candidates.add(candidate);
                }
        candidates.sort(java.util.Comparator.comparingInt(candidate -> candidate[1]));
        for (int[] candidate : candidates.subList(0, Math.min(SKY_CONFIRMED, candidates.size()))) {
            Size size = size(exact, candidate[2], candidate[3], true);
            if (size != null) return new int[] { candidate[2], candidate[3], size.ordinal() };
        }
        return null;
    }

    /** {points of a village disc off the island, relief of those on it, x, z}. */
    private static int[] probe(IntBinaryOperator height, int cx, int cz) {
        int missing = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int dx = -SKY_PROBE_RADIUS; dx <= SKY_PROBE_RADIUS; dx += 12)
            for (int dz = -SKY_PROBE_RADIUS; dz <= SKY_PROBE_RADIUS; dz += 12) {
                if (dx * dx + dz * dz > SKY_PROBE_RADIUS * SKY_PROBE_RADIUS) continue;
                int h = height.applyAsInt(cx + dx, cz + dz);
                if (h == NO_GROUND) {
                    missing++;
                    continue;
                }
                min = Math.min(min, h);
                max = Math.max(max, h);
            }
        return new int[] { missing, max < min ? Integer.MAX_VALUE : max - min, cx, cz };
    }

    /** Whether two boxes overlap or share a side (horizontally). */
    static boolean touches(StructureBoundingBox a, StructureBoundingBox b) {
        return a.maxX >= b.minX - 1 && a.minX <= b.maxX + 1 && a.maxZ >= b.minZ - 1 && a.minZ <= b.maxZ + 1;
    }

    /** The 3D biome at a village's start: its island's habitat in the sky, the surface identity on the ground. */
    private BiomeGenBase biome(Layer layer, int x, int y, int z, World world) {
        BiomeGenBase surface = world.getWorldChunkManager()
            .getBiomeGenAt(x, z);
        if (layer == Layer.GROUND) return surface;
        // The rasterized top can sit a block above where the island's exact field turns positive.
        for (int below = 0; below < 4; below++) {
            BiomeGenBase sky = terrain.skyBiome(x, y - below, z);
            if (sky != null) return sky;
        }
        return surface;
    }

    /** The random state vanilla's structure generator holds when it builds the start in this chunk. */
    Random structureRandom(int chunkX, int chunkZ) {
        Random random = new Random(worldSeed);
        long xMultiplier = random.nextLong();
        long zMultiplier = random.nextLong();
        random.setSeed(chunkX * xMultiplier ^ chunkZ * zMultiplier ^ worldSeed);
        random.nextInt();
        return random;
    }

    boolean hasSuitableCenter(int x, int z) {
        return resolvedProfile(macroSampler.sample(x, z)).getKind() == OrigenBiomeProfile.Kind.LAND
            && terrain.riverStrength(x, z) <= MAX_RIVER_STRENGTH
            && terrain.groundHeight(x, z) >= seaLevel + MIN_HEIGHT_ABOVE_SEA;
    }

    /** The ground village layer: dry land only. */
    private int groundHeight(int x, int z) {
        int height = terrain.groundHeight(x, z);
        return height <= seaLevel ? NO_GROUND : height;
    }

    /** Island tops, checked in discs. */
    private interface SkyHeight extends IntBinaryOperator {
    }

    /** The sky village layer: the top of the island in the column, if any. */
    private int skyHeight(int x, int z) {
        int span = terrain.skySpan(x, z);
        return span < 0 ? NO_GROUND : StructureTerrain.spanTop(span);
    }

    /**
     * Highest minus lowest ground on a grid within the radius; MAX_VALUE where any point has none. Stops at
     * {@code limit} or more, which the caller rejects anyway: the corners and middles go first, so rough ground is
     * known after a few samples.
     */
    private static int relief(IntBinaryOperator height, int cx, int cz, int radius, int step, int limit) {
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int pass = 0; pass < 2; pass++)
            for (int dx = -radius; dx <= radius; dx += step) for (int dz = -radius; dz <= radius; dz += step) {
                boolean coarse = (dx == -radius || dx == 0 || dx == radius)
                    && (dz == -radius || dz == 0 || dz == radius);
                if (coarse != (pass == 0)) continue;
                int h = height.applyAsInt(cx + dx, cz + dz);
                if (h == NO_GROUND) return Integer.MAX_VALUE;
                min = Math.min(min, h);
                max = Math.max(max, h);
                if (max - min >= limit) return max - min;
            }
        return max - min;
    }

    /**
     * Improved Village Placement's check: every grid point within ±tolerance of the start's ground, none missing. Sky
     * checks probe a disc rather than a square: a square on a diagonal ring band would need the band 1.4 times as wide.
     */
    private boolean flat(IntBinaryOperator height, int cx, int cz, int radius, int step) {
        boolean round = height instanceof SkyHeight;
        // Island tops are domed; shaping levels each house's pad.
        int tolerance = round ? this.tolerance * 3 / 2 : this.tolerance;
        int start = height.applyAsInt(cx, cz);
        for (int dx = -radius; dx <= radius; dx += step) for (int dz = -radius; dz <= radius; dz += step) {
            if (round && dx * dx + dz * dz > radius * radius) continue;
            int h = height.applyAsInt(cx + dx, cz + dz);
            if (h == NO_GROUND || Math.abs(h - start) > tolerance) return false;
        }
        return true;
    }

    /**
     * A house's pad: the mean ground over its footprint widened by {@link #PAD_SPREAD} on every side, on every other
     * column. Neighbouring houses average over overlapping ground and so sit close in height, and the ground between
     * them needs no cliff; shaping sets the footprint to the pad, so the house measures exactly this value.
     */
    private static int averageHeight(IntBinaryOperator height, StructureBoundingBox box) {
        long sum = 0;
        int count = 0;
        for (int x = box.minX - PAD_SPREAD; x <= box.maxX + PAD_SPREAD; x += 2)
            for (int z = box.minZ - PAD_SPREAD; z <= box.maxZ + PAD_SPREAD; z += 2) {
                int h = height.applyAsInt(x, z);
                if (h == NO_GROUND) continue;
                sum += h;
                count++;
            }
        return count == 0 ? NO_GROUND : (int) Math.round((double) sum / count);
    }

    private static OrigenBiomeProfile resolvedProfile(MacroTerrainSample macro) {
        OrigenProvinceSample province = macro.getOrigenProvince();
        return province.getPrimary();
    }
}
