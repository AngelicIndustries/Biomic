package industries.angelic.biomic.content;

import industries.angelic.biomic.content.block.BlockCoralWallFan;

/** Exact wall attachments: water needs its own material; facing and death fit in metadata. */
public final class BiomicCoralWallFans {

    private static final BlockCoralWallFan[][] BLOCKS = new BlockCoralWallFan[5][2];
    static {
        for (int species = 0; species < 5; species++) {
            BLOCKS[species][0] = new BlockCoralWallFan(species, false);
            BLOCKS[species][1] = new BlockCoralWallFan(species, true);
        }
    }

    private BiomicCoralWallFans() {}

    public static BlockCoralWallFan get(int species, boolean wet) {
        if (species < 0 || species >= BLOCKS.length) throw new IllegalArgumentException("Unknown coral species");
        return BLOCKS[species][wet ? 1 : 0];
    }

    public static String registryName(int species, boolean wet) {
        return BlockCoralWallFan.speciesName(species) + "_coral_wall_fan" + (wet ? "_wet" : "");
    }
}
