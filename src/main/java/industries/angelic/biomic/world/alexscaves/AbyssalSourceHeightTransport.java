package industries.angelic.biomic.world.alexscaves;

import java.util.Objects;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.modern.ModernVerticalTransform;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjection;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjections;

/** Checked source/physical Y transport for the source-authored Abyssal terrain and ecology. */
public final class AbyssalSourceHeightTransport {

    private final ChunkVerticalLayout layout;
    private final AuthoredVerticalProjection projection;
    private final boolean compressed;

    private AbyssalSourceHeightTransport(ChunkVerticalLayout layout, AuthoredVerticalProjection projection,
        boolean compressed) {
        this.layout = Objects.requireNonNull(layout, "layout");
        this.projection = projection;
        this.compressed = compressed;
    }

    /** Exact historical Ocean Trench projection, including its many-to-one compression. */
    public static AbyssalSourceHeightTransport legacy(ChunkVerticalLayout layout) {
        Objects.requireNonNull(layout, "layout");
        if (layout.height() < 256)
            throw new IllegalArgumentException("Legacy Abyssal transport requires 256 physical blocks");
        return new AbyssalSourceHeightTransport(layout, null, true);
    }

    /** Uncompressed Origen: source Y is authored Y, one-to-one on physical Y + 128. */
    public static AbyssalSourceHeightTransport shifted512() {
        return new AbyssalSourceHeightTransport(
            ChunkVerticalLayout.EXTENDED_512,
            AuthoredVerticalProjections.shifted512(),
            false);
    }

    public ChunkVerticalLayout layout() {
        return layout;
    }

    public boolean compressed() {
        return compressed;
    }

    public int physicalY(int sourceY) {
        if (sourceY < -64 || sourceY >= 320)
            throw new IllegalArgumentException("Abyssal source Y outside [-64,320): " + sourceY);
        return compressed ? ModernVerticalTransform.toLegacyY(sourceY)
            : projection.toPhysicalBlockY(sourceY)
                .orElseThrow(() -> new IllegalArgumentException("Unmapped Abyssal source Y " + sourceY));
    }

    /** Only exact for shifted transport. Legacy returns the historical representative source Y. */
    public int sourceY(int physicalY) {
        if (physicalY < 0 || physicalY >= layout.height())
            throw new IllegalArgumentException("Physical Y outside layout: " + physicalY);
        if (compressed) {
            if (physicalY >= 256) throw new IllegalArgumentException(
                "Physical Y is legacy storage headroom, not authored Abyssal terrain: " + physicalY);
            return ModernVerticalTransform.toModernY(physicalY);
        }
        return projection.toSourceBlockY(physicalY)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Physical Y outside shifted Abyssal authored envelope: " + physicalY));
    }

    public int sourceDepth(int physicalTopY, int physicalBottomY) {
        return sourceY(physicalTopY) - sourceY(physicalBottomY);
    }

    public int physicalTubeCeilingY() {
        return physicalY(OceanTrenchPlanner.SOURCE_SEA_LEVEL - 30);
    }

    /** Lower host geology [0,64) and upper headroom [448,512) never receive Abyssal ecology. */
    public boolean containsEcologyY(int physicalY) {
        return compressed ? physicalY >= 0 && physicalY < Math.min(256, layout.height())
            : projection.containsPhysicalBlockY(physicalY);
    }
}
