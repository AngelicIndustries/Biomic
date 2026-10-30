package industries.angelic.biomic.mixins.late.etfuturum;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ganymedes01.etfuturum.client.renderer.block.BlockPinkPetalsRenderer;
import industries.angelic.biomic.content.block.BlockClover;

/** Applies the biome grass tint to Biomic's grayscale clover faces in EFR's petal renderer. */
@Mixin(value = BlockPinkPetalsRenderer.class, remap = false)
public abstract class MixinBlockPinkPetalsRenderer {

    @Redirect(
        method = "renderWorldBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;setColorOpaque_F(FFF)V",
            remap = true),
        remap = false,
        require = 4,
        expect = 4)
    private void biomic$tintCloverFaces(Tessellator tessellator, float red, float green, float blue, IBlockAccess world,
        int blockX, int blockY, int blockZ, Block block, int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockClover)) {
            tessellator.setColorOpaque_F(red, green, blue);
            return;
        }
        int color = world.getBiomeGenForCoords(blockX, blockZ)
            .getBiomeGrassColor(blockX, blockY, blockZ);
        tessellator.setColorOpaque_F((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
    }
}
