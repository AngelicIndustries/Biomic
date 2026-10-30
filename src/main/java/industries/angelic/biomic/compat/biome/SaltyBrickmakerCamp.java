package industries.angelic.biomic.compat.biome;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;

/**
 * Bounded transaction around SaltyMod's native brickmaker-camp generator. The native generator still owns the
 * rotation, tents, campfire, drying place and ChestGenHooks loot; no partial structure reaches the physical world.
 */
public final class SaltyBrickmakerCamp {

    private static final int HORIZONTAL_LIMIT = 40;
    private static final int MAX_WRITES = 16_384;
    private static final ThreadLocal<String> LAST_REJECTION = new ThreadLocal<>();

    private SaltyBrickmakerCamp() {}

    /** One-shot server-thread diagnostic for the audit trace; ordinary generation never logs rejected camps. */
    public static String takeLastRejection() {
        String result = LAST_REJECTION.get();
        LAST_REJECTION.remove();
        return result;
    }

    static boolean insideHorizontalEnvelope(int originX, int originZ, int x, int z) {
        return Math.abs((long) x - originX) <= HORIZONTAL_LIMIT && Math.abs((long) z - originZ) <= HORIZONTAL_LIMIT;
    }

    public interface Surface {

        int minimumY();

        int maximumYExclusive();

        Block block(int x, int y, int z);

        int metadata(int x, int y, int z);

        int heightValue(int x, int z);

        /** Must cover habitat, collision/protection and the complete clearing/foundation envelope. */
        boolean allowsWrite(int x, int y, int z, Block before, int beforeMetadata, Block after, int afterMetadata);
    }

    public record Position(int x, int y, int z) {}

    public record Placement(String state, NBTTagCompound tileEntity) {

        public Placement {
            tileEntity = tileEntity == null ? null : (NBTTagCompound) tileEntity.copy();
        }

        @Override
        public NBTTagCompound tileEntity() {
            return tileEntity == null ? null : (NBTTagCompound) tileEntity.copy();
        }

        public boolean hasTileEntity() {
            return tileEntity != null;
        }
    }

    public record Plan(Map<Position, Placement> placements) {

