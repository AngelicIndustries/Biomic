package industries.angelic.biomic.world;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;

/**
 * Byte ids (0..254) for the Tartarus pack's block states, shared by the terrain workers and the server thread. Air is
 * 0.
 * Each id resolves once to a block and metadata.
 */
final class NetherStates {

    static final byte AIR = 0;
    /** A mod's block by registry name and metadata, for other mods' Nether grounds: native:modid:name@meta. */
    static final String NATIVE = "native:";

    static String nativeState(Block block, int metadata) {
        return NATIVE + Block.blockRegistry.getNameForObject(block) + "@" + metadata;
    }

    private final Map<String, Byte> ids = new HashMap<>();
    private String[] states = { "minecraft:air" };
    private Block[] blocks = { Blocks.air };
    private byte[] metadata = { 0 };
    private byte[] emission = { 0 }, opacity = { 0 };

    NetherStates() {
        ids.put("minecraft:air", AIR);
    }

    synchronized byte id(String state) {
        Byte id = ids.get(state);
        if (id != null) return id;
        // 255 states at most: byte -1 (id 255) is the "no write" value in feature overlays.
        if (states.length == 255) throw new IllegalStateException("Tartarus produced more than 255 block states");
        Block block;
        int meta;
        if (state.startsWith(NATIVE)) {
            int at = state.lastIndexOf('@');
            block = Block.getBlockFromName(state.substring(NATIVE.length(), at));
            meta = Integer.parseInt(state.substring(at + 1));
            if (block == null || block == Blocks.air) throw new IllegalArgumentException("No such block: " + state);
        } else {
            var resolved = new OrigenBlockStateResolver().resolve(state);
            block = resolved.block();
            meta = resolved.metadata();
        }
        int next = states.length;
        states = Arrays.copyOf(states, next + 1);
        blocks = Arrays.copyOf(blocks, next + 1);
        metadata = Arrays.copyOf(metadata, next + 1);
        emission = Arrays.copyOf(emission, next + 1);
        opacity = Arrays.copyOf(opacity, next + 1);
        states[next] = state;
        blocks[next] = block;
        metadata[next] = (byte) meta;
        emission[next] = (byte) Math.min(15, blocks[next].getLightValue());
        opacity[next] = (byte) Math.min(15, blocks[next].getLightOpacity());
        ids.put(state, id = (byte) next);
        return id;
    }

    /** Arrays only grow and entries never change, so a snapshot stays valid for every id it holds. */
    synchronized String[] states() {
        return states;
    }

    synchronized Block[] blocks() {
        return blocks;
    }

    synchronized byte[] metadata() {
        return metadata;
    }

    /** Light each state gives off, 0..15. */
    synchronized byte[] emission() {
        return emission;
    }

    /** Light each state absorbs, 0..15. */
    synchronized byte[] opacity() {
        return opacity;
    }
}
