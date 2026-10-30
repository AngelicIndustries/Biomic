package industries.angelic.biomic.world;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.storage.ThreadedFileIOBase;

import org.junit.Test;

import cpw.mods.fml.relauncher.ReflectionHelper;

/** The exact MCP/SRG aliases used by the owner-only native save fence must resolve in Forge. */
public class DeterministicNativeReflectionLinkageTest {

    @Test
    public void anvilPendingAndThreadedIoQueueFieldsResolveWithPinnedAliases() {
        var lock = ReflectionHelper.findField(AnvilChunkLoader.class, "syncLockObject", "field_75827_c");
        var pending = ReflectionHelper
            .findField(AnvilChunkLoader.class, "pendingAnvilChunksCoordinates", "field_75826_b");
        var queue = ReflectionHelper.findField(ThreadedFileIOBase.class, "threadedIOQueue", "field_75739_b");
        assertEquals(Object.class, lock.getType());
        assertTrue(Set.class.isAssignableFrom(pending.getType()));
        assertTrue(List.class.isAssignableFrom(queue.getType()));
    }
}
