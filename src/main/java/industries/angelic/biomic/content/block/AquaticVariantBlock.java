package industries.angelic.biomic.content.block;

/** Supplies stable metadata variant names to the shared aquatic ItemBlock. */
public interface AquaticVariantBlock {

    int getVariantCount();

    String getVariantName(int metadata);
}
