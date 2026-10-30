package industries.angelic.biomic.world;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.zip.ZipFile;

import org.junit.Assume;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Exact updated-dependency site: cancellation must retain root status and avoid depth/finally mutation. */
public class OrdinaryPopulationAgedHookTest {

    @Test
    public void exactPinnedAgedHookPrecedesDepthIncrementAndPopulateTryCleanup() throws Exception {
        String path = System.getProperty("biomic.currentHodgeJar");
        Assume.assumeNotNull(path);
        var code = new ArrayList<String>();
        var labels = new IdentityHashMap<Label, Integer>();
        var tries = new ArrayList<Label>();
        var locals = new HashMap<String, List<Object>>();
        try (var jar = new ZipFile(path);
            var in = jar
                .getInputStream(jar.getEntry("com/mitchej123/hodgepodge/mixins/hooks/ChunkGenScheduler.class"))) {
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                    if (!n.equals("forceProcessAged")) return null;
                    return new MethodVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitInsn(int op) {
                            code.add("op:" + op);
                        }

                        @Override
                        public void visitVarInsn(int op, int v) {
                            code.add("var:" + op + ":" + v);
                        }

                        @Override
                        public void visitJumpInsn(int op, Label l) {
                            code.add("jump:" + op);
                        }

                        @Override
                        public void visitIincInsn(int v, int i) {
                            code.add("inc:" + v);
                        }

                        @Override
                        public void visitIntInsn(int op, int v) {
                            code.add("int:" + op);
                        }

                        @Override
                        public void visitTypeInsn(int op, String t) {
                            code.add("type:" + op);
                        }

                        @Override
                        public void visitLdcInsn(Object v) {
                            code.add("ldc");
                        }

                        @Override
                        public void visitFieldInsn(int op, String owner, String name, String desc) {
                            code.add("field:" + op + ":" + owner + ":" + name + ":" + desc);
                        }

                        @Override
                        public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                            code.add("call:" + owner + ":" + name);
                        }

                        @Override
                        public void visitLabel(Label l) {
                            labels.put(l, code.size());
                        }

                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, String t) {
                            tries.add(start);
                        }

                        @Override
                        public void visitLocalVariable(String name, String d, String sig, Label start, Label end,
                            int index) {
                            locals.put(name, List.of(start, end, index));
                        }
                    };
                }
            }, 0);
        }
        String owner = "com/mitchej123/hodgepodge/mixins/hooks/ChunkGenScheduler";
        int read = code.indexOf("field:" + Opcodes.GETFIELD + ":" + owner + ":populationDepth:I");
        int write = code.indexOf("field:" + Opcodes.PUTFIELD + ":" + owner + ":populationDepth:I");
        int pop = code.indexOf("call:net/minecraft/world/gen/ChunkProviderServer:populate");
        assertTrue(read >= 0 && read < write && write < pop);
        assertTrue(
            tries.stream()
                .map(labels::get)
                .anyMatch(i -> i > read && i <= pop));
        for (String local : List.of("cx", "cz")) {
            var l = locals.get(local);
            assertNotNull(l);
            assertTrue(labels.get(l.get(0)) <= read && labels.get(l.get(1)) > read);
            assertEquals(local.equals("cx") ? 12 : 13, l.get(2));
        }
        int clear = -1;
        for (int i = pop + 1; i < code.size(); i++) if (code.get(i)
            .equals("call:" + owner + ":removeStatus")) {
                clear = i;
                break;
            }
        assertTrue(clear > pop);
    }
}
