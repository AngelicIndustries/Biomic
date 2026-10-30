package industries.angelic.biomic.network;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class BiomicPeerRequirementsTest {

    private static final BiomicPeerRequirements.Direction SERVER = BiomicPeerRequirements.Direction.SERVER_CHECKING_CLIENT;
    private static final BiomicPeerRequirements.Capabilities NONE = BiomicPeerRequirements.Capabilities.of(null, null);

    @Test
    public void serverDecisionTableKeepsSchemaAndHeightIndependent() {
        record Case(BiomicPeerRequirements.Requirements requirements, BiomicPeerRequirements.Capabilities capabilities,
            boolean accepted) {}
        var schema = BiomicPeerRequirements.Capabilities.of("2", null);
        var height = BiomicPeerRequirements.Capabilities.of(null, "height512-v1");
        var both = BiomicPeerRequirements.Capabilities.of("2", "height512-v1");
        List<Case> cases = List.of(
            new Case(BiomicPeerRequirements.Requirements.of(1, 256), NONE, true),
            new Case(BiomicPeerRequirements.Requirements.of(2, 256), NONE, false),
            new Case(BiomicPeerRequirements.Requirements.of(2, 256), schema, true),
            new Case(BiomicPeerRequirements.Requirements.of(1, 512), NONE, false),
            new Case(BiomicPeerRequirements.Requirements.of(1, 512), height, true),
            new Case(BiomicPeerRequirements.Requirements.of(2, 512), schema, false),
            new Case(BiomicPeerRequirements.Requirements.of(2, 512), height, false),
            new Case(BiomicPeerRequirements.Requirements.of(2, 512), both, true));
        for (Case value : cases) assertEquals(
            value.toString(),
            value.accepted(),
            BiomicPeerRequirements.accepts(SERVER, value.requirements(), value.capabilities()));
    }

    @Test
    public void wrongRevisionsAreRejected() {
        var required = BiomicPeerRequirements.Requirements.of(2, 512);
        assertFalse(
            BiomicPeerRequirements.accepts(
                SERVER,
                required,
                BiomicPeerRequirements.Capabilities.of("dirty-same-mod-version", "height512-v1")));
        assertFalse(
            BiomicPeerRequirements.accepts(SERVER, required, BiomicPeerRequirements.Capabilities.of("2", "0.0.1")));
        assertFalse(
            BiomicPeerRequirements.accepts(SERVER, required, BiomicPeerRequirements.Capabilities.of("2", null)));
    }

    @Test
    public void uninitializedServerFailsClosedButClientInspectionIsPermissive() {
        var uninitialized = BiomicPeerRequirements.Requirements.uninitialized();
        assertFalse(BiomicPeerRequirements.accepts(SERVER, uninitialized, NONE));
        assertTrue(
            BiomicPeerRequirements
                .accepts(BiomicPeerRequirements.Direction.CLIENT_INSPECTING_SERVER, uninitialized, NONE));
    }

    @Test
    public void valueObjectsRejectAmbiguousInvalidState() {
        assertThrows(IllegalArgumentException.class, () -> new BiomicPeerRequirements.Requirements(false, 1, 256));
        assertThrows(IllegalArgumentException.class, () -> BiomicPeerRequirements.Requirements.of(3, 512));
        assertThrows(IllegalArgumentException.class, () -> BiomicPeerRequirements.Requirements.of(2, 384));
        assertThrows(
            NullPointerException.class,
            () -> new BiomicPeerRequirements.Capabilities(null, java.util.Optional.empty()));
    }
}
