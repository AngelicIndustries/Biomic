package industries.angelic.biomic.compat.flora;

import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** A synchronous, world-identity-scoped bridge for audited native generators. Never captures other threads. */
public final class NativeFeatureCapture {

    public interface Access {

        Block block(int x, int y, int z);

        int metadata(int x, int y, int z);

        boolean set(int x, int y, int z, Block block, int metadata);

        default int heightValue(int x, int z) {
            throw new UnsupportedOperationException("Native height query not provided by this capture");
        }

        default int topSolidOrLiquid(int x, int z) {
            return heightValue(x, z);
        }

        /** Physical transaction envelope. Existing 1.7 captures remain byte-for-byte 0..255 by default. */
        default int minimumY() {
            return 0;
        }

        default int maximumYExclusive() {
            return 256;
        }
    }

    /**
     * A capture that confines a generator to a box of the real world (village pieces): tile entities and spawned
     * entities outside it are refused, inside they are the world's own. Light and block updates stay the world's.
     */
    public interface BoxedAccess extends Access {

        net.minecraft.tileentity.TileEntity tileEntity(int x, int y, int z);

        boolean spawnEntity(net.minecraft.entity.Entity entity);
    }

    /** Opt-in tile/light transaction for audited underground features; ordinary flora captures are unchanged. */
    public interface IsolatedAccess extends Access {

        net.minecraft.tileentity.TileEntity tileEntity(int x, int y, int z);

        /** Direct tile mutation cannot escape a detached transaction. Default is explicit fail-closed. */
        default void tileMutation(int x, int y, int z, net.minecraft.tileentity.TileEntity tile) {
            throw new UnsupportedOperationException("Native capture does not support direct tile mutation");
        }

        int savedLight(net.minecraft.world.EnumSkyBlock kind, int x, int y, int z);

        java.util.Random random();
    }

    /** Surface feature queries must not ask the real World to load lighting chunks. */
    public interface SurfaceAccess extends IsolatedAccess {

        int fullLight(int x, int y, int z);

        boolean seesSky(int x, int y, int z);
    }

    /** Explicit opt-in only; ordinary captures and ordinary world spawning are unchanged. */
    public interface EntitySpawnAccess extends SurfaceAccess {

        boolean spawnEntity(net.minecraft.entity.Entity entity);
    }

    /** Explicit biome/freezing reads for native ponds; earlier captures keep their existing behavior. */
    public interface BiomeSurfaceAccess extends SurfaceAccess {

        net.minecraft.world.biome.BiomeGenBase biome(int x, int z);

        boolean freezable(int x, int y, int z);
    }

    /**
     * A native feature reached outside the envelope its adapter audited (a read or write beyond its bounds). The
     * adapter's buffered proposal is dropped and the whole feature rejected; it never crashes the server.
     */
    public static final class OutsideEnvelope extends RuntimeException {

        private OutsideEnvelope(String what) {
            super(what, null, false, false);
        }
    }

    private static final java.util.Set<String> WARNED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** The rejection to throw; the first of each kind is logged. */
    public static OutsideEnvelope outside(String what) {
        if (WARNED.add(what))
            industries.angelic.biomic.Biomic.LOG.warn("Native feature rejected (logged once per kind): {}", what);
        return new OutsideEnvelope(what);
    }

    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<>();
    private static final java.util.concurrent.atomic.AtomicInteger CAPTURES = new java.util.concurrent.atomic.AtomicInteger();
    private static boolean installed;

    private NativeFeatureCapture() {}

    /** Called by the transformed World class. Fail closed if the capture mixin was not installed. */
    public static boolean install() {
        installed = true;
        return true;
    }

    public static boolean installed() {
        return installed;
    }

    public static boolean containsPhysicalY(Access access, int y) {
        int minimum = access.minimumY(), maximum = access.maximumYExclusive();
        if (minimum < 0 || maximum <= minimum || maximum - minimum > 512)
            throw new IllegalArgumentException("Invalid native feature physical envelope");
        return y >= minimum && y < maximum;
    }

    /** Placement floor remains exclusive, matching the legacy adapters' y>=1 rule. */
    public static boolean containsFeatureY(Access access, int y) {
        return containsPhysicalY(access, y) && y > access.minimumY();
    }

    public static <T> T run(World world, Access access, Supplier<T> operation) {
        if (!installed) throw new IllegalStateException("Native feature capture World hooks are unavailable");
        return scoped(world, access, operation);
    }

    static <T> T scoped(World world, Access access, Supplier<T> operation) {
        if (ACTIVE.get() != null) throw new IllegalStateException("Nested native feature capture");
        Frame frame = new Frame(world, access);
        ACTIVE.set(frame);
        CAPTURES.incrementAndGet();
        try {
            return operation.get();
        } finally {
            ACTIVE.remove();
            CAPTURES.decrementAndGet();
        }
    }

