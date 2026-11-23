package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.settings.ClientSettings;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Draw;

/** JVM heap usage, with an optional bar showing the fill ratio. */
public class MemoryHud extends HudModule {
    private final BooleanSetting bar = new BooleanSetting("Bar", "Show usage bar", true);
    private final BooleanSetting mb = new BooleanSetting("In MB", "Show MB instead of GB", false);

    public MemoryHud() {
        super("Memory", "JVM heap usage", 'M');
        this.x = 4;
        this.y = 4;
        addSetting(bar);
        addSetting(mb);
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();

        String label = mb.isEnabled()
                ? String.format(java.util.Locale.ROOT, "%d / %d MB", used >> 20, max >> 20)
                : String.format(java.util.Locale.ROOT, "%.1f / %.1f GB", used / 1073741824.0, max / 1073741824.0);
        String text = "MEM " + label;

        int w = ctx.textWidth(text);
        int h = ctx.textHeight() + (bar.isEnabled() ? 5 : 0);

        drawPanel(ctx, (int) x - 4, (int) y - 3, w + 8, h + 6, cornerRadiusPx(), 0x8E97C4, 0.94f, 0f, 70);
        ctx.drawText(text, x, y, Theme.TEXT_SECONDARY, false);

        if (bar.isEnabled()) {
            float ratio = max <= 0 ? 0f : (float) used / max;
            int barY = (int) y + ctx.textHeight() + 1;
            ctx.fill(x, barY, x + w, barY + 2, Theme.withAlpha(0xFFFFFF, 40));
            int fill = Math.round(w * Math.min(1f, ratio));
            ctx.fill(x, barY, x + fill, barY + 2,
                    ratio > 0.85f ? 0xFFFF6B6B : ClientSettings.accentRgb());
        }

        width = w + 6;
        height = h + 4;
    }
}
