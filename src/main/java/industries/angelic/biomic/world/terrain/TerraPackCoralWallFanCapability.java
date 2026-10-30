package industries.angelic.biomic.world.terrain;

import java.util.LinkedHashMap;
import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraftforge.common.util.ForgeDirection;

import industries.angelic.biomic.content.BiomicCoralWallFans;
import industries.angelic.biomic.content.block.BlockCoralWallFan;

/** Exact modern warm-ocean wall model; historical modern wall-to-floor aliases are not changed here. */
public final class TerraPackCoralWallFanCapability {

    public record NativeState(net.minecraft.block.Block block, int metadata) {

        public NativeState {
            java.util.Objects.requireNonNull(block);
        }
    }

    private TerraPackCoralWallFanCapability() {}

    public record Representation(int species, boolean dead, ForgeDirection facing, boolean waterlogged) {

        public Representation {
            BlockCoralWallFan.speciesName(species);
            BlockCoralWallFan.facingMetadata(facing);
        }

        public int metadata() {
            return BlockCoralWallFan.facingMetadata(facing) | (dead ? 4 : 0);
        }

        public String modernState() {
            return "minecraft:" + (dead ? "dead_" : "")
                + BlockCoralWallFan.speciesName(species)
                + "_coral_wall_fan[facing="
                + facing.name()
                    .toLowerCase(Locale.ROOT)
                + ",waterlogged="
                + waterlogged
                + "]";
        }

        public String materializationState() {
            return "biomic:" + BlockCoralWallFan.speciesName(species)
                + "_coral_wall_fan[facing="
                + facing.name()
                    .toLowerCase(Locale.ROOT)
                + ",dead="
                + dead
                + ",waterlogged="
                + waterlogged
                + "]";
        }
    }

    public static boolean authored(String state) {
        return state != null && state.contains("_coral_wall_fan")
            && (state.startsWith("minecraft:") || state.startsWith("biomic:"));
    }

    public static Representation parse(String state) {
        if (state == null || !state.endsWith("]"))
            throw new IllegalArgumentException("Explicit wall facing/water state required");
        int bracket = state.indexOf('[');
        if (bracket < 0) throw new IllegalArgumentException("Explicit wall facing/water state required");
        String base = state.substring(0, bracket);
        boolean alias = base.startsWith("biomic:");
        var props = new LinkedHashMap<String, String>();
        for (String pair : state.substring(bracket + 1, state.length() - 1)
            .split(",", -1)) {
            String[] parts = pair.split("=", -1);
            if (parts.length != 2 || props.putIfAbsent(parts[0], parts[1]) != null)
                throw new IllegalArgumentException("Malformed wall fan properties");
        }
        if (props.size() != (alias ? 3 : 2) || !props.containsKey("facing")
            || !props.containsKey("waterlogged")
            || alias && !props.containsKey("dead")) throw new IllegalArgumentException("Unknown wall fan properties");
        boolean wet = bool(props.get("waterlogged"));
        boolean dead = alias ? bool(props.get("dead")) : base.startsWith("minecraft:dead_");
        String prefix = alias ? "biomic:" : dead ? "minecraft:dead_" : "minecraft:";
        int species = -1;
        for (int i = 0; i < 5; i++)
            if (base.equals(prefix + BlockCoralWallFan.speciesName(i) + "_coral_wall_fan")) species = i;
        ForgeDirection facing = switch (props.get("facing")) {
            case "north" -> ForgeDirection.NORTH;
            case "east" -> ForgeDirection.EAST;
            case "south" -> ForgeDirection.SOUTH;
            case "west" -> ForgeDirection.WEST;
            default -> throw new IllegalArgumentException("Horizontal wall fan required");
        };
        var result = new Representation(species, dead, facing, wet);
        if (alias && !result.materializationState()
            .equals(state)) throw new IllegalArgumentException("Noncanonical wall fan alias");
        return result;
    }

    private static boolean bool(String value) {
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        throw new IllegalArgumentException("Exact boolean required");
    }

    public static NativeState resolveInstalled(String authored) {
        var p = parse(authored);
        var state = new NativeState(BiomicCoralWallFans.get(p.species(), p.waterlogged()), p.metadata());
        requireRepresentation(authored, state);
        return state;
    }

    public static void requireRepresentation(String authored, Object resolved) {
        var p = parse(authored);
        if (!(resolved instanceof NativeState state)
            || state.block() != BiomicCoralWallFans.get(p.species(), p.waterlogged())
            || state.block()
                .getClass() != BlockCoralWallFan.class
            || state.metadata() != p.metadata()
            || state.block()
                .hasTileEntity(state.metadata())
            || !("biomic:" + BiomicCoralWallFans.registryName(p.species(), p.waterlogged()))
                .equals(Block.blockRegistry.getNameForObject(state.block()))
            || Block.blockRegistry.getObject("biomic:" + BiomicCoralWallFans.registryName(p.species(), p.waterlogged()))
                != state.block())
            throw new IllegalArgumentException("Exact installed wall fan required");
    }

    public static String modernState(Block block, int metadata) {
        if (!(block instanceof BlockCoralWallFan fan) || !BlockCoralWallFan.validMetadata(metadata)
            || BiomicCoralWallFans.get(fan.species(), fan.isWaterlogged()) != fan)
            throw new IllegalArgumentException("Unknown native wall fan");
        var p = new Representation(
            fan.species(),
            (metadata & 4) != 0,
            BlockCoralWallFan.facing(metadata),
            fan.isWaterlogged());
        requireRepresentation(p.modernState(), new NativeState(block, metadata));
        return p.modernState();
    }
}
