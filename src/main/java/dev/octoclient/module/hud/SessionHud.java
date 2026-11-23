package dev.octoclient.module.hud;

import dev.octoclient.ui.Theme;

import java.util.Locale;

/** Wall-clock length of the current session, restarted whenever the module is enabled. */
public class SessionHud extends HudModule {
    private long start = System.currentTimeMillis();

    public SessionHud() {
        super("Session", "How long you have been playing", 'T');
        this.x = 4;
        this.y = 4;
    }

    @Override
    protected void onEnable() {
        start = System.currentTimeMillis();
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - start) / 1000L);
        String text = String.format(Locale.ROOT, "%02d:%02d:%02d",
                seconds / 3600L, (seconds % 3600L) / 60L, seconds % 60L);
        chip(ctx, text, Theme.TEXT_SECONDARY);
    }
}
