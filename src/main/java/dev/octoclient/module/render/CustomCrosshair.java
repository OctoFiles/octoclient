package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Replaces the vanilla first-person crosshair with a configurable reticle. */
public class CustomCrosshair extends Module {
    private static final int MAX_UNDO = 32;
    private static State clipboard;

    private final ModeSetting style = new ModeSetting("Style", "Reticle style",
            "Cross", List.of("Cross", "Dot", "Circle", "T"));
    private final ModeSetting presets = new ModeSetting("Presets", "Apply a built-in reticle preset",
            "Custom", List.of("Custom", "Classic", "Minimal", "Dot", "Circle"));
    private final NumberSetting size = new NumberSetting("Size", "Overall stroke length", 9, 2, 24, 1);
    private final NumberSetting thickness = new NumberSetting("Thickness", "Reticle stroke thickness", 2, 1, 8, 1);
    private final NumberSetting gap = new NumberSetting("Gap", "Space between the center and strokes", 3, 0, 16, 1);
    private final NumberSetting rotation =
            new NumberSetting("Rotation", "Rotate the reticle in degrees", 0, -180, 180, 1);
    private final NumberSetting scale =
            new NumberSetting("Scale", "Reticle size multiplier", 1, 0.25, 3, 0.05);
    private final ColorSetting color = new ColorSetting("Color", "Reticle stroke color", 0xFFFFFFFF);
    private final BooleanSetting dynamicColor =
            new BooleanSetting("Dynamic Color", "Change color when aiming at an entity", false);
    private final BooleanSetting invertedColor =
            new BooleanSetting("Inverted Color", "Invert the final reticle RGB color", false);
    private final BooleanSetting scaleIndicator =
            new BooleanSetting("Scale Indicator", "Show the configured multiplier beside the reticle", false);
    private final BooleanSetting showInF5 =
            new BooleanSetting("F5", "Show the custom reticle in third-person view", false);
    private final BooleanSetting dot =
            new BooleanSetting("Dot", "Draw a center dot in addition to the selected style", false);
    private final BooleanSetting outline =
            new BooleanSetting("Outline", "Draw an outline around the strokes", true);
    private final ColorSetting outlineColor =
            new ColorSetting("Outline Color", "Color of the stroke outline", 0xFF000000);
    private final NumberSetting outlineThickness =
            new NumberSetting("Outline Thickness", "Outline width in pixels", 1, 0, 5, 1);
    private final BooleanSetting grid =
            new BooleanSetting("Grid", "Show alignment grid in the crosshair editor", true);
    private final Deque<State> undo = new ArrayDeque<>();
    private String previousPreset;

    public CustomCrosshair() {
        super("Custom Crosshair", "Create and configure your own crosshair", 'X');
        addSetting(style);
        addSetting(presets);
        addSetting(dynamicColor);
        addSetting(invertedColor);
        addSetting(scaleIndicator);
        addSetting(showInF5);
        addSetting(dot);
        addSetting(size);
        addSetting(thickness);
        addSetting(gap);
        addSetting(rotation);
        addSetting(scale);
        addSetting(color);
        addSetting(outline);
        addSetting(outlineColor);
        addSetting(outlineThickness);
        addSetting(grid);
        addSetting(new ActionSetting("Crosshair Editor", "Open the live crosshair editor",
                "Open Editor", () -> {
                    Screen parent = Minecraft.getInstance().gui.screen();
                    Minecraft.getInstance().setScreenAndShow(new dev.octoclient.ui.CrosshairEditorScreen(parent, this));
                }));
        addSetting(new ActionSetting("Undo", "Restore the previous crosshair edit", "Undo", this::undo));
        addSetting(new ActionSetting("Clear", "Reset every crosshair option to its default", "Clear", this::clear));
        addSetting(new ActionSetting("Copy", "Copy this crosshair to the internal client clipboard", "Copy", this::copy));
        addSetting(new ActionSetting("Paste", "Apply the copied crosshair", "Paste", this::paste));
    }

