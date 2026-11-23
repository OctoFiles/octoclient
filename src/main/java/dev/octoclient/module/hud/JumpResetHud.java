package dev.octoclient.module.hud;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;
import dev.octoclient.ui.Theme;

/** Damage-timing cue that leaves the jump input under the player's control. */
public class JumpResetHud extends HudModule implements Module.Tickable {
    private final NumberSetting cueTicks =
            new NumberSetting("Cue Duration", "How long to show the jump cue in ticks", 4.0, 1.0, 12.0, 1.0);
    private final BooleanSetting movingOnly =
            new BooleanSetting("Moving Only", "Only show the cue while moving", true);
    private final ColorSetting cueColor = new ColorSetting("Cue Color", "Color of the jump cue", 0xFFFFC857);

    private float previousHealth = Float.NaN;
    private int remainingCueTicks;

    public JumpResetHud() {
        super("Jump Reset", "Shows a manual cue when incoming damage may be reset with a timed jump", 'J');
        addSetting(cueTicks);
        addSetting(movingOnly);
        addSetting(cueColor);
        x = 4;
        y = 64;
    }

    @Override
    protected void onEnable() {
        previousHealth = Float.NaN;
        remainingCueTicks = 0;
    }

    @Override
    public void onTick(net.minecraft.client.Minecraft client) {
        if (client.player == null || client.level == null) {
            previousHealth = Float.NaN;
            remainingCueTicks = 0;
            return;
        }
        float health = client.player.getHealth();
        if (!Float.isNaN(previousHealth) && health < previousHealth
                && (!movingOnly.isEnabled() || client.player.getDeltaMovement().horizontalDistanceSqr() > 0.0025)) {
            remainingCueTicks = cueTicks.getInt();
        } else if (remainingCueTicks > 0) {
            remainingCueTicks--;
        }
        previousHealth = health;
    }

    @Override
    public void render(HudDrawContext ctx, float tickDelta) {
        if (remainingCueTicks > 0) {
            chip(ctx, "JUMP!", cueColor.getValue());
        } else {
            chip(ctx, "Jump Reset ready", Theme.TEXT_MUTED);
        }
    }
}
