package industries.angelic.biomic.world;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Exact native class available in this harness; actual live Mixin transform remains a separate gate. */
public class OwnerNormalStopSelectorTest {

    @Test
    public void actualNativeRunHasSoleFlagReadAndSleepQueryAndLiteralRequestWrite() throws Exception {
        ClassNode type = new ClassNode();
        try (InputStream in = getClass().getResourceAsStream("/net/minecraft/server/MinecraftServer.class")) {
            assertNotNull("Native MinecraftServer bytes must be present", in);
            new ClassReader(in).accept(type, 0);
        }
        MethodNode run = null, request = null;
        for (MethodNode m : type.methods) {
            if (m.name.equals("run") && m.desc.equals("()V")) run = m;
            if (m.name.equals("initiateShutdown") && m.desc.equals("()V")) request = m;
        }
        assertNotNull(run);
        assertNotNull(request);
        int reads = 0, sleepQueries = 0, writes = 0;
        for (AbstractInsnNode i = run.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETFIELD
                && f.owner.equals(type.name)
                && f.name.equals("serverRunning")
                && f.desc.equals("Z")) reads++;
            if (i instanceof MethodInsnNode m && m.owner.equals("net/minecraft/world/WorldServer")
                && m.name.equals("areAllPlayersAsleep")
                && m.desc.equals("()Z")) sleepQueries++;
        }
        for (AbstractInsnNode i = request.instructions.getFirst(); i != null; i = i.getNext())
            if (i instanceof FieldInsnNode f && f.getOpcode() == Opcodes.PUTFIELD
                && f.owner.equals(type.name)
                && f.name.equals("serverRunning")
                && f.desc.equals("Z")) {
                    AbstractInsnNode previous = i.getPrevious();
                    while (previous != null && previous.getOpcode() < 0) previous = previous.getPrevious();
                    assertNotNull(previous);
                    assertEquals(Opcodes.ICONST_0, previous.getOpcode());
                    writes++;
                }
        assertEquals("sole run GETFIELD required", 1, reads);
        assertEquals("sole asleep invoke required", 1, sleepQueries);
        assertEquals("original initiateShutdown false flag write", 1, writes);
    }
}