    /**
     * Inside an access's own world call (its frame delegating), runs {@code action} with the frame capturing again:
     * a written block's reaction (onBlockAdded) then reads and writes through the access, not the world.
     */
    public static <T> T captured(Supplier<T> action) {
        Frame frame = ACTIVE.get();
        if (frame == null || !frame.delegating) return action.get();
        frame.delegating = false;
        try {
            return action.get();
        } finally {
            frame.delegating = true;
        }
    }

    public static Frame current(World world) {
        if (CAPTURES.get() == 0) return null; // No thread-local lookup in ordinary block reads/writes.
        Frame frame = ACTIVE.get();
        return frame != null && !frame.delegating && frame.world == world ? frame : null;
    }

    /**
     * The isolated random of this thread's capture, whatever its world, or null outside one: for another mod's code
     * that draws on {@code Math.random()} without a world in reach (BOP's weighted grass and flower picks).
     */
    public static java.util.Random isolatedRandomOnThread() {
        if (CAPTURES.get() == 0) return null;
        Frame frame = ACTIVE.get();
        return frame != null && !frame.delegating && frame.isolated() ? frame.random() : null;
    }

    /** Diagnostic predicate only; it never exposes or joins the active frame. */
    public static boolean activeOnThread(World world) {
        Frame frame = ACTIVE.get();
        return frame != null && frame.world == world;
    }

    public static final class Frame {

        private final World world;
        private final Access access;
        private boolean delegating;

        Frame(World world, Access access) {
            this.world = world;
            this.access = access;
        }

        // The bounded view may inspect already-loaded physical blocks. Those reads must not recurse
        // into this capture. The view itself owns bounds, transactions and cross-chunk queuing.
        private <T> T delegate(Supplier<T> action) {
            delegating = true;
            try {
                return action.get();
            } finally {
                delegating = false;
            }
        }

        public Block block(int x, int y, int z) {
            return delegate(() -> access.block(x, y, z));
        }

        public int metadata(int x, int y, int z) {
            return delegate(() -> access.metadata(x, y, z));
        }

        public boolean set(int x, int y, int z, Block block, int metadata) {
            return delegate(() -> access.set(x, y, z, block, metadata));
        }

        public int heightValue(int x, int z) {
            return delegate(() -> access.heightValue(x, z));
        }

        public int topSolidOrLiquid(int x, int z) {
            return delegate(() -> access.topSolidOrLiquid(x, z));
        }

        public boolean isolated() {
            return access instanceof IsolatedAccess;
        }

        public boolean boxed() {
            return access instanceof BoxedAccess;
        }

        public net.minecraft.tileentity.TileEntity boxedTile(int x, int y, int z) {
            return delegate(() -> ((BoxedAccess) access).tileEntity(x, y, z));
        }

        public boolean boxedSpawn(net.minecraft.entity.Entity entity) {
            return delegate(() -> ((BoxedAccess) access).spawnEntity(entity));
        }

        public boolean surface() {
            return access instanceof SurfaceAccess;
        }

        public boolean entitySpawns() {
            return access instanceof EntitySpawnAccess;
        }

        public boolean spawnEntity(net.minecraft.entity.Entity entity) {
            return delegate(() -> ((EntitySpawnAccess) access).spawnEntity(entity));
        }

        public boolean biomeSurface() {
            return access instanceof BiomeSurfaceAccess;
        }

        public net.minecraft.world.biome.BiomeGenBase biome(int x, int z) {
            return delegate(() -> ((BiomeSurfaceAccess) access).biome(x, z));
        }

        public boolean freezable(int x, int y, int z) {
            return delegate(() -> ((BiomeSurfaceAccess) access).freezable(x, y, z));
        }

        public int fullLight(int x, int y, int z) {
            return delegate(() -> ((SurfaceAccess) access).fullLight(x, y, z));
        }

        public boolean seesSky(int x, int y, int z) {
            return delegate(() -> ((SurfaceAccess) access).seesSky(x, y, z));
        }

        public net.minecraft.tileentity.TileEntity tileEntity(int x, int y, int z) {
            return delegate(() -> ((IsolatedAccess) access).tileEntity(x, y, z));
        }

        public void tileMutation(int x, int y, int z, net.minecraft.tileentity.TileEntity tile) {
            delegate(() -> {
                ((IsolatedAccess) access).tileMutation(x, y, z, tile);
                return null;
            });
        }

        public int savedLight(net.minecraft.world.EnumSkyBlock kind, int x, int y, int z) {
            return delegate(() -> ((IsolatedAccess) access).savedLight(kind, x, y, z));
        }

        public java.util.Random random() {
            return ((IsolatedAccess) access).random();
        }
    }
}
