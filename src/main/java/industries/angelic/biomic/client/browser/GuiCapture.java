package industries.angelic.biomic.client.browser;

import java.io.File;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/**
 * The biome browser's capture mode: the world without the browser, the HUD or the hand, and one button. Movement keys
 * still move the player and the right mouse button held turns the view, so the shot can be framed; the button (or
 * Enter) takes it as the biome's thumbnail, without itself, and goes back to the browser. Esc goes back without one.
 */
@SideOnly(Side.CLIENT)
final class GuiCapture extends GuiScreen {

    private static final int BUTTON_W = 150, BUTTON_H = 20;

    /** The capture waiting for the end of the frame that draws nothing over the world. */
    private static GuiCapture pending;

    private final String key, name;
    private final GuiScreen back;
    /** The developer's check (BrowserKeys): shoot on its own, then quit the game. */
    private final boolean test;
    private int ticks;
    private boolean hideGui;
    /** Set when the shot is asked for: the next frame draws nothing, and its end takes the shot. */
    private boolean shooting;

    GuiCapture(String key, String name, GuiScreen back) {
        this(key, name, back, false);
    }

    GuiCapture(String key, String name, GuiScreen back, boolean test) {
        this.key = key;
        this.name = name;
        this.back = back;
        this.test = test;
    }

    @Override
    public void updateScreen() {
        if (test && ++ticks == 40) shoot();
    }

    static void register() {
        Hooks hooks = new Hooks();
        MinecraftForge.EVENT_BUS.register(hooks);
        FMLCommonHandler.instance()
            .bus()
            .register(hooks);
    }

    @Override
    public void initGui() {
        hideGui = mc.gameSettings.hideGUI;
        // No hand, no block outline (EntityRenderer); the HUD is left out by Hooks.
        mc.gameSettings.hideGUI = true;
    }

    @Override
    public void onGuiClosed() {
        mc.gameSettings.hideGUI = hideGui;
        KeyBinding.unPressAllKeys();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private int buttonX() {
        return width / 2 - BUTTON_W - 2;
    }

    private int buttonY() {
        return height - 34;
    }

    @Override
    public void drawScreen(int mx, int my, float partialTicks) {
        if (shooting) return;
        drawCropGuide();
        int x = buttonX(), y = buttonY();
        drawButton(x, y, BUTTON_W, "Capture " + fontRendererObj.trimStringToWidth(name, BUTTON_W - 60), mx, my, true);
        drawButton(x + BUTTON_W + 4, y, BUTTON_W, "Cancel", mx, my, false);
        String hint = "WASD moves, hold the right mouse button to look, Enter captures, Esc cancels";
        drawCenteredString(fontRendererObj, hint, width / 2, y - 14, 0xFFE6EDF3);
    }

    /** Dims what falls outside the 16:9 the thumbnail keeps. */
    private void drawCropGuide() {
        int cropH = Math.min(height, width * 9 / 16), cropW = Math.min(width, height * 16 / 9);
        int top = (height - cropH) / 2, left = (width - cropW) / 2;
        if (top > 0) {
            drawRect(0, 0, width, top, 0x80000000);
            drawRect(0, top + cropH, width, height, 0x80000000);
        }
        if (left > 0) {
            drawRect(0, 0, left, height, 0x80000000);
            drawRect(left + cropW, 0, width, height, 0x80000000);
        }
    }

    private void drawButton(int x, int y, int w, String label, int mx, int my, boolean primary) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + BUTTON_H;
        int background = primary ? (hover ? 0xF04FC3A1 : 0xD03A9C80) : (hover ? 0xE01C232C : 0xE0101418);
        drawRect(x, y, x + w, y + BUTTON_H, background);
        drawCenteredString(fontRendererObj, label, x + w / 2, y + 6, 0xFFFFFFFF);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if (button != 0 || my < buttonY() || my >= buttonY() + BUTTON_H) return;
        if (mx >= buttonX() && mx < buttonX() + BUTTON_W) shoot();
        else if (mx >= buttonX() + BUTTON_W + 4 && mx < buttonX() + 2 * BUTTON_W + 4) mc.displayGuiScreen(back);
    }

    @Override
    public void handleMouseInput() {
        // The right button held turns the view, as the grabbed mouse would (EntityRenderer's sensitivity).
        if (Mouse.isButtonDown(1) && mc.thePlayer != null) {
            float sensitivity = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
            float scale = sensitivity * sensitivity * sensitivity * 8.0F;
            mc.thePlayer.setAngles(
                Mouse.getEventDX() * scale,
                Mouse.getEventDY() * scale * (mc.gameSettings.invertMouse ? -1 : 1));
        }
        super.handleMouseInput();
    }

    @Override
    public void handleKeyboardInput() {
        int key = Keyboard.getEventKey();
        if (key == Keyboard.KEY_ESCAPE || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
            super.handleKeyboardInput();
            return;
        }
        // Everything else reaches the game's key bindings, so the movement keys move the player.
        KeyBinding.setKeyBindState(key, Keyboard.getEventKeyState());
    }

    @Override
    protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(back);
        else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) shoot();
    }

    private void shoot() {
        shooting = true;
        pending = this;
    }

    /** At the end of the frame that drew nothing over the world: the shot, saved, and back to the browser. */
    private void take() {
        String result;
        try {
            File file = BrowserThumbnails.save(key, BrowserThumbnails.grabFrame());
            result = "Captured " + name + " (" + file.getPath() + ")";
        } catch (Exception failure) {
            Biomic.LOG.warn("Could not capture the thumbnail of {}", key, failure);
            result = "The capture failed: " + failure;
        }
        GuiBiomeBrowser.captured(result);
        mc.displayGuiScreen(back);
        if (test) {
            Biomic.LOG.info("Biome browser capture test: {}", result);
            mc.shutdown();
        }
    }

    /** Leaves the HUD out of capture mode and takes a pending shot when its frame is done. */
    public static final class Hooks {

        @SubscribeEvent
        public void overlay(RenderGameOverlayEvent.Pre event) {
            if (event.type == RenderGameOverlayEvent.ElementType.ALL
                && Minecraft.getMinecraft().currentScreen instanceof GuiCapture) event.setCanceled(true);
        }

        @SubscribeEvent
        public void frame(TickEvent.RenderTickEvent event) {
            if (event.phase != TickEvent.Phase.END || pending == null) return;
            GuiCapture capture = pending;
            pending = null;
            if (Minecraft.getMinecraft().currentScreen == capture) capture.take();
        }
    }
}
