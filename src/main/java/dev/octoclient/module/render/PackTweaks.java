package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;

public class PackTweaks extends Module {
    private static PackTweaks instance;

    private final BooleanSetting hideSky = new BooleanSetting("Hide Sky", "Skip the sky render pass", false);
    private final BooleanSetting hideClouds = new BooleanSetting("Hide Clouds", "Skip the cloud render pass", false);
    private final BooleanSetting hidePumpkinOverlay =
            new BooleanSetting("Hide Pumpkin Overlay", "Hide the carved-pumpkin view overlay", false);
    private final BooleanSetting noVignette =
            new BooleanSetting("No Vignette", "Disable the screen-edge vignette", false);

    public PackTweaks() {
        super("Pack Tweaks", "Toggle sky and cloud rendering", 'P');
        addSetting(hideSky);
        addSetting(hideClouds);
        addSetting(hidePumpkinOverlay);
        addSetting(noVignette);
        instance = this;
    }

    public static boolean hidesSky() {
        return instance != null && instance.isEnabled() && instance.hideSky.isEnabled();
    }

    public static boolean hidesClouds() {
        return instance != null && instance.isEnabled() && instance.hideClouds.isEnabled();
    }

    public static boolean hidesPumpkinOverlay() {
        return instance != null && instance.isEnabled() && instance.hidePumpkinOverlay.isEnabled();
    }

    public static boolean hidesVignette() {
        return instance != null && instance.isEnabled() && instance.noVignette.isEnabled();
    }
}
