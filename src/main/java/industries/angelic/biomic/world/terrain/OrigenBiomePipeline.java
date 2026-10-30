package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.algorithms.sampler.DomainWarpedSampler;
import com.dfsek.seismic.algorithms.sampler.noise.cellular.CellularStyleSampler.CellularReturnType;
import com.dfsek.seismic.algorithms.sampler.noise.random.WhiteNoiseSampler;
import com.dfsek.seismic.type.DistanceFunction;
import com.dfsek.seismic.type.sampler.Sampler;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Functional Java port of Origen's Terra 6.4.2 {@code biome-provider-pipeline-v2} graph.
 *
 * <p>
 * The upstream graph has one fractal expansion, so it can be evaluated lazily without materializing Terra's
 * 129-by-129 working arrays: point stages run on the eight-block source lattice, expansion fills the four-block
 * lattice, and the two smoothing passes only need their four immediate neighbors.
 * </p>
 */
final class OrigenBiomePipeline {

    /** Biomic: 480-block biome cells (Origen 350), biomes about 390 blocks across (vanilla 1.20: 420). */
    private static final double CELL_FREQUENCY = 1.0D / 480.0D;
    /** Biomic: patchwork splits at 1.4 times the cell frequency (Origen 2.0), so patchwork biomes are not slivers. */
    private static final double PATCHWORK_FREQUENCY = 1.4D;
    private static final double CELL_JITTER = 0.9D;
    private static final int PIPELINE_RESOLUTION = 4;
    /** How far a sinkhole keeps from coast and sea, and the grid offsets probed for them (rings at 1/2 and full). */
    private static final int SINKHOLE_SHORE_GAP = 32;
    private static final int[][] SHORE_PROBES = shoreProbes();

    private static int[][] shoreProbes() {
        java.util.List<int[]> probes = new java.util.ArrayList<>();
        for (int radius : new int[] { SINKHOLE_SHORE_GAP / 2, SINKHOLE_SHORE_GAP }) {
            int steps = radius / 2;
            for (int i = 0; i < steps; i++) {
                double angle = 2 * Math.PI * i / steps;
                probes.add(
                    new int[] { (int) Math.round(Math.cos(angle) * radius / PIPELINE_RESOLUTION),
                        (int) Math.round(Math.sin(angle) * radius / PIPELINE_RESOLUTION) });
            }
        }
        return probes.toArray(new int[0][]);
    }

    private static final int FROSTY_MARSH_POLISH_RADIUS = 3;
    private static final int FROSTY_MARSH_POLISH_PAIR_MIN = 40;
    private static final int CACHE_SIZE = 262_144;
    private static final int CACHE_LOCK_STRIPES = 64;

