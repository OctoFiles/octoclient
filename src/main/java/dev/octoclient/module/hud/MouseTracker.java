package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.ClickCounter;
import dev.octoclient.util.Draw;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Visualizes relative mouse movement and current button presses in a HUD panel. */
public class MouseTracker extends HudModule {
    private final BooleanSetting showMovement = new BooleanSetting("Movement", "Show a directional movement pad", true);
    private final BooleanSetting showClicks = new BooleanSetting("Clicks", "Show left and right mouse buttons", true);
    private final NumberSetting sensitivity = new NumberSetting("Sensitivity", "Movement indicator response", 1, 0.25, 4, 0.25);
    private final NumberSetting corners = new NumberSetting("Corners", "Widget and control corner radius", 8, 0, 20, 1);
    private final ColorSetting dotColor = new ColorSetting("Dot Color", "Mouse position and click color", 0xFF66D9EF);
    private final ModeSetting shape = new ModeSetting("Shape", "Mouse position marker shape", "Circle", List.of("Circle", "Square"));
    private final NumberSetting dotSize = new NumberSetting("Dot Size", "Mouse position marker size", 6, 2, 16, 1);
    private final BooleanSetting showCenterCross = new BooleanSetting("Show Center Cross", "Draw the movement pad center guides", true);
    private final BooleanSetting showTrail = new BooleanSetting("Show Trail", "Draw a fading trail behind the movement marker", true);
    private final BooleanSetting showCps = new BooleanSetting("Show CPS", "Show left and right clicks per second", true);
    private final NumberSetting decaySpeed = new NumberSetting("Decay Speed", "Trail opacity fade per second in percent", 100, 1, 500, 1);
    private final NumberSetting trailLength = new NumberSetting("Trail Length", "Maximum stored mouse trail points", 10, 1, 40, 1);
    private final NumberSetting updateInterval = new NumberSetting("Update Interval", "Trail sample interval in milliseconds", 16, 1, 200, 1);
    private final Deque<TrailPoint> trail = new ArrayDeque<>();
    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private float indicatorX;
    private float indicatorY;
    private long lastTrailUpdate;

    public MouseTracker() {
        super("Mousetracker", "Visualize mouse movement and clicks", 'M');
        addSetting(showMovement);
        addSetting(showClicks);
        addSetting(sensitivity);
        addSetting(corners);
        addSetting(dotColor);
        addSetting(shape);
        addSetting(dotSize);
        addSetting(showCenterCross);
        addSetting(showTrail);
        addSetting(showCps);
        addSetting(decaySpeed);
        addSetting(trailLength);
        addSetting(updateInterval);
        x = 10;
        y = 170;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        double mouseX = ctx.mc.mouseHandler.xpos();
        double mouseY = ctx.mc.mouseHandler.ypos();
        if (!Double.isNaN(lastX)) {
            indicatorX = clamp((float) ((mouseX - lastX) * sensitivity.getFloat() * 0.08), -13, 13);
            indicatorY = clamp((float) ((mouseY - lastY) * sensitivity.getFloat() * 0.08), -13, 13);
        }
        lastX = mouseX;
        lastY = mouseY;
        updateTrail();

        int pad = 6;
        int padSize = showMovement.isEnabled() ? 38 : 0;
        int clickWidth = showClicks.isEnabled() ? 66 : 0;
        int gap = padSize > 0 && clickWidth > 0 ? 6 : 0;
        int panelWidth = pad * 2 + padSize + clickWidth + gap;
        int panelHeight = pad * 2 + Math.max(padSize, showClicks.isEnabled() ? 24 : 0);
        width = panelWidth;
        height = panelHeight;
        drawPanel(ctx, (int) x, (int) y, panelWidth, panelHeight, corners.getInt(), 0x8E97C4, 0.94f, 0f, 70);

        float currentX = x + pad;
        if (showMovement.isEnabled()) {
            float centerX = currentX + 19;
            float centerY = y + pad + 19;
            Draw.shapeF(ctx.gfx, currentX, y + pad, 38, 38, corners.getInt(), Theme.withAlpha(Theme.DARK_900, 180));
            if (showCenterCross.isEnabled()) {
                ctx.fill(centerX - 10, centerY, centerX + 11, centerY + 1, Theme.withAlpha(Theme.TEXT_FAINT, 100));
                ctx.fill(centerX, centerY - 10, centerX + 1, centerY + 11, Theme.withAlpha(Theme.TEXT_FAINT, 100));
            }
            if (showTrail.isEnabled()) {
                int count = 0;
                for (TrailPoint point : trail) {
                    float ageSeconds = (System.currentTimeMillis() - point.timestamp) / 1000f;
                    int alpha = Math.max(0, Math.min(255,
                            Math.round(255f * (1f - ageSeconds * decaySpeed.getFloat() / 100f))));
                    float size = Math.max(2f, dotSize.getFloat() * (0.55f + 0.45f * alpha / 255f));
                    drawMarker(ctx, centerX + point.x - size / 2, centerY + point.y - size / 2, size,
                            Theme.withAlpha(dotColor.getValue(), alpha));
                    if (++count >= trailLength.getInt()) {
                        break;
                    }
                }
            }
            float markerSize = dotSize.getFloat();
            drawMarker(ctx, centerX + indicatorX - markerSize / 2, centerY + indicatorY - markerSize / 2,
                    markerSize, dotColor.getValue());
            currentX += padSize + gap;
        }
        if (showClicks.isEnabled()) {
            boolean left = ctx.mc.mouseHandler.isLeftPressed();
            boolean right = ctx.mc.mouseHandler.isRightPressed();
            int idle = Theme.withAlpha(Theme.TEXT_SECONDARY, 150);
            int active = dotColor.getValue();
            Draw.shapeF(ctx.gfx, currentX, y + pad + 2, 30, 20, corners.getInt(),
                    left ? Theme.withAlpha(active, 140) : Theme.withAlpha(Theme.DARK_900, 180));
            Draw.shapeF(ctx.gfx, currentX + 34, y + pad + 2, 30, 20, corners.getInt(),
                    right ? Theme.withAlpha(active, 140) : Theme.withAlpha(Theme.DARK_900, 180));
            if (showCps.isEnabled()) {
                ClickCounter clicks = ClickCounter.get();
                ctx.drawText(Integer.toString(clicks.getLeftCps()), currentX + 10, y + pad + 8,
                        left ? active : idle, false);
                ctx.drawText(Integer.toString(clicks.getRightCps()), currentX + 44, y + pad + 8,
                        right ? active : idle, false);
            } else {
                ctx.drawText("L", currentX + 11, y + pad + 8, left ? active : idle, false);
                ctx.drawText("R", currentX + 45, y + pad + 8, right ? active : idle, false);
            }
        }
    }

    private void updateTrail() {
        long now = System.currentTimeMillis();
        if (!showTrail.isEnabled()) {
            trail.clear();
            return;
        }
        if (now - lastTrailUpdate < updateInterval.getInt()) {
            return;
        }
        lastTrailUpdate = now;
        trail.addFirst(new TrailPoint(indicatorX, indicatorY, now));
        while (trail.size() > trailLength.getInt()) {
            trail.removeLast();
        }
    }

    private void drawMarker(HudDrawContext ctx, float markerX, float markerY, float size, int color) {
        float radius = shape.getValue().equals("Circle") ? size / 2f : Math.min(corners.getInt(), size / 4f);
        Draw.shapeF(ctx.gfx, markerX, markerY, size, size, radius, color);
    }

    private record TrailPoint(float x, float y, long timestamp) {
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
