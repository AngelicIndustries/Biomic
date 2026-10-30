package industries.angelic.biomic.world.terrain;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Pinned Terra Sponge-loader subset: offsets, X/Z/Y visitation and structure-void semantics.
 * State orientation remains as authored, matching Terra. Unsupported payloads fail closed.
 */
final class TerraPackSchematic {

    interface Sink {

        void put(int x, int y, int z, String state);
    }

    private final int width, height, length, offsetX, offsetY, offsetZ;
    private final String[] states;

    TerraPackSchematic(String encoded) {
        try {
            byte[] bytes = Base64.getDecoder()
                .decode(encoded);
            if (bytes.length > 16_777_216) throw new IllegalArgumentException("Schematic payload too large");
            NBTTagCompound root = CompressedStreamTools.readCompressed(new ByteArrayInputStream(bytes));
            int version = root.getInteger("Version");
            width = root.getShort("Width") & 65535;
            height = root.getShort("Height") & 65535;
            length = root.getShort("Length") & 65535;
            long volume = (long) width * height * length;
            if (width < 1 || height < 1
                || length < 1
                || width > 256
                || height > 512
                || length > 256
                || volume > 2_000_000) throw new IllegalArgumentException("Unsupported schematic dimensions");
            if (version == 2) {
                NBTTagCompound metadata = root.getCompoundTag("Metadata");
                boolean x = metadata.hasKey("WEOffsetX"), y = metadata.hasKey("WEOffsetY"),
                    z = metadata.hasKey("WEOffsetZ");
                if ((x || y || z) && !(x && y && z)) throw new IllegalArgumentException("Incomplete WorldEdit offset");
                offsetX = metadata.getInteger("WEOffsetX");
                offsetY = metadata.getInteger("WEOffsetY");
                offsetZ = metadata.getInteger("WEOffsetZ");
            } else if (version == 3) {
                int[] offset = root.getIntArray("Offset");
                if (offset.length != 3) throw new IllegalArgumentException("Invalid schematic offset");
                offsetX = offset[0];
                offsetY = offset[1];
                offsetZ = offset[2];
            } else throw new IllegalArgumentException("Unsupported schematic version " + version);
            if (Math.abs((long) offsetX) > 256 || Math.abs((long) offsetY) > 512 || Math.abs((long) offsetZ) > 256)
                throw new IllegalArgumentException("Excessive schematic offset");
            for (String payload : new String[] { "Entities", "BlockEntities", "TileEntities" })
                if (root.getTagList(payload, 10)
                    .tagCount() > 0)
                    throw new IllegalArgumentException("Schematic payload adapter required: " + payload);
            Map<Integer, String> palette = new HashMap<>();
            NBTTagCompound raw = root.getCompoundTag("Palette");
            @SuppressWarnings("unchecked")
            Set<String> keys = raw.func_150296_c();
            for (String state : keys) {
                int index = raw.getInteger(state);
                // The pinned Terra loader reads signed single bytes, not general VarInts. All accepted fixtures must
                // fit.
                if (index < 0 || index > 127 || !state.contains(":"))
                    throw new IllegalArgumentException("Unsupported source schematic palette");
                if (palette.put(index, state) != null)
                    throw new IllegalArgumentException("Duplicate schematic palette index");
            }
            byte[] data = root.getByteArray("BlockData");
            if (data.length != volume) throw new IllegalArgumentException("Schematic voxel count mismatch");
            states = new String[data.length];
            for (int i = 0; i < data.length; i++) {
                String state = palette.get((int) data[i]);
                if (state == null) throw new IllegalArgumentException("Unknown schematic palette index");
                states[i] = state.startsWith("minecraft:structure_void") ? null : state;
            }
        } catch (java.io.IOException error) {
            throw new IllegalArgumentException("Invalid compressed schematic", error);
        }
    }

    void generate(int ox, int oy, int oz, int rotation, Sink sink) {
        for (int x = 0; x < width; x++) for (int z = 0; z < length; z++) {
            int dx = x + offsetX, dz = z + offsetZ;
            int rx = switch (rotation) {
                case 0 -> dx;
                case 90 -> dz;
                case 180 -> -dx;
                case 270 -> -dz;
                default -> throw new IllegalArgumentException("Invalid rotation");
            };
            int rz = switch (rotation) {
                case 0 -> dz;
                case 90 -> -dx;
                case 180 -> -dz;
                case 270 -> dx;
                default -> throw new IllegalArgumentException("Invalid rotation");
            };
            for (int y = 0; y < height; y++) {
                String state = states[x + z * width + y * width * length];
                if (state != null) sink.put(ox + rx, oy + y + offsetY, oz + rz, state);
            }
        }
    }
}
