package industries.angelic.biomic.compat.food;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

/** The ground wild plants grow on: natural ground in the overworld, nether ground in the Nether. */
public final class WildCrops {

    private static volatile Set<Block> netherSoil;

    private WildCrops() {}

    /** Grass, dirt of any kind (podzol, coarse) and sand for the coastal crops. */
    public static boolean naturalGround(Block soil) {
        Material material = soil.getMaterial();
        return material == Material.grass || material == Material.ground && soil != Blocks.farmland
            || soil == Blocks.sand;
    }

    /** Netherrack, soul sand, and EFR's nylium and soul soil. */
    public static boolean netherGround(Block soil) {
        Set<Block> known = netherSoil;
        if (known == null) {
            Set<Block> nether = new HashSet<>(Set.of(Blocks.netherrack, Blocks.soul_sand));
            for (String id : new String[] { "etfuturum:nylium", "etfuturum:soul_soil" }) {
                Block block = Block.getBlockFromName(id);
                if (block != null && block != Blocks.air) nether.add(block);
            }
            known = netherSoil = Set.copyOf(nether);
        }
        return known.contains(soil);
    }
}
