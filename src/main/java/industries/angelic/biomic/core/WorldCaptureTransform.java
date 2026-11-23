package industries.angelic.biomic.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;

/**
 * Puts the native feature capture's hooks (compat.flora.WorldCaptureHooks) at the head of World's methods:
 * {@code if (WorldCaptureHooks.xApplies(this, args)) return WorldCaptureHooks.x(this, args);}. Run from the early
 * mixin config's postApply (EarlyMixinPlugin), after every mixin on World applied, so another mod's overwrite cannot
 * drop the hooks, and ahead of every other mod's code at the head. Unlike a cancellable injection it builds nothing
 * while no capture runs, which is nearly always: World.getBlock alone made a fifth of the client's garbage.
 */
public final class WorldCaptureTransform {

    private static final String HOOKS = "industries/angelic/biomic/compat/flora/WorldCaptureHooks";

    /** A World method by its development and its runtime (SRG) name, and the hook pair that answers it. */
    record Hook(String name, String srg, String desc, String hook) {}

    static final List<Hook> WORLD = List.of(
        new Hook("getBlock", "func_147439_a", "(III)Lnet/minecraft/block/Block;", "block"),
        new Hook("getBlockMetadata", "func_72805_g", "(III)I", "metadata"),
        new Hook("setBlock", "func_147465_d", "(IIILnet/minecraft/block/Block;II)Z", "setBlock"),
        new Hook("setBlockMetadataWithNotify", "func_72921_c", "(IIIII)Z", "setMetadata"),
        new Hook("getHeightValue", "func_72976_f", "(II)I", "height"),
        new Hook("getTopSolidOrLiquidBlock", "func_72825_h", "(II)I", "topSolidOrLiquid"),
        new Hook("getTileEntity", "func_147438_o", "(III)Lnet/minecraft/tileentity/TileEntity;", "tile"),
        new Hook("spawnEntityInWorld", "func_72838_d", "(Lnet/minecraft/entity/Entity;)Z", "spawn"),
        new Hook("getBiomeGenForCoords", "func_72807_a", "(II)Lnet/minecraft/world/biome/BiomeGenBase;", "biome"),
        new Hook("setTileEntity", "func_147455_a", "(IIILnet/minecraft/tileentity/TileEntity;)V", "setTile"),
        new Hook("removeTileEntity", "func_147475_p", "(III)V", "removeTile"),
        new Hook("getSavedLightValue", "func_72972_b", "(Lnet/minecraft/world/EnumSkyBlock;III)I", "savedLight"),
        new Hook("getFullBlockLightValue", "func_72883_k", "(III)I", "surfaceLight"),
        new Hook("getBlockLightValue", "func_72957_l", "(III)I", "surfaceLight"),
        new Hook("canBlockSeeTheSky", "func_72937_j", "(III)Z", "seesSky"),
        new Hook("isBlockFreezable", "func_72884_u", "(III)Z", "freezable"),
        new Hook("isSideSolid", "isSideSolid", "(IIILnet/minecraftforge/common/util/ForgeDirection;Z)Z", "sideSolid"),
        new Hook("markBlockForUpdate", "func_147471_g", "(III)V", "markUpdate"),
        new Hook(
            "markTileEntityChunkModified",
            "func_147476_b",
            "(IIILnet/minecraft/tileentity/TileEntity;)V",
            "markTileChunk"),
        new Hook("updateLightByType", "func_147463_c", "(Lnet/minecraft/world/EnumSkyBlock;III)Z", "relight"));

    private WorldCaptureTransform() {}

    /** Inserts every hook; a World method that is missing stops the game, as a required injection would. */
    public static void apply(ClassNode world) {
        Set<Hook> missing = new LinkedHashSet<>(WORLD);
        for (MethodNode method : world.methods) for (Hook hook : WORLD) {
            if (!method.desc.equals(hook.desc()) || !method.name.equals(hook.name()) && !method.name.equals(hook.srg()))
                continue;
            method.instructions.insert(guard(hook));
            missing.remove(hook);
        }
        if (!missing.isEmpty()) throw new IllegalStateException(
            "World lacks the methods Biomic's native feature capture hooks: " + missing);
    }

    private static InsnList guard(Hook hook) {
        Type[] arguments = Type.getArgumentTypes(hook.desc());
        Type result = Type.getReturnType(hook.desc());
        String parameters = "(Lnet/minecraft/world/World;" + hook.desc()
            .substring(
                1,
                hook.desc()
                    .indexOf(')'))
            + ")";
        InsnList code = new InsnList();
        LabelNode world = new LabelNode();
        load(code, arguments);
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook.hook() + "Applies", parameters + "Z", false));
        code.add(new JumpInsnNode(Opcodes.IFEQ, world));
        load(code, arguments);
        code.add(
            new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook.hook(), parameters + result.getDescriptor(), false));
        code.add(new InsnNode(result.getOpcode(Opcodes.IRETURN)));
        code.add(world);
        return code;
    }

    private static void load(InsnList code, Type[] arguments) {
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int local = 1;
        for (Type argument : arguments) {
            code.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), local));
            local += argument.getSize();
        }
    }
}
