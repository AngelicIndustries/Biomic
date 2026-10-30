package industries.angelic.biomic.client.render;

import static org.junit.Assert.*;

import org.junit.Test;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class RuShaderMaterialsTest {

    @Test
    public void inheritsPackIdsButKeepsExplicitOverridesAndDoesNotAliasMaps() {
        Int2IntMap source = entry(0, 1234), target = entry(4, 9876);
        for (int meta = 0; meta < 16; meta++) RuShaderMaterials.copyFallback(target, meta, source, 0);
        assertEquals(1234, target.get(0));
        assertEquals(9876, target.get(4));
        assertEquals(1234, target.get(15));
        target.put(0, 55);
        assertEquals(1234, source.get(0));
    }

    @Test
    public void absentMappingsAndExplicitWildcardsAreNotOverwritten() {
        Int2IntMap target = new Int2IntOpenHashMap();
        RuShaderMaterials.copyFallback(target, 0, null, 0);
        RuShaderMaterials.copyFallback(target, 0, new Int2IntOpenHashMap(), 0);
        assertTrue(target.isEmpty());
        target.put(1 << 30, 34);
        RuShaderMaterials.copyFallback(target, 0, entry(0, 12), 0);
        assertEquals(1, target.size());
    }

    @Test
    public void doublePlantUpperHalfUsesItsOwnPackEntry() {
        Int2IntMap source = entry(2, 11), target = new Int2IntOpenHashMap();
        source.put(10, 22);
        RuShaderMaterials.copyFallback(target, 0, source, 2);
        RuShaderMaterials.copyFallback(target, 8, source, 10);
        assertEquals(11, target.get(0));
        assertEquals(22, target.get(8));
    }

    @Test
    public void onlyNativeBopKelpMetasInheritVegetationWaving() {
        for (int metadata = 0; metadata < 16; metadata++) assertEquals(
            metadata >= 8 && metadata <= 11,
            RuShaderMaterials.isBopKelp("BiomesOPlenty:coral1", metadata));
        assertTrue(RuShaderMaterials.isBopKelp("biomesoplenty:CORAL1", 8));
        assertFalse(RuShaderMaterials.isBopKelp("BiomesOPlenty:coral2", 8));
        assertFalse(RuShaderMaterials.isBopKelp(null, 8));
    }

    @Test
    public void saltyReedsUseSeparateLowerAndUpperPlantMaterialsWithoutTouchingOtherFlora() {
        assertEquals(0, RuShaderMaterials.saltyReedHalf("saltymod:marsh_reeds_b"));
        assertEquals(1, RuShaderMaterials.saltyReedHalf("saltymod:marsh_reeds_t"));
        assertEquals(-1, RuShaderMaterials.saltyReedHalf("saltymod:saltworts"));
        assertEquals(-1, RuShaderMaterials.saltyReedHalf(null));
    }

    @Test
    public void gardensShaderFallbackIsLimitedToNonemissiveSoftVegetation() {
        assertEquals("vine", RuShaderMaterials.gardensVegetationKind("biomic:gardens_soul_spore"));
        assertEquals("mushroom", RuShaderMaterials.gardensVegetationKind("biomic:gardens_soulblight_fungus"));
        assertEquals("grass", RuShaderMaterials.gardensVegetationKind("BIOMIC:GARDENS_SOULBLIGHT_SPROUTS"));
        for (String excluded : new String[] { "biomic:gardens_glowing_soul_spore", "biomic:gardens_blistercrown",
            "biomic:gardens_tall_blistercrown", "biomic:gardens_whistlecane", "biomic:gardens_soulblight_stem",
            "biomic:gardens_blightwart_block" })
            assertNull(excluded, RuShaderMaterials.gardensVegetationKind(excluded));
        assertNull(RuShaderMaterials.gardensVegetationKind(null));
    }

    @Test
    public void forlornShaderFallbackMovesOnlySoftUnderweedAndNeverWoodOrCaveMaterial() {
        assertEquals("grass", RuShaderMaterials.forlornVegetationKind("biomic:alexs_forlorn_underweed"));
        assertEquals("grass", RuShaderMaterials.forlornVegetationKind("BIOMIC:ALEXS_FORLORN_UNDERWEED"));
        for (String rigid : new String[] { "biomic:alexs_forlorn_thornwood_branch",
            "biomic:alexs_forlorn_thornwood_log", "biomic:alexs_forlorn_thornwood_wood_log",
            "biomic:alexs_forlorn_guano_layer", "biomic:alexs_forlorn_guanostone", "biomic:alexs_forlorn_coprolith" })
            assertNull(rigid, RuShaderMaterials.forlornVegetationKind(rigid));
        assertNull(RuShaderMaterials.forlornVegetationKind(null));
    }

    @Test
    public void rigidWetRuinCarriersDoNotInheritVegetationShaderMovement() {
        for (net.minecraft.block.Block block : new net.minecraft.block.Block[] {
            industries.angelic.biomic.unused.alexscaves.AbyssalRuinBlocks.RUSTY_WET_CHAIN,
            industries.angelic.biomic.unused.alexscaves.AbyssalRuinBlocks.RUSTY_WET_WEATHERED_SLAB,
            industries.angelic.biomic.unused.alexscaves.AbyssalRuinBlocks.RUSTY_WET_WAXED_OXIDIZED_STAIRS[1] })
            assertNull(block.toString(), RuShaderMaterials.fallback(block, 0));
    }

    private static Int2IntMap entry(int metadata, int id) {
        Int2IntMap map = new Int2IntOpenHashMap();
        map.defaultReturnValue(-1);
        map.put(metadata, id);
        return map;
    }
}
