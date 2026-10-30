package industries.angelic.biomic.network;

import java.util.Objects;

/**
 * Pure, process-local publication lifecycle for the requirements of one server session.
 * Network/FML registration is deliberately outside this class.
 */
public final class BiomicPeerSession {

    public enum PublicationResult {
        PUBLISHED,
        ALREADY_PUBLISHED_IDENTICAL,
        REJECTED_NO_ACTIVE_SESSION,
        REJECTED_STALE_TOKEN,
        REJECTED_UNINITIALIZED_REQUIREMENTS,
        REJECTED_CONFLICT
    }

    public record Snapshot(long token, boolean active, boolean published,
        BiomicPeerRequirements.Requirements requirements) {

        public Snapshot {
            Objects.requireNonNull(requirements, "requirements");
            if (token < 0 || (!active && published)
                || (!published && requirements.initialized())
                || (published && !requirements.initialized()))
                throw new IllegalArgumentException("Invalid peer-session snapshot");
        }
    }

    private long lastToken;
    private volatile Snapshot snapshot;

    public BiomicPeerSession() {
        this(0L);
    }

    BiomicPeerSession(long lastToken) {
        if (lastToken < 0) throw new IllegalArgumentException("Negative session token");
        this.lastToken = lastToken;
        snapshot = inactive(lastToken);
    }

    /** Starts or supersedes a local server session and clears any previously published policy. */
    public synchronized long beginSession() {
        long token = Math.addExact(lastToken, 1L);
        lastToken = token;
        snapshot = new Snapshot(token, true, false, BiomicPeerRequirements.Requirements.uninitialized());
        return token;
    }

    /** Publishes exactly one initialized policy. Repeating the identical publication is idempotent. */
    public synchronized PublicationResult publish(long token, BiomicPeerRequirements.Requirements requirements) {
        Objects.requireNonNull(requirements, "requirements");
        Snapshot current = snapshot;
        if (token != current.token()) return PublicationResult.REJECTED_STALE_TOKEN;
        if (!current.active()) return PublicationResult.REJECTED_NO_ACTIVE_SESSION;
        if (!requirements.initialized()) return PublicationResult.REJECTED_UNINITIALIZED_REQUIREMENTS;
        if (current.published()) return current.requirements()
            .equals(requirements) ? PublicationResult.ALREADY_PUBLISHED_IDENTICAL : PublicationResult.REJECTED_CONFLICT;
        snapshot = new Snapshot(token, true, true, requirements);
        return PublicationResult.PUBLISHED;
    }

    /** Ends only the matching active session; a stale stop cannot clear a newer publication. */
    public synchronized boolean endSession(long token) {
        Snapshot current = snapshot;
        if (!current.active() || token != current.token()) return false;
        snapshot = inactive(token);
        return true;
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    /** Checks a peer against one coherent volatile snapshot of the current publication. */
    public boolean accepts(BiomicPeerRequirements.Direction direction, BiomicPeerRequirements.Capabilities remote) {
        Snapshot current = snapshot;
        return BiomicPeerRequirements.accepts(direction, current.requirements(), remote);
    }

    private static Snapshot inactive(long token) {
        return new Snapshot(token, false, false, BiomicPeerRequirements.Requirements.uninitialized());
    }
}
