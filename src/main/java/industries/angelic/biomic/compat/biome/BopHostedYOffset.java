package industries.angelic.biomic.compat.biome;

import net.minecraft.block.Block;

/** Constant-Y source view for exact legacy BOP geometry on a shifted physical host. */
public final class BopHostedYOffset implements BopNativePond.HostedAccess {

    private final BopNativePond.HostedAccess actual;
    private final int offset;
    private final int minimumY, maximumYExclusive;

    public BopHostedYOffset(BopNativePond.HostedAccess actual, int offset) {
        this.actual = java.util.Objects.requireNonNull(actual, "Hosted BOP access");
        this.offset = offset;
        int translatedMinimum = Math.subtractExact(actual.minimumY(), offset);
        int translatedMaximum = Math.subtractExact(actual.maximumYExclusive(), offset);
        minimumY = Math.max(0, translatedMinimum);
        maximumYExclusive = Math.min(256, translatedMaximum);
        if (maximumYExclusive <= minimumY)
            throw new IllegalArgumentException("Shifted hosted BOP view does not intersect native 0..255 height");
    }

    private int y(int sourceY) {
        return Math.addExact(sourceY, offset);
    }

    @Override
    public Block block(int x, int y, int z) {
        return actual.block(x, y(y), z);
    }

    @Override
    public int metadata(int x, int y, int z) {
        return actual.metadata(x, y(y), z);
    }

    @Override
    public boolean set(int x, int y, int z, Block block, int metadata) {
        return actual.set(x, y(y), z, block, metadata);
    }

    @Override
    public int heightValue(int x, int z) {
        return Math.subtractExact(actual.heightValue(x, z), offset);
    }

    @Override
    public int minimumY() {
        return minimumY;
    }

    @Override
    public int maximumYExclusive() {
        return maximumYExclusive;
    }

    @Override
    public boolean immutableCarrier(int x, int y, int z, Block block, int metadata) {
        return actual.immutableCarrier(x, y(y), z, block, metadata);
    }

    @Override
    public String immutableCarrierRejection(int x, int y, int z, Block block, int metadata) {
        return actual.immutableCarrierRejection(x, y(y), z, block, metadata);
    }
}
