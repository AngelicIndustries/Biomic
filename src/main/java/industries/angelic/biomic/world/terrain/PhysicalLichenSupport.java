package industries.angelic.biomic.world.terrain;

import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/** Forge's actual face rule on a bounded, read-only physical block/metadata view. No World/chunk requests. */
public final class PhysicalLichenSupport {

    private static final String[] FACES = { "down", "up", "north", "south", "west", "east" };

    public record Cell(Block block, int metadata) {}

    @FunctionalInterface
    public interface Cells {

        Cell read(int x, int y, int z);
    }

    private PhysicalLichenSupport() {}

    public static int faces(Map<String, String> properties) {
        int result = 0;
        for (int side = 0; side < 6; side++) if (Boolean.parseBoolean(properties.get(FACES[side]))) result |= 1 << side;
        return result;
    }

    public static String state(int faces, Map<String, String> original) {
        if (faces <= 0 || faces > 63) throw new IllegalArgumentException("Invalid lichen faces");
        Map<String, String> properties = new java.util.LinkedHashMap<>(original);
        for (int side = 0; side < 6; side++) properties.put(FACES[side], Boolean.toString((faces & (1 << side)) != 0));
        StringBuilder state = new StringBuilder("minecraft:glow_lichen[");
        for (var property : properties.entrySet()) {
            if (state.charAt(state.length() - 1) != '[') state.append(',');
            state.append(property.getKey())
                .append('=')
                .append(property.getValue());
        }
        return state.append(']')
            .toString();
    }

    public static int supported(int faces, int x, int y, int z, int height, Cells cells) {
        if (faces < 0 || faces > 63 || height <= 0 || y < 0 || y >= height)
            throw new IllegalArgumentException("Invalid lichen support query");
        var view = new IBlockAccess() {

            private int reads;

            private Cell read(int cx, int cy, int cz) {
                if (++reads > 128 || Math.abs((long) cx - x) > 2
                    || Math.abs((long) cy - y) > 2
                    || Math.abs((long) cz - z) > 2) throw new UnsupportedOperationException();
                return cells.read(cx, cy, cz);
            }

            public Block getBlock(int cx, int cy, int cz) {
                return read(cx, cy, cz).block();
            }

            public int getBlockMetadata(int cx, int cy, int cz) {
                return read(cx, cy, cz).metadata();
            }

            public boolean isAirBlock(int cx, int cy, int cz) {
                return getBlock(cx, cy, cz).isAir(this, cx, cy, cz);
            }

            public boolean isSideSolid(int cx, int cy, int cz, ForgeDirection side, boolean fallback) {
                return cy >= 0 && cy < height && getBlock(cx, cy, cz).isSideSolid(this, cx, cy, cz, side);
            }

            public TileEntity getTileEntity(int cx, int cy, int cz) {
                throw new UnsupportedOperationException();
            }

            public int getLightBrightnessForSkyBlocks(int cx, int cy, int cz, int light) {
                throw new UnsupportedOperationException();
            }

            public int isBlockProvidingPowerTo(int cx, int cy, int cz, int side) {
                throw new UnsupportedOperationException();
            }

            public BiomeGenBase getBiomeGenForCoords(int cx, int cz) {
                throw new UnsupportedOperationException();
            }

            public int getHeight() {
                return height;
            }

            public boolean extendedLevelsInChunkCache() {
                throw new UnsupportedOperationException();
            }
        };
        int result = 0;
        for (var d : ForgeDirection.VALID_DIRECTIONS) if ((faces & (1 << d.ordinal())) != 0) {
            try {
                if (view.isSideSolid(x + d.offsetX, y + d.offsetY, z + d.offsetZ, d.getOpposite(), false))
                    result |= 1 << d.ordinal();
            } catch (UnsupportedOperationException unsupported) {
                // Tile/light/biome-dependent or unbounded support cannot be proven by the physical transport.
            }
        }
        return result;
    }
}