    public void render(GuiGraphicsExtractor gfx, Minecraft client) {
        syncPreset();
        int centerX = gfx.guiWidth() / 2;
        int centerY = gfx.guiHeight() / 2;
        int arm = size.getInt();
        int line = thickness.getInt();
        int spacing = gap.getInt();
        int foreground = effectiveColor(client);
        int edge = outline.isEnabled() ? outlineThickness.getInt() : 0;
        int edgeColor = outlineColor.getValue();

        gfx.pose().pushMatrix();
        gfx.pose().translate(centerX, centerY);
        gfx.pose().rotate((float) Math.toRadians(rotation.getFloat()));
        gfx.pose().scale(scale.getFloat(), scale.getFloat());
        switch (style.getValue()) {
            case "Dot" -> drawDot(gfx, 0, 0, line, foreground, edgeColor, edge);
            case "Circle" -> drawCircle(gfx, 0, 0, Math.max(2, arm / 2), line, foreground, edgeColor, edge);
            case "T" -> {
                drawRect(gfx, -line / 2, -spacing - arm, line, arm, foreground, edgeColor, edge);
                drawRect(gfx, -arm / 2, spacing, arm, line, foreground, edgeColor, edge);
            }
            default -> {
                drawRect(gfx, -line / 2, -spacing - arm, line, arm, foreground, edgeColor, edge);
                drawRect(gfx, -line / 2, spacing + 1, line, arm, foreground, edgeColor, edge);
                drawRect(gfx, -spacing - arm, -line / 2, arm, line, foreground, edgeColor, edge);
                drawRect(gfx, spacing + 1, -line / 2, arm, line, foreground, edgeColor, edge);
            }
        }
        if (dot.isEnabled() && !style.getValue().equals("Dot")) {
            drawDot(gfx, 0, 0, line, foreground, edgeColor, edge);
        }
        gfx.pose().popMatrix();

        if (scaleIndicator.isEnabled()) {
            String label = String.format(java.util.Locale.ROOT, "x%.2f", scale.getFloat());
            gfx.text(client.font, label,
                    centerX + Math.round((arm + spacing + 6) * scale.getFloat()), centerY + 2, foreground, true);
        }
    }

    private int effectiveColor(Minecraft client) {
        int value = color.getValue();
        HitResult hit = client.hitResult;
        if (dynamicColor.isEnabled() && hit != null && hit.getType() == HitResult.Type.ENTITY
                && hit instanceof EntityHitResult) {
            value = 0xFF55E68A;
        }
        if (invertedColor.isEnabled()) {
            value = (value & 0xFF000000) | ((~value) & 0x00FFFFFF);
        }
        return value;
    }

    private static void drawRect(GuiGraphicsExtractor gfx, int x, int y, int width, int height,
                                 int foreground, int edgeColor, int edge) {
        if (edge > 0) {
            gfx.fill(x - edge, y - edge, x + width + edge, y + height + edge, edgeColor);
        }
        gfx.fill(x, y, x + width, y + height, foreground);
    }

    private static void drawDot(GuiGraphicsExtractor gfx, int x, int y, int size,
                                int foreground, int edgeColor, int edge) {
        drawRect(gfx, x - size / 2, y - size / 2, size, size, foreground, edgeColor, edge);
    }

