// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.testterra.pipelinelegacy.api;

import industries.angelic.biomic.testterra.pipelinelegacy.api.delegate.BiomeDelegate;
import industries.angelic.biomic.testterra.pipelinelegacy.api.stage.type.BiomeExpander;
import industries.angelic.biomic.testterra.pipelinelegacy.api.stage.type.BiomeMutator;
import industries.angelic.biomic.testterra.pipelinelegacy.source.BiomeSource;

public interface BiomeHolder {

    BiomeHolder expand(BiomeExpander expander, long seed);

    void mutate(BiomeMutator mutator, long seed);

    void fill(BiomeSource source, long seed);

    BiomeDelegate getBiome(int x, int z);

    BiomeDelegate getBiomeRaw(int x, int z);
}
