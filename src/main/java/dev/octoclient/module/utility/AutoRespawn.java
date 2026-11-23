package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;

/** Clicks respawn for you after a configurable delay on the death screen. */
public class AutoRespawn extends Module implements Module.Tickable {
    private final NumberSetting delay = new NumberSetting("Delay", "Ticks to wait before respawning",
            20.0, 0.0, 100.0, 1.0);
    private final BooleanSetting closeScreen = new BooleanSetting("Close Screen", "Dismiss the death screen too", true);

    private int counter;
    private boolean fired;

    public AutoRespawn() {
        super("Auto Respawn", "Respawns without clicking", 'R');
        addSetting(delay);
        addSetting(closeScreen);
    }

    @Override
    protected void onEnable() {
        counter = 0;
        fired = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (!(client.gui.screen() instanceof DeathScreen)) {
            // Waiting for the world: re-arm for the next death.
            counter = 0;
            fired = false;
            return;
        }
        if (fired || client.player == null) {
            return;
        }
        if (counter++ < delay.getInt()) {
            return;
        }
        fired = true;
        client.player.respawn();
        if (closeScreen.isEnabled()) {
            client.setScreenAndShow(null);
        }
    }
}
