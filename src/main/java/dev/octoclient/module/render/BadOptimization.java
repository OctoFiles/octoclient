package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;

public class BadOptimization extends Module implements Module.Tickable {
    private static BadOptimization instance;

    private final BooleanSetting hideClouds = new BooleanSetting("Hide Clouds", "Skip cloud rendering", false);
    private final BooleanSetting hideWeather = new BooleanSetting("Hide Weather", "Skip rain and snow rendering", false);
    private final BooleanSetting hideShadows = new BooleanSetting("Hide Entity Shadows", "Skip entity shadow rendering", false);
    private final BooleanSetting limitParticles = new BooleanSetting("Limit Particles", "Limit particles created per tick", false);
    private final BooleanSetting skinAtlas =
            new BooleanSetting("Skin Atlas", "Pack player skins into a shared GPU texture", false);
    private final BooleanSetting iconAtlas =
            new BooleanSetting("Icon Atlas", "Render nametag icons from a shared icon texture", false);
    private final BooleanSetting renderPassBindSkip =
            new BooleanSetting("Render Pass Bind Skip", "Skip repeated binds of the same pipeline in a render pass", false);
    private final NumberSetting particlesPerTick =
            new NumberSetting("Particles Per Tick", "Maximum non-important particles per tick", 40, 0, 200, 5);
    private static int particlesThisTick;

    public BadOptimization() {
        super("Bad Optimization", "Experimental per-feature rendering and particle limits", 'B');
        addSetting(hideClouds);
        addSetting(hideWeather);
        addSetting(hideShadows);
        addSetting(limitParticles);
        addSetting(particlesPerTick);
        addSetting(skinAtlas);
        addSetting(iconAtlas);
        addSetting(renderPassBindSkip);
        instance = this;
    }

    @Override
    public void onTick(net.minecraft.client.Minecraft client) {
        particlesThisTick = 0;
    }

    public static boolean hidesClouds() {
        return instance != null && instance.isEnabled() && instance.hideClouds.isEnabled();
    }

    public static boolean hidesWeather() {
        return instance != null && instance.isEnabled() && instance.hideWeather.isEnabled();
    }

    public static boolean hidesShadows() {
        return instance != null && instance.isEnabled() && instance.hideShadows.isEnabled();
    }

    public static boolean skipsRepeatedRenderPassBinds() {
        return instance != null && instance.isEnabled() && instance.renderPassBindSkip.isEnabled();
    }

    public static boolean usesSkinAtlas() {
        return instance != null && instance.isEnabled() && instance.skinAtlas.isEnabled();
    }

    public static boolean usesIconAtlas() {
        return instance != null && instance.isEnabled() && instance.iconAtlas.isEnabled();
    }

    public static boolean suppressParticle(boolean force, boolean important) {
        if (instance == null || !instance.isEnabled() || !instance.limitParticles.isEnabled() || force || important) {
            return false;
        }
        return particlesThisTick++ >= instance.particlesPerTick.getInt();
    }
}
