/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Faithful Forge 1.7.10 adaptation of astrsh/Aeropelago terrain.yml at
 * bc525814ba84d961310d1c359a724382c54fd5d4, licensed under GNU GPL v3.
 * Biomic changes only the vertical coordinate projection and the external zone envelope used to coexist with
 * Origen, and reads the mass field once as a plan for its deck-and-keel islands (IslandKeel). The sampler graph and
 * authored constants in this class remain the upstream Aeropelago 0.1.0 values.
 */
package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;
import industries.angelic.biomic.internal.terra.noise.normalizer.ProbabilityNormalizer;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.fractal.BrownianMotionSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.fractal.RidgedFractalSampler;
import industries.angelic.biomic.internal.terra.noise.samplers.noise.simplex.OpenSimplex2Sampler;

/** Executes Aeropelago 0.1.0's complete authored density equation. */
final class AeropelagoDensitySampler {

    static final double AUTHORED_MIN_Y = -40.0D;
    static final double AUTHORED_MAX_Y = 250.0D;
    static final double AUTHORED_FLATNESS = 2.0D;

    /** The complete authored -40..250 envelope is squeezed into this 1.7 sky band. */
    static final double PHYSICAL_MIN_Y = FloatingIslandDensitySampler.MIN_Y;
    static final double PHYSICAL_MAX_Y = FloatingIslandDensitySampler.MAX_Y;
    /** Uniform source-to-1.7 scale; applying it on every axis preserves Aeropelago's authored proportions. */
    static final double PHYSICAL_SCALE = (PHYSICAL_MAX_Y - PHYSICAL_MIN_Y) / (AUTHORED_MAX_Y - AUTHORED_MIN_Y);

    private final long worldSeed;
    private final NoiseSampler simplex3;

    AeropelagoDensitySampler(long worldSeed) {
        this.worldSeed = worldSeed;

        OpenSimplex2Sampler massBase = new OpenSimplex2Sampler();
        massBase.setFrequency(0.002D);
        BrownianMotionSampler massFbm = new BrownianMotionSampler(massBase);
        massFbm.setOctaves(5);
        massFbm.setGain(0.5D);
        massFbm.setLacunarity(2.0D);
        massFbm.setWeightedStrength(0.0D);
        simplex3 = new ProbabilityNormalizer(massFbm);

        OpenSimplex2Sampler undercarriageBase = new OpenSimplex2Sampler();
        undercarriageBase.setFrequency(0.005D);
        RidgedFractalSampler undercarriageRidged = new RidgedFractalSampler(undercarriageBase);
        undercarriageRidged.setOctaves(3);
        undercarriageRidged.setGain(1.0D);
        undercarriageRidged.setLacunarity(2.0D);
        undercarriageRidged.setWeightedStrength(0.0D);
    }

    /** The authored height inside the islands' full-mass band (above the lower taper, below the upper one). */
    static final double PLAN_Y = 120.0D;

    /** The mass field at {@link #PLAN_Y}: where islands lie in plan, before any vertical shaping. */
    double massPlan(double x, double z) {
        return simplex3.noise(worldSeed, x, PLAN_Y * AUTHORED_FLATNESS, z);
    }

    static double toAuthoredHorizontal(double physicalOffset) {
        return physicalOffset / PHYSICAL_SCALE;
    }

}
