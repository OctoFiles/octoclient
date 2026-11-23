package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ModeSetting;
import net.minecraft.client.Minecraft;
import java.util.List;

/** Overrides the visible client-side weather without changing the server world. */
public class NoWeather extends Module implements Module.Tickable {
    private final ModeSetting weather = new ModeSetting("Weather", "Choose the weather rendered by this client",
            "Clear", List.of("Clear", "Rain", "Thunder", "Snow"));

    public NoWeather() {
        super("Weather Changer", "Choose client-side clear, rainy, stormy, or snowy weather", 'W');
        addSetting(weather);
    }

    @Override
    public void onTick(Minecraft client) {
        var level = client.level;
        if (level == null) {
            return;
        }
        String selection = weather.getValue();
        level.setRainLevel(selection.equals("Rain") || selection.equals("Thunder") || selection.equals("Snow") ? 1f : 0f);
        level.setThunderLevel(selection.equals("Thunder") ? 1f : 0f);
    }
}
