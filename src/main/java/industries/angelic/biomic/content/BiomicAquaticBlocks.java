package industries.angelic.biomic.content;

import industries.angelic.biomic.content.block.BlockAquaticPlant;
import industries.angelic.biomic.content.block.BlockAquaticPlant.Kind;
import industries.angelic.biomic.content.block.BlockCoral;

/** Isolated holder for modern aquatic content that EFR intentionally does not backport. */
public final class BiomicAquaticBlocks {

    public static final BlockAquaticPlant KELP = new BlockAquaticPlant("kelp", Kind.KELP, "kelp_plant", "kelp");
    public static final BlockAquaticPlant SEAGRASS = new BlockAquaticPlant(
        "seagrass",
        Kind.SEAGRASS,
        "seagrass",
        "tall_seagrass_bottom",
        "tall_seagrass_top");
    public static final BlockAquaticPlant SEA_PICKLE = new BlockAquaticPlant(
        "sea_pickle",
        Kind.SEA_PICKLE,
        "sea_pickle",
        "sea_pickle",
        "sea_pickle",
        "sea_pickle");
    public static final BlockAquaticPlant CORAL_PLANT = new BlockAquaticPlant(
        "coral_plant",
        Kind.CORAL_PLANT,
        "brain_coral",
        "bubble_coral",
        "fire_coral",
        "horn_coral",
        "tube_coral",
        "dead_brain_coral",
        "dead_bubble_coral",
        "dead_fire_coral",
        "dead_horn_coral",
        "dead_tube_coral");
    public static final BlockAquaticPlant CORAL_FAN = new BlockAquaticPlant(
        "coral_fan",
        Kind.CORAL_FAN,
        "brain_coral_fan",
        "bubble_coral_fan",
        "fire_coral_fan",
        "horn_coral_fan",
        "tube_coral_fan",
        "dead_brain_coral_fan",
        "dead_bubble_coral_fan",
        "dead_fire_coral_fan",
        "dead_horn_coral_fan",
        "dead_tube_coral_fan");
    public static final BlockCoral CORAL_BLOCK = new BlockCoral();

    private BiomicAquaticBlocks() {}
}
