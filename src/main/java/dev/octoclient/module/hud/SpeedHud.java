package dev.octoclient.module.hud;

import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.ui.Theme;
import dev.octoclient.util.Draw;
import net.minecraft.client.player.LocalPlayer;

/** Horizontal movement speed in blocks per second. */
public class SpeedHud extends HudModule {
    private final BooleanSetting vertical = new BooleanSetting("Vertical", "Include Y speed", false);

    /** Exponentially smoothed reading so the digits don't flicker every frame. */
    private float smoothed;
    private boolean primed;

    public SpeedHud() {
        super("Speed", "Movement in blocks per second", 'S');
        this.x = 4;
        this.y = 4;
        addSetting(vertical);
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        LocalPlayer player = ctx.player();
        if (player == null) {
            return;
        }

        var delta = player.getDeltaMovement();
        double raw = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        if (vertical.isEnabled()) {
            raw = delta.length();
        }
        // getDeltaMovement is per tick; multiply to get blocks per second.
        float bps = (float) (raw * 20.0);

        if (!primed) {
            primed = true;
            smoothed = bps;
        }
        // Smooth over roughly a quarter second of frames.
        smoothed += (bps - smoothed) * 0.25f;

        String text = String.format(java.util.Locale.ROOT, "SPEED %.2f bps", smoothed);
        chip(ctx, text, Theme.TEXT_SECONDARY);
    }
}
