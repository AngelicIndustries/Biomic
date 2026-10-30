package industries.angelic.biomic.world;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Pin the unique vanilla Y constant; neither X/Z parser bound is modified. */
public class SpawnCommandMixinLinkageTest {

    @Test
    public void worldSpawnHasOne256MaximumInProcessCommand() throws Exception {
        assertEquals(1, yMaximumCount("net/minecraft/command/server/CommandSetDefaultSpawnpoint"));
    }

    @Test
    public void playerSpawnHasOne256MaximumInProcessCommand() throws Exception {
        assertEquals(1, yMaximumCount("net/minecraft/command/CommandSetSpawnpoint"));
    }

    private static int yMaximumCount(String internalName) throws Exception {
        int[] found = { 0 };
        try (InputStream input = SpawnCommandMixinLinkageTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            assertNotNull(internalName, input);
            new ClassReader(input).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                    String[] exceptions) {
                    if (!name.equals("processCommand") && !name.equals("func_71515_b")) return null;
                    return new MethodVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitIntInsn(int opcode, int operand) {
                            if (opcode == Opcodes.SIPUSH && operand == 256) found[0]++;
                        }
                    };
                }
            }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        return found[0];
    }
}
