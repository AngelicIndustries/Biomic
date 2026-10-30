/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.paralithic.noise;

import com.dfsek.paralithic.functions.dynamic.Context;

public class SeedContext implements Context {

    private long seed;

    public SeedContext(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }
}
