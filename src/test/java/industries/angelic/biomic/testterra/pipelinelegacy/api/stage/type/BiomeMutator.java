// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.testterra.pipelinelegacy.api.stage.type;

import industries.angelic.biomic.testterra.pipelinelegacy.api.BiomeHolder;
import industries.angelic.biomic.testterra.pipelinelegacy.api.delegate.BiomeDelegate;

public interface BiomeMutator {

    BiomeDelegate mutate(ViewPoint viewPoint, double x, double z, long seed);

    default Iterable<BiomeDelegate> getBiomes(Iterable<BiomeDelegate> biomes) {
        return biomes;
    }

    class ViewPoint {

        private final BiomeHolder biomes;
        private final int offX;
        private final int offZ;

        public ViewPoint(BiomeHolder biomes, int offX, int offZ) {
            this.biomes = biomes;
            this.offX = offX;
            this.offZ = offZ;
        }

        public BiomeDelegate getBiome(int x, int z) {
            return biomes.getBiomeRaw(x + offX, z + offZ);
        }
    }
}
