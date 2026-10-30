package industries.angelic.biomic.content;

import industries.angelic.biomic.content.block.BlockBigDripleaf;
import industries.angelic.biomic.content.block.BlockSmallDripleaf;

/** Isolated holder so Origen block-state tests do not initialize EFR-backed content unnecessarily. */
public final class BiomicDripleafBlocks {

    public static final BlockBigDripleaf BIG_DRIPLEAF = new BlockBigDripleaf(false);
    public static final BlockBigDripleaf BIG_DRIPLEAF_STEM = new BlockBigDripleaf(true);
    public static final BlockSmallDripleaf SMALL_DRIPLEAF = new BlockSmallDripleaf();
    public static final BlockBigDripleaf WET_BIG_DRIPLEAF = new BlockBigDripleaf(false, true);
    public static final BlockBigDripleaf WET_BIG_DRIPLEAF_STEM = new BlockBigDripleaf(true, true);
    public static final BlockSmallDripleaf WET_SMALL_DRIPLEAF = new BlockSmallDripleaf(true);

    private BiomicDripleafBlocks() {}
}
