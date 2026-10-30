package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import industries.angelic.biomic.api.ChunkVerticalLayout;

/**
 * Explicit coordinate contract for transporting Origen's authored feature cells into physical chunk storage.
 *
 * <p>
 * The legacy static feature helpers clamp authored coordinates for historical callers. This typed transport
 * instead rejects coordinates outside the authored envelope. Physical headroom is valid storage but has no authored
 * aliases.
 * </p>
 */
public final class OrigenFeatureHeightTransport {

    private final AuthoredVerticalProjection projection;
    private final ChunkVerticalLayout layout;
    private final boolean compressed;

    public OrigenFeatureHeightTransport(AuthoredVerticalProjection projection, ChunkVerticalLayout layout) {
        if (projection != AuthoredVerticalProjections.origenCompressed256()
            && projection != AuthoredVerticalProjections.shifted512()) {
            throw new IllegalArgumentException("Origen features require a canonical Origen projection");
        }
        if (layout == null || !projection.supportsStorageHeight(layout.height())) {
            throw new IllegalArgumentException("Physical layout cannot contain the Origen feature projection");
        }
        this.projection = projection;
        this.layout = layout;
        compressed = projection == AuthoredVerticalProjections.origenCompressed256();
    }

    public AuthoredVerticalProjection projection() {
        return projection;
    }

    public ChunkVerticalLayout layout() {
        return layout;
    }

    public boolean compressed() {
        return compressed;
    }

    public boolean containsAuthoredY(int authoredY) {
        return projection.containsSourceBlockY(authoredY);
    }

    /** Storage membership; shifted headroom is deliberately included. */
    public boolean containsPhysicalY(int physicalY) {
        return layout.containsY(physicalY);
    }

    /** Checked source-to-physical conversion; unlike the legacy static helper this never clamps. */
    public int physicalY(int authoredY) {
        return projection.toPhysicalBlockY(authoredY)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Authored Y " + authoredY + " is outside Origen's feature envelope"));
    }

    /**
     * Immutable authored layers represented by one physical cell. Valid physical headroom returns an empty list;
     * coordinates outside the selected storage layout fail.
     */
    public List<Integer> authoredYsAtPhysical(int physicalY) {
        if (!layout.containsY(physicalY)) {
            throw new IllegalArgumentException("Physical Y " + physicalY + " is outside the selected chunk layout");
        }
        if (!projection.containsPhysicalBlockY(physicalY)) return List.of();
        if (!compressed) return List.of(
            projection.toSourceBlockY(physicalY)
                .orElseThrow());
        int[] aliases = OrigenVerticalProfileSampler.authoredBlockYsAt(physicalY);
        List<Integer> copied = new ArrayList<>(aliases.length);
        for (int alias : aliases) copied.add(alias);
        return Collections.unmodifiableList(copied);
    }

    public int physicalIndex(int localX, int physicalY, int localZ) {
        return layout.index(localX, physicalY, localZ);
    }
}
