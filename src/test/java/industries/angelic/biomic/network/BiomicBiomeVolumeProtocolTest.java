package industries.angelic.biomic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;

public class BiomicBiomeVolumeProtocolTest {

    @Test
    public void markerDeclaresExactIndependentProtocolRevision() {
        Mod marker = BiomicBiomeVolumeProtocol.class.getAnnotation(Mod.class);
        assertNotNull(marker);
        assertEquals(BiomicPeerRequirements.BIOME_VOLUME_MARKER, marker.modid());
        assertEquals(BiomicPeerRequirements.BIOME_VOLUME_REVISION, marker.version());
        assertEquals("*", marker.acceptableRemoteVersions());
        assertFalse(marker.useMetadata());
        try {
            var handler = BiomicBiomeVolumeProtocol.class.getDeclaredMethod("compatible", Map.class, Side.class);
            assertNotNull(handler.getAnnotation(NetworkCheckHandler.class));
            assertEquals(boolean.class, handler.getReturnType());
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    @Test
    public void lifecycleFailsClosedPublishesSchemaOneAndClearsOnStop() {
        var marker = new BiomicBiomeVolumeProtocol();
        assertTrue(marker.compatible(Map.of(), Side.SERVER));
        assertFalse(marker.compatible(Map.of(), Side.CLIENT));
        marker.aboutToStart(null);
        assertFalse(marker.compatible(Map.of(), Side.CLIENT));
        marker.publishValidatedRequirements(1, 256);
        assertTrue(marker.compatible(Map.of(), Side.CLIENT));
        marker.stopped(null);
        assertFalse(marker.compatible(Map.of(), Side.CLIENT));
    }

    @Test
    public void physical512RequiresExactHeightMarkerButNeverSchemaTwoMarker() {
        BiomicBiomeVolumeProtocol.validateLocalHeightProtocol(256, null);
        assertThrows(
            IllegalStateException.class,
            () -> BiomicBiomeVolumeProtocol.validateLocalHeightProtocol(512, null));
        assertThrows(
            IllegalStateException.class,
            () -> BiomicBiomeVolumeProtocol.validateLocalHeightProtocol(512, "0.0.1"));
        BiomicBiomeVolumeProtocol.validateLocalHeightProtocol(512, BiomicPeerRequirements.HEIGHT_512_REVISION);

        var marker = new BiomicBiomeVolumeProtocol();
        marker.aboutToStart(null);
        marker.publishValidatedRequirements(1, 512);
        assertFalse(marker.compatible(Map.of(), Side.CLIENT));
        assertFalse(marker.compatible(Map.of(BiomicPeerRequirements.HEIGHT_MARKER, "wrong"), Side.CLIENT));
        assertTrue(
            marker.compatible(
                Map.of(BiomicPeerRequirements.HEIGHT_MARKER, BiomicPeerRequirements.HEIGHT_512_REVISION),
                Side.CLIENT));
        assertThrows(
            IllegalArgumentException.class,
            () -> BiomicBiomeVolumeProtocol
                .validateLocalHeightProtocol(384, BiomicPeerRequirements.HEIGHT_512_REVISION));
    }

    @Test
    public void repeatedOrOutOfSessionPublicationCannotChangePolicy() {
        var marker = new BiomicBiomeVolumeProtocol();
        assertThrows(IllegalStateException.class, () -> marker.publishValidatedRequirements(1, 256));
        marker.aboutToStart(null);
        marker.publishValidatedRequirements(1, 256);
        marker.publishValidatedRequirements(1, 256);
        assertThrows(IllegalStateException.class, () -> marker.publishValidatedRequirements(1, 512));
        assertTrue(marker.compatible(Map.of(), Side.CLIENT));
    }

    @Test
    public void tallPolicyRequiresBothExactMarkersAndRevokesWriterAdmissionOnStop() {
        var marker = new BiomicBiomeVolumeProtocol();
        var previous = BiomicBiomeVolumeProtocol.INSTANCE;
        try {
            BiomicBiomeVolumeProtocol.INSTANCE = marker;
            marker.aboutToStart(null);
            assertFalse(BiomicBiomeVolumeProtocol.schemaTwoSessionPublished());
            assertThrows(IllegalArgumentException.class, () -> marker.publishValidatedRequirements(2, 256));
            marker.publishValidatedRequirements(2, 512);
            assertTrue(BiomicBiomeVolumeProtocol.schemaTwoSessionPublished());
            assertFalse(marker.compatible(Map.of(), Side.CLIENT));
            assertFalse(
                marker.compatible(
                    Map.of(BiomicPeerRequirements.HEIGHT_MARKER, BiomicPeerRequirements.HEIGHT_512_REVISION),
                    Side.CLIENT));
            assertFalse(marker.compatible(Map.of(BiomicPeerRequirements.BIOME_VOLUME_MARKER, "2"), Side.CLIENT));
            assertFalse(
                marker.compatible(
                    Map.of(
                        BiomicPeerRequirements.BIOME_VOLUME_MARKER,
                        "1",
                        BiomicPeerRequirements.HEIGHT_MARKER,
                        BiomicPeerRequirements.HEIGHT_512_REVISION),
                    Side.CLIENT));
            assertTrue(
                marker.compatible(
                    Map.of(
                        BiomicPeerRequirements.BIOME_VOLUME_MARKER,
                        "2",
                        BiomicPeerRequirements.HEIGHT_MARKER,
                        BiomicPeerRequirements.HEIGHT_512_REVISION),
                    Side.CLIENT));
            marker.stopped(null);
            assertFalse(BiomicBiomeVolumeProtocol.schemaTwoSessionPublished());
            marker.aboutToStart(null);
            marker.publishValidatedRequirements(1, 512);
            assertFalse(BiomicBiomeVolumeProtocol.schemaTwoSessionPublished());
        } finally {
            BiomicBiomeVolumeProtocol.INSTANCE = previous;
        }
    }
}
