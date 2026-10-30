package industries.angelic.biomic.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;

/** Debug: {@code /copypos} copies the block the player stands in, as "x y z", to the clipboard. Client side only. */
public final class CopyPosCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "copypos";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/copypos";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        EntityClientPlayerMP player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return;
        String position = MathHelper.floor_double(player.posX) + " "
            + MathHelper.floor_double(player.boundingBox.minY)
            + " "
            + MathHelper.floor_double(player.posZ);
        GuiScreen.setClipboardString(position);
        sender.addChatMessage(new ChatComponentText("Copied " + position));
    }
}
