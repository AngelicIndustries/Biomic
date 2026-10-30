package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Test;

public class OrigenBlockStateResolverRegistryTest {

    @Test
    public void disabledButConstructedEfrSiblingCannotResolveAsInstalled() {
        Block constructed = new Block(Material.rock) {};
        assertFalse(OrigenBlockStateResolver.installedEfr(false, constructed, block -> true));
    }

    @Test
    public void enabledButUnregisteredEfrSiblingCannotResolveAsInstalled() {
        Block orphan = new Block(Material.rock) {};
        assertFalse(OrigenBlockStateResolver.installedEfr(true, orphan, block -> false));
        assertTrue(OrigenBlockStateResolver.installedEfr(true, orphan, block -> block == orphan));
    }
}
