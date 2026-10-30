package industries.angelic.biomic.client.browser;

import java.io.File;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/**
 * A developer's check of the biome browser's screen ({@code -Dbiomic.browserScreenTest=<png>}, a client JVM argument),
 * for a game no one is at: ten seconds into a world the browser opens, Redwoods is selected and gets a note and an
 * issue written as the buttons write them, the screen is saved to the file, the player is moved away and sent back to
 * the issue by clicking it, and the game quits. The log says where the issue was and where the player ended up.
 */
@SideOnly(Side.CLIENT)
public final class BrowserScreenTest {

    private static final String FILE = System.getProperty("biomic.browserScreenTest");
    /** A server ("host:port") to join from the menu, once every mod has loaded (rather than --server at launch). */
    private static final String SERVER = System.getProperty("biomic.browserScreenTestServer");

    private int ticks;
    private GuiBiomeBrowser screen;
    /** Where the next frame goes, or null. */
    private String shoot;
    private String issueAt;

    private BrowserScreenTest() {}

    static void register() {
        if (FILE != null) FMLCommonHandler.instance()
            .bus()
            .register(new BrowserScreenTest());
    }

    private int menuTicks, attempts;
    private String lastScreen;

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END) return;
        if (SERVER != null && mc.theWorld == null && mc.currentScreen != null) {
            String screenName = mc.currentScreen.getClass()
                .getName();
            if (!screenName.equals(lastScreen)) {
                lastScreen = screenName;
                Biomic.LOG.info("Browser screen test: screen {}", screenName);
                if (mc.currentScreen instanceof net.minecraft.client.gui.GuiDisconnected) try {
                    Object reason = cpw.mods.fml.relauncher.ReflectionHelper.getPrivateValue(
                        net.minecraft.client.gui.GuiDisconnected.class,
                        (net.minecraft.client.gui.GuiDisconnected) mc.currentScreen,
                        "field_146304_f");
                    Biomic.LOG.info(
                        "Browser screen test: disconnected: {}",
                        reason instanceof net.minecraft.util.IChatComponent chat ? chat.getUnformattedText() : reason);
                } catch (RuntimeException unreadable) {
                    Biomic.LOG.info("Browser screen test: disconnected, reason unreadable: {}", unreadable.toString());
                }
            }
            boolean connecting = mc.currentScreen instanceof net.minecraft.client.multiplayer.GuiConnecting;
            // From the menu once everything has loaded, again after a disconnect, three times at most.
            if (!connecting && ++menuTicks >= 100 && attempts < 3) {
                menuTicks = 0;
                attempts++;
                Biomic.LOG.info("Browser screen test: joining {} from {} (attempt {})", SERVER, screenName, attempts);
                // As the multiplayer menu joins: FML sets its handshake up there.
                cpw.mods.fml.client.FMLClientHandler.instance()
                    .setupServerList();
                cpw.mods.fml.client.FMLClientHandler.instance()
                    .connectToServer(
                        mc.currentScreen,
                        new net.minecraft.client.multiplayer.ServerData("Browser screen test", SERVER));
            }
        }
        if (mc.thePlayer == null) return;
        mc.gameSettings.pauseOnLostFocus = false;
        switch (++ticks) {
            case 200 -> mc.displayGuiScreen(screen = new GuiBiomeBrowser());
            case 300 -> {
                boolean selected = screen.testSelect("biomic:redwoods");
                screen.testWrite(false, "test note, written by the screen test");
                issueAt = (int) Math.floor(mc.thePlayer.posX) + " "
                    + (int) Math.floor(mc.thePlayer.boundingBox.minY)
                    + " "
                    + (int) Math.floor(mc.thePlayer.posZ);
                screen.testWrite(true, "test issue, written by the screen test");
                Biomic.LOG.info(
                    "Browser screen test: selected {}, issue at {}, status: {}",
                    selected,
                    issueAt,
                    GuiBiomeBrowser.testStatus());
            }
            case 320 -> shoot = FILE;
            case 330 -> {
                BrowserState.Issue issue = BrowserState.get()
                    .lastIssue();
                Biomic.LOG.info(
                    "Browser screen test: selected the issue's biome: {}",
                    issue != null && screen.testSelect(issue.key()));
            }
            case 340 -> shoot = FILE.replace(".png", "-issue.png");
            case 360 -> {
                mc.displayGuiScreen(null);
                mc.thePlayer.sendChatMessage("/tp " + mc.thePlayer.getCommandSenderName() + " ~64 ~ ~");
            }
            case 420 -> {
                Biomic.LOG.info(
                    "Browser screen test: moved to {} {} {}",
                    (int) Math.floor(mc.thePlayer.posX),
                    (int) Math.floor(mc.thePlayer.boundingBox.minY),
                    (int) Math.floor(mc.thePlayer.posZ));
                mc.displayGuiScreen(screen);
            }
            case 440 -> Biomic.LOG.info("Browser screen test: clicked the issue: {}", screen.testGoBack());
            case 520 -> {
                Biomic.LOG.info(
                    "Browser screen test: back at {} {} {} (the issue was at {}), status: {}",
                    (int) Math.floor(mc.thePlayer.posX),
                    (int) Math.floor(mc.thePlayer.boundingBox.minY),
                    (int) Math.floor(mc.thePlayer.posZ),
                    issueAt,
                    GuiBiomeBrowser.testStatus());
                mc.displayGuiScreen(screen);
                screen.testSortByDistance();
            }
            case 600 -> {
                // A Go while the survey runs: it waits for one biome's search at most.
                goneAt = ticks;
                screen.testGo("biomic:pine_taiga");
            }
            default -> {
                if (goneAt > 0 && !landed
                    && String.valueOf(GuiBiomeBrowser.testStatus())
                        .startsWith("Landed")) {
                    landed = true;
                    Biomic.LOG.info(
                        "Browser screen test: Go during the survey landed after {} ticks: {}",
                        ticks - goneAt,
                        GuiBiomeBrowser.testStatus());
                    mc.displayGuiScreen(screen);
                }
                var progress = industries.angelic.biomic.browser.BrowserReplies.surveyProgress();
                boolean surveyed = progress != null && progress.done() == progress.total();
                if (ticks > 600 && shot == 0 && (surveyed || ticks > 600 + 20 * 600)) {
                    shot = ticks;
                    if (mc.currentScreen != screen) mc.displayGuiScreen(screen);
                    Biomic.LOG.info(
                        "Browser screen test: survey {} after {} s; nearest: {}",
                        progress == null ? "none" : progress.done() + " of " + progress.total(),
                        (ticks - 520) / 20,
                        screen.testTop(8));
                }
                if (shot > 0 && ticks == shot + 20) shoot = FILE.replace(".png", "-distance.png");
                if (shot > 0 && ticks == shot + 60) mc.shutdown();
            }
        }
    }

    private int goneAt, shot;
    private boolean landed;

    @SubscribeEvent
    public void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || shoot == null) return;
        String file = shoot;
        shoot = null;
        try {
            ImageIO.write(BrowserThumbnails.grabFrame(), "png", new File(file));
            Biomic.LOG.info("Browser screen test: screen saved to {}", file);
        } catch (Exception failure) {
            Biomic.LOG.warn("Browser screen test: could not save the screen", failure);
        }
    }
}
