package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Pins the local world's displayed time to the selected Minecraft tick. */
public class TimeChanger extends Module implements Module.Tickable {
    private final NumberSetting time = new NumberSetting("Time", "Time of day from 0 to 24000 ticks", 6000, 0, 24000, 100);

    public TimeChanger() {
        super("Time Changer", "Set the time of day locally", 'T');
        addSetting(time);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level != null) {
            client.level.setTimeFromServer(time.getInt());
        }
    }
}
