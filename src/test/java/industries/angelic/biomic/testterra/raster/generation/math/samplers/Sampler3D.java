// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.testterra.raster.generation.math.samplers;

import industries.angelic.biomic.testterra.raster.config.noise.BiomeNoiseProperties;
import industries.angelic.biomic.testterra.raster.fixture.BiomeProvider;
import industries.angelic.biomic.testterra.raster.fixture.PropertyKey;
import industries.angelic.biomic.testterra.raster.generation.math.interpolation.ChunkInterpolator;
import industries.angelic.biomic.testterra.raster.generation.math.interpolation.ElevationInterpolator;

public class Sampler3D {

    private final ChunkInterpolator interpolator;
    private final ElevationInterpolator elevationInterpolator;

    public Sampler3D(int x, int z, long seed, int minHeight, int maxHeight, BiomeProvider provider, int elevationSmooth,
        PropertyKey<BiomeNoiseProperties> noisePropertiesKey, int maxBlend) {
        this.interpolator = new ChunkInterpolator(
            seed,
            x,
            z,
            provider,
            minHeight,
            maxHeight,
            noisePropertiesKey,
            maxBlend);
        this.elevationInterpolator = new ElevationInterpolator(
            seed,
            x,
            z,
            provider,
            elevationSmooth,
            noisePropertiesKey);
    }

    public double sample(double x, double y, double z) {
        return interpolator.getNoise(x, y, z)
            + elevationInterpolator.getElevation((int) Math.round(x), (int) Math.round(z));
    }

    public double sample(int x, int y, int z) {
        return interpolator.getNoise(x, y, z) + elevationInterpolator.getElevation(x, z);
    }
}
