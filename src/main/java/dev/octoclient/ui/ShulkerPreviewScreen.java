package dev.octoclient.ui;

import dev.octoclient.util.Draw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Shulker contents preview modelled after kgriff0n/shulker-preview: a compact
 * 3x9 grid card with stack counts and item tooltips, opened with a keybind.
 */
public class ShulkerPreviewScreen extends Screen {
    private static final int PANEL_W = 176;
    private static final int PANEL_H = 82;

    private final ItemStack shulker;
    private final Screen parent;
    private final List<ItemStack> items = new ArrayList<>(27);
    private int panelX;
    private int panelY;

    public ShulkerPreviewScreen(ItemStack shulker, Screen parent) {
        super(Component.literal("Shulker Preview"));
        this.shulker = shulker;
        this.parent = parent;
        ItemContainerContents contents = shulker.get(DataComponents.CONTAINER);
        net.minecraft.core.NonNullList<ItemStack> slots = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
        if (contents != null) {
            contents.copyInto(slots);
        }
        items.addAll(slots);
    }

    /** True when the stack is a shulker box that has (or may have) contents. */
    public static boolean isShulker(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem block
                && block.getBlock() instanceof ShulkerBoxBlock;
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Slight dim so the panel reads on busy inventory backgrounds.
        gfx.fill(0, 0, width, height, 0x40000000);

        Draw.roundedRect(gfx, panelX - 1, panelY - 1, PANEL_W + 2, PANEL_H + 2, 7, 0xFF2A2D34);
        Draw.roundedRect(gfx, panelX, panelY, PANEL_W, PANEL_H, 6, 0xFA15171C);
        Draw.roundedOutline(gfx, panelX, panelY, PANEL_W, PANEL_H, 6, 0xFF3A3D45);
        gfx.fill(panelX + 1, panelY + 1, panelX + PANEL_W - 1, panelY + 2, 0xFF4F8BF7);

        gfx.text(font, shulker.getHoverName(), panelX + 8, panelY + 6, 0xFFF2F4F8, true);

        int gridX = panelX + 8;
        int gridY = panelY + 18;
        gfx.fill(gridX, gridY, gridX + 9 * 18, gridY + 3 * 18 - 2, 0x66101116);

        int hovered = -1;
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            int x = gridX + (i % 9) * 18 + 1;
            int y = gridY + (i / 9) * 18 + 1;
            if (item.isEmpty()) {
                continue;
            }
            gfx.item(item, x, y);
            gfx.itemDecorations(font, item, x, y);
            if (mouseX >= x - 1 && mouseX < x + 17 && mouseY >= y - 1 && mouseY < y + 17) {
                hovered = i;
            }
        }
        if (hovered >= 0) {
            gfx.fill(gridX + (hovered % 9) * 18, gridY + (hovered / 9) * 18,
                    gridX + (hovered % 9) * 18 + 18, gridY + (hovered / 9) * 18 + 18, 0x33FFFFFF);
            ItemStack item = items.get(hovered);
            if (!item.isEmpty()) {
                gfx.setTooltipForNextFrame(font, item, mouseX, mouseY);
            }
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreenAndShow(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
