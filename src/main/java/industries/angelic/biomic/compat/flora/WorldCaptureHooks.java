package industries.angelic.biomic.compat.flora;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * What a native feature capture answers in place of World, called from the head of World's methods
 * (core.WorldCaptureTransform). Each hook is a pair: {@code xApplies} says whether a capture answers this call, and
 * {@code x} answers it. While no capture runs an Applies is a counter read and nothing more: the cancellable mixin
 * injections these replace built a callback object on every block, light and tile lookup of the game.
 */
public final class WorldCaptureHooks {

    private WorldCaptureHooks() {}

    private static NativeFeatureCapture.Frame frame(World world) {
        return NativeFeatureCapture.current(world);
    }

    private static NativeFeatureCapture.Frame isolated(World world) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame != null && frame.isolated() ? frame : null;
    }

    private static NativeFeatureCapture.Frame boxed(World world) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame != null && frame.boxed() ? frame : null;
    }

    public static boolean blockApplies(World world, int x, int y, int z) {
        return frame(world) != null;
    }

    public static Block block(World world, int x, int y, int z) {
        return frame(world).block(x, y, z);
    }

    public static boolean metadataApplies(World world, int x, int y, int z) {
        return frame(world) != null;
    }

    public static int metadata(World world, int x, int y, int z) {
        return frame(world).metadata(x, y, z);
    }

    public static boolean setBlockApplies(World world, int x, int y, int z, Block block, int metadata, int flags) {
        return frame(world) != null;
    }

    public static boolean setBlock(World world, int x, int y, int z, Block block, int metadata, int flags) {
        return frame(world).set(x, y, z, block, metadata);
    }

    public static boolean setMetadataApplies(World world, int x, int y, int z, int metadata, int flags) {
        return frame(world) != null;
    }

    /** A metadata write keeps the block the capture holds there now. */
    public static boolean setMetadata(World world, int x, int y, int z, int metadata, int flags) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame.set(x, y, z, frame.block(x, y, z), metadata);
    }

    public static boolean heightApplies(World world, int x, int z) {
        return frame(world) != null;
    }

    public static int height(World world, int x, int z) {
        return frame(world).heightValue(x, z);
    }

    public static boolean topSolidOrLiquidApplies(World world, int x, int z) {
        return frame(world) != null;
    }

    public static int topSolidOrLiquid(World world, int x, int z) {
        return frame(world).topSolidOrLiquid(x, z);
    }

    /** Isolated (ecology) captures keep their own tiles; boxed ones read the box's. */
    public static boolean tileApplies(World world, int x, int y, int z) {
        return isolated(world) != null || boxed(world) != null;
    }

    public static TileEntity tile(World world, int x, int y, int z) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame.isolated() ? frame.tileEntity(x, y, z) : frame.boxedTile(x, y, z);
    }

    public static boolean spawnApplies(World world, Entity entity) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame != null && (frame.boxed() || frame.isolated() && frame.entitySpawns());
    }

    public static boolean spawn(World world, Entity entity) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame.boxed() ? frame.boxedSpawn(entity) : frame.spawnEntity(entity);
    }

    /**
     * A box-confined generator gets a column's biome from the biome map, never from a loaded chunk (whether the
     * column's chunk was loaded yet depended on timing); an isolated surface capture answers its own.
     */
    public static boolean biomeApplies(World world, int x, int z) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame != null && (frame.boxed() || frame.isolated() && frame.biomeSurface());
    }

    public static BiomeGenBase biome(World world, int x, int z) {
        NativeFeatureCapture.Frame frame = frame(world);
        return frame.boxed() ? world.getWorldChunkManager()
            .getBiomeGenAt(x, z) : frame.biome(x, z);
    }

    public static boolean setTileApplies(World world, int x, int y, int z, TileEntity tile) {
        return isolated(world) != null;
    }

    public static void setTile(World world, int x, int y, int z, TileEntity tile) {
        isolated(world).tileMutation(x, y, z, tile);
    }

    public static boolean removeTileApplies(World world, int x, int y, int z) {
        return isolated(world) != null;
    }

    public static void removeTile(World world, int x, int y, int z) {
        isolated(world).tileMutation(x, y, z, null);
    }

    public static boolean savedLightApplies(World world, EnumSkyBlock kind, int x, int y, int z) {
        return isolated(world) != null;
    }

    public static int savedLight(World world, EnumSkyBlock kind, int x, int y, int z) {
        return isolated(world).savedLight(kind, x, y, z);
    }

    public static boolean surfaceLightApplies(World world, int x, int y, int z) {
        NativeFeatureCapture.Frame frame = isolated(world);
        return frame != null && frame.surface();
    }

    public static int surfaceLight(World world, int x, int y, int z) {
        return isolated(world).fullLight(x, y, z);
    }

    public static boolean seesSkyApplies(World world, int x, int y, int z) {
        NativeFeatureCapture.Frame frame = isolated(world);
        return frame != null && frame.surface();
    }

    public static boolean seesSky(World world, int x, int y, int z) {
        return isolated(world).seesSky(x, y, z);
    }

    public static boolean freezableApplies(World world, int x, int y, int z) {
        NativeFeatureCapture.Frame frame = isolated(world);
        return frame != null && frame.biomeSurface();
    }

    public static boolean freezable(World world, int x, int y, int z) {
        return isolated(world).freezable(x, y, z);
    }

    public static boolean sideSolidApplies(World world, int x, int y, int z, ForgeDirection side, boolean fallback) {
        return isolated(world) != null;
    }

    public static boolean sideSolid(World world, int x, int y, int z, ForgeDirection side, boolean fallback) {
        return isolated(world).block(x, y, z)
            .isSideSolid(world, x, y, z, side);
    }

    /** Isolated captures never mark blocks for update or tile chunks modified, nor relight. */
    public static boolean markUpdateApplies(World world, int x, int y, int z) {
        return isolated(world) != null;
    }

    public static void markUpdate(World world, int x, int y, int z) {}

    public static boolean markTileChunkApplies(World world, int x, int y, int z, TileEntity tile) {
        return isolated(world) != null;
    }

    public static void markTileChunk(World world, int x, int y, int z, TileEntity tile) {}

    public static boolean relightApplies(World world, EnumSkyBlock kind, int x, int y, int z) {
        return isolated(world) != null;
    }

    public static boolean relight(World world, EnumSkyBlock kind, int x, int y, int z) {
        return true;
    }
}