    private static void drawCircle(GuiGraphicsExtractor gfx, int centerX, int centerY, int radius, int stroke,
                                   int foreground, int edgeColor, int edge) {
        if (edge > 0) {
            int outer = radius + edge;
            int inner = Math.max(0, radius - edge);
            for (int dy = -outer; dy <= outer; dy++) {
                for (int dx = -outer; dx <= outer; dx++) {
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= outer && distance >= inner) {
                        gfx.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, edgeColor);
                    }
                }
            }
        }
        int fillInner = Math.max(0, radius - stroke);
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance <= radius && distance >= fillInner) {
                    gfx.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, foreground);
                }
            }
        }
    }

    public boolean shouldReplaceVanillaCrosshair() {
        syncPreset();
        return isEnabled() && (showInF5.isEnabled()
                || Minecraft.getInstance().options.getCameraType() == net.minecraft.client.CameraType.FIRST_PERSON);
    }

    public boolean gridEnabled() {
        return grid.isEnabled();
    }

    public void rememberEdit() {
        if (undo.size() == MAX_UNDO) {
            undo.removeLast();
        }
        undo.addFirst(snapshot());
    }

    public void undo() {
        if (!undo.isEmpty()) {
            restore(undo.removeFirst());
        }
    }

    public void clear() {
        rememberEdit();
        style.reset();
        presets.setValue("Custom");
        size.reset();
        thickness.reset();
        gap.reset();
        rotation.reset();
        scale.reset();
        color.reset();
        dynamicColor.reset();
        invertedColor.reset();
        scaleIndicator.reset();
        showInF5.reset();
        dot.reset();
        outline.reset();
        outlineColor.reset();
        outlineThickness.reset();
        previousPreset = "Custom";
    }

    public void copy() {
        clipboard = snapshot();
    }

    public void paste() {
        if (clipboard != null) {
            rememberEdit();
            restore(clipboard);
            presets.setValue("Custom");
            previousPreset = "Custom";
        }
    }

    private void syncPreset() {
        String selected = presets.getValue();
        if (previousPreset == null) {
            previousPreset = selected;
        } else if (!previousPreset.equals(selected)) {
            rememberEdit();
            switch (selected) {
                case "Classic" -> {
                    style.setValue("Cross");
                    size.setRaw(8);
                    thickness.setRaw(2);
                    gap.setRaw(3);
                    scale.setRaw(1);
                    color.setValue(0xFFFFFFFF);
                    outline.setValue(true);
                }
                case "Minimal" -> {
                    style.setValue("Cross");
                    size.setRaw(5);
                    thickness.setRaw(1);
                    gap.setRaw(4);
                    scale.setRaw(0.9);
                    outline.setValue(false);
                }
                case "Dot" -> {
                    style.setValue("Dot");
                    size.setRaw(4);
                    thickness.setRaw(2);
                    gap.setRaw(0);
                }
                case "Circle" -> {
                    style.setValue("Circle");
                    size.setRaw(12);
                    thickness.setRaw(2);
                    gap.setRaw(3);
                }
                default -> {
                }
            }
            previousPreset = selected;
        }
    }

    private State snapshot() {
        return new State(style.getValue(), size.getValue(), thickness.getValue(), gap.getValue(),
                rotation.getValue(), scale.getValue(), color.getValue(), dynamicColor.getValue(),
                invertedColor.getValue(), scaleIndicator.getValue(), showInF5.getValue(), dot.getValue(),
                outline.getValue(), outlineColor.getValue(), outlineThickness.getValue(), presets.getValue());
    }

    private void restore(State state) {
        style.setValue(state.style());
        size.setValue(state.size());
        thickness.setValue(state.thickness());
        gap.setValue(state.gap());
        rotation.setValue(state.rotation());
        scale.setValue(state.scale());
        color.setValue(state.color());
        dynamicColor.setValue(state.dynamicColor());
        invertedColor.setValue(state.invertedColor());
        scaleIndicator.setValue(state.scaleIndicator());
        showInF5.setValue(state.showInF5());
        dot.setValue(state.dot());
        outline.setValue(state.outline());
        outlineColor.setValue(state.outlineColor());
        outlineThickness.setValue(state.outlineThickness());
        presets.setValue(state.preset());
        previousPreset = state.preset();
    }

    private record State(String style, double size, double thickness, double gap, double rotation, double scale,
                         int color, boolean dynamicColor, boolean invertedColor, boolean scaleIndicator,
                         boolean showInF5, boolean dot, boolean outline, int outlineColor,
                         double outlineThickness, String preset) {
    }
}
