package industries.angelic.biomic.compat.efr;

import net.minecraft.block.Block.SoundType;

/**
 * Et Futurum Requiem's modern sound types by name, or the vanilla one given when EFR is absent. Only this class names
 * EFR's sound class, and only once EFR is known to be loaded.
 */
public final class EfrSounds {

    private static final boolean PRESENT = EfrTiles.PRESENT;

    private EfrSounds() {}

    public static SoundType of(String name, SoundType fallback) {
        return PRESENT ? Present.of(name, fallback) : fallback;
    }

    /** Loaded only with EFR present. */
    private static final class Present {

        static SoundType of(String name, SoundType fallback) {
            SoundType sound = switch (name) {
                case "amethyst_cluster" -> ganymedes01.etfuturum.client.sound.ModSounds.soundAmethystCluster;
                case "amethyst_block" -> ganymedes01.etfuturum.client.sound.ModSounds.soundAmethystBlock;
                case "netherrack" -> ganymedes01.etfuturum.client.sound.ModSounds.soundNetherrack;
                case "deepslate" -> ganymedes01.etfuturum.client.sound.ModSounds.soundDeepslate;
                case "sculk" -> ganymedes01.etfuturum.client.sound.ModSounds.soundSculk;
                case "crop_warts" -> ganymedes01.etfuturum.client.sound.ModSounds.soundCropWarts;
                case "nether_sprouts" -> ganymedes01.etfuturum.client.sound.ModSounds.soundNetherSprouts;
                case "nylium" -> ganymedes01.etfuturum.client.sound.ModSounds.soundNylium;
                case "shroomlight" -> ganymedes01.etfuturum.client.sound.ModSounds.soundShroomlight;
                case "wet_grass" -> ganymedes01.etfuturum.client.sound.ModSounds.soundWetGrass;
                case "vines" -> ganymedes01.etfuturum.client.sound.ModSounds.soundVines;
                case "weeping_vines" -> ganymedes01.etfuturum.client.sound.ModSounds.soundWeepingVines;
                default -> null;
            };
            return sound == null ? fallback : sound;
        }
    }
}
