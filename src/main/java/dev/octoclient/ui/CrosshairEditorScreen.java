package dev.octoclient.ui;

import dev.octoclient.config.ConfigManager;
import dev.octoclient.module.render.CustomCrosshair;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Button-driven editor with a live reticle preview; settings save on close. */
public final class CrosshairEditorScreen extends Screen {
    private static final int[] PALETTE = {
            0xFFFFFFFF, 0xFFFF4B4B, 0xFFFFA62B, 0xFFFFE34D,
            0xFF64E572, 0xFF3DE3D1, 0xFF4AA3FF, 0xFF9B70FF,
            0xFFFF70C8, 0xFFB8BBC8, 0xFF40434F, 0xFF16171B
    };

    private final Screen parent;
    private final CustomCrosshair module;
    private final ModeSetting style;
    private final NumberSetting size;
    private final NumberSetting thickness;
    private final NumberSetting gap;
    private final ColorSetting color;
    private final BooleanSetting outline;

    public CrosshairEditorScreen(Screen parent, CustomCrosshair module) {
        super(Component.literal("Custom Crosshair Editor"));
        this.parent = parent;
        this.module = module;
        style = module.setting("Style", ModeSetting.class);
        size = module.setting("Size", NumberSetting.class);
        thickness = module.setting("Thickness", NumberSetting.class);
        gap = module.setting("Gap", NumberSetting.class);
        color = module.setting("Color", ColorSetting.class);
        outline = module.setting("Outline", BooleanSetting.class);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0xF0101010);
        graphics.centeredText(font, "Build Your Crosshair", width / 2, 22, 0xFFFFFFFF);
        graphics.centeredText(font, "Click the buttons to adjust it; the result updates immediately",
                width / 2, 40, 0xFFB8BBC8);

        int centerX = width / 2;
        int centerY = height / 2 - 16;
        if (module.gridEnabled()) {
            for (int offset = -48; offset <= 48; offset += 8) {
                graphics.fill(centerX + offset, centerY - 48, centerX + offset + 1, centerY + 49,
                        0x225C6472);
                graphics.fill(centerX - 48, centerY + offset, centerX + 49, centerY + offset + 1,
                        0x225C6472);
            }
        }
        module.render(graphics, Minecraft.getInstance());
        int pixels = size.getInt();
        int line = thickness.getInt();
        int spacing = gap.getInt();
        graphics.centeredText(font, "Preview", width / 2, centerY + 20, 0xFFB8BBC8);

