package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.material.Material;

/** Hidden exact dry companions: never advertise water to vanilla entity/material physics. */
final class BlockAbyssalDryAquatic extends BlockAbyssalAquatic {

    BlockAbyssalDryAquatic(String name, Kind kind, String... textures) {
        super(name, kind, kind == Kind.MUSSEL ? Material.circuits : Material.rock, textures);
        if (kind != Kind.MUSSEL && kind != Kind.VENT_MEDIUM && kind != Kind.VENT_THIN)
            throw new IllegalArgumentException("Unsupported dry Abyssal companion");
    }

    @Override
    public boolean isDryCarrier() {
        return true;
    }
}