    private static final WeightedTable SOURCE = table(Placeholder.OCEAN, 1, Placeholder.LAND, 1);
    private static final WeightedTable CANYON_GATE = table(Placeholder.SELF, 16, Placeholder.CANYON, 7);
    private static final WeightedTable OCEAN_COAST_KIND = table(
        Placeholder.SELF,
        1,
        Placeholder.OCEAN_COAST_SMALL,
        2,
        Placeholder.OCEAN_COAST_WIDE,
        1);
    private static final WeightedTable SMALL_COAST_COLD = table(
        OrigenBiomeProfile.SHALE_BEACH,
        1,
        OrigenBiomeProfile.BEACH,
        3);
    // Biomic: mangroves grow on warm coasts only (Origen put them on medium ones, next to snowy land).
    private static final WeightedTable SMALL_COAST_MEDIUM = table(
        OrigenBiomeProfile.SHALE_BEACH,
        2,
        OrigenBiomeProfile.PALM_BEACH,
        1,
        OrigenBiomeProfile.BEACH,
        2);
    private static final WeightedTable SMALL_COAST_WARM = table(
        OrigenBiomeProfile.SHALE_BEACH,
        1,
        OrigenBiomeProfile.PALM_BEACH,
        4,
        OrigenBiomeProfile.BEACH,
        3,
        OrigenBiomeProfile.MANGROVE_SWAMP,
        2);
    private static final WeightedTable WIDE_COAST_COLD = table(
        OrigenBiomeProfile.BEACH,
        2,
        OrigenBiomeProfile.MARINE_MONOLITHS,
        1);
    private static final WeightedTable WIDE_COAST_MEDIUM = table(
        OrigenBiomeProfile.TEMPERATE_SEA_ARCHES,
        2,
        OrigenBiomeProfile.PALM_BEACH,
        2,
        OrigenBiomeProfile.MARINE_MONOLITHS,
        1);
    private static final WeightedTable WIDE_COAST_WARM = WIDE_COAST_MEDIUM;
    private static final WeightedTable OCEAN_COLD = table(
        OrigenBiomeProfile.OCEAN,
        2,
        OrigenBiomeProfile.ICEBERG_OCEAN,
        2,
        OrigenBiomeProfile.FROZEN_OCEAN,
        3,
        OrigenBiomeProfile.ABYSSAL_ALLEYS,
        2,
        OrigenBiomeProfile.ARCTIC_ARCHES,
        1);
    private static final WeightedTable OCEAN_MEDIUM = table(
        OrigenBiomeProfile.OCEAN,
        3,
        OrigenBiomeProfile.DEEP_OCEAN,
        2,
        OrigenBiomeProfile.ABYSSAL_ALLEYS,
        2,
        OrigenBiomeProfile.STONEGATE_SEAS,
        1);
    private static final WeightedTable OCEAN_WARM = table(
        OrigenBiomeProfile.OCEAN,
        2,
        OrigenBiomeProfile.DEEP_OCEAN,
        2,
        OrigenBiomeProfile.TROPICAL_OCEAN,
        3,
        OrigenBiomeProfile.TROPICAL_DEEP_OCEAN,
        2,
        OrigenBiomeProfile.ABYSSAL_ALLEYS,
        2,
        OrigenBiomeProfile.CORAL_OCEAN,
        4,
        OrigenBiomeProfile.STONEGATE_SEAS,
        1);
    private static final WeightedTable LAND_TEMPERATURE = table(
        Placeholder.COLD,
        4,
        Placeholder.MEDIUM,
        4,
        Placeholder.WARM,
        4);
    private static final WeightedTable DIRECT_COLD = table(
        OrigenBiomeProfile.FROSTY_FINGERS,
        2,
        OrigenBiomeProfile.COLD_STEPPE,
        1,
        OrigenBiomeProfile.SNOWY_MEADOW,
        1,
        OrigenBiomeProfile.SNOWY_MOUNTAINS,
        1,
        OrigenBiomeProfile.MOUNTAIN_MIRRORS,
        2,
        OrigenBiomeProfile.ALPINE_ASCENDANCY,
        2,
        OrigenBiomeProfile.BARE_BOULDERFIELDS,
        1,
        OrigenBiomeProfile.TUNDRA_TRACKS,
        2,
        OrigenBiomeProfile.FROZEN_FUNGI,
        2,
        OrigenBiomeProfile.SNOWY_SPIRES,
        3,
        OrigenBiomeProfile.TALL_TIMBERLAND,
        1,
        Placeholder.PATCHWORK_COLD,
        15);
    private static final WeightedTable DIRECT_MEDIUM = table(
        OrigenBiomeProfile.FOSSILIZED_FENLANDS,
        3,
        OrigenBiomeProfile.MURKY_MARSHLANDS,
        2,
        OrigenBiomeProfile.VERTICAL_VISTAS,
        2,
        OrigenBiomeProfile.VERDANT_VALLEYS,
        2,
        OrigenBiomeProfile.BLACK_FOREST,
        1,
        OrigenBiomeProfile.FLOWERING_FOREST,
        1,
        Placeholder.PILLOW_PLAINS,
        2,
        Placeholder.SECLUDED_VALLEYS,
        1,
        OrigenBiomeProfile.SAKURA_STREAMS,
        1,
        OrigenBiomeProfile.GLOOMY_GORGE,
        2,
        OrigenBiomeProfile.ROCKY_REFUGE,
        2,
        OrigenBiomeProfile.WHITE_WALLOWS,
        2,
        Placeholder.PLATEAU,
        2,
        Placeholder.PATCHWORK_MEDIUM,
        18);
    private static final WeightedTable DIRECT_WARM = table(
        OrigenBiomeProfile.MESA_MONUMENTS,
        2,
        OrigenBiomeProfile.LUSH_LOOPS,
        2,
        OrigenBiomeProfile.CARVING_CREAKS,
        2,
        OrigenBiomeProfile.ARID_ARBORETUM,
        2,
        OrigenBiomeProfile.SALT_FLATS,
        1,
        OrigenBiomeProfile.DESERT_SPIKES_GOLD,
        1,
        OrigenBiomeProfile.OVERGROWN_CLIFFS,
        2,
        OrigenBiomeProfile.CANOPY_CASCADES,
        2,
        OrigenBiomeProfile.WATERY_WILDS,
        2,
        Placeholder.DESERT,
        2,
        OrigenBiomeProfile.BAMBOO_BASIN,
        2,
        OrigenBiomeProfile.BADLANDS_BALCONIES,
        2,
        OrigenBiomeProfile.SCARLET_SANCTUARY,
        2,
        Placeholder.PATCHWORK_WARM,
        18);
    private static final WeightedTable PATCHWORK_COLD = table(
        OrigenBiomeProfile.FROSTY_FINGERS,
        2,
        OrigenBiomeProfile.COLD_STEPPE,
        3,
        OrigenBiomeProfile.SNOWY_MEADOW,
        3,
        OrigenBiomeProfile.SNOWY_MOUNTAINS,
        3,
        OrigenBiomeProfile.TUNDRA_TRACKS,
        3,
        OrigenBiomeProfile.SNOWY_SPIRES,
        2,
        OrigenBiomeProfile.TALL_TIMBERLAND,
        3);
    private static final WeightedTable PATCHWORK_MEDIUM = table(
        OrigenBiomeProfile.VERTICAL_VISTAS,
        2,
        OrigenBiomeProfile.BLACK_FOREST,
        3,
        OrigenBiomeProfile.SECLUDED_VALLEY_OUTER,
        3,
        OrigenBiomeProfile.MURKY_MARSHLANDS,
        3,
        OrigenBiomeProfile.FOSSILIZED_FENLANDS,
        3,
        OrigenBiomeProfile.VERDANT_VALLEYS,
        3,
        OrigenBiomeProfile.FOLIAGE_FORTRESS_OUTER,
        3,
        OrigenBiomeProfile.GLOOMY_GORGE,
        3,
        OrigenBiomeProfile.WHITE_WALLOWS,
        2);
    private static final WeightedTable PATCHWORK_WARM = table(
        OrigenBiomeProfile.SALT_FLATS,
        2,
        OrigenBiomeProfile.MESA_MONUMENTS,
        3,
        OrigenBiomeProfile.DESERT_SPIKES_GOLD,
        3,
        OrigenBiomeProfile.OVERGROWN_CLIFFS,
        3,
        OrigenBiomeProfile.WATERY_WILDS,
        3,
        OrigenBiomeProfile.DESERT,
        2,
        OrigenBiomeProfile.BADLANDS_BALCONIES,
        3);
    private static final WeightedTable PILLOW_BORDERS = table(
        OrigenBiomeProfile.PILLOW_PLAINS_INNER,
        88,
        OrigenBiomeProfile.PILLOW_PLAINS_MIDDLE,
        12,
        OrigenBiomeProfile.PILLOW_PLAINS_OUTER,
        100);
    private static final WeightedTable SECLUDED_BORDERS = table(
        OrigenBiomeProfile.SECLUDED_VALLEY_OUTER,
        1,
        OrigenBiomeProfile.SECLUDED_VALLEY,
        1);
    private static final WeightedTable DESERT_BORDERS = table(
        OrigenBiomeProfile.DESERT,
        1,
        OrigenBiomeProfile.OASIS,
        1);
    private static final WeightedTable PLATEAU_OUTER = table(
        OrigenBiomeProfile.FOLIAGE_FORTRESS_OUTER,
        1,
        Placeholder.PLATEAU_CENTER,
        1);
    private static final WeightedTable PLATEAU_CENTER = table(
        OrigenBiomeProfile.FOLIAGE_FORTRESS_MIDDLE,
        1,
        OrigenBiomeProfile.FOLIAGE_FORTRESS_INNER,
        1);

