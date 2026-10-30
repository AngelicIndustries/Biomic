package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.BlockGrass;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.biomic.biome.BiomicBiomes;

/** Renders vanilla grass with the pre-colored Alpha artwork inside Alpha Grove without changing saved block IDs. */
@Mixin(value = BlockGrass.class, priority = 500)
public abstract class MixinBlockGrassAlphaRendering {

    @Unique
    private static final String BIOMIC$ALPHA_GROVE = "biomic:alpha_grove";

    @Unique
    private static volatile BiomeGenBase biomic$alphaGrove;

    @Unique
    private IIcon biomic$alphaTop;

    @Unique
    private IIcon biomic$alphaSide;

    @Unique
    private IIcon biomic$alphaSnowySide;

    @Inject(method = "registerBlockIcons", at = @At("TAIL"))
    private void biomic$registerAlphaGrassIcons(IIconRegister register, CallbackInfo callbackInfo) {
        biomic$alphaTop = register.registerIcon("biomic:ru_decor/alpha_grass_block_top");
        biomic$alphaSide = register.registerIcon("biomic:ru_decor/alpha_grass_side");
        biomic$alphaSnowySide = register.registerIcon("biomic:ru_decor/alpha_grass_side_snow");
    }

    @Inject(
        method = "getIcon(Lnet/minecraft/world/IBlockAccess;IIII)Lnet/minecraft/util/IIcon;",
        at = @At("HEAD"),
        cancellable = true)
    private void biomic$useAlphaGrassIcon(IBlockAccess world, int blockX, int blockY, int blockZ, int side,
        CallbackInfoReturnable<IIcon> callbackInfo) {
        if (!biomic$isAlphaGrove(world, blockX, blockZ)) return;
        if (side == 1) {
            callbackInfo.setReturnValue(biomic$alphaTop);
        } else if (side != 0) {
            Material above = world.getBlock(blockX, blockY + 1, blockZ)
                .getMaterial();
            callbackInfo.setReturnValue(
                above != Material.snow && above != Material.craftedSnow ? biomic$alphaSide : biomic$alphaSnowySide);
        }
    }

    @WrapMethod(method = "colorMultiplier")
    private int biomic$disableTintForPrecoloredAlphaGrass(IBlockAccess world, int blockX, int blockY, int blockZ,
        Operation<Integer> original) {
        if (biomic$isAlphaGrove(world, blockX, blockZ)) return 0xFFFFFF;
        return original.call(world, blockX, blockY, blockZ);
    }

    @Unique
    private static boolean biomic$isAlphaGrove(IBlockAccess world, int blockX, int blockZ) {
        BiomeGenBase alphaGrove = biomic$alphaGrove;
        if (alphaGrove == null) {
            alphaGrove = BiomicBiomes.getBiome(BIOMIC$ALPHA_GROVE);
            biomic$alphaGrove = alphaGrove;
        }
        return alphaGrove != null && world.getBiomeGenForCoords(blockX, blockZ) == alphaGrove;
    }
}
