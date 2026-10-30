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
import org.objectweb.asm.tree.TypeInsnNode;

/** Native constructor/class resources in actual focused harness; live Mixin transform is a later gate. */
public class NativeOwnerConstructorSelectorTest {

    static ClassNode type(String name) throws Exception {
        var node = new ClassNode();
        try (InputStream in = NativeOwnerConstructorSelectorTest.class.getResourceAsStream('/' + name + ".class")) {
            assertNotNull(in);
            new ClassReader(in).accept(node, 0);
        }
        return node;
    }

    @Test
    public void actualWorldSuperclassRunsBeforeServerAssignmentAndHotloadHasSoleExactCtor() throws Exception {
        var world = type("net/minecraft/world/WorldServer");
        boolean found = false;
        for (MethodNode m : world.methods)
            if (m.name.equals("<init>") && m.desc.startsWith("(Lnet/minecraft/server/MinecraftServer;")) {
                int superIndex = -1, assignIndex = -1, index = 0;
                for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext(), index++) {
                    if (i instanceof MethodInsnNode v && v.getOpcode() == Opcodes.INVOKESPECIAL
                        && v.owner.equals("net/minecraft/world/World")
                        && v.name.equals("<init>")) superIndex = index;
                    if (i instanceof FieldInsnNode f && f.getOpcode() == Opcodes.PUTFIELD
                        && f.owner.equals(world.name)
                        && f.name.equals("mcServer")) assignIndex = index;
                }
                assertTrue(superIndex >= 0 && assignIndex > superIndex);
                found = true;
            }
        assertTrue(found);
        var forge = type("net/minecraftforge/common/DimensionManager");
        int news = 0, ctors = 0;
        for (MethodNode m : forge.methods) if (m.name.equals("initDimension") && m.desc.equals("(I)V"))
            for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
                if (i instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW
                    && t.desc.equals("net/minecraft/world/WorldServerMulti")) news++;
                if (i instanceof MethodInsnNode c && c.getOpcode() == Opcodes.INVOKESPECIAL
                    && c.owner.equals("net/minecraft/world/WorldServerMulti")
                    && c.name.equals("<init>")) {
                    assertEquals(
                        "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/world/storage/ISaveHandler;Ljava/lang/String;ILnet/minecraft/world/WorldSettings;Lnet/minecraft/world/WorldServer;Lnet/minecraft/profiler/Profiler;)V",
                        c.desc);
                    ctors++;
                }
            }
        assertEquals(1, news);
        assertEquals(1, ctors);
    }
}
