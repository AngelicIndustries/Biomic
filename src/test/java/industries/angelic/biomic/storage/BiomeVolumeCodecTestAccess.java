package industries.angelic.biomic.storage;

import java.nio.ByteBuffer;
import java.util.Arrays;

/** Package bridge for byte-exact codec assertions outside the storage test package. */
public final class BiomeVolumeCodecTestAccess {

    private BiomeVolumeCodecTestAccess() {}

    public static byte[] packet(BiomeVolume volume) {
        ByteBuffer buffer = ByteBuffer.allocate(BiomeVolumeCodec.MAX_PACKET_PAYLOAD);
        BiomeVolumeCodec.writePacket(volume, buffer);
        return Arrays.copyOf(buffer.array(), buffer.position());
    }
}
