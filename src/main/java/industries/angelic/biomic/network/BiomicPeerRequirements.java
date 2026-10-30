package industries.angelic.biomic.network;

import java.util.Objects;
import java.util.Optional;

/** Pure peer-capability decision. Registration and lifecycle publication are intentionally external. */
public final class BiomicPeerRequirements {

    public static final String BIOME_VOLUME_MARKER = "biomic_biome_volume_protocol";
    public static final String BIOME_VOLUME_REVISION = "2";
    public static final String HEIGHT_MARKER = "hohenangst_height_protocol";
    public static final String HEIGHT_512_REVISION = "height512-v1";

    public enum Direction {
        SERVER_CHECKING_CLIENT,
        CLIENT_INSPECTING_SERVER
    }

    public record Requirements(boolean initialized, int biomeVolumeSchema, int requiredPhysicalHeight) {

        public Requirements {
            if (!initialized) {
                if (biomeVolumeSchema != 0 || requiredPhysicalHeight != 0)
                    throw new IllegalArgumentException("Uninitialized requirements cannot carry world policy");
            } else if ((biomeVolumeSchema != 1 && biomeVolumeSchema != 2)
                || (requiredPhysicalHeight != 256 && requiredPhysicalHeight != 512))
                throw new IllegalArgumentException("Unsupported peer requirements");
        }

        public static Requirements uninitialized() {
            return new Requirements(false, 0, 0);
        }

        public static Requirements of(int schema, int height) {
            return new Requirements(true, schema, height);
        }
    }

    public record Capabilities(Optional<String> biomeVolumeRevision, Optional<String> heightRevision) {

        public Capabilities {
            Objects.requireNonNull(biomeVolumeRevision, "biomeVolumeRevision");
            Objects.requireNonNull(heightRevision, "heightRevision");
        }

        public static Capabilities of(String biomeVolumeRevision, String heightRevision) {
            return new Capabilities(Optional.ofNullable(biomeVolumeRevision), Optional.ofNullable(heightRevision));
        }
    }

    private BiomicPeerRequirements() {}

    /** Whether the remote side may connect: a client must carry the protocols the server's world needs. */
    public static boolean accepts(Direction direction, Requirements localServer, Capabilities remote) {
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(localServer, "localServer");
        Objects.requireNonNull(remote, "remote");
        if (direction == Direction.CLIENT_INSPECTING_SERVER) return true;
        if (!localServer.initialized()) return false;
        if (localServer.biomeVolumeSchema() == 2 && !remote.biomeVolumeRevision()
            .filter(BIOME_VOLUME_REVISION::equals)
            .isPresent()) return false;
        return localServer.requiredPhysicalHeight() != 512 || remote.heightRevision()
            .filter(HEIGHT_512_REVISION::equals)
            .isPresent();
    }
}
