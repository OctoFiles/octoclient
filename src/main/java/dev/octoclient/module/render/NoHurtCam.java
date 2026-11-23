package dev.octoclient.module.render;

import dev.octoclient.module.Module;

public class NoHurtCam extends Module {
    private static NoHurtCam instance;

    public NoHurtCam() {
        super("NoHurtCam", "Disable camera shake when damaged", 'N');
        instance = this;
    }

    public static boolean shouldDisable() {
        return instance != null && instance.isEnabled();
    }
}
