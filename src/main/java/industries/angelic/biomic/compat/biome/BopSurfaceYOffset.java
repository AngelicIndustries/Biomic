package industries.angelic.biomic.compat.biome;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Coordinates only: all reads, writes and light queries stay in the original bounded transaction. */
public final class BopSurfaceYOffset implements NativeFeatureCapture.SurfaceAccess {

    private final NativeFeatureCapture.SurfaceAccess actual;
    private final int offset;

    public BopSurfaceYOffset(NativeFeatureCapture.SurfaceAccess actual, int offset) {
        this.actual = java.util.Objects.requireNonNull(actual);
        this.offset = offset;
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
    public boolean set(int x, int y, int z, Block block, int meta) {
        return actual.set(x, y(y), z, block, meta);
    }

    @Override
    public int heightValue(int x, int z) {
        return Math.subtractExact(actual.heightValue(x, z), offset);
    }

    @Override
    public int minimumY() {
        return Math.subtractExact(actual.minimumY(), offset);
    }

    @Override
    public int maximumYExclusive() {
        return Math.subtractExact(actual.maximumYExclusive(), offset);
    }

    @Override
    public TileEntity tileEntity(int x, int y, int z) {
        return actual.tileEntity(x, y(y), z);
    }

    @Override
    public int savedLight(EnumSkyBlock kind, int x, int y, int z) {
        return actual.savedLight(kind, x, y(y), z);
    }

    @Override
    public int fullLight(int x, int y, int z) {
        return actual.fullLight(x, y(y), z);
    }

    @Override
    public boolean seesSky(int x, int y, int z) {
        return actual.seesSky(x, y(y), z);
    }

    @Override
    public Random random() {
        return actual.random();
    }
}
