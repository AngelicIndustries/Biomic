package industries.angelic.biomic.compat.biome;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Bounded, per-attempt native view. Does not run the World's lighting engine or acquire chunks. */
public class BopSurfaceCapture implements NativeFeatureCapture.SurfaceAccess {

    private final NativeFeatureCapture.Access view;
    private final Random random;
    private final int accessLimit, writeLimit;
    private final Map<Long, int[]> skylight = new HashMap<>();
    private int accesses, writes;

    public BopSurfaceCapture(NativeFeatureCapture.Access view, Random random) {
        this(view, random, 200_000, 32768);
    }

    private BopSurfaceCapture(NativeFeatureCapture.Access view, Random random, int accessLimit, int writeLimit) {
        this.view = view;
        this.random = random;
        this.accessLimit = accessLimit;
        this.writeLimit = writeLimit;
    }

    /** The native height50 Sacred Oak rewrites overlapping leaf clusters well beyond the ordinary tree budget. */
    public static BopSurfaceCapture sacredOak(NativeFeatureCapture.Access view, Random random) {
        return new BopSurfaceCapture(view, random, 1_000_000, 131072);
    }

    /** Preserve hosted provenance only when the wrapped access actually owns that capability. */
    public static BopSurfaceCapture hostedAware(NativeFeatureCapture.Access view, Random random) {
        return view instanceof BopNativePond.HostedAccess hosted ? new Hosted(view, hosted, random)
            : new BopSurfaceCapture(view, random);
    }

    protected final void check() {
        if (++accesses > accessLimit) throw new IllegalStateException("BOP native feature access budget exceeded");
    }

    @Override
    public Block block(int x, int y, int z) {
        check();
        return view.block(x, y, z);
    }

    @Override
    public int metadata(int x, int y, int z) {
        check();
        return view.metadata(x, y, z);
    }

    @Override
    public int heightValue(int x, int z) {
        check();
        return view.heightValue(x, z);
    }

    @Override
    public int minimumY() {
        return view.minimumY();
    }

    @Override
    public int maximumYExclusive() {
        return view.maximumYExclusive();
    }

    @Override
    public boolean set(int x, int y, int z, Block block, int metadata) {
        check();
        if (++writes > writeLimit) throw new IllegalStateException("BOP native feature write budget exceeded");
        skylight.remove(key(x, z));
        return view.set(x, y, z, block, metadata);
    }

    @Override
    public TileEntity tileEntity(int x, int y, int z) {
        throw new IllegalStateException("Unexpected surface tile query");
    }

    @Override
    public Random random() {
        return random;
    }

    @Override
    public int savedLight(EnumSkyBlock kind, int x, int y, int z) {
        return kind == EnumSkyBlock.Sky ? fullLight(x, y, z) : 0;
    }

    @Override
    public int fullLight(int x, int y, int z) {
        check();
        if (y < minimumY()) return 0;
        if (y >= maximumYExclusive()) return 15;
        return skylight.computeIfAbsent(key(x, z), ignored -> skyColumn(x, z))[y - minimumY()];
    }

    @Override
    public boolean seesSky(int x, int y, int z) {
        return fullLight(x, y, z) == 15;
    }

    private int[] skyColumn(int x, int z) {
        // Conservative direct daylight only: no invented lateral/block light. A shaded patch may be
        // suppressed, never admitted using a fabricated full-bright value. Admitted blocks have static opacity.
        int minimum = minimumY(), maximum = maximumYExclusive();
        if (minimum < 0 || maximum <= minimum || maximum - minimum > 512)
            throw new IllegalArgumentException("Invalid native feature physical envelope");
        int[] values = new int[maximum - minimum];
        int light = 15;
        for (int y = maximum - 1; y >= minimum; y--) {
            light = Math.max(0, light - Math.min(15, block(x, y, z).getLightOpacity()));
            values[y - minimum] = light;
        }
        return values;
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xffffffffL);
    }

    private static final class Hosted extends BopSurfaceCapture implements BopNativePond.HostedAccess {

        private final BopNativePond.HostedAccess hosted;

        private Hosted(NativeFeatureCapture.Access view, BopNativePond.HostedAccess hosted, Random random) {
            super(view, random);
            this.hosted = hosted;
        }

        @Override
        public boolean immutableCarrier(int x, int y, int z, Block block, int metadata) {
            check();
            return hosted.immutableCarrier(x, y, z, block, metadata);
        }

        @Override
        public String immutableCarrierRejection(int x, int y, int z, Block block, int metadata) {
            check();
            return hosted.immutableCarrierRejection(x, y, z, block, metadata);
        }
    }
}