    private final long worldSeed;
    private final OrigenSinkholeSampler sinkholes;
    private final OrigenClimateSampler climate;
    private final Sampler sourceCells;
    private final Sampler temperatureCells;
    private final Sampler cellValues;
    private final Sampler coastCellValues;
    private final Sampler cellDistances;
    private final Sampler cellEdges;
    private final Sampler canyonMask;
    private final Sampler canyonBase;
    private final Sampler patchwork;
    private final Sampler river;
    private final Sampler blend;
    private final Sampler borders;
    private final Sampler wobble;
    private final Sampler whiteNoise;
    private final LongEntryCache sourceCache = new LongEntryCache(CACHE_SIZE);
    /** The source before sinkholes, which the shore test around a sinkhole reads. */
    private final LongEntryCache landformCache = new LongEntryCache(CACHE_SIZE);
    private final LongProfileCache finalCache = new LongProfileCache(CACHE_SIZE);
    private final LongProfileCache polishedCache = new LongProfileCache(CACHE_SIZE);

    OrigenBiomePipeline(long worldSeed) {
        this.worldSeed = worldSeed;
        sinkholes = new OrigenSinkholeSampler();
        climate = new OrigenClimateSampler(worldSeed);
        sourceCells = cellularLookup(CELL_FREQUENCY, 0L, climate.continentsSampler(), CELL_JITTER);
        temperatureCells = cellularLookup(CELL_FREQUENCY, 0L, climate.temperatureSampler(), CELL_JITTER);
        cellValues = cellular(CELL_FREQUENCY, 0L, CellularReturnType.CellValue, CELL_JITTER);
        coastCellValues = cellular(CELL_FREQUENCY, 8_726_345L, CellularReturnType.CellValue, CELL_JITTER);
        cellDistances = cellular(CELL_FREQUENCY, 0L, CellularReturnType.Distance, CELL_JITTER);
        cellEdges = cellular(CELL_FREQUENCY, 0L, CellularReturnType.Distance2Sub, CELL_JITTER);
        canyonMask = cellular(CELL_FREQUENCY * 0.15D, 0L, CellularReturnType.Distance, 1.0D);
        canyonBase = new Terra64OpenSimplex2SSampler(CELL_FREQUENCY, 0L);
        Sampler patchworkCells = cellular(CELL_FREQUENCY * PATCHWORK_FREQUENCY, 0L, CellularReturnType.CellValue, 1.0D);
        Sampler patchworkGradient = new Terra64OpenSimplex2Sampler(CELL_FREQUENCY / 1.6D, 0L);
        Sampler patchworkShape = new TwoDimensionalSampler() {

            @Override
            public double getSample(long seed, double x, double z) {
                double cells = patchworkCells.getSample(seed, x, z);
                return cells > 0.0D ? (patchworkCells.getSample(seed, x * 2.0D, z * 1.3D) - 0.5D) * 2.0D
                    : patchworkGradient.getSample(seed, x, z);
            }
        };
        patchwork = new DomainWarpedSampler(
            patchworkShape,
            new Terra64OpenSimplex2Sampler(CELL_FREQUENCY * 4.0D, 0L),
            40.0D);
        river = new Terra64OpenSimplex2Sampler(0.0013D, 0L);
        blend = new Terra64OpenSimplex2Sampler(0.012D, 0L);
        borders = new Terra64OpenSimplex2Sampler(1.0D / 300.0D, 0L);
        wobble = new Terra64OpenSimplex2Sampler(1.0D / 32.0D, 0L);
        // Terra 6.4.2's generic noise template defaults to 0.02 when the Origen
        // pipeline declares a bare WHITE_NOISE sampler. Seismic requires the
        // frequency explicitly and otherwise has no equivalent default.
        whiteNoise = new WhiteNoiseSampler(0.02D, 0L);
    }

