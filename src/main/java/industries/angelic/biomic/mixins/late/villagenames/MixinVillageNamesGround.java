package industries.angelic.biomic.mixins.late.villagenames;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockPackedIce;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import astrotibs.villagenames.utility.FunctionsVN;
import astrotibs.villagenames.village.StructureVillageVN;
import industries.angelic.biomic.world.structure.VillageContext;

/**
 * Village Names' ground and paths in a Biomic village being populated (see VillageContext). Its ground query looks
 * only in the village's layer. Its paths convert grass, dirt and sand and otherwise search downward to sea level 63;
 * on Biomic's sandstone, terracotta or rock that meant no path, and at 512 a block changed a hundred blocks down in
 * some cave. On the village's own terrain surface such ground is paved with Village Names' path block for the
 * village's style (as it paves sand), the search stops just under the surface, and off an island nothing is laid.
 */
@Pseudo
@Mixin(targets = "astrotibs.villagenames.village.StructureVillageVN", remap = false)
public abstract class MixinVillageNamesGround {

    /**
     * Village Names names and colours a village from the position of the first piece that asks, so with chunks in
     * another order a village came out in other colours. In a Biomic village every piece asks at the village's anchor.
     */
    @Inject(method = "getOrMakeVNInfo", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void biomic$anchoredInfo(World world, int x, int y, int z,
        CallbackInfoReturnable<net.minecraft.nbt.NBTTagCompound> result) {
        int[] anchor = VillageContext.anchor();
        if (anchor == null || anchor[0] == x && anchor[1] == y && anchor[2] == z) return;
        result.setReturnValue(StructureVillageVN.getOrMakeVNInfo(world, anchor[0], anchor[1], anchor[2]));
    }

    @Inject(
        method = "getAboveTopmostSolidOrLiquidBlockVN",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1)
    private static void biomic$villageGround(World world, int x, int z, CallbackInfoReturnable<Integer> result) {
        int ground = VillageContext.groundAbove(world, x, z, true);
        if (ground != Integer.MIN_VALUE) result.setReturnValue(ground);
    }

    @Inject(
        method = "setPathSpecificBlock(Lnet/minecraft/world/World;Lastrotibs/villagenames/utility/FunctionsVN$MaterialType;Lnet/minecraft/world/biome/BiomeGenBase;ZIIIZZ)I",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1)
    private static void biomic$pathOnBiomicGround(World world, FunctionsVN.MaterialType materialType,
        BiomeGenBase biome, boolean disallowModSubs, int x, int y, int z, boolean searchDownward,
        boolean allowGrassPath, CallbackInfoReturnable<Integer> result) {
        int surface = VillageContext.pathSurface(x, z);
        if (surface == Integer.MIN_VALUE) return;
        if (surface < 0) {
            result.setReturnValue(-1);
            return;
        }
        int surfaceY = searchDownward ? StructureVillageVN.getAboveTopmostSolidOrLiquidBlockVN(world, x, z) - 1 : y;
        if (surfaceY != surface) return;
        Block block = world.getBlock(x, surfaceY, z);
        Material material = block.getMaterial();
        // Village Names' own rules: grass and dirt to grass path, sand to its path block, water to a bridge.
        if (block instanceof BlockGrass || block instanceof BlockDirt
            || material == Material.grass
            || block instanceof BlockSand
            || material.isLiquid()
            || block instanceof BlockIce
            || block instanceof BlockPackedIce
            || Block.blockRegistry.getNameForObject(block)
                .toLowerCase()
                .contains("mud"))
            return;
        if (material != Material.rock && material != Material.clay
            && material != Material.sand
            && material != Material.ground
            && material != Material.craftedSnow) return;
        if (materialType == null) materialType = FunctionsVN.MaterialType.getMaterialTemplateForBiome(world, x, z);
        if (biome == null) biome = world.getBiomeGenForCoords(x, z);
        Object[] path = StructureVillageVN
            .getBiomeSpecificBlockObject(Blocks.gravel, 0, materialType, biome, disallowModSubs);
        Object[] base = StructureVillageVN
            .getBiomeSpecificBlockObject(Blocks.cobblestone, 0, materialType, biome, disallowModSubs);
        world.setBlock(x, surfaceY, z, (Block) path[0], (Integer) path[1], 2);
        world.setBlock(x, surfaceY - 1, z, (Block) base[0], (Integer) base[1], 2);
        result.setReturnValue(surfaceY);
    }

    /** Grass and flowers stood on the ground the path was laid in; a path keeps nothing growing on it. */
    @Inject(
        method = "setPathSpecificBlock(Lnet/minecraft/world/World;Lastrotibs/villagenames/utility/FunctionsVN$MaterialType;Lnet/minecraft/world/biome/BiomeGenBase;ZIIIZZ)I",
        at = @At("RETURN"),
        remap = false,
        require = 1)
    private static void biomic$clearPath(World world, FunctionsVN.MaterialType materialType, BiomeGenBase biome,
        boolean disallowModSubs, int x, int y, int z, boolean searchDownward, boolean allowGrassPath,
        CallbackInfoReturnable<Integer> result) {
        int surfaceY = result.getReturnValue();
        if (surfaceY < 0 || VillageContext.pathSurface(x, z) == Integer.MIN_VALUE) return;
        Material above = world.getBlock(x, surfaceY + 1, z)
            .getMaterial();
        if (above == Material.plants || above == Material.vine || above == Material.cactus)
            world.setBlock(x, surfaceY + 1, z, Blocks.air, 0, 2);
    }

    @Redirect(
        method = "setPathSpecificBlock(Lnet/minecraft/world/World;Lastrotibs/villagenames/utility/FunctionsVN$MaterialType;Lnet/minecraft/world/biome/BiomeGenBase;ZIIIZZ)I",
        at = @At(
            value = "FIELD",
            target = "Lastrotibs/villagenames/village/StructureVillageVN;seaLevel:I",
            opcode = org.objectweb.asm.Opcodes.GETSTATIC),
        remap = false,
        require = 1)
    private static int biomic$pathSearchFloor() {
        return VillageContext.pathFloor(StructureVillageVN.seaLevel);
    }
}
