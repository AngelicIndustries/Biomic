package industries.angelic.biomic.client.browser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The key that opens the biome browser (unbound until the user binds it), and {@code /biomes}, which does too. */
@SideOnly(Side.CLIENT)
public final class BrowserKeys {

    private static final KeyBinding OPEN = new KeyBinding(
        "key.biomic.browser",
        Keyboard.KEY_NONE,
        "key.categories.biomic");
    /** Set by the command: the chat closes after it runs, so the browser opens on the next tick. */
    private static volatile boolean openNextTick;
    /**
     * A developer's check of capture ({@code -Dbiomic.browserCaptureTest=<biome key>}): ten seconds into a world,
     * capture
     * mode opens for the biome, takes its shot on its own and the game quits; the log says where the thumbnail went.
     */
    private static final String CAPTURE_TEST = System.getProperty("biomic.browserCaptureTest");
    private static int captureTestTicks;

    private BrowserKeys() {}

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN);
        FMLCommonHandler.instance()
            .bus()
            .register(new BrowserKeys());
        GuiCapture.register();
        BrowserScreenTest.register();
        net.minecraftforge.client.ClientCommandHandler.instance
            .registerCommand(new net.minecraft.command.CommandBase() {

                @Override
                public String getCommandName() {
                    return "biomes";
                }

                @Override
                public String getCommandUsage(net.minecraft.command.ICommandSender sender) {
                    return "/biomes";
                }

                @Override
                public int getRequiredPermissionLevel() {
                    return 0;
                }

                @Override
                public boolean canCommandSenderUseCommand(net.minecraft.command.ICommandSender sender) {
                    return true;
                }

                @Override
                public void processCommand(net.minecraft.command.ICommandSender sender, String[] args) {
                    openNextTick = true;
                }
            });
    }

    @SubscribeEvent
    public void tick(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        // How a search ended, while the browser is closed (or a dimension change closed it), goes to the chat.
        if (mc.thePlayer != null && !(mc.currentScreen instanceof GuiBiomeBrowser)) {
            industries.angelic.biomic.browser.BrowserReplies.Status status = industries.angelic.biomic.browser.BrowserReplies
                .takeStatus();
            if (status != null
                && status.progress() != industries.angelic.biomic.browser.BrowserMessages.Progress.LOOKING)
                chat(status.text());
        }
        if (CAPTURE_TEST != null && mc.thePlayer != null && ++captureTestTicks == 200) {
            // An unfocused window would pause the game behind its menu: the test goes over whatever is open.
            mc.gameSettings.pauseOnLostFocus = false;
            mc.displayGuiScreen(new GuiCapture(CAPTURE_TEST, CAPTURE_TEST, null, true));
        }
        if (!openNextTick || mc.currentScreen != null || mc.theWorld == null) return;
        openNextTick = false;
        mc.displayGuiScreen(new GuiBiomeBrowser());
    }

    static void chat(String text) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        net.minecraft.util.ChatComponentText line = new net.minecraft.util.ChatComponentText(text);
        line.getChatStyle()
            .setColor(net.minecraft.util.EnumChatFormatting.GRAY);
        mc.thePlayer.addChatMessage(line);
    }

    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OPEN.isPressed() && mc.currentScreen == null && mc.theWorld != null)
            mc.displayGuiScreen(new GuiBiomeBrowser());
    }
}