    OrigenProvinceSample sample(int blockX, int blockZ) {
        return new OrigenProvinceSample(sampleProfile(blockX, blockZ));
    }

    OrigenBiomeProfile sampleProfile(int blockX, int blockZ) {
        int warpedX = warpedX(blockX, blockZ);
        int warpedZ = warpedZ(warpedX, blockZ);
        int gridX = warpedX / PIPELINE_RESOLUTION;
        int gridZ = warpedZ / PIPELINE_RESOLUTION;
        return polishedAt(gridX, gridZ);
    }

    /**
     * Where a column looks up the biome grid. Origen's ±6-block blend warp, plus Biomic's: a ±40-block warp over
     * 300 blocks so cell edges stop running straight for hundreds of blocks, and a ±3-block wobble over 32 so the
     * four-block grid's steps do not show (slow enough never to fold the map into specks).
     */
    private int warpedX(int blockX, int blockZ) {
        return (int) Math.floor(
            blockX + blend.getSample(worldSeed + 1L, blockX, blockZ) * 6.0D
                + borders.getSample(worldSeed + 11L, blockX, blockZ) * 40.0D
                + wobble.getSample(worldSeed + 21L, blockX, blockZ) * 3.0D);
    }

    private int warpedZ(int warpedX, int blockZ) {
        return (int) Math.floor(
            blockZ + blend.getSample(worldSeed + 2L, warpedX, blockZ) * 6.0D
                + borders.getSample(worldSeed + 12L, warpedX, blockZ) * 40.0D
                + wobble.getSample(worldSeed + 22L, warpedX, blockZ) * 3.0D);
    }

    private OrigenBiomeProfile finalAt(int gridX, int gridZ) {
        long key = coordinateKey(gridX, gridZ);
        OrigenBiomeProfile cached = finalCache.get(key);
        if (cached != null) return cached;
        OrigenBiomeProfile sampled = smoothRiver(gridX, gridZ);
        return finalCache.putIfAbsent(key, sampled);
    }

    /**
     * Removes thin axis-aligned Murky Marshlands protrusions into Frosty Fingers after Terra's two grid-smoothing
     * passes. The shipped pipeline can leave a three-by-one coarse-grid finger whose moss/tuff palette appears as
     * a rectangular green bite in otherwise continuous snow. Restricting the majority pass to this exact warm/cold
     * pair preserves ordinary small biomes and every unrelated Origen boundary.
     */
    private OrigenBiomeProfile polishedAt(int gridX, int gridZ) {
        long key = coordinateKey(gridX, gridZ);
        OrigenBiomeProfile cached = polishedCache.get(key);
        if (cached != null) return cached;

        OrigenBiomeProfile center = finalAt(gridX, gridZ);
        if (center != OrigenBiomeProfile.MURKY_MARSHLANDS
            || !hasFrostyCardinal(gridX, gridZ, FROSTY_MARSH_POLISH_RADIUS)) {
            return polishedCache.putIfAbsent(key, center);
        }

        int frosty = 0;
        int marsh = 0;
        for (int offsetZ = -FROSTY_MARSH_POLISH_RADIUS; offsetZ <= FROSTY_MARSH_POLISH_RADIUS; offsetZ++) {
            for (int offsetX = -FROSTY_MARSH_POLISH_RADIUS; offsetX <= FROSTY_MARSH_POLISH_RADIUS; offsetX++) {
                OrigenBiomeProfile nearby = finalAt(gridX + offsetX, gridZ + offsetZ);
                if (nearby == OrigenBiomeProfile.FROSTY_FINGERS) frosty++;
                else if (nearby == OrigenBiomeProfile.MURKY_MARSHLANDS) marsh++;
            }
        }
        OrigenBiomeProfile polished = frosty + marsh >= FROSTY_MARSH_POLISH_PAIR_MIN && frosty > marsh
            ? OrigenBiomeProfile.FROSTY_FINGERS
            : center;
        return polishedCache.putIfAbsent(key, polished);
    }

    private boolean hasFrostyCardinal(int gridX, int gridZ, int distance) {
        return finalAt(gridX + distance, gridZ) == OrigenBiomeProfile.FROSTY_FINGERS
            || finalAt(gridX - distance, gridZ) == OrigenBiomeProfile.FROSTY_FINGERS
            || finalAt(gridX, gridZ + distance) == OrigenBiomeProfile.FROSTY_FINGERS
            || finalAt(gridX, gridZ - distance) == OrigenBiomeProfile.FROSTY_FINGERS;
    }

    private OrigenBiomeProfile smoothRiver(int gridX, int gridZ) {
        return smooth(
            riverAt(gridX, gridZ),
            riverAt(gridX + 1, gridZ),
            riverAt(gridX - 1, gridZ),
            riverAt(gridX, gridZ + 1),
            riverAt(gridX, gridZ - 1),
            gridX,
            gridZ);
    }

