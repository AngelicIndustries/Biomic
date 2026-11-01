package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.Block;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.content.block.BlockWoodSign;

/**
 * The sign editor turns a standing sign to face the player only when it is vanilla's standing sign block, and turns
 * every other sign as a wall sign: Biomic's woods' standing signs showed up turned by their facing, often backwards.
 * They are taken for vanilla's standing sign there.
 */
@Mixin(GuiEditSign.class)
public abstract class MixinGuiEditSignWoodSign {

    @Redirect(
        method = "drawScreen",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/tileentity/TileEntitySign;getBlockType()Lnet/minecraft/block/Block;"),
        require = 1)
    private Block biomic$standingWoodSign(TileEntitySign sign) {
        Block block = sign.getBlockType();
        return block instanceof BlockWoodSign wood && wood.standing() ? Blocks.standing_sign : block;
    }
}
