package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockGrass;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockVine;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.client.HostedBiomeColors;

/** Vanilla fallback. Angelica replaces these calls with its own separately bridged 3D cache. */
@Mixin({ BlockGrass.class, BlockLeaves.class, BlockTallGrass.class, BlockDoublePlant.class, BlockLiquid.class,
    BlockVine.class })
public abstract class MixinVanillaBiomeColors {

    @Redirect(
        method = "colorMultiplier",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/IBlockAccess;getBiomeGenForCoords(II)Lnet/minecraft/world/biome/BiomeGenBase;"),
        require = 0)
    private BiomeGenBase biomic$biome(IBlockAccess access, int x, int z, IBlockAccess original, int bx, int by,
        int bz) {
        return HostedBiomeColors.biome(access, x, by, z);
    }
}