    private OrigenBiomeProfile riverAt(int gridX, int gridZ) {
        OrigenBiomeProfile profile = firstSmoothAt(gridX, gridZ);
        double value = river.getSample(worldSeed, worldCoordinate(gridX), worldCoordinate(gridZ));
        return Math.abs(value) < 0.04D ? riverVariant(profile) : profile;
    }

    private OrigenBiomeProfile firstSmoothAt(int gridX, int gridZ) {
        return smooth(
            expandedAt(gridX, gridZ),
            expandedAt(gridX + 1, gridZ),
            expandedAt(gridX - 1, gridZ),
            expandedAt(gridX, gridZ + 1),
            expandedAt(gridX, gridZ - 1),
            gridX,
            gridZ);
    }

    private OrigenBiomeProfile smooth(OrigenBiomeProfile center, OrigenBiomeProfile positiveX,
        OrigenBiomeProfile negativeX, OrigenBiomeProfile positiveZ, OrigenBiomeProfile negativeZ, int gridX,
        int gridZ) {
        double roll = whiteNoise.getSample(worldSeed, worldCoordinate(gridX), worldCoordinate(gridZ));
        boolean alongX = positiveX == negativeX;
        boolean alongZ = positiveZ == negativeZ;
        if (alongX && alongZ) {
            return roll > 0.0D ? roll > 0.25D ? positiveZ : negativeZ : roll > -0.25D ? positiveX : negativeX;
        }
        if (alongX) return roll > 0.0D ? positiveX : negativeX;
        if (alongZ) return roll > 0.0D ? positiveZ : negativeZ;
        return center;
    }

    private OrigenBiomeProfile expandedAt(int gridX, int gridZ) {
        if ((gridX & 1) == 0 && (gridZ & 1) == 0) {
            return requireProfile(sourceAt(gridX, gridZ));
        }
        double roll = whiteNoise.getSample(worldSeed, worldCoordinate(gridX), worldCoordinate(gridZ));
        if ((gridX & 1) != 0 && (gridZ & 1) == 0) {
            return requireProfile(sourceAt(roll > 0.0D ? gridX - 1 : gridX + 1, gridZ));
        }
        if ((gridX & 1) == 0) {
            return requireProfile(sourceAt(gridX, roll > 0.0D ? gridZ - 1 : gridZ + 1));
        }
        if (roll > 0.0D) {
            return requireProfile(sourceAt(roll > 0.25D ? gridX - 1 : gridX + 1, gridZ + 1));
        }
        return requireProfile(sourceAt(roll > -0.25D ? gridX - 1 : gridX + 1, gridZ - 1));
    }

    private Entry sourceAt(int gridX, int gridZ) {
        long key = coordinateKey(gridX, gridZ);
        Entry cached = sourceCache.get(key);
        if (cached != null) return cached;
        int blockX = worldCoordinate(gridX);
        int blockZ = worldCoordinate(gridZ);
        Entry entry = landformAt(gridX, gridZ);
        if (sinkholes != null) {
            OrigenBiomeProfile hole = sinkholes.sample(worldSeed, blockX, blockZ);
            // Biomic: the inner sinkhole follows the cell's climate band (all three share one landform).
            if (hole != null && hole != OrigenBiomeProfile.SINKHOLE_OUTER) {
                int band = band(blockX, blockZ);
                hole = band == 0 ? OrigenBiomeProfile.SINKHOLE_FROZEN
                    : band == 1 ? OrigenBiomeProfile.SINKHOLE_FOREST : OrigenBiomeProfile.SINKHOLE_JUNGLE;
            }
            if (hole != null && inland(gridX, gridZ)) entry = Entry.of(hole);
        }
        return sourceCache.putIfAbsent(key, entry);
    }

    /**
     * Sinkholes stay off the shore (Biomic; upstream's preset has none): a sinkhole cell needs land, no coast or sea,
     * within SINKHOLE_SHORE_GAP blocks, or its rim cut a dry trench through a beach (review, 2026-10-25).
     */
    private boolean inland(int gridX, int gridZ) {
        for (int[] probe : SHORE_PROBES) {
            Entry around = landformAt(gridX + probe[0], gridZ + probe[1]);
            OrigenBiomeProfile.Kind kind = around.profile == null ? null : around.profile.getKind();
            if (kind == OrigenBiomeProfile.Kind.OCEAN || kind == OrigenBiomeProfile.Kind.COAST) return false;
        }
        return true;
    }

    private Entry landformAt(int gridX, int gridZ) {
        long key = coordinateKey(gridX, gridZ);
        Entry cached = landformCache.get(key);
        if (cached != null) return cached;
        int blockX = worldCoordinate(gridX);
        int blockZ = worldCoordinate(gridZ);
        Entry entry = SOURCE.pick(sourceCells.getSample(worldSeed, blockX, blockZ));
        entry = applyCanyons(entry, blockX, blockZ);
        entry = applyCoasts(entry, blockX, blockZ);
        entry = fillCoasts(entry, blockX, blockZ);
        entry = fillOceans(entry, blockX, blockZ);
        entry = spreadTemperature(entry, blockX, blockZ);
        entry = fillTemperature(entry, blockX, blockZ);
        entry = fillPatchwork(entry, blockX, blockZ);
        entry = fillBorders(entry, blockX, blockZ);
        return landformCache.putIfAbsent(key, entry);
    }

