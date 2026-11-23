package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ModeSetting;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Port of Meteor's AntiVoid: jumps or enables Flight when falling into the void. */
public class AntiVoid extends Module implements Module.Tickable {
    private final ModeSetting mode = new ModeSetting("mode",
            "The method to prevent you from falling into the void.", "Jump",
            List.of("Flight", "Jump"));

    private boolean wasFlightEnabled;
    private boolean hasRun;

    public AntiVoid() {
        super("AntiVoid", "Attempts to prevent you from falling into the void.", 'V');
        addSetting(mode);
    }

    @Override
    protected void onEnable() {
        if (mode.getValue().equals("Flight")) {
            Fly flight = Fly.get();
            wasFlightEnabled = flight != null && flight.isEnabled();
        }
    }

    @Override
    protected void onDisable() {
        Fly flight = Fly.get();
        if (!wasFlightEnabled && mode.getValue().equals("Flight") && flight != null) {
            flight.setEnabled(false);
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        int minY = client.level.getMinY();
        if (client.player.getY() > minY || client.player.getY() < minY - 15) {
            Fly flight = Fly.get();
            if (hasRun && mode.getValue().equals("Flight") && flight != null) {
                flight.setEnabled(false);
                hasRun = false;
            }
            return;
        }
        if (mode.getValue().equals("Flight")) {
            Fly flight = Fly.get();
            if (flight != null) {
                flight.setEnabled(true);
                hasRun = true;
            }
        } else {
            client.player.jumpFromGround();
        }
    }
}
