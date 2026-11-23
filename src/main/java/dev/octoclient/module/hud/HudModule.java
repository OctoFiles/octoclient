package dev.octoclient.module.hud;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.module.settings.ClientSettings;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Draw;

/**
 * A module that renders on the in-game HUD and can be dragged around
 * in the HUD Edit screen.
 */
public abstract class HudModule extends Module {
    /** Anchor-based position, in scaled GUI coordinates. */
    public float x;
    public float y;

    /** Cached size used by the edit screen for hit-testing before first render. */
    public float width = 60;
    public float height = 20;
    private final NumberSetting size = new NumberSetting("HUD Scale", "Overall HUD element size", 1.0, 0.5, 2.0, 0.05);
    private final BooleanSetting noBackground =
            new BooleanSetting("No Background", "Hide this HUD element's background", false);
    private float measuredScale = 1f;

    protected HudModule(String name, String description, char icon) {
        super(name, description, ModuleCategory.HUD, icon);
        addSetting(size);
        addSetting(noBackground);
    }

    /** Called every frame while the module is enabled and HUD edit is closed. */
    public abstract void render(HudDrawContext ctx, float tickDelta);

    /** Called while the HUD edit screen is open to draw the module preview. */
    public void renderEdit(HudDrawContext ctx, float tickDelta, boolean selected) {
        render(ctx, tickDelta);
    }

    public NumberSetting sizeSetting() {
        return size;
    }

    public boolean backgroundsEnabled() {
        return !noBackground.isEnabled() && !ClientSettings.noBackgroundEnabled();
    }

    public void renderScaled(HudDrawContext ctx, float tickDelta, boolean editing, boolean selected) {
        width /= measuredScale;
        height /= measuredScale;
        float scale = size.getFloat();
        ctx.gfx.pose().pushMatrix();
        ctx.gfx.pose().translate(x, y);
        ctx.gfx.pose().scale(scale, scale);
        ctx.gfx.pose().translate(-x, -y);
        try {
            if (editing) {
                renderEdit(ctx, tickDelta, selected);
            } else {
                render(ctx, tickDelta);
            }
        } finally {
            ctx.gfx.pose().popMatrix();
            width *= scale;
            height *= scale;
            measuredScale = scale;
        }
    }

    protected void drawPanel(HudDrawContext ctx, int x, int y, int width, int height, int radius,
                             int tint, float saturation, float edge, int blur) {
        if (!backgroundsEnabled()) {
            return;
        }
        String style = effectiveCardStyle();
        if (style.equals(ClientSettings.STYLE_FROSTED)) {
            // Dark frosted glass: matte graphite pane with a heavy backdrop
            // frost and a single hairline rim, for HUD boxes and chips.
            Draw.frostedGlass(ctx.gfx, x, y, width, height, radius, tint, saturation, edge, blur);
            return;
        }
        if (style.equals("Clear")) {
            // Outline only: no fill so the game stays fully visible.
            Draw.roundedOutline(ctx.gfx, x, y, width, height, radius, Theme.withAlpha(0xFFFFFF, 70));
            return;
        }
        if (style.equals("Solid")) {
            Draw.roundedRect(ctx.gfx, x, y, width, height, radius, 0xEE111216);
            Draw.roundedOutline(ctx.gfx, x, y, width, height, radius, Theme.withAlpha(0xFFFFFF, 40));
            return;
        }
        Draw.liquidGlass(ctx.gfx, x, y, width, height, radius, tint, saturation, edge, blur);
    }

    /**
     * Draws a text pill sized to the text and refreshes {@link #width}/{@link #height}
     * so the HUD editor can hit-test it. Its glass background can be hidden with
     * the per-module or global no-background setting.
     */
    protected void chip(HudDrawContext ctx, String text, int textColor) {
        int textW = ctx.textWidth(text);
        int h = ctx.textHeight();
        int padX = 6;
        int padY = 4;
        int dot = 5;
        int w = textW + padX * 2 + dot;
        int ph = h + padY * 2;
        int px = (int) x - padX;
        int py = (int) y - padY;
        drawPanel(ctx, px, py, w, ph, Math.min(cornerRadiusPx(), ph / 2), 0x8E97C4, 0.94f, 0f, 76);
        ctx.drawText(text, x, y, textColor, false);
        // Status dot: filled, in the chip's own colour (FPS tier, etc.).
        int dotX = px + w - padX - dot + 1;
        int dotY = py + (ph - dot) / 2;
        Draw.shapeF(ctx.gfx, dotX, dotY, dot, dot, dot / 2f, Theme.withAlpha(textColor, 235));
        width = w;
        height = ph;
    }
}
