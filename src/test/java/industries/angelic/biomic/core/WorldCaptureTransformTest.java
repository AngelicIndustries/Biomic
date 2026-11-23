package industries.angelic.biomic.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;

import industries.angelic.biomic.compat.flora.WorldCaptureHooks;

/**
 * The transform calls WorldCaptureHooks by name and descriptor; a hook whose signature drifted from its World method
 * would only fail when World loads. Every hook pair exists with the World method's arguments, and every method gets
 * its guard ahead of the code already there.
 */
public class WorldCaptureTransformTest {

    @Test
    public void everyHookPairMatchesItsWorldMethod() {
        Map<String, Method> hooks = new HashMap<>();
        for (Method method : WorldCaptureHooks.class.getMethods())
            hooks.put(method.getName() + Type.getMethodDescriptor(method), method);
        for (WorldCaptureTransform.Hook hook : WorldCaptureTransform.WORLD) {
            String parameters = "(Lnet/minecraft/world/World;" + hook.desc()
                .substring(
                    1,
                    hook.desc()
                        .indexOf(')'))
                + ")";
            assertNotNull(hook + " Applies", hooks.get(hook.hook() + "Applies" + parameters + "Z"));
            assertNotNull(
                hook + " answer",
                hooks.get(
                    hook.hook() + parameters
                        + Type.getReturnType(hook.desc())
                            .getDescriptor()));
        }
    }

    @Test
    public void guardsGoAheadOfExistingCode() {
        ClassNode world = new ClassNode();
        world.name = "net/minecraft/world/World";
        for (WorldCaptureTransform.Hook hook : WorldCaptureTransform.WORLD) {
            MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, hook.srg(), hook.desc(), null, null);
            method.instructions.add(new InsnNode(Opcodes.NOP));
            world.methods.add(method);
        }
        WorldCaptureTransform.apply(world);
        for (MethodNode method : world.methods) {
            var first = method.instructions.getFirst();
            while (!(first instanceof MethodInsnNode)) first = first.getNext();
            MethodInsnNode call = (MethodInsnNode) first;
            assertEquals("industries/angelic/biomic/compat/flora/WorldCaptureHooks", call.owner);
            assertTrue(call.name.endsWith("Applies"));
            assertEquals(
                Opcodes.NOP,
                method.instructions.getLast()
                    .getOpcode());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void aMissingWorldMethodStopsTheGame() {
        WorldCaptureTransform.apply(new ClassNode());
    }
}
