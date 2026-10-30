// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.testterra.pipelinelegacy.expand;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;
import industries.angelic.biomic.testterra.pipelinelegacy.api.delegate.BiomeDelegate;
import industries.angelic.biomic.testterra.pipelinelegacy.api.stage.type.BiomeExpander;
import industries.angelic.biomic.testterra.pipelinelegacy.fixture.MathUtil;

public class FractalExpander implements BiomeExpander {

    private final NoiseSampler sampler;

    public FractalExpander(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    public BiomeDelegate getBetween(double x, double z, long seed, BiomeDelegate... others) {
        return others[MathUtil.normalizeIndex(sampler.noise(seed, x, z), others.length)];
    }
}
