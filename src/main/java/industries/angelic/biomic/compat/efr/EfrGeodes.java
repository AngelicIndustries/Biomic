package industries.angelic.biomic.compat.efr;

import net.minecraft.world.gen.feature.WorldGenerator;

/** Et Futurum Requiem's amethyst geode generator with its configured shells, or null without EFR. */
public final class EfrGeodes {

    private EfrGeodes() {}

    public static WorldGenerator create() {
        return EfrTiles.PRESENT ? Present.create() : null;
    }

    /** Loaded only with EFR present. */
    private static final class Present {

        static WorldGenerator create() {
            var outer = ganymedes01.etfuturum.configuration.configs.ConfigWorld.amethystOuterBlock;
            var middle = ganymedes01.etfuturum.configuration.configs.ConfigWorld.amethystMiddleBlock;
            return outer == null || middle == null ? null
                : new ganymedes01.etfuturum.world.generate.feature.WorldGenGeode(outer, middle);
        }
    }
}
