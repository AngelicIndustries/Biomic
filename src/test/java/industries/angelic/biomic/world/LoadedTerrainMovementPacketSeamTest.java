package industries.angelic.biomic.world;

import static org.junit.Assert.*;

import java.util.ArrayList;

import net.minecraft.network.NetHandlerPlayServer;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Actual installed test-classpath vanilla member ordering. Runtime transformed injection remains a native gate. */
public class LoadedTerrainMovementPacketSeamTest {

    @Test
    public void normalThirdUpdateAndFirstCollisionKeepStanceBorderAndSpeedChecksInOriginalOrder() throws Exception {
        var n = new ClassNode();
        try (var in = NetHandlerPlayServer.class.getResourceAsStream("NetHandlerPlayServer.class")) {
            assertNotNull(in);
            new ClassReader(in).accept(n, 0);
        }
        var body = n.methods.stream()
            .filter(
                m -> m.name.equals("processPlayer")
                    && m.desc.equals("(Lnet/minecraft/network/play/client/C03PacketPlayer;)V"))
            .findFirst()
            .orElseThrow();
        var updates = new ArrayList<Integer>();
        var collisions = new ArrayList<Integer>();
        int stance = -1, border = -1, speed = -1, index = 0;
        for (var insn : body.instructions.toArray()) {
            if (insn instanceof MethodInsnNode call) {
                if (call.owner.equals("net/minecraft/entity/player/EntityPlayerMP")
                    && call.name.equals("onUpdateEntity")
                    && call.desc.equals("()V")) updates.add(index);
                if (call.owner.equals("net/minecraft/world/WorldServer")
                    && call.name.equals("getCollidingBoundingBoxes")
                    && call.desc
                        .equals("(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"))
                    collisions.add(index);
            }
            if (insn instanceof LdcInsnNode constant && constant.cst instanceof String text) {
                if (text.equals("Illegal stance")) stance = index;
                if (text.equals("Illegal position")) border = index;
                if (text.contains("moved too quickly")) speed = index;
            }
            index++;
        }
        assertEquals("Mount, sleeping, normal updates must be exactly distinct", 3, updates.size());
        assertEquals(2, collisions.size());
        assertTrue(stance >= 0 && border >= 0 && stance < updates.get(2) && border < updates.get(2));
        assertTrue(
            "Final hold is after actual speed branch, before first world collision",
            updates.get(2) < speed && speed < collisions.get(0));
        assertTrue(updates.get(0) < stance && updates.get(1) < stance);
    }
}
