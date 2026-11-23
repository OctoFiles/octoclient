package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Periodically jumps while idle to avoid inactivity timeouts. */
public class AntiAfk extends Module implements Module.Tickable {
    private final NumberSetting interval = new NumberSetting(
            "Interval", "Seconds between idle jumps", 60, 10, 300, 5);
    private int ticks;

    public AntiAfk() {
        super("Anti AFK", "Periodically jump while standing still", 'A');
        addSetting(interval);
    }

    @Override
    protected void onEnable() {
        ticks = 0;
    }

    @Override
    protected void onDisable() {
        ticks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null) {
            ticks = 0;
            return;
        }
        if (client.player.isPassenger() || !client.player.onGround()
                || client.options.keyUp.isDown() || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown() || client.options.keyRight.isDown()) {
            ticks = 0;
            return;
        }
        if (++ticks >= interval.getInt() * 20) {
            client.player.jumpFromGround();
            ticks = 0;
        }
    }
}
