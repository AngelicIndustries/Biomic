package industries.angelic.biomic.world;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.server.management.ServerConfigurationManager;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Actual installed Forge member chronology; does not assert that FML applied the new mixins. */
public class ColdLoginNativeSeamTest {

    static ClassNode node(Class<?> c) throws Exception {
        ClassNode n = new ClassNode();
        try (var in = c.getResourceAsStream(c.getSimpleName() + ".class")) {
            assertNotNull(in);
            new ClassReader(in).accept(n, 0);
        }
        return n;
    }

    @Test
    public void originalThreeArgumentLoginRestoresNBTBeforeWorldSelectionAndKeepsRidingAfterLoggedIn()
        throws Exception {
        var n = node(ServerConfigurationManager.class);
        var body = n.methods.stream()
            .filter(
                m -> m.name.equals("initializeConnectionToPlayer") && m.desc.equals(
                    "(Lnet/minecraft/network/NetworkManager;Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/network/NetHandlerPlayServer;)V"))
            .findFirst()
            .orElseThrow();
        int read = -1, select = -1, setWorld = -1, logged = -1, riding = -1, readCount = 0, index = 0;
        for (var insn : body.instructions.toArray()) {
            if (insn instanceof MethodInsnNode call) {
                if (call.name.equals("readPlayerDataFromFile")) {
                    read = index;
                    readCount++;
                }
                if (call.name.equals("worldServerForDimension") && select < 0) select = index;
                if (call.name.equals("setWorld") && setWorld < 0) setWorld = index;
                if (call.name.equals("firePlayerLoggedIn")) logged = index;
                if (call.name.equals("createEntityFromNBT")) riding = index;
            }
            index++;
        }
        assertEquals(1, readCount);
        assertTrue(read >= 0 && read < select && select < setWorld);
        assertTrue(logged > setWorld && riding > logged);
    }

    @Test
    public void originalLogoutWritesAfterForgeLogoutEventAndEntitySerializerKeepsSpatialContract() throws Exception {
        var n = node(ServerConfigurationManager.class);
        var logout = n.methods.stream()
            .filter(
                m -> m.name.equals("playerLoggedOut")
                    && m.desc.equals("(Lnet/minecraft/entity/player/EntityPlayerMP;)V"))
            .findFirst()
            .orElseThrow();
        int event = -1, save = -1, index = 0;
        for (var insn : logout.instructions.toArray()) {
            if (insn instanceof MethodInsnNode call) {
                if (call.name.equals("firePlayerLoggedOut")) event = index;
                if (call.name.equals("writePlayerData")) save = index;
            }
            index++;
        }
        assertTrue(event >= 0 && save > event);
        var serializer = node(Entity.class).methods.stream()
            .filter(m -> m.name.equals("writeToNBT") && m.desc.equals("(Lnet/minecraft/nbt/NBTTagCompound;)V"))
            .findFirst()
            .orElseThrow();
        Set<String> keys = new HashSet<>();
        for (var insn : serializer.instructions.toArray())
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String key) keys.add(key);
        assertTrue(
            keys.containsAll(List.of("Pos", "Motion", "Rotation", "FallDistance", "OnGround", "Dimension", "Riding")));
    }
}
