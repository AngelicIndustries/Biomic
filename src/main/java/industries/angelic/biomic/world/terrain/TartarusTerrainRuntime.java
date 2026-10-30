package industries.angelic.biomic.world.terrain;

import com.google.gson.JsonObject;

/** Pinned adapter for the requested Tartarus fork; authored GPL data stays in its own resource directory. */
public final class TartarusTerrainRuntime extends TerraPackTerrainRuntime {

    // Source options.yml pins bedrock-top-y=255; modern Nether has a 0..255 build envelope.
    public static final int SOURCE_MIN_Y = 0;
    public static final int SOURCE_MAX_Y = 256;
    public static final String RESOURCE = "/assets/biomic/tartarus/terrain-density.json";
    public static final String SOURCE_COMMIT = "0f4d435c8eec4287a7176c895ba14667053d1cf5";

    public static TartarusTerrainRuntime load(JsonObject manifest) {
        return new TartarusTerrainRuntime(manifest);
    }

    TartarusTerrainRuntime(JsonObject manifest) {
        super(manifest, "Jason-Ding19/Tartarus", SOURCE_COMMIT);
    }
}
