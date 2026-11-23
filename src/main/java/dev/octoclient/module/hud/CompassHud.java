package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Anim;
import dev.octoclient.util.Draw;
import net.minecraft.client.Minecraft;

/** Compass strip at the top of the screen with the facing direction and degrees. */
public class CompassHud extends HudModule {
    private final BooleanSetting showDegrees = new BooleanSetting("Degrees", "Show the yaw angle", true);

    private static final String[] DIRECTIONS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    private float appearAnim;

    public CompassHud() {
        super("Direction Compass", "Compass strip with facing direction", 'C');
        addSetting(showDegrees);
        this.x = 120;
        this.y = 2;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Minecraft mc = ctx.mc;
        if (mc.player == null) {
            return;
        }
        appearAnim = Anim.approach(appearAnim, 1, 0.4f, tickDelta);

        float yaw = mc.player.getYRot();
        int deg = ((int) Math.round(yaw) % 360 + 360) % 360;
        int idx = ((int) Math.round(yaw / 45.0) % 8 + 8) % 8;

        int pad = 8;
        int chipPad = 6;
        int gap = 8;
        int lineH = ctx.textHeight();

        // Widths of the five visible entries (idx-2 .. idx+2).
        String[] slots = new String[5];
        boolean[] center = new boolean[5];
        for (int i = 0; i < 5; i++) {
            int d = ((idx - 2 + i) % 8 + 8) % 8;
            slots[i] = DIRECTIONS[d];
            center[i] = i == 2;
        }
        String centerText = slots[2] + (showDegrees.isEnabled() ? " (" + deg + "\u00B0)" : "");

        int width = pad * 2;
        for (int i = 0; i < 5; i++) {
            width += (center[i] ? ctx.textWidth(centerText) + chipPad * 2 : ctx.textWidth(slots[i])) + gap;
        }
        width -= gap;
        int height = lineH + pad * 2;
        this.width = width;
        this.height = height;

        float alphaF = Anim.clamp01(appearAnim);
        int alpha = (int) (alphaF * 255);
        drawPanel(ctx, (int) x, (int) y, width, height, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);
        ctx.enableScissor(x, y, x + width, y + height);

        float cx = x + pad;
        float cy = y + pad;
        for (int i = 0; i < 5; i++) {
            if (center[i]) {
                int chipW = ctx.textWidth(centerText) + chipPad * 2;
                Draw.roundedRect(ctx.gfx, (int) cx, (int) cy - 2, chipW, lineH + 4, 4,
                        Theme.withAlpha(Theme.DARK_800, alpha));
                ctx.outline(cx, cy - 2, cx + chipW, cy + lineH + 2, Theme.withAlpha(Theme.DARK_600, alpha));
                ctx.drawText(centerText, cx + chipPad, cy,
                        Theme.withAlpha(Theme.TEXT_PRIMARY, alpha), false);
                cx += chipW + gap;
            } else {
                ctx.drawText(slots[i], cx, cy, Theme.withAlpha(Theme.TEXT_FAINT, alpha), false);
                cx += ctx.textWidth(slots[i]) + gap;
            }
        }
        ctx.disableScissor();

        // Center the widget horizontally when left at the default position.
        // (Positioning is handled by the HUD editor; defaults are pre-centered.)
    }
}
