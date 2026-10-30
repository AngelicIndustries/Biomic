package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/** Removes narrowly classified disconnected terrain crumbs before they can acquire dependent surface decoration. */
final class OrigenSmallOrphanFilter {

    private static final int MIN_AUTHORED_Y = 63;
    private static final int MAX_AUTHORED_Y = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - 1;
    private static final int VISUAL_CEILING = 158;
    private static final int MAX_SEARCH_BLOCKS = 256;
    private static final int SEARCH_CAPACITY = MAX_SEARCH_BLOCKS + 1;
    private static final int AUTHORED_HEIGHT = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE
        - OrigenDensityField.UPSTREAM_MIN_Y;
    private static final int PROFILE_COUNT = OrigenBiomeProfile.values().length;

    private final OrigenDensityField densityField;
    private final ThreadLocal<ComponentScratch> scratch = ThreadLocal.withInitial(ComponentScratch::new);
    private final ThreadLocal<it.unimi.dsi.fastutil.longs.LongOpenHashSet> largeScratch = ThreadLocal
        .withInitial(it.unimi.dsi.fastutil.longs.LongOpenHashSet::new);

    OrigenSmallOrphanFilter(OrigenDensityField densityField) {
        if (densityField == null) throw new IllegalArgumentException("Origen density field cannot be null");
        this.densityField = densityField;
    }

    /**
     * Removes complete qualifying components from both the authored population baseline and its physical ground
     * projection. Components are classified against the immutable world field, so chunk generation order cannot
     * turn a cross-border component into a partial removal.
     */
    void cull(byte[] authoredMaterials, short[] authoredProfiles, byte[] physicalMaterials, byte[] solidOrigins,
        int chunkX, int chunkZ) {
        validate(authoredMaterials, authoredProfiles, physicalMaterials, solidOrigins);
        cullInternal(authoredMaterials, authoredProfiles, physicalMaterials, solidOrigins, chunkX, chunkZ);
    }

    /** Source-only path: no compressed physical indices are touched before the new projection is applied. */
    void cullAuthored(byte[] authoredMaterials, short[] authoredProfiles, int chunkX, int chunkZ) {
        int size = BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH * AUTHORED_HEIGHT;
        if (authoredMaterials == null || authoredMaterials.length != size
            || authoredProfiles == null
            || authoredProfiles.length != size) {
            throw new IllegalArgumentException("Origen orphan filtering requires complete authored chunk arrays");
        }
        cullInternal(authoredMaterials, authoredProfiles, null, null, chunkX, chunkZ);
    }

