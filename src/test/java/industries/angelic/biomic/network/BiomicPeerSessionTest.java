package industries.angelic.biomic.network;

import static org.junit.Assert.*;

import org.junit.Test;

public class BiomicPeerSessionTest {

    private static final BiomicPeerRequirements.Requirements LEGACY = BiomicPeerRequirements.Requirements.of(1, 256);
    private static final BiomicPeerRequirements.Requirements EXPANDED = BiomicPeerRequirements.Requirements.of(2, 512);

    @Test
    public void beginResetsPublicationAndRestartUsesMonotonicToken() {
        var session = new BiomicPeerSession();
        assertUninitialized(session.snapshot(), 0, false);
        long first = session.beginSession();
        assertEquals(1, first);
        assertUninitialized(session.snapshot(), first, true);
        assertEquals(BiomicPeerSession.PublicationResult.PUBLISHED, session.publish(first, LEGACY));
        long second = session.beginSession();
        assertEquals(2, second);
        assertUninitialized(session.snapshot(), second, true);
        assertEquals(BiomicPeerSession.PublicationResult.REJECTED_STALE_TOKEN, session.publish(first, LEGACY));
    }

    @Test
    public void publicationIsOneShotButIdenticalRepeatIsIdempotent() {
        var session = new BiomicPeerSession();
        long token = session.beginSession();
        assertEquals(BiomicPeerSession.PublicationResult.PUBLISHED, session.publish(token, EXPANDED));
        assertEquals(BiomicPeerSession.PublicationResult.ALREADY_PUBLISHED_IDENTICAL, session.publish(token, EXPANDED));
        assertEquals(BiomicPeerSession.PublicationResult.REJECTED_CONFLICT, session.publish(token, LEGACY));
        assertEquals(
            EXPANDED,
            session.snapshot()
                .requirements());
        assertTrue(
            session.snapshot()
                .published());
    }

    @Test
    public void uninitializedAndOutOfSessionPublicationFailClosed() {
        var session = new BiomicPeerSession();
        assertEquals(BiomicPeerSession.PublicationResult.REJECTED_NO_ACTIVE_SESSION, session.publish(0, LEGACY));
        long token = session.beginSession();
        assertEquals(
            BiomicPeerSession.PublicationResult.REJECTED_UNINITIALIZED_REQUIREMENTS,
            session.publish(token, BiomicPeerRequirements.Requirements.uninitialized()));
        assertUninitialized(session.snapshot(), token, true);
        assertFalse(
            session.accepts(
                BiomicPeerRequirements.Direction.SERVER_CHECKING_CLIENT,
                BiomicPeerRequirements.Capabilities.of(null, null)));
    }

    @Test
    public void staleStopCannotClearNewerSameJvmServerSession() {
        var session = new BiomicPeerSession();
        long oldToken = session.beginSession();
        assertEquals(BiomicPeerSession.PublicationResult.PUBLISHED, session.publish(oldToken, LEGACY));
        long currentToken = session.beginSession();
        assertEquals(BiomicPeerSession.PublicationResult.PUBLISHED, session.publish(currentToken, EXPANDED));
        assertFalse(session.endSession(oldToken));
        assertTrue(
            session.snapshot()
                .active());
        assertEquals(
            EXPANDED,
            session.snapshot()
                .requirements());
        assertTrue(session.endSession(currentToken));
        assertUninitialized(session.snapshot(), currentToken, false);
        assertFalse(session.endSession(currentToken));
    }

    @Test
    public void checkUsesPublishedSnapshotWithoutInventingCapabilities() {
        var session = new BiomicPeerSession();
        long token = session.beginSession();
        session.publish(token, EXPANDED);
        assertFalse(
            session.accepts(
                BiomicPeerRequirements.Direction.SERVER_CHECKING_CLIENT,
                BiomicPeerRequirements.Capabilities.of(null, null)));
        assertTrue(
            session.accepts(
                BiomicPeerRequirements.Direction.SERVER_CHECKING_CLIENT,
                BiomicPeerRequirements.Capabilities.of("2", "height512-v1")));
    }

    @Test
    public void tokenOverflowCannotWrapOrReplaceExistingSnapshot() {
        var session = new BiomicPeerSession(Long.MAX_VALUE);
        var before = session.snapshot();
        assertThrows(ArithmeticException.class, session::beginSession);
        assertSame(before, session.snapshot());
    }

    private static void assertUninitialized(BiomicPeerSession.Snapshot snapshot, long token, boolean active) {
        assertEquals(token, snapshot.token());
        assertEquals(active, snapshot.active());
        assertFalse(snapshot.published());
        assertEquals(BiomicPeerRequirements.Requirements.uninitialized(), snapshot.requirements());
    }
}
