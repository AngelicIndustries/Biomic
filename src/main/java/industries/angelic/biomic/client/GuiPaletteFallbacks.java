package industries.angelic.biomic.client;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.world.palette.BlockPalette;

/**
 * Shown once per start, before the first screen after loading, when defaults of the block palette are not installed
 * (BlockPalette#fallbacks): which ids miss their block, the 1.7.10 block each places instead, and whether to go on.
 * Continue opens the screen it stood in front of; Quit Game closes the game.
 */
@SideOnly(Side.CLIENT)
public final class GuiPaletteFallbacks extends GuiScreen {

    private static final int CONTINUE = 0, QUIT = 1, SCROLL_UP = 2, SCROLL_DOWN = 3, ROW = 12, TOP = 64, FOOT = 64;

    private final GuiScreen next;
    private final List<String> ids;
    private final String[] uses;
    private Rows rows;

    GuiPaletteFallbacks(GuiScreen next, List<String> ids) {
        this.next = next;
        this.ids = ids;
        this.uses = new String[ids.size()];
        for (int index = 0; index < ids.size(); index++) uses[index] = name(BlockPalette.choice(ids.get(index)));
    }

    /** Puts the screen in front of the first one opened once the palette is resolved, if it has fallbacks. */
    public static final class Hook {

        private boolean shown;

        @SubscribeEvent
        public void open(GuiOpenEvent event) {
            if (shown || event.gui == null || event.gui instanceof GuiPaletteFallbacks) return;
            List<String> ids = BlockPalette.fallbacks();
            if (ids.isEmpty()) return;
            shown = true;
            event.gui = new GuiPaletteFallbacks(event.gui, ids);
        }
    }

    @Override
    public void initGui() {
        buttonList.clear();
        buttonList.add(
            new GuiButton(
                CONTINUE,
                width / 2 - 154,
                height - 28,
                150,
                20,
                I18n.format("biomic.palette.fallback.continue")));
        buttonList.add(new GuiButton(QUIT, width / 2 + 4, height - 28, 150, 20, I18n.format("menu.quit")));
        rows = new Rows(mc);
        rows.registerScrollButtons(SCROLL_UP, SCROLL_DOWN);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == CONTINUE) mc.displayGuiScreen(next);
        else if (button.id == QUIT) mc.shutdown();
        else rows.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char character, int key) {
        // Escape goes on, like Continue: closing to no screen would lose the one waiting.
        if (key == 1) mc.displayGuiScreen(next);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        rows.drawScreen(mouseX, mouseY, partialTicks);
        drawCenteredString(fontRendererObj, I18n.format("biomic.palette.fallback.title"), width / 2, 12, 0xFFFFFF);
        String why = Loader.isModLoaded("etfuturum") ? "biomic.palette.fallback.disabled"
            : "biomic.palette.fallback.missing";
        drawCenteredString(fontRendererObj, I18n.format(why, ids.size()), width / 2, 30, 0xFFFF55);
        drawCenteredString(fontRendererObj, I18n.format("biomic.palette.fallback.instead"), width / 2, 44, 0xA0A0A0);
        List<?> hint = fontRendererObj
            .listFormattedStringToWidth(I18n.format("biomic.palette.fallback.hint"), width - 40);
        for (int line = 0; line < hint.size() && line < 2; line++) drawCenteredString(
            fontRendererObj,
            (String) hint.get(line),
            width / 2,
            height - FOOT + 8 + line * 10,
            0xA0A0A0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * The block an entry places, by its item's name, else its own (crops, sugar cane); else as the palette spells it.
     */
    private static String name(BlockPalette.Choice choice) {
        if (choice == null) return "?";
        Block block = choice.block();
        if (block == Blocks.air) return I18n.format("biomic.palette.fallback.nothing");
        Item item = Item.getItemFromBlock(block);
        try {
            if (item != null) return new ItemStack(item, 1, choice.fixedMetadata()).getDisplayName();
            String key = block.getUnlocalizedName() + ".name";
            String name = block.getLocalizedName();
            if (name != null && !name.isEmpty() && !name.equals(key)) return name;
        } catch (RuntimeException ignored) {
            // A block with no name for this metadata.
        }
        return choice.spelled();
    }

    private final class Rows extends GuiSlot {

        Rows(Minecraft mc) {
            super(
                mc,
                GuiPaletteFallbacks.this.width,
                GuiPaletteFallbacks.this.height,
                TOP,
                GuiPaletteFallbacks.this.height - FOOT,
                ROW);
        }

        @Override
        protected int getSize() {
            return ids.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick, int mouseX, int mouseY) {}

        @Override
        protected boolean isSelected(int index) {
            return false;
        }

        @Override
        protected void drawBackground() {}

        @Override
        public int getListWidth() {
            return Math.min(GuiPaletteFallbacks.this.width - 40, 420);
        }

        @Override
        protected int getScrollBarX() {
            return GuiPaletteFallbacks.this.width / 2 + getListWidth() / 2 + 4;
        }

        @Override
        protected void drawSlot(int index, int x, int y, int slotHeight, Tessellator tessellator, int mouseX,
            int mouseY) {
            int half = getListWidth() / 2;
            fontRendererObj.drawString(fontRendererObj.trimStringToWidth(ids.get(index), half - 8), x, y + 1, 0xA0A0A0);
            fontRendererObj.drawString(
                "→ " + fontRendererObj.trimStringToWidth(uses[index], half - 16),
                x + half,
                y + 1,
                0xFFFFFF);
        }
    }
}
