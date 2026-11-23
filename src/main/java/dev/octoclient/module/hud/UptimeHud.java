package dev.octoclient.module.hud;

import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.ui.Theme;

import java.util.List;
import java.util.Locale;

/** Time elapsed since OctoClient initialized for this game process. */
public class UptimeHud extends HudModule {
    private final long startedAt = System.currentTimeMillis();
    private final ModeSetting format = new ModeSetting("Format", "Choose uptime formatting",
            "Clock", List.of("Clock", "Compact"));

    public UptimeHud() {
        super("Uptime", "How long the game has been open", 'U');
        addSetting(format);
        x = 4;
        y = 28;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - startedAt) / 1000L);
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        long remainingSeconds = seconds % 60L;
        String text = format.getValue().equals("Compact")
                ? (days > 0 ? days + "d " : "")
                + String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainingSeconds)
                : String.format(Locale.ROOT, "%02d:%02d:%02d:%02d", days, hours, minutes, remainingSeconds);
        chip(ctx, "UPTIME " + text, Theme.TEXT_SECONDARY);
    }
}
