package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;

public class LowFire extends Module {
    private static LowFire instance;

    private final NumberSetting lowerAmount = new NumberSetting("Low Fire Height", "Move the fire overlay down", 0.35, 0, 0.8, 0.01);
    private final NumberSetting opacity = new NumberSetting("Opacity", "Fire overlay opacity in percent", 55, 0, 100, 1);

    public LowFire() {
        super("Low Fire", "Lower or fade the first-person fire overlay", 'F');
        addSetting(lowerAmount);
        addSetting(opacity);
        instance = this;
    }

    public static boolean active() {
        return instance != null && instance.isEnabled();
    }

    public static float lowerAmount() {
        return instance == null ? 0 : instance.lowerAmount.getFloat();
    }

    public static int opacityPercent() {
        return instance == null ? 100 : instance.opacity.getInt();
    }
}