    private Entry applyCanyons(Entry entry, int blockX, int blockZ) {
        if (!entry.is(Placeholder.LAND)) return entry;
        double mask = canyonMask.getSample(worldSeed, blockX, blockZ * 1.3D);
        double base = canyonBase.getSample(worldSeed, blockX, blockZ);
        double gate = Math.min(Math.max(-mask - 0.93D, 0.0D) * 18.0D, 1.0D)
            * Math.max((-Math.abs(base) + 0.3D) * 2.0D, 0.0D)
            * 5.0D;
        Entry selected = CANYON_GATE.pick(gate);
        if (selected.is(Placeholder.SELF)) return entry;
        // Biomic: a canyon takes its cell's climate band, not the raw temperature (which flipped canyons from snow
        // to red sand mid-gorge); medium cells get Biomic's temperate canyon.
        int band = band(blockX, blockZ);
        return Entry.of(
            band == 0 ? OrigenBiomeProfile.ICY_INCISIONS
                : band == 2 ? OrigenBiomeProfile.SANDY_SPLITS : OrigenBiomeProfile.FERN_FISSURES);
    }

    /** The cell's climate band: 0 cold, 1 medium, 2 warm. */
    int band(int blockX, int blockZ) {
        return normalizeIndex(temperatureCells.getSample(worldSeed, blockX, blockZ), 3);
    }

    private Entry applyCoasts(Entry entry, int blockX, int blockZ) {
        if (!entry.is(Placeholder.OCEAN)) return entry;
        Entry kind = OCEAN_COAST_KIND.pick(cellValues.getSample(worldSeed, blockX, blockZ));
        if (kind.is(Placeholder.SELF)) return entry;
        if (kind.is(Placeholder.OCEAN_COAST_SMALL) && coastBorder(blockX, blockZ, 30)) {
            return Entry.of(Placeholder.COAST_SMALL);
        }
        if (kind.is(Placeholder.OCEAN_COAST_WIDE) && coastBorder(blockX, blockZ, 60)) {
            return Entry.of(Placeholder.COAST_WIDE);
        }
        return entry;
    }

    private Entry fillCoasts(Entry entry, int blockX, int blockZ) {
        boolean small = entry.is(Placeholder.COAST_SMALL);
        boolean wide = entry.is(Placeholder.COAST_WIDE);
        if (!small && !wide) return entry;
        int climateBand = normalizeIndex(temperatureCells.getSample(worldSeed, blockX, blockZ), 3);
        double selector = coastCellValues.getSample(worldSeed, blockX, blockZ);
        if (small) {
            return climateBand == 0 ? SMALL_COAST_COLD.pick(selector)
                : climateBand == 1 ? SMALL_COAST_MEDIUM.pick(selector) : SMALL_COAST_WARM.pick(selector);
        }
        return climateBand == 0 ? WIDE_COAST_COLD.pick(selector)
            : climateBand == 1 ? WIDE_COAST_MEDIUM.pick(selector) : WIDE_COAST_WARM.pick(selector);
    }

    private Entry fillOceans(Entry entry, int blockX, int blockZ) {
        if (!entry.is(Placeholder.OCEAN)) return entry;
        int climateBand = normalizeIndex(temperatureCells.getSample(worldSeed, blockX, blockZ), 3);
        double selector = cellValues.getSample(worldSeed, blockX, blockZ);
        return climateBand == 0 ? OCEAN_COLD.pick(selector)
            : climateBand == 1 ? OCEAN_MEDIUM.pick(selector) : OCEAN_WARM.pick(selector);
    }

    private Entry spreadTemperature(Entry entry, int blockX, int blockZ) {
        return entry.is(Placeholder.LAND) ? LAND_TEMPERATURE.pick(temperatureCells.getSample(worldSeed, blockX, blockZ))
            : entry;
    }

    private Entry fillTemperature(Entry entry, int blockX, int blockZ) {
        double selector = cellValues.getSample(worldSeed, blockX, blockZ);
        if (entry.is(Placeholder.COLD)) return DIRECT_COLD.pick(selector);
        if (entry.is(Placeholder.MEDIUM)) return DIRECT_MEDIUM.pick(selector);
        if (entry.is(Placeholder.WARM)) return DIRECT_WARM.pick(selector);
        return entry;
    }

    private Entry fillPatchwork(Entry entry, int blockX, int blockZ) {
        double selector = patchwork.getSample(worldSeed, blockX, blockZ);
        if (entry.is(Placeholder.PATCHWORK_COLD)) return PATCHWORK_COLD.pick(selector);
        if (entry.is(Placeholder.PATCHWORK_MEDIUM)) return PATCHWORK_MEDIUM.pick(selector);
        if (entry.is(Placeholder.PATCHWORK_WARM)) return PATCHWORK_WARM.pick(selector);
        return entry;
    }

