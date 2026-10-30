package industries.angelic.biomic.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

/** Backward-identical schema-one codec plus explicit-height schema-two support; admission is manager-owned. */
final class BiomeVolumeCodec {

    static final int SCHEMA_VERSION = 1, SCHEMA2_VERSION = 2;
    static final int MAX_PACKED_BYTES = packedByteCount(
        BiomeVolume.MAX_CELL_COUNT,
        bitsForPalette(BiomeVolume.MAX_PALETTE_ENTRIES));
    static final int MAX_PACKET_PAYLOAD = 1 + Integer.BYTES
        + 1
        + Short.BYTES
        + BiomeVolume.MAX_PALETTE_ENTRIES * (Short.BYTES + BiomeVolume.MAX_KEY_BYTES)
        + 1
        + Short.BYTES
        + MAX_PACKED_BYTES;

    private static final String SCHEMA_KEY = "schema";
    private static final String GENERATOR_KEY = "generator";
    private static final String HEIGHT_KEY = "height";
    private static final String PALETTE_KEY = "palette";
    private static final String BITS_KEY = "bits";
    private static final String DATA_KEY = "data";

    private BiomeVolumeCodec() {}

    static void writeNbt(BiomeVolume volume, NBTTagCompound nbt) {
        short[] indices = volume.copyPaletteIndices();
        int bits = bitsForPalette(
            volume.getPalette()
                .size());

        int schema = volume.getBlockHeight() == BiomeVolume.BLOCK_HEIGHT ? SCHEMA_VERSION : SCHEMA2_VERSION;
        nbt.setInteger(SCHEMA_KEY, schema);
        if (schema == SCHEMA2_VERSION) nbt.setInteger(HEIGHT_KEY, volume.getBlockHeight());
        else nbt.removeTag(HEIGHT_KEY);
        nbt.setInteger(GENERATOR_KEY, volume.getGeneratorVersion());
        NBTTagList paletteTag = new NBTTagList();
        for (String key : volume.getPalette()) {
            paletteTag.appendTag(new NBTTagString(key));
        }
        nbt.setTag(PALETTE_KEY, paletteTag);
        nbt.setByte(BITS_KEY, (byte) bits);
        nbt.setByteArray(DATA_KEY, pack(indices, bits));
    }

    static BiomeVolume readNbt(NBTTagCompound nbt) {
        require(nbt.hasKey(SCHEMA_KEY, Constants.NBT.TAG_INT), "Missing biome-volume schema");
        int schema = nbt.getInteger(SCHEMA_KEY);
        require(schema == SCHEMA_VERSION || schema == SCHEMA2_VERSION, "Unsupported biome-volume schema");
        require(nbt.hasKey(GENERATOR_KEY, Constants.NBT.TAG_INT), "Missing biome-volume generator version");
        require(nbt.hasKey(PALETTE_KEY, Constants.NBT.TAG_LIST), "Missing biome-volume palette");
        require(nbt.hasKey(BITS_KEY, Constants.NBT.TAG_BYTE), "Missing biome-volume bit width");
        require(nbt.hasKey(DATA_KEY, Constants.NBT.TAG_BYTE_ARRAY), "Missing biome-volume packed data");

        NBTTagList paletteTag = nbt.getTagList(PALETTE_KEY, Constants.NBT.TAG_STRING);
        require(
            paletteTag.tagCount() >= 1 && paletteTag.tagCount() <= BiomeVolume.MAX_PALETTE_ENTRIES,
            "Invalid biome palette size");
        List<String> palette = new ArrayList<>(paletteTag.tagCount());
        for (int index = 0; index < paletteTag.tagCount(); index++) {
            palette.add(paletteTag.getStringTagAt(index));
        }

        int height = schema == SCHEMA_VERSION ? BiomeVolume.BLOCK_HEIGHT : requiredHeight(nbt);
        int cells = height / 4 * BiomeVolume.QUART_WIDTH * BiomeVolume.QUART_WIDTH, bits = nbt.getByte(BITS_KEY) & 0xFF;
        validatePackedShape(palette.size(), bits, nbt.getByteArray(DATA_KEY).length, cells);
        short[] indices = unpack(nbt.getByteArray(DATA_KEY), bits, cells);
        return BiomeVolume.create(nbt.getInteger(GENERATOR_KEY), height, palette, indices);
    }

    static void writePacket(BiomeVolume volume, ByteBuffer buffer) {
        List<String> palette = volume.getPalette();
        short[] indices = volume.copyPaletteIndices();
        int bits = bitsForPalette(palette.size());
        byte[] packed = pack(indices, bits);

        int schema = volume.getBlockHeight() == BiomeVolume.BLOCK_HEIGHT ? SCHEMA_VERSION : SCHEMA2_VERSION;
        buffer.put((byte) schema);
        if (schema == SCHEMA2_VERSION) buffer.putShort((short) volume.getBlockHeight());
        buffer.putInt(volume.getGeneratorVersion());
        buffer.put((byte) palette.size());
        for (String key : palette) {
            byte[] encodedKey = key.getBytes(StandardCharsets.UTF_8);
            buffer.putShort((short) encodedKey.length);
            buffer.put(encodedKey);
        }
        buffer.put((byte) bits);
        buffer.putShort((short) packed.length);
        buffer.put(packed);
    }