        public Plan {
            placements = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(placements));
        }
    }

    public static Plan generate(Constructor<?> generatorConstructor, World world, Surface surface, Random random, int x,
        int y, int z) {
        if (!NativeFeatureCapture.installed())
            throw new IllegalStateException("Native feature capture hooks unavailable");
        LAST_REJECTION.remove();
        Map<Position, Cell> proposal = new LinkedHashMap<>();
        NativeFeatureCapture.IsolatedAccess capture = new NativeFeatureCapture.IsolatedAccess() {

            private void checkHorizontal(int px, int pz) {
                if (!insideHorizontalEnvelope(x, z, px, pz))
                    throw new OutsideCamp("horizontal-envelope", px, Integer.MIN_VALUE, pz);
            }

            private Cell cell(int px, int py, int pz) {
                return proposal.get(new Position(px, py, pz));
            }

            @Override
            public Block block(int px, int py, int pz) {
                checkHorizontal(px, pz);
                if (py < surface.minimumY() || py >= surface.maximumYExclusive()) return Blocks.air;
                Cell cell = cell(px, py, pz);
                return cell == null ? surface.block(px, py, pz) : cell.block;
            }

            @Override
            public int metadata(int px, int py, int pz) {
                checkHorizontal(px, pz);
                if (py < surface.minimumY() || py >= surface.maximumYExclusive()) return 0;
                Cell cell = cell(px, py, pz);
                return cell == null ? surface.metadata(px, py, pz) : cell.metadata;
            }

            @Override
            public int heightValue(int px, int pz) {
                checkHorizontal(px, pz);
                return surface.heightValue(px, pz);
            }

            @Override
            public int topSolidOrLiquid(int px, int pz) {
                return heightValue(px, pz);
            }

            @Override
            public boolean set(int px, int py, int pz, Block after, int afterMetadata) {
                checkHorizontal(px, pz);
                if (after == null || py < surface.minimumY()
                    || py >= surface.maximumYExclusive()
                    || proposal.size() >= MAX_WRITES)
                    throw new OutsideCamp(
                        after == null ? "null-block"
                            : proposal.size() >= MAX_WRITES ? "write-budget" : "vertical-envelope",
                        px,
                        py,
                        pz);
                Block before = block(px, py, pz);
                int beforeMetadata = metadata(px, py, pz);
                if (!surface.allowsWrite(px, py, pz, before, beforeMetadata, after, afterMetadata))
                    throw new OutsideCamp("write-policy", px, py, pz);
                // Resolve every state during proposal construction so an unsupported native block rejects atomically.
                try {
                    ModernStateCompatibility.resolve(encode(after, afterMetadata));
                } catch (RuntimeException unsupported) {
                    throw new OutsideCamp("unsupported-state", px, py, pz);
                }
                TileEntity tile = null;
                if (after.hasTileEntity(afterMetadata)) {
                    tile = after.createTileEntity(world, afterMetadata);
                    if (tile == null) throw new OutsideCamp("tile-construction", px, py, pz);
                    tile.setWorldObj(world);
                    tile.xCoord = px;
                    tile.yCoord = py;
                    tile.zCoord = pz;
                }
                proposal.put(new Position(px, py, pz), new Cell(after, afterMetadata, tile));
                return before != after || beforeMetadata != afterMetadata;
            }

            @Override
            public TileEntity tileEntity(int px, int py, int pz) {
                checkHorizontal(px, pz);
                Cell cell = cell(px, py, pz);
                return cell == null ? null : cell.tile;
            }

            @Override
            public int savedLight(EnumSkyBlock kind, int px, int py, int pz) {
                checkHorizontal(px, pz);
                return 0;
            }

            @Override
            public Random random() {
                return random;
            }
        };
        try {
            WorldGenerator generator = (WorldGenerator) generatorConstructor.newInstance();
            boolean generated = NativeFeatureCapture
                .run(world, capture, () -> generator.generate(world, random, x, y, z));
            if (!generated || proposal.isEmpty()) {
                LAST_REJECTION.set(!generated ? "native-ground-validation" : "empty-native-plan");
                return null;
            }
            Map<Position, Placement> result = new LinkedHashMap<>();
            for (Map.Entry<Position, Cell> entry : proposal.entrySet()) {
                Cell cell = entry.getValue();
                NBTTagCompound tile = null;
                if (cell.tile != null) {
                    tile = new NBTTagCompound();
                    cell.tile.writeToNBT(tile);
                }
                result.put(entry.getKey(), new Placement(encode(cell.block, cell.metadata), tile));
            }
            return new Plan(result);
        } catch (OutsideCamp rejected) {
            LAST_REJECTION.set(rejected.reason + "@" + rejected.x + "," + rejected.y + "," + rejected.z);
            return null;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot invoke native Salty brickmaker camp", error);
        }
    }

    static String encode(Block block, int metadata) {
        String nativeState = NativeBlockCatalogue.encode(block, metadata);
        if (nativeState != null) {
            NativeBlockCatalogue.encodeTerrainWrite(block, metadata);
            return nativeState;
        }
        String registry = block == null ? null : Block.blockRegistry.getNameForObject(block);
        String exactCampState = encodeVanillaCampState(registry, metadata);
        if (exactCampState != null) return exactCampState;
        return ModernStateCompatibility.modernState(block, metadata);
    }

    /** Lossless transport for every metadata-bearing vanilla state emitted by Salty's four camp components. */
    static String encodeVanillaCampState(String registry, int metadata) {
        if (registry == null) return null;
        boolean supported = switch (registry) {
            case "minecraft:wool" -> metadata == 0 || metadata == 7
                || metadata == 8
                || metadata == 12
                || metadata == 15;
            case "minecraft:bed" -> metadata >= 0 && metadata <= 3 || metadata >= 8 && metadata <= 11;
            case "minecraft:chest" -> metadata >= 2 && metadata <= 5;
            case "minecraft:fence_gate" -> metadata >= 0 && metadata <= 3;
            case "minecraft:hay_block" -> metadata == 0 || metadata == 4 || metadata == 8;
            case "minecraft:wooden_slab" -> metadata == 0;
            default -> false;
        };
        return supported ? registry + "[legacy_meta=" + metadata + "]" : null;
    }

    private record Cell(Block block, int metadata, TileEntity tile) {}

    private static final class OutsideCamp extends RuntimeException {

        final String reason;
        final int x, y, z;

        OutsideCamp(String reason, int x, int y, int z) {
            super(null, null, false, false);
            this.reason = reason;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }
}
