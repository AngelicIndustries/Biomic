package industries.angelic.biomic.world.structure;

import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

/** Moves a freshly laid-out vanilla start vertically, exactly as vanilla's own markAvailableHeight does. */
final class StructureOffsets {

    private StructureOffsets() {}

    static StructureStart raise(StructureStart start, int dy) {
        if (dy == 0 || start == null) return start;
        start.getBoundingBox()
            .offset(0, dy, 0);
        for (Object component : start.getComponents()) {
            ((StructureComponent) component).getBoundingBox()
                .offset(0, dy, 0);
        }
        return start;
    }
}
