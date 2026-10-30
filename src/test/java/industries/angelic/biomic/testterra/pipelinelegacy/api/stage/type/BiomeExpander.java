// Generated test-only Terra 3745c3e oracle; MIT, see adjacent LICENSE.
/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.testterra.pipelinelegacy.api.stage.type;

import industries.angelic.biomic.testterra.pipelinelegacy.api.delegate.BiomeDelegate;

public interface BiomeExpander {

    BiomeDelegate getBetween(double x, double z, long seed, BiomeDelegate... others);
}