    private Entry fillBorders(Entry entry, int blockX, int blockZ) {
        double cells = cellDistances.getSample(worldSeed, blockX, blockZ);
        if (entry.is(Placeholder.PILLOW_PLAINS)) {
            double edges = cellEdges.getSample(worldSeed, blockX, blockZ);
            return PILLOW_BORDERS.pick(Math.abs(edges) - 0.9D + cells * 0.09D);
        }
        if (entry.is(Placeholder.SECLUDED_VALLEYS)) {
            return SECLUDED_BORDERS.pick(cells < -0.7D ? 1.0D : -1.0D);
        }
        if (entry.is(Placeholder.DESERT)) {
            return DESERT_BORDERS.pick(cells < -0.991D ? 1.0D : -1.0D);
        }
        if (entry.is(Placeholder.PLATEAU)) {
            entry = PLATEAU_OUTER.pick(cells < -0.8D ? 1.0D : -1.0D);
        }
        if (entry.is(Placeholder.PLATEAU_CENTER)) {
            return PLATEAU_CENTER.pick(cells < -0.88D ? 1.0D : -1.0D);
        }
        return entry;
    }

    private boolean coastBorder(int blockX, int blockZ, int width) {
        // Origen's wide expression reuses oceanEdgeSampler from the small-coast YAML anchor. The outer expression
        // changes width to 60, but the aliased kernel still multiplies its coordinates by 30. Preserve that authored
        // behavior: the wide kernel is centered at half the requested world coordinate with 30-block neighbors.
        double innerWidth = 30.0D;
        double scale = innerWidth / width;
        double sampledX = blockX * scale;
        double sampledZ = blockZ * scale;
        boolean center = isLandCell(sampledX, sampledZ);
        return isLandCell(sampledX + innerWidth, sampledZ) != center
            || isLandCell(sampledX - innerWidth, sampledZ) != center
            || isLandCell(sampledX, sampledZ + innerWidth) != center
            || isLandCell(sampledX, sampledZ - innerWidth) != center;
    }

    private boolean isLandCell(double blockX, double blockZ) {
        return sourceCells.getSample(worldSeed, blockX, blockZ) >= 0.0D;
    }

    private static OrigenBiomeProfile riverVariant(OrigenBiomeProfile profile) {
        switch (profile) {
            case VERTICAL_VISTAS:
                return OrigenBiomeProfile.VISTAS_RIVER;
            case MOUNTAIN_MIRRORS:
                return OrigenBiomeProfile.MOUNTAIN_MIRRORS_RIVER;
            case LUSH_LOOPS:
                return OrigenBiomeProfile.LOOPS_RIVER;
            case VERDANT_VALLEYS:
                return OrigenBiomeProfile.VERDANT_RIVER;
            case SNOWY_MOUNTAINS:
            case SNOWY_SPIRES:
                return OrigenBiomeProfile.FROZEN_RIVER;
            case CANOPY_CASCADES:
                return OrigenBiomeProfile.VERTICAL_JUNGLE_RIVER;
            case BADLANDS_BALCONIES:
                return OrigenBiomeProfile.BAD_BALCOONIES_RIVER;
            case ALPINE_ASCENDANCY:
                return OrigenBiomeProfile.ALPINE_ASCENDANCY_RIVER;
            case FOLIAGE_FORTRESS_MIDDLE:
                return OrigenBiomeProfile.PLATEAO_RIVER_MIDDLE;
            case FOLIAGE_FORTRESS_INNER:
                return OrigenBiomeProfile.PLATEAO_RIVER_INNER;
            case SCARLET_SANCTUARY:
                return OrigenBiomeProfile.SCARLET_SANCTUARY_RIVER;
            case FROZEN_FUNGI:
                return OrigenBiomeProfile.FROZEN_FUNGI_RIVER;
            case GLOOMY_GORGE:
                return OrigenBiomeProfile.GLOOMY_GORGE_RIVER;
            default:
                return usesGeneralRiver(profile) ? OrigenBiomeProfile.RIVER : profile;
        }
    }

    private static boolean usesGeneralRiver(OrigenBiomeProfile profile) {
        switch (profile) {
            case ARID_ARBORETUM:
            case BARE_BOULDERFIELDS:
            case BEACH:
            case BLACK_FOREST:
            case COLD_STEPPE:
            case DESERT:
            case DESERT_SPIKES_GOLD:
            case FLOWERING_FOREST:
            case FOLIAGE_FORTRESS_OUTER:
            case FOSSILIZED_FENLANDS:
            case FROSTY_FINGERS:
            case MANGROVE_SWAMP:
            case MESA_MONUMENTS:
            case MURKY_MARSHLANDS:
            case OASIS:
            case OVERGROWN_CLIFFS:
            case PALM_BEACH:
            case PILLOW_PLAINS_OUTER:
            case ROCKY_REFUGE:
            case SALT_FLATS:
            case SHALE_BEACH:
            case SNOWY_MEADOW:
            case TALL_TIMBERLAND:
            case TEMPERATE_SEA_ARCHES:
            case TUNDRA_TRACKS:
            case WATERY_WILDS:
            case WHITE_WALLOWS:
                return true;
            default:
                return false;
        }
    }

    private static OrigenBiomeProfile requireProfile(Entry entry) {
        if (entry.profile == null) {
            throw new IllegalStateException("Origen pipeline leaked placeholder " + entry.placeholder);
        }
        return entry.profile;
    }

    private static int worldCoordinate(int gridCoordinate) {
        return gridCoordinate * PIPELINE_RESOLUTION;
    }

    private static long coordinateKey(int x, int z) {
        return (long) x << 32 ^ z & 0xFFFF_FFFFL;
    }

    private static Sampler cellular(double frequency, long salt, CellularReturnType returnType, double jitter) {
        return new Terra64CellularSampler(
            frequency,
            salt,
            Sampler.zero(),
            DistanceFunction.EuclideanSq,
            returnType,
            jitter);
    }

