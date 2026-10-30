package industries.angelic.biomic.debug;

import static org.junit.Assert.*;

import net.minecraft.command.server.CommandTeleport;
import net.minecraft.entity.player.EntityPlayerMP;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Actual test-classpath native seams; effective runtime transformed injection remains OPEN. */
public class NativeTeleportMemberOrderTest {

    private static ClassNode read(Class<?> type) throws Exception {
        var n = new ClassNode();
        try (var in = type.getResourceAsStream(type.getSimpleName() + ".class")) {
            assertNotNull(in);
            new ClassReader(in).accept(n, 0);
        }
        return n;
    }

    @Test
    public void commandSuccessNotificationsAreExactlyTheThreeBoundNativeCalls() throws Exception {
        var n = read(CommandTeleport.class);
        var m = n.methods.stream()
            .filter(
                x -> x.name.equals("processCommand")
                    && x.desc.equals("(Lnet/minecraft/command/ICommandSender;[Ljava/lang/String;)V"))
            .findFirst()
            .orElseThrow();
        int notices = 0;
        for (var i : m.instructions.toArray())
            if (i instanceof MethodInsnNode call && call.name.equals("func_152373_a")) {
                assertEquals("net/minecraft/command/server/CommandTeleport", call.owner);
                assertEquals(
                    "(Lnet/minecraft/command/ICommandSender;Lnet/minecraft/command/ICommand;Ljava/lang/String;[Ljava/lang/Object;)V",
                    call.desc);
                notices++;
            }
        assertEquals(3, notices);
    }

    @Test
    public void numericPlayerTeleportDelegatesToOneNativeSetterWithoutPremutation() throws Exception {
        var n = read(EntityPlayerMP.class);
        var m = n.methods.stream()
            .filter(x -> x.name.equals("setPositionAndUpdate") && x.desc.equals("(DDD)V"))
            .findFirst()
            .orElseThrow();
        int setters = 0;
        for (var i : m.instructions.toArray()) if (i instanceof MethodInsnNode call) {
            assertFalse(call.name.equals("setPosition") || call.name.equals("setLocationAndAngles"));
            if (call.name.equals("setPlayerLocation")) {
                assertEquals("(DDDFF)V", call.desc);
                setters++;
            }
        }
        assertEquals(1, setters);
    }
}
