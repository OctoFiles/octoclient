package dev.octoclient.ui;

import dev.octoclient.config.ConfigManager;
import dev.octoclient.module.render.Particles;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Searchable per-particle filter editor opened from the Particles settings. */
public final class ParticleSelectorScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int LIST_TOP = 78;
    private static final int VISIBLE_ROWS = 19;

    private final Screen parent;
    private final Particles module;
    private final List<Map.Entry<String, BooleanSetting>> entries;
    private String search = "";
    private int scroll;

    public ParticleSelectorScreen(Screen parent, Particles module) {
        super(Component.literal("Particle Filters"));
        this.parent = parent;
        this.module = module;
        entries = new ArrayList<>(module.particleSettings().entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
    }

    private List<Map.Entry<String, BooleanSetting>> filtered() {
        String query = search.toLowerCase(Locale.ROOT);
        return entries.stream().filter(entry -> entry.getKey().toLowerCase(Locale.ROOT).contains(query)
                || entry.getValue().getName().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xF0101010);
        graphics.centeredText(font, "Individual Particle Filters", width / 2, 18, 0xFFFFFFFF);
        graphics.centeredText(font, "Type to search  |  Click a row to suppress that particle",
                width / 2, 34, 0xFFB8BBC8);

        int left = Math.max(24, width / 2 - 190);
        int right = Math.min(width - 24, width / 2 + 190);
        int searchRight = right - 126;
        int allOnLeft = right - 120;
        int allOffLeft = right - 60;
        graphics.fill(left, 48, searchRight, 68, 0xFF282832);
        graphics.outline(left, 48, searchRight - left, 20, 0xFF6D7180);
        graphics.text(font, search.isEmpty() ? "Search particle types..." : search, left + 8, 54,
                search.isEmpty() ? 0xFF858895 : 0xFFFFFFFF, false);
        graphics.fill(allOnLeft, 48, allOnLeft + 55, 68, 0xFF31523D);
        graphics.outline(allOnLeft, 48, 55, 20, 0xFF6D7180);
        graphics.centeredText(font, "All On", allOnLeft + 27, 54, 0xFFD8F4DF);
        graphics.fill(allOffLeft, 48, right, 68, 0xFF512F35);
        graphics.outline(allOffLeft, 48, 60, 20, 0xFF6D7180);
        graphics.centeredText(font, "All Off", allOffLeft + 30, 54, 0xFFFFD8DC);

        List<Map.Entry<String, BooleanSetting>> visible = filtered();
        int total = visible.size();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - VISIBLE_ROWS)));
        int count = Math.min(VISIBLE_ROWS, total - scroll);
        for (int i = 0; i < count; i++) {
            Map.Entry<String, BooleanSetting> entry = visible.get(scroll + i);
            int y = LIST_TOP + i * ROW_HEIGHT;
            boolean hovered = mouseX >= left && mouseX <= right && mouseY >= y && mouseY < y + ROW_HEIGHT;
            if (hovered) {
                graphics.fill(left, y, right, y + ROW_HEIGHT, 0x664A5070);
            }
            String label = entry.getKey().replace(':', ' ').replace('_', ' ');
            graphics.text(font, label, left + 8, y + 5, 0xFFE2E4EA, false);
            int toggleColor = entry.getValue().isEnabled() ? 0xFFFF6B6B : 0xFF5C667A;
            graphics.fill(right - 25, y + 4, right - 9, y + 16, toggleColor);
            graphics.outline(right - 25, y + 4, 16, 12, 0xFFB7BAC4);
            graphics.text(font, entry.getValue().isEnabled() ? "ON" : "OFF",
                    right - 53, y + 5, entry.getValue().isEnabled() ? 0xFFFFA0A0 : 0xFFA8ABB5, false);
        }
        graphics.centeredText(font, (total == 0 ? "No particle types match" :
                        (scroll + 1) + "-" + (scroll + count) + " of " + total)
                        + "    |    Click outside / Esc to return",
                width / 2, height - 18, 0xFFB8BBC8);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        int left = Math.max(24, width / 2 - 190);
        int right = Math.min(width - 24, width / 2 + 190);
        int allOnLeft = right - 120;
        int allOffLeft = right - 60;
        if (event.y() >= 48 && event.y() < 68) {
            if (event.x() >= allOnLeft && event.x() < allOnLeft + 55) {
                module.particleSettings().values().forEach(setting -> setting.setValue(true));
                ConfigManager.save();
            } else if (event.x() >= allOffLeft && event.x() <= right) {
                module.particleSettings().values().forEach(setting -> setting.setValue(false));
                ConfigManager.save();
            } else if (event.x() < left || event.x() > right) {
                onClose();
            }
            return true;
        }
        int row = ((int) event.y() - LIST_TOP) / ROW_HEIGHT + scroll;
        List<Map.Entry<String, BooleanSetting>> visible = filtered();
        if (event.y() >= LIST_TOP && row >= scroll && row < Math.min(visible.size(), scroll + VISIBLE_ROWS)) {
            visible.get(row).getValue().toggle();
            ConfigManager.save();
            return true;
        }
        onClose();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = Math.max(0, filtered().size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(scrollY) * 3));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            scroll = 0;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String value = event.codepointAsString();
        if (value != null && !value.isBlank() && search.length() < 48) {
            search += value;
            scroll = 0;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