    private static Sampler cellularLookup(double frequency, long salt, Sampler lookup, double jitter) {
        return new Terra64CellularSampler(
            frequency,
            salt,
            lookup,
            DistanceFunction.EuclideanSq,
            CellularReturnType.NoiseLookup,
            jitter);
    }

    private static int normalizeIndex(double value, int size) {
        return Math.max(Math.min((int) Math.floor((value + 1.0D) * 0.5D * size), size - 1), 0);
    }

    private static WeightedTable table(Object... weightedEntries) {
        if ((weightedEntries.length & 1) != 0) {
            throw new IllegalArgumentException("Weighted pipeline tables need entry/weight pairs");
        }
        int size = 0;
        for (int index = 1; index < weightedEntries.length; index += 2) {
            size += ((Integer) weightedEntries[index]).intValue();
        }
        Entry[] entries = new Entry[size];
        int cursor = 0;
        for (int index = 0; index < weightedEntries.length; index += 2) {
            Entry entry = Entry.of(weightedEntries[index]);
            int weight = ((Integer) weightedEntries[index + 1]).intValue();
            for (int copy = 0; copy < weight; copy++) {
                entries[cursor++] = entry;
            }
        }
        return new WeightedTable(entries);
    }

    private static int slot(long key, int mask) {
        return (int) WorldgenContext.mix64(key) & mask;
    }

    private enum Placeholder {
        SELF,
        LAND,
        OCEAN,
        CANYON,
        OCEAN_COAST_SMALL,
        OCEAN_COAST_WIDE,
        COAST_SMALL,
        COAST_WIDE,
        COLD,
        MEDIUM,
        WARM,
        PATCHWORK_COLD,
        PATCHWORK_MEDIUM,
        PATCHWORK_WARM,
        PILLOW_PLAINS,
        SECLUDED_VALLEYS,
        DESERT,
        PLATEAU,
        PLATEAU_CENTER
    }

    private static final class Entry {

        private final OrigenBiomeProfile profile;
        private final Placeholder placeholder;

        private Entry(OrigenBiomeProfile profile, Placeholder placeholder) {
            this.profile = profile;
            this.placeholder = placeholder;
        }

        private static Entry of(Object value) {
            if (value instanceof OrigenBiomeProfile) {
                return new Entry((OrigenBiomeProfile) value, null);
            }
            if (value instanceof Placeholder) {
                return new Entry(null, (Placeholder) value);
            }
            throw new IllegalArgumentException("Unsupported pipeline entry " + value);
        }

        private boolean is(Placeholder expected) {
            return placeholder == expected;
        }
    }

    /** Fixed-memory cache: collisions only cause deterministic recomputation, never an incorrect hit. */
    private static final class LongEntryCache {

        private final long[] keys;
        private final Entry[] values;
        private final Object[] locks;
        private final int mask;

        private LongEntryCache(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Origen pipeline cache size must be a power of two");
            }
            keys = new long[size];
            values = new Entry[size];
            locks = cacheLocks(size);
            mask = size - 1;
        }

        private Entry get(long key) {
            int index = slot(key, mask);
            synchronized (locks[index & locks.length - 1]) {
                return values[index] != null && keys[index] == key ? values[index] : null;
            }
        }

        private Entry putIfAbsent(long key, Entry value) {
            int index = slot(key, mask);
            synchronized (locks[index & locks.length - 1]) {
                if (values[index] != null && keys[index] == key) {
                    return values[index];
                }
                keys[index] = key;
                values[index] = value;
                return value;
            }
        }
    }

    /** Enum-reference variant of the fixed-memory cache used by the final pipeline stage. */
    private static final class LongProfileCache {

        private final long[] keys;
        private final OrigenBiomeProfile[] values;
        private final Object[] locks;
        private final int mask;

        private LongProfileCache(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Origen pipeline cache size must be a power of two");
            }
            keys = new long[size];
            values = new OrigenBiomeProfile[size];
            locks = cacheLocks(size);
            mask = size - 1;
        }

        private OrigenBiomeProfile get(long key) {
            int index = slot(key, mask);
            synchronized (locks[index & locks.length - 1]) {
                return values[index] != null && keys[index] == key ? values[index] : null;
            }
        }

        private OrigenBiomeProfile putIfAbsent(long key, OrigenBiomeProfile value) {
            int index = slot(key, mask);
            synchronized (locks[index & locks.length - 1]) {
                if (values[index] != null && keys[index] == key) {
                    return values[index];
                }
                keys[index] = key;
                values[index] = value;
                return value;
            }
        }
    }

    private static Object[] cacheLocks(int cacheSize) {
        Object[] locks = new Object[Math.min(CACHE_LOCK_STRIPES, cacheSize)];
        for (int index = 0; index < locks.length; index++) locks[index] = new Object();
        return locks;
    }

    private static final class WeightedTable {

        private final Entry[] entries;

        private WeightedTable(Entry[] entries) {
            this.entries = entries;
        }

        private Entry pick(double noise) {
            return entries[normalizeIndex(noise, entries.length)];
        }
    }

    private abstract static class TwoDimensionalSampler implements Sampler {

        @Override
        public double getSample(long seed, double x, double y, double z) {
            return getSample(seed, x, z);
        }
    }
}
