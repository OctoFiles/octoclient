package dev.octoclient.module.utility;

import dev.octoclient.module.Module;

public class WheelModule extends Module {
    private static WheelModule instance;

    public WheelModule() {
        super("Wheel", "Open player actions by right-clicking a player name in chat", 'W');
        instance = this;
    }

    public static boolean active() {
        return instance != null && instance.isEnabled();
    }
}
