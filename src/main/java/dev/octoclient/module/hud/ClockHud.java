package dev.octoclient.module.hud;

import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Draw;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Shows either the real clock or the in-game world time. */
public class ClockHud extends HudModule {
    private final ModeSetting source = new ModeSetting("Source", "Which clock to show",
            "Real time", List.of("Real time", "World time", "Both"));
    private final ModeSetting format = new ModeSetting("Format", "12h or 24h clock",
            "24h", List.of("24h", "12h"));

    private static final DateTimeFormatter F24 = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter F12 = DateTimeFormatter.ofPattern("hh:mm a");

    public ClockHud() {
        super("Clock", "Real or in-game time", 'C');
        this.x = 4;
        this.y = 4;
        addSetting(source);
        addSetting(format);
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        DateTimeFormatter fmt = "12h".equals(format.getValue()) ? F12 : F24;
        String mode = source.getValue();

        String line;
        if ("World time".equals(mode)) {
            line = worldTime(ctx);
        } else if ("Both".equals(mode)) {
            line = worldTime(ctx) + "  " + LocalDateTime.now().format(fmt);
        } else {
            line = LocalDateTime.now().format(fmt);
        }

        int w = ctx.textWidth(line);
        chip(ctx, line, Theme.TEXT_SECONDARY);
        width = Math.max(width, w + 8);
    }

    /** Minecraft day ticks: 0 = 06:00, 6000 = noon, 18000 = midnight. */
    private String worldTime(HudDrawContext ctx) {
        var level = ctx.mc.level;
        if (level == null) {
            return "--:--";
        }
        long ticks = level.getOverworldClockTime() % 24000L;
        long minutes = ((ticks + 6000L) % 24000L) * 60L / 1000L;
        LocalTime time = LocalTime.of((int) ((minutes / 60) % 24), (int) (minutes % 60));
        return time.format("12h".equals(format.getValue()) ? F12 : F24);
    }
}