    private void cullInternal(byte[] authoredMaterials, short[] authoredProfiles, byte[] physicalMaterials,
        byte[] solidOrigins, int chunkX, int chunkZ) {
        int worldStartX = chunkX << 4;
        int worldStartZ = chunkZ << 4;
        // Above the visual ceiling the 256 projection folds several authored rows into one physical row: a crumb
        // there shares its cell with real terrain, so only the uncompressed (authored-only) path goes higher.
        int limit = physicalMaterials == null ? MAX_AUTHORED_Y : VISUAL_CEILING;
        // Cells of components already found too big for any policy: starts inside them would search them again.
        it.unimi.dsi.fastutil.longs.LongOpenHashSet large = largeScratch.get();
        large.clear();
        scratch.get().large = large;
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                for (int authoredY = MIN_AUTHORED_Y; authoredY <= limit; authoredY++) {
                    if (!isCurrentChunkSolid(authoredProfiles, localX, authoredY, localZ)
                        || isCurrentChunkSolid(authoredProfiles, localX, authoredY - 1, localZ)) continue;

                    if (large.contains(ComponentScratch.key(worldStartX + localX, authoredY, worldStartZ + localZ)))
                        continue;
                    OrigenBiomeProfile startProfile = profile(
                        worldStartX + localX,
                        authoredY,
                        worldStartZ + localZ,
                        authoredProfiles,
                        chunkX,
                        chunkZ);
                    Policy policy = Policy.forStart(authoredY, startProfile);
                    if (policy == null) continue;
                    ComponentScratch component = resolve(
                        worldStartX + localX,
                        authoredY,
                        worldStartZ + localZ,
                        authoredProfiles,
                        chunkX,
                        chunkZ,
                        policy,
                        limit);
                    if (!component.cull) {
                        if (component.oversize && policy == Policy.CRUMBS) large.addAll(component.members);
                        continue;
                    }
                    for (int index = 0; index < component.size; index++) {
                        int componentX = component.x[index];
                        int componentZ = component.z[index];
                        if (Math.floorDiv(componentX, BiomicTerrainSampler.CHUNK_WIDTH) != chunkX
                            || Math.floorDiv(componentZ, BiomicTerrainSampler.CHUNK_WIDTH) != chunkZ) continue;
                        clear(
                            authoredMaterials,
                            authoredProfiles,
                            physicalMaterials,
                            solidOrigins,
                            Math.floorMod(componentX, BiomicTerrainSampler.CHUNK_WIDTH),
                            component.y[index],
                            Math.floorMod(componentZ, BiomicTerrainSampler.CHUNK_WIDTH));
                    }
                }
            }
        }
    }

    private ComponentScratch resolve(int startX, int startY, int startZ, short[] currentProfiles, int currentChunkX,
        int currentChunkZ, Policy policy, int limit) {
        ComponentScratch component = scratch.get();
        component.reset();
        if (!isSolid(startX, startY, startZ, currentProfiles, currentChunkX, currentChunkZ)) return component;
        component.add(startX, startY, startZ);
        for (int cursor = 0; cursor < component.size; cursor++) {
            int x = component.x[cursor];
            int y = component.y[cursor];
            int z = component.z[cursor];
            OrigenBiomeProfile sampled = profile(x, y, z, currentProfiles, currentChunkX, currentChunkZ);
            component.observe(sampled);
            if (y > limit || !policy.accepts(y, sampled)) {
                component.cull = false;
                return component;
            }
            if (!addIfSolid(component, x + 1, y, z, currentProfiles, currentChunkX, currentChunkZ, policy)
                || !addIfSolid(component, x - 1, y, z, currentProfiles, currentChunkX, currentChunkZ, policy)
                || !addIfSolid(component, x, y + 1, z, currentProfiles, currentChunkX, currentChunkZ, policy)
                || !addIfSolid(component, x, y - 1, z, currentProfiles, currentChunkX, currentChunkZ, policy)
                || !addIfSolid(component, x, y, z + 1, currentProfiles, currentChunkX, currentChunkZ, policy)
                || !addIfSolid(component, x, y, z - 1, currentProfiles, currentChunkX, currentChunkZ, policy)) {
                component.cull = false;
                return component;
            }
        }
        component.cull = component.size <= policy.maximumBlocks && policy.complete(component);
        return component;
    }

    /** False means the component exceeded the bounded orphan search and must be retained. */
    private boolean addIfSolid(ComponentScratch component, int x, int y, int z, short[] currentProfiles,
        int currentChunkX, int currentChunkZ, Policy policy) {
        if (!isSolid(x, y, z, currentProfiles, currentChunkX, currentChunkZ) || component.contains(x, y, z)) {
            return true;
        }
        // Joined to a component already found too big: so is this one.
        if (component.large.contains(ComponentScratch.key(x, y, z))) {
            component.oversize = true;
            return false;
        }
        int capacity = policy.maximumBlocks + 1;
        if (component.size >= capacity) {
            component.oversize = true;
            return false;
        }
        component.add(x, y, z);
        component.oversize = component.size >= capacity;
        return !component.oversize;
    }

    private boolean isSolid(int blockX, int authoredY, int blockZ, short[] currentProfiles, int currentChunkX,
        int currentChunkZ) {
        if (authoredY < OrigenDensityField.UPSTREAM_MIN_Y || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE)
            return false;
        if (currentProfiles != null && Math.floorDiv(blockX, BiomicTerrainSampler.CHUNK_WIDTH) == currentChunkX
            && Math.floorDiv(blockZ, BiomicTerrainSampler.CHUNK_WIDTH) == currentChunkZ) {
            return isCurrentChunkSolid(
                currentProfiles,
                Math.floorMod(blockX, BiomicTerrainSampler.CHUNK_WIDTH),
                authoredY,
                Math.floorMod(blockZ, BiomicTerrainSampler.CHUNK_WIDTH));
        }
        return densityField.isUnfilteredSolidAtUpstream(blockX, authoredY, blockZ);
    }

    private OrigenBiomeProfile profile(int blockX, int authoredY, int blockZ, short[] currentProfiles,
        int currentChunkX, int currentChunkZ) {
        if (currentProfiles != null && Math.floorDiv(blockX, BiomicTerrainSampler.CHUNK_WIDTH) == currentChunkX
            && Math.floorDiv(blockZ, BiomicTerrainSampler.CHUNK_WIDTH) == currentChunkZ) {
            int encoded = currentProfiles[OrigenSurfaceRuntime.authoredMaterialIndex(
                Math.floorMod(blockX, BiomicTerrainSampler.CHUNK_WIDTH),
                authoredY,
                Math.floorMod(blockZ, BiomicTerrainSampler.CHUNK_WIDTH))] & 0xFFFF;
            return OrigenBiomeProfile.values()[encoded % PROFILE_COUNT];
        }
        return densityField.sampleProfile(blockX, authoredY, blockZ);
    }

    private enum Policy {

        // Lush Loops' near-sea blend may leave a small cross-profile lobe detached from both
        // its riverbed and the admitted arches. The source arches are larger, extend above
        // this band, or remain wholly inside one profile and therefore fail this policy.
        LOW_LOOPS(63, 79, 48),
        // Any profile, from sea level to the top of the authored world (512 keeps all of it): a piece of rock
        // touching nothing is never authored terrain (Origen has no floating islands; Biomic's own come later).
        // Thin density spikes break into crumbs (Scarlet Sanctuary's one-column spires, single voxels over
        // Overgrown Cliffs), Gloomy Gorge leaves detached shelves (a 95-block deepslate shelf, review 2026-10-24),
        // and a tilted plateau's blended scarp can leave a lump behind (191 blocks, review 2026-10-24).
        CRUMBS(63, MAX_AUTHORED_Y, 256);

        private static final Policy[] VALUES = values();
        private final int minimumY, maximumY, maximumBlocks;

        Policy(int minimumY, int maximumY, int maximumBlocks) {
            this.minimumY = minimumY;
            this.maximumY = maximumY;
            this.maximumBlocks = maximumBlocks;
        }

        private static Policy forStart(int y, OrigenBiomeProfile profile) {
            for (Policy policy : VALUES)
                if (y >= policy.minimumY && y <= policy.maximumY && policy.profile(profile)) return policy;
            return null;
        }

        private boolean accepts(int y, OrigenBiomeProfile profile) {
            return y >= minimumY && y <= maximumY && profile(profile);
        }

        private boolean profile(OrigenBiomeProfile profile) {
            return switch (this) {
                case LOW_LOOPS -> profile == OrigenBiomeProfile.LUSH_LOOPS || profile == OrigenBiomeProfile.LOOPS_RIVER;
                case CRUMBS -> true;
            };
        }

        private boolean complete(ComponentScratch component) {
            // Low Lush Loops crumbs occur where the blended biome boundary leaves a tiny
            // disconnected remnant. Do not reinterpret a small interior loop/island as an orphan.
            return this != LOW_LOOPS || component.sawLushLoops && component.sawLoopsRiver
                || component.size <= 16 && component.sawLoopsRiver && !component.sawLushLoops;
        }
    }

    private static boolean isCurrentChunkSolid(short[] profiles, int localX, int authoredY, int localZ) {
        if (authoredY < OrigenDensityField.UPSTREAM_MIN_Y || authoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE)
            return false;
        int encoded = profiles[OrigenSurfaceRuntime.authoredMaterialIndex(localX, authoredY, localZ)] & 0xFFFF;
        return encoded / PROFILE_COUNT == 1;
    }

    private static void clear(byte[] authoredMaterials, short[] authoredProfiles, byte[] physicalMaterials,
        byte[] solidOrigins, int localX, int authoredY, int localZ) {
        int authoredIndex = OrigenSurfaceRuntime.authoredMaterialIndex(localX, authoredY, localZ);
        authoredMaterials[authoredIndex] = BiomicTerrainSampler.AIR;
        authoredProfiles[authoredIndex] = (short) ((authoredProfiles[authoredIndex] & 0xFFFF) % PROFILE_COUNT);

        if (physicalMaterials == null) return;

        int physicalY = OrigenVerticalProfileSampler.toBiomicBlockY(authoredY);
        int physicalIndex = BiomicTerrainSampler.index(localX, physicalY, localZ);
        if (solidOrigins == null) {
            physicalMaterials[physicalIndex] = BiomicTerrainSampler.AIR;
            return;
        }
        solidOrigins[physicalIndex] &= ~TerrainDensitySampler.GROUND_ORIGIN;
        if ((solidOrigins[physicalIndex] & TerrainDensitySampler.FLOATING_ORIGIN) == 0) {
            physicalMaterials[physicalIndex] = BiomicTerrainSampler.AIR;
        }
    }

    private static void validate(byte[] authoredMaterials, short[] authoredProfiles, byte[] physicalMaterials,
        byte[] solidOrigins) {
        int authoredSize = BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH * AUTHORED_HEIGHT;
        if (authoredMaterials == null || authoredMaterials.length != authoredSize
            || authoredProfiles == null
            || authoredProfiles.length != authoredSize
            || physicalMaterials == null
            || physicalMaterials.length != BiomicTerrainSampler.BLOCK_COUNT
            || solidOrigins != null && solidOrigins.length != BiomicTerrainSampler.BLOCK_COUNT) {
            throw new IllegalArgumentException("Origen orphan filtering requires complete chunk arrays");
        }
    }

    private static final class ComponentScratch {

        private final int[] x = new int[SEARCH_CAPACITY];
        private final int[] y = new int[SEARCH_CAPACITY];
        private final int[] z = new int[SEARCH_CAPACITY];
        private final it.unimi.dsi.fastutil.longs.LongOpenHashSet members = new it.unimi.dsi.fastutil.longs.LongOpenHashSet(
            SEARCH_CAPACITY * 2);
        /** This pass's cells of components too big for any policy (see cullInternal). */
        private it.unimi.dsi.fastutil.longs.LongOpenHashSet large = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        private int size;
        private boolean cull, oversize;
        private boolean sawLushLoops, sawLoopsRiver;

        private void reset() {
            size = 0;
            members.clear();
            cull = false;
            oversize = false;
            sawLushLoops = false;
            sawLoopsRiver = false;
        }

        private boolean contains(int candidateX, int candidateY, int candidateZ) {
            return members.contains(key(candidateX, candidateY, candidateZ));
        }

        private void add(int blockX, int authoredY, int blockZ) {
            x[size] = blockX;
            y[size] = authoredY;
            z[size] = blockZ;
            members.add(key(blockX, authoredY, blockZ));
            size++;
        }

        private static long key(int blockX, int authoredY, int blockZ) {
            return (long) blockX << 38 ^ (long) (blockZ & 0x3FFFFFF) << 12 ^ authoredY & 0xFFF;
        }

        private void observe(OrigenBiomeProfile profile) {
            sawLushLoops |= profile == OrigenBiomeProfile.LUSH_LOOPS;
            sawLoopsRiver |= profile == OrigenBiomeProfile.LOOPS_RIVER;
        }
    }
}
