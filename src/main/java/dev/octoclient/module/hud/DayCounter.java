package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.ui.Theme;

import java.util.Locale;

/** In-game day counter with the clock time of the current world. */
public class DayCounter extends HudModule {
    private final BooleanSetting showDay = new BooleanSetting("Day", "Show the day number", true);
    private final BooleanSetting showTime = new BooleanSetting("Time", "Show the in-game clock", true);

    public DayCounter() {
        super("Day Counter", "World day and in-game clock", 'D');
        addSetting(showDay);
        addSetting(showTime);
        this.x = 4;
        this.y = 150;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (ctx.mc.level == null) {
            return;
        }
        long ticks = ctx.mc.level.getOverworldClockTime();
        int day = (int) (ticks / 24000L) + 1;
        int timeOfDay = (int) (ticks % 24000L);
        int hours = (timeOfDay / 1000 + 6) % 24;
        int minutes = (timeOfDay % 1000) * 60 / 1000;
        String time = String.format(Locale.ROOT, "%02d:%02d", hours, minutes);

        String text;
        if (showDay.isEnabled() && showTime.isEnabled()) {
            text = "Day " + day + " \u00b7 " + time;
        } else if (showDay.isEnabled()) {
            text = "Day " + day;
        } else if (showTime.isEnabled()) {
            text = time;
        } else {
            return;
        }
        boolean night = hours >= 18 || hours < 6;
        chip(ctx, text, night ? Theme.TEXT_MUTED : Theme.TEXT_SECONDARY);
    }
}
