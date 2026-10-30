package industries.angelic.biomic.world.preparation;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import org.junit.Assume;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/** Checks the exact pinned 2.7.216 dev selectors used by automatic admission; native boot checks remapping. */
public class HodgeAutomaticAdmissionBytecodeTest {

    @Test
    public void currentSchedulerRetainsExactLoadAndPopulationAdmissionSites() throws Exception {
        String named = System.getProperty("biomic.currentHodgeJar");
        Assume.assumeTrue("Specify current dev Hodge jar for dependency contract audit", named != null);
        assertTrue(Files.isRegularFile(Path.of(named)));
        final Map<String, Integer> sites = new HashMap<>();
        final Map<String, Set<String>> locals = new HashMap<>();
        try (var jar = new ZipFile(named);
            var stream = jar
                .getInputStream(jar.getEntry("com/mitchej123/hodgepodge/mixins/hooks/ChunkGenScheduler.class"))) {
            new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public MethodVisitor visitMethod(int access, final String method, String descriptor, String signature,
                    String[] exceptions) {
                    return new MethodVisitor(Opcodes.ASM5) {

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                            String key = method + ":" + owner + ":" + name + desc;
                            sites.put(key, sites.getOrDefault(key, 0) + 1);
                        }

                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start,
                            Label end, int index) {
                            locals.computeIfAbsent(method, ignored -> new HashSet<>())
                                .add(name);
                        }
                    };
                }
            }, ClassReader.SKIP_FRAMES);
        }
        String cps = "net/minecraft/world/gen/ChunkProviderServer:";
        String hodge = "com/mitchej123/hodgepodge/mixins/hooks/ChunkGenScheduler:";
        assertEquals(
            Integer.valueOf(1),
            sites.get("fillMissingNeighbors:" + cps + "provideChunk(II)Lnet/minecraft/world/chunk/Chunk;"));
        assertEquals(
            Integer.valueOf(1),
            sites.get("forceProcessAged:" + cps + "provideChunk(II)Lnet/minecraft/world/chunk/Chunk;"));
        assertEquals(
            Integer.valueOf(1),
            sites.get("generateTerrain:it/unimi/dsi/fastutil/longs/LongList:removeLong(I)J"));
        assertEquals(
            Integer.valueOf(1),
            sites.get("processPopulationQueue:" + hodge + "has3x3Terrain(Lnet/minecraft/util/LongHashMap;II)Z"));
        assertEquals(
            Integer.valueOf(1),
            sites.get("forceProcessAged:" + hodge + "minPlayerDistSq(Lnet/minecraft/world/WorldServer;III)D"));
        assertTrue(
            locals.get("fillMissingNeighbors")
                .containsAll(Set.of("filled", "nx", "nz")));
        assertTrue(
            locals.get("forceProcessAged")
                .containsAll(Set.of("cx", "cz", "neighborsFilled", "nx", "nz")));
        assertTrue(
            locals.get("generateTerrain")
                .containsAll(Set.of("totalGen", "packed")));
    }
}
