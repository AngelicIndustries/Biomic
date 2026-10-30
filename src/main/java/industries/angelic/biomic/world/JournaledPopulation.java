package industries.angelic.biomic.world;

import industries.angelic.biomic.world.terrain.OrigenFeatureRuntime;

/** A provider whose population is order-independent (OrigenFeatureRuntime's journal). */
public interface JournaledPopulation {

    /** The runtime that keeps the journal, the provided terrain and the population tags. */
    OrigenFeatureRuntime populationRuntime();
}
