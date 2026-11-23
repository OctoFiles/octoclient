package dev.octoclient.ui;

import dev.octoclient.config.ConfigManager;
import dev.octoclient.module.hud.Waypoints;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Search-filtered view of saved locations with target selection and deletion. */
public final class WaypointScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 64;

    private final Screen parent;
    private final Waypoints module;
    private int scroll;

    public WaypointScreen(Screen parent, Waypoints module) {
        super(Component.literal("Waypoints"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xF0101010);
        graphics.centeredText(font, "Waypoints", width / 2, 18, 0xFFFFC857);
        graphics.centeredText(font, "Click a row to set a target; click Delete to remove it",
                width / 2, 36, 0xFFB8BBC8);

        List<Waypoints.Entry> entries = module.entries();
        int left = Math.max(20, width / 2 - 230);
        int right = Math.min(width - 20, width / 2 + 230);
        int visibleRows = Math.max(1, (height - LIST_TOP - 28) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - visibleRows)));
        int count = Math.min(visibleRows, entries.size() - scroll);
        Waypoints.Entry target = module.target();
        for (int i = 0; i < count; i++) {
            Waypoints.Entry entry = entries.get(scroll + i);
            int y = LIST_TOP + i * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX <= right && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (hovered || target != null && target.id() == entry.id()) {
                graphics.fill(left, y, right, y + ROW_HEIGHT, 0x664A5070);
            }
            String label = (entry.deathpoint() ? "[Death] " : "") + entry.name()
                    + "  " + entry.world() + "  "
                    + Math.round(entry.x()) + ", " + Math.round(entry.y()) + ", " + Math.round(entry.z());
            graphics.text(font, label, left + 8, y + 6, 0xFFE2E4EA, false);
            graphics.text(font, "Delete", right - 46, y + 6, 0xFFFF8A8A, false);
        }
        String status = entries.isEmpty() ? "No waypoints match the world filter"
                : (scroll + 1) + "-" + (scroll + count) + " of " + entries.size();
        graphics.centeredText(font, status + "  |  Esc to return", width / 2, height - 16, 0xFFB8BBC8);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        List<Waypoints.Entry> entries = module.entries();
        int left = Math.max(20, width / 2 - 230);
        int right = Math.min(width - 20, width / 2 + 230);
        int visibleRows = Math.max(1, (height - LIST_TOP - 28) / ROW_HEIGHT);
        int row = ((int) event.y() - LIST_TOP) / ROW_HEIGHT + scroll;
        if (event.x() < left || event.x() > right || event.y() < LIST_TOP
                || row < scroll || row >= Math.min(entries.size(), scroll + visibleRows)) {
            return true;
        }
        Waypoints.Entry entry = entries.get(row);
        if (event.x() >= right - 60) {
            module.remove(entry.id());
            scroll = Math.min(scroll, Math.max(0, module.entries().size() - visibleRows));
        } else {
            module.select(entry.id());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<Waypoints.Entry> entries = module.entries();
        int visibleRows = Math.max(1, (height - LIST_TOP - 28) / ROW_HEIGHT);
        scroll = Math.max(0, Math.min(Math.max(0, entries.size() - visibleRows),
                scroll - (int) Math.signum(scrollY) * 3));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
