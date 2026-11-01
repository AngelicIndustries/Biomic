package industries.angelic.biomic.content.block;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Every water tick asks RetainedPlantSourceWater (MixinRetainedPlantWaterDecay), Et Futurum Requiem or not: a class
 * it names that extends EFR's loads EFR's, and without EFR that is a NoClassDefFoundError on the server thread. The
 * test classpath has EFR, so the crash cannot be run here; the bytecode is checked instead.
 */
public class RetainedPlantSourceWaterEfrTest {

    @Test
    public void namesNoClassBuiltOnEtFuturum() throws IOException {
        Set<String> named = new TreeSet<>();
        read(
            RetainedPlantSourceWater.class.getName()
                .replace('.', '/')).accept(new ClassVisitor(Opcodes.ASM5) {

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                        String[] exceptions) {
                        return new MethodVisitor(Opcodes.ASM5) {

                            @Override
                            public void visitTypeInsn(int opcode, String type) {
                                named.add(type);
                            }

                            @Override
                            public void visitMethodInsn(int opcode, String owner, String method, String desc,
                                boolean itf) {
                                named.add(owner);
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String field, String desc) {
                                named.add(owner);
                            }
                        };
                    }
                }, ClassReader.SKIP_FRAMES);
        assertTrue("read no references", !named.isEmpty());
        for (String type : named) if (type.startsWith("industries/angelic/biomic/"))
            assertTrue(type + " is built on an EFR class", !builtOnEfr(type));
    }

    private static boolean builtOnEfr(String type) throws IOException {
        for (String current = type; current != null && !current.startsWith("java/");) {
            if (current.startsWith("ganymedes01/")) return true;
            if (!current.startsWith("industries/angelic/biomic/")) return false;
            current = read(current).getSuperName();
        }
        return false;
    }

    private static ClassReader read(String internalName) throws IOException {
        try (InputStream stream = RetainedPlantSourceWaterEfrTest.class.getClassLoader()
            .getResourceAsStream(internalName + ".class")) {
            return new ClassReader(stream);
        }
    }
}