    static BiomeVolume readPacket(ByteBuffer buffer) {
        require(buffer != null && buffer.remaining() >= 1, "Truncated biome-volume packet header");
        int schema = buffer.get() & 0xFF;
        require(schema == SCHEMA_VERSION || schema == SCHEMA2_VERSION, "Unsupported biome-volume packet schema");
        require(
            buffer.remaining() >= (schema == SCHEMA2_VERSION ? Short.BYTES : 0) + Integer.BYTES + 1,
            "Truncated biome-volume packet header");
        int height = schema == SCHEMA_VERSION ? BiomeVolume.BLOCK_HEIGHT : (buffer.getShort() & 0xFFFF);
        if (schema == SCHEMA2_VERSION)
            require(height == BiomeVolume.FULL_BLOCK_HEIGHT, "Invalid schema-2 biome-volume height");
        int generatorVersion = buffer.getInt();
        int paletteSize = buffer.get() & 0xFF;
        require(paletteSize >= 1 && paletteSize <= BiomeVolume.MAX_PALETTE_ENTRIES, "Invalid biome palette size");

        List<String> palette = new ArrayList<>(paletteSize);
        for (int index = 0; index < paletteSize; index++) {
            require(buffer.remaining() >= Short.BYTES, "Truncated biome palette key length");
            int byteLength = buffer.getShort() & 0xFFFF;
            require(byteLength >= 1 && byteLength <= BiomeVolume.MAX_KEY_BYTES, "Invalid biome palette key length");
            require(buffer.remaining() >= byteLength, "Truncated biome palette key");
            byte[] encodedKey = new byte[byteLength];
            buffer.get(encodedKey);
            palette.add(new String(encodedKey, StandardCharsets.UTF_8));
        }

        require(buffer.remaining() >= 1 + Short.BYTES, "Truncated biome-volume packed header");
        int bits = buffer.get() & 0xFF;
        int packedLength = buffer.getShort() & 0xFFFF;
        int cells = height / 4 * BiomeVolume.QUART_WIDTH * BiomeVolume.QUART_WIDTH;
        validatePackedShape(paletteSize, bits, packedLength, cells);
        require(buffer.remaining() >= packedLength, "Truncated biome-volume packed data");
        byte[] packed = new byte[packedLength];
        buffer.get(packed);
        return BiomeVolume.create(generatorVersion, height, palette, unpack(packed, bits, cells));
    }

    static void clearNbt(NBTTagCompound nbt) {
        nbt.removeTag(SCHEMA_KEY);
        nbt.removeTag(GENERATOR_KEY);
        nbt.removeTag(HEIGHT_KEY);
        nbt.removeTag(PALETTE_KEY);
        nbt.removeTag(BITS_KEY);
        nbt.removeTag(DATA_KEY);
    }

    private static byte[] pack(short[] indices, int bits) {
        byte[] packed = new byte[packedByteCount(indices.length, bits)];
        if (bits == 0) {
            return packed;
        }

        int bitOffset = 0;
        for (short paletteIndex : indices) {
            int value = paletteIndex & 0xFFFF;
            for (int bit = 0; bit < bits; bit++, bitOffset++) {
                if ((value & 1 << bit) != 0) {
                    packed[bitOffset >>> 3] |= (byte) (1 << (bitOffset & 7));
                }
            }
        }
        return packed;
    }

    private static short[] unpack(byte[] packed, int bits, int cells) {
        short[] indices = new short[cells];
        if (bits == 0) {
            return indices;
        }

        int bitOffset = 0;
        for (int cell = 0; cell < indices.length; cell++) {
            int value = 0;
            for (int bit = 0; bit < bits; bit++, bitOffset++) {
                if ((packed[bitOffset >>> 3] & 1 << (bitOffset & 7)) != 0) {
                    value |= 1 << bit;
                }
            }
            indices[cell] = (short) value;
        }
        return indices;
    }

    private static void validatePackedShape(int paletteSize, int bits, int packedLength, int cells) {
        require(paletteSize >= 1 && paletteSize <= BiomeVolume.MAX_PALETTE_ENTRIES, "Invalid biome palette size");
        int expectedBits = bitsForPalette(paletteSize);
        require(bits == expectedBits, "Biome-volume bit width does not match its palette");
        require(
            cells >= BiomeVolume.CELL_COUNT && cells <= BiomeVolume.MAX_CELL_COUNT
                && packedLength == packedByteCount(cells, bits),
            "Biome-volume packed length does not match its bit width");
    }

    private static int bitsForPalette(int paletteSize) {
        return paletteSize <= 1 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(paletteSize - 1);
    }

    private static int packedByteCount(int entryCount, int bits) {
        return (entryCount * bits + Byte.SIZE - 1) / Byte.SIZE;
    }

    private static int requiredHeight(NBTTagCompound nbt) {
        require(nbt.hasKey(HEIGHT_KEY, Constants.NBT.TAG_INT), "Missing schema-2 biome-volume height");
        int height = nbt.getInteger(HEIGHT_KEY);
        require(height == BiomeVolume.FULL_BLOCK_HEIGHT, "Invalid schema-2 biome-volume height");
        return height;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
