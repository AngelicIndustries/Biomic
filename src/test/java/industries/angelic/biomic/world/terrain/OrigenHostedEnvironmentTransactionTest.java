package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class OrigenHostedEnvironmentTransactionTest {

    @Test
    public void admittedCommitDoesNotTreatAnIdenticalFirstCellAsFailure() {
        var writes = new LinkedHashMap<Long, String>();
        writes.put(1L, "same");
        writes.put(2L, "different");
        AtomicInteger calls = new AtomicInteger();
        assertTrue(OrigenFeatureRuntime.commitAdmittedHostedEnvironmentWrites(writes, false, (key, value) -> {
            calls.incrementAndGet();
            return !value.equals("same");
        }));
        assertEquals(2, calls.get());
    }

    @Test
    public void boundaryRejectionCommitsNothing() {
        var writes = new LinkedHashMap<Long, String>();
        writes.put(1L, "outside");
        AtomicInteger calls = new AtomicInteger();
        assertFalse(OrigenFeatureRuntime.commitAdmittedHostedEnvironmentWrites(writes, true, (key, value) -> {
            calls.incrementAndGet();
            return true;
        }));
        assertEquals(0, calls.get());
    }

    @Test
    public void transactionHasExplicitProductionCap() {
        assertEquals(65_536, OrigenFeatureRuntime.HOSTED_ENVIRONMENT_TRANSACTION_CAP);
        java.util.Map<Long, String> oversized = new java.util.AbstractMap<>() {

            public int size() {
                return 65_537;
            }

            public java.util.Set<Entry<Long, String>> entrySet() {
                return java.util.Set.of();
            }
        };
        assertFalse(OrigenFeatureRuntime.commitAdmittedHostedEnvironmentWrites(oversized, false, (key, value) -> true));
    }

    @Test
    public void rejectionRestoresExistingAuthoredCellAndRemovesNewCellAfterRepeatedWrites() {
        var authored = new LinkedHashMap<Long, String>();
        authored.put(1L, "prior-feature");
        var journal = new OrigenFeatureRuntime.HostedAuthoredJournal<String>();
        assertTrue(journal.record(1L, () -> authored.get(1L)));
        authored.put(1L, "ore");
        assertTrue(journal.record(1L, () -> authored.get(1L)));
        authored.put(1L, "later-ore");
        assertTrue(journal.record(2L, () -> authored.get(2L)));
        authored.put(2L, "new-ore");
        journal.restore((p, s) -> {
            if (s == null) authored.remove(p);
            else authored.put(p, s);
        });
        assertEquals(java.util.Map.of(1L, "prior-feature"), authored);
    }

    @Test
    public void successfulProposalKeepsAuthoredAndPhysicalWritesAndUnexpectedErrorsPropagate() {
        var authored = new LinkedHashMap<Long, String>();
        var physical = new LinkedHashMap<Long, String>();
        var journal = new OrigenFeatureRuntime.HostedAuthoredJournal<String>();
        journal.record(1L, () -> authored.get(1L));
        authored.put(1L, "ore");
        assertTrue(OrigenFeatureRuntime.commitAdmittedHostedEnvironmentWrites(authored, false, (p, s) -> {
            physical.put(p, s);
            return true;
        }));
        assertEquals(authored, physical);
        assertEquals("ore", authored.get(1L));
        var expected = new IllegalStateException("unrelated writer failure");
        assertSame(
            expected,
            assertThrows(
                IllegalStateException.class,
                () -> OrigenFeatureRuntime
                    .commitAdmittedHostedEnvironmentWrites(authored, false, (p, s) -> { throw expected; })));
    }

    @Test
    public void acceptedEntitiesPublishInSourceOrderOnlyAfterBlockCommit() {
        var events = new java.util.ArrayList<String>();
        var writes = new LinkedHashMap<Long, String>();
        writes.put(1L, "block");
        boolean committed = OrigenFeatureRuntime.commitAdmittedHostedEnvironmentWrites(writes, false, (p, s) -> {
            events.add(s);
            return true;
        });
        OrigenFeatureRuntime
            .publishHostedProposalEntities(java.util.List.of("bee-one", "bee-two"), committed, events::add);
        assertEquals(java.util.List.of("block", "bee-one", "bee-two"), events);
        var expected = new IllegalStateException("unexpected spawn failure");
        assertSame(
            expected,
            assertThrows(
                IllegalStateException.class,
                () -> OrigenFeatureRuntime
                    .publishHostedProposalEntities(java.util.List.of("bee"), true, s -> { throw expected; })));
    }
}