        int y = Math.min(height - 104, centerY + 48);
        drawButton(graphics, "Style: " + style.getValue(), width / 2 - 100, y, 200, 22, mouseX, mouseY);
        drawStepper(graphics, "Size: " + pixels, width / 2 - 100, y + 28, size, mouseX, mouseY);
        drawStepper(graphics, "Thickness: " + line, width / 2 - 100, y + 56, thickness, mouseX, mouseY);
        drawStepper(graphics, "Gap: " + spacing, width / 2 - 100, y + 84, gap, mouseX, mouseY);
        graphics.centeredText(font, "Color palette", width / 2, y + 120, 0xFFB8BBC8);
        int paletteX = width / 2 - (PALETTE.length * 22 - 4) / 2;
        for (int i = 0; i < PALETTE.length; i++) {
            int x = paletteX + i * 22;
            graphics.fill(x, y + 132, x + 18, y + 150, PALETTE[i]);
            graphics.outline(x, y + 132, 18, 18, color.getValue() == PALETTE[i] ? 0xFFFFFFFF : 0xFF676A76);
        }
        String[] actions = {"Grid: " + (module.gridEnabled() ? "On" : "Off"), "Undo", "Clear", "Copy", "Paste"};
        int buttonWidth = Math.min(72, (width - 24) / actions.length);
        int buttonGap = 4;
        int actionX = width / 2 - (actions.length * buttonWidth + (actions.length - 1) * buttonGap) / 2;
        for (int i = 0; i < actions.length; i++) {
            drawButton(graphics, actions[i], actionX + i * (buttonWidth + buttonGap),
                    height - 38, buttonWidth, 22, mouseX, mouseY);
        }
        drawButton(graphics, "Outline: " + (outline.isEnabled() ? "On" : "Off"),
                width / 2 - 62, height - 66, 124, 22, mouseX, mouseY);
    }

    private void drawButton(GuiGraphicsExtractor gfx, String label, int x, int y, int w, int h,
                            int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
        gfx.fill(x, y, x + w, y + h, hover ? 0xFF454956 : 0xFF292B34);
        gfx.outline(x, y, w, h, 0xFF747887);
        gfx.centeredText(font, label, x + w / 2, y + 7, 0xFFFFFFFF);
    }

    private void drawStepper(GuiGraphicsExtractor gfx, String label, int x, int y, NumberSetting setting,
                             int mouseX, int mouseY) {
        drawButton(gfx, "-", x, y, 26, 22, mouseX, mouseY);
        drawButton(gfx, label, x + 30, y, 140, 22, mouseX, mouseY);
        drawButton(gfx, "+", x + 174, y, 26, 22, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        int centerY = height / 2 - 16;
        int y = Math.min(height - 104, centerY + 48);
        int x = width / 2 - 100;
        if (inside(event, x, y, 200, 22)) {
            module.rememberEdit();
            style.cycle();
        } else if (inside(event, x, y + 28, 26, 22)) {
            module.rememberEdit();
            size.setRaw(size.getValue() - size.getStep());
        } else if (inside(event, x + 174, y + 28, 26, 22)) {
            module.rememberEdit();
            size.setRaw(size.getValue() + size.getStep());
        } else if (inside(event, x, y + 56, 26, 22)) {
            module.rememberEdit();
            thickness.setRaw(thickness.getValue() - thickness.getStep());
        } else if (inside(event, x + 174, y + 56, 26, 22)) {
            module.rememberEdit();
            thickness.setRaw(thickness.getValue() + thickness.getStep());
        } else if (inside(event, x, y + 84, 26, 22)) {
            module.rememberEdit();
            gap.setRaw(gap.getValue() - gap.getStep());
        } else if (inside(event, x + 174, y + 84, 26, 22)) {
            module.rememberEdit();
            gap.setRaw(gap.getValue() + gap.getStep());
        } else {
            int paletteX = width / 2 - (PALETTE.length * 22 - 4) / 2;
            int paletteY = y + 132;
            for (int i = 0; i < PALETTE.length; i++) {
                if (inside(event, paletteX + i * 22, paletteY, 18, 18)) {
                    module.rememberEdit();
                    color.setValue(PALETTE[i]);
                    return true;
                }
            }
            if (inside(event, width / 2 - 62, height - 66, 124, 22)) {
                module.rememberEdit();
                outline.toggle();
                return true;
            }
            String[] actions = {"Grid", "Undo", "Clear", "Copy", "Paste"};
            int buttonWidth = Math.min(72, (width - 24) / actions.length);
            int buttonGap = 4;
            int actionX = width / 2 - (actions.length * buttonWidth + (actions.length - 1) * buttonGap) / 2;
            for (int i = 0; i < actions.length; i++) {
                if (!inside(event, actionX + i * (buttonWidth + buttonGap), height - 38,
                        buttonWidth, 22)) {
                    continue;
                }
                switch (actions[i]) {
                    case "Grid" -> module.setting("Grid", BooleanSetting.class).toggle();
                    case "Undo" -> module.undo();
                    case "Clear" -> module.clear();
                    case "Copy" -> module.copy();
                    case "Paste" -> module.paste();
                    default -> throw new IllegalStateException("Unknown crosshair editor action");
                }
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        return true;
    }

    private static boolean inside(MouseButtonEvent event, int x, int y, int w, int h) {
        return event.x() >= x && event.x() < x + w && event.y() >= y && event.y() < y + h;
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
