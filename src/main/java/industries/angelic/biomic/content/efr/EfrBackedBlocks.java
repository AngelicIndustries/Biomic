package industries.angelic.biomic.content.efr;

import net.minecraft.block.Block;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.compat.efr.EfrTiles;

/**
 * Biomic's blocks built on Et Futurum Requiem's: clover on its pink petals, icicles on its pointed dripstone, and the
 * Skylands' waterlogged glow lichen and mangrove roots. Built and registered only with EFR installed; null without it,
 * when the block palette maps the ids that place them (regions_unexplored:clover, regions_unexplored:icicle). Fields
 * are plain Blocks so that nothing outside this class names a class extending EFR's.
 */
public final class EfrBackedBlocks {

    private EfrBackedBlocks() {}

    public static Block clover() {
        return EfrTiles.PRESENT ? Clover.BLOCK : null;
    }

    public static Block icicle() {
        return EfrTiles.PRESENT ? Icicle.BLOCK : null;
    }

    public static Block wetMangroveRoots() {
        return EfrTiles.PRESENT ? SkylandsWet.MANGROVE_ROOTS : null;
    }

    public static Block wetGlowLichen() {
        return EfrTiles.PRESENT ? SkylandsWet.GLOW_LICHEN : null;
    }

    /** From BiomicBlocks.register, in the same place the blocks always were (registry order unchanged with EFR). */
    public static void registerClover() {
        if (EfrTiles.PRESENT) GameRegistry.registerBlock(Clover.BLOCK, "clover");
    }

    public static void registerSkylandsWet() {
        if (EfrTiles.PRESENT) SkylandsWet.register();
    }

    public static void registerIcicle() {
        if (EfrTiles.PRESENT) GameRegistry.registerBlock(Icicle.BLOCK, "icicle");
    }

    // One holder each, loaded only with EFR present, so that asking for one block builds no other.

    private static final class Clover {

        static final Block BLOCK = new industries.angelic.biomic.content.block.BlockClover();
    }

    private static final class Icicle {

        static final Block BLOCK = industries.angelic.biomic.content.BiomicBlocks
            .creative(new industries.angelic.biomic.content.block.BlockIcicle());
    }

    private static final class SkylandsWet {

        static final Block MANGROVE_ROOTS = new industries.angelic.biomic.content.block.BlockSkylandsWetMangroveRoots();
        static final Block GLOW_LICHEN = new industries.angelic.biomic.content.block.BlockSkylandsWetGlowLichen();

        static void register() {
            GameRegistry.registerBlock(
                MANGROVE_ROOTS,
                industries.angelic.biomic.content.item.ItemBlockSkylandsWetMangroveRoots.class,
                "skylands_wet_mangrove_roots");
            net.minecraftforge.common.MinecraftForge.EVENT_BUS
                .register(new industries.angelic.biomic.content.block.SkylandsWetMangroveBucket());
            GameRegistry.registerBlock(
                GLOW_LICHEN,
                industries.angelic.biomic.content.item.ItemBlockSkylandsWetGlowLichen.class,
                "skylands_wet_glow_lichen");
            net.minecraftforge.common.MinecraftForge.EVENT_BUS
                .register(new industries.angelic.biomic.content.block.SkylandsWetGlowLichenBucket());
        }
    }
}
