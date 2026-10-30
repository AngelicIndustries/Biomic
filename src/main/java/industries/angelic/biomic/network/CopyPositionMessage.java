package industries.angelic.biomic.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client-bound block coordinates; arbitrary clipboard contents never cross the network. */
public final class CopyPositionMessage implements IMessage {

    private int blockX;
    private int blockY;
    private int blockZ;

    public CopyPositionMessage() {}

    @Override
    public void fromBytes(ByteBuf buffer) {
        blockX = buffer.readInt();
        blockY = buffer.readInt();
        blockZ = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(blockX);
        buffer.writeInt(blockY);
        buffer.writeInt(blockZ);
    }

    static String coordinateString(int blockX, int blockY, int blockZ) {
        return blockX + " " + blockY + " " + blockZ;
    }

    public static final class Handler implements IMessageHandler<CopyPositionMessage, IMessage> {

        @Override
        public IMessage onMessage(CopyPositionMessage message, MessageContext context) {
            if (!context.side.isClient()) {
                return null;
            }
            Minecraft minecraft = Minecraft.getMinecraft();
            int blockX = message.blockX;
            int blockY = message.blockY;
            int blockZ = message.blockZ;
            minecraft.func_152344_a(() -> copyPosition(minecraft, blockX, blockY, blockZ));
            return null;
        }

        private static void copyPosition(Minecraft minecraft, int blockX, int blockY, int blockZ) {
            if (minecraft.thePlayer == null) {
                return;
            }
            String coordinates = coordinateString(blockX, blockY, blockZ);
            GuiScreen.setClipboardString(coordinates);
            ChatComponentText confirmation = new ChatComponentText("Copied coordinates: " + coordinates);
            confirmation.getChatStyle()
                .setColor(EnumChatFormatting.GREEN);
            minecraft.thePlayer.addChatMessage(confirmation);
        }
    }
}
