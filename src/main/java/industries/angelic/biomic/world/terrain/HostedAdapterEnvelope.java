package industries.angelic.biomic.world.terrain;

/**
 * Exact existing hosted adapter limit, not a declaration about Origen's authored writers.
 * Every admitted column read and buffered block write passes this limit. Rejected accesses
 * remain observable but cannot influence a successfully completed native transaction.
 */
final class HostedAdapterEnvelope {

    static final int MIN_OFFSET = -48;
    static final int MAX_OFFSET = 63;

    private HostedAdapterEnvelope() {}

    static int minimum(int sourceChunk) {
        return Math.toIntExact((long) sourceChunk * 16 + MIN_OFFSET);
    }

    static int maximum(int sourceChunk) {
        return Math.toIntExact((long) sourceChunk * 16 + MAX_OFFSET);
    }

}
