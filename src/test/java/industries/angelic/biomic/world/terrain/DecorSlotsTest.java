package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import industries.angelic.biomic.api.EcologyOverlays;

public class DecorSlotsTest {

    @Test
    public void biomicsOwnDecorNamesNoOtherModsBlocks() {
        DecorSlots slots = DecorSlots.parse(EcologyOverlays.compose("decor.json"));
        List<String> names = new ArrayList<>(slots.cave.values());
        names.addAll(slots.marine.values());
        slots.marineLists.values()
            .forEach(names::addAll);
        for (String name : names) assertTrue(
            name,
            name.isEmpty() || name.startsWith("minecraft:")
                || name.startsWith("etfuturum:")
                || name.startsWith("biomic:"));
        assertEquals("minecraft:vine:0", slots.cave.get("mossy_wall"));
    }
}
