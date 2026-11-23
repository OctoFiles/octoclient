package dev.octoclient.module.hud;

import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.ServerTpsTracker;

import java.util.Locale;

/** Displays a smoothed estimate based on authoritative server world-time updates. */
public class TpsHud extends HudModule {
    private final NumberSetting lowThreshold =
            new NumberSetting("Low TPS Threshold", "Mark TPS below this value as low", 18.0, 1.0, 20.0, 0.5);
    private final ColorSetting normalColor = new ColorSetting("Normal Color", "Color at or above the threshold", 0xFF42D392);
    private final ColorSetting lowColor = new ColorSetting("Low Color", "Color below the threshold", 0xFFFF5F6D);

    public TpsHud() {
        super("TPS", "Estimate server ticks per second", 'T');
        addSetting(lowThreshold);
        addSetting(normalColor);
        addSetting(lowColor);
        x = 4;
        y = 46;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        double tps = ServerTpsTracker.getTps();
        String text = tps < 0.0 ? "TPS N/A" : String.format(Locale.ROOT, "TPS %.1f", tps);
        int color = tps >= 0.0 && tps < lowThreshold.getSnapped()
                ? lowColor.getValue()
                : tps < 0.0 ? Theme.TEXT_MUTED : normalColor.getValue();
        chip(ctx, text, color);
    }
}
