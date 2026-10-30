// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.testterra.pipelinelegacy.mutator;

import java.util.Objects;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;
import industries.angelic.biomic.testterra.pipelinelegacy.api.delegate.BiomeDelegate;
import industries.angelic.biomic.testterra.pipelinelegacy.api.stage.type.BiomeMutator;
import industries.angelic.biomic.testterra.pipelinelegacy.fixture.MathUtil;

public class SmoothMutator implements BiomeMutator {

    private final NoiseSampler sampler;

    public SmoothMutator(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public BiomeDelegate mutate(ViewPoint viewPoint, double x, double z, long seed) {
        BiomeDelegate top = viewPoint.getBiome(1, 0);
        BiomeDelegate bottom = viewPoint.getBiome(-1, 0);
        BiomeDelegate left = viewPoint.getBiome(0, 1);
        BiomeDelegate right = viewPoint.getBiome(0, -1);

        boolean vert = Objects.equals(top, bottom) && top != null;
        boolean horiz = Objects.equals(left, right) && left != null;

        if (vert && horiz) {
            return MathUtil.normalizeIndex(sampler.noise(seed, x, z), 2) == 0 ? left : top;
        }

        if (vert) return top;
        if (horiz) return left;

        return viewPoint.getBiome(0, 0);
    }
}
