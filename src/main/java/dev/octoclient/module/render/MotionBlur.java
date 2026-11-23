package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.natural.motionblur.config.ConfigEntries;
import net.natural.motionblur.config.ConfigManager;

import java.util.List;

/** Controls the bundled Natural Motion Blur implementation from the dashboard. */
public class MotionBlur extends Module {
    private final NumberSetting strength =
            new NumberSetting("Strength", "Motion blur intensity", 1.0, 0.0, 2.0, 0.01);
    private final ModeSetting algorithm = new ModeSetting("Algorithm", "Frame blending method",
            "Velocity Based", List.of("Velocity Based", "Frame Blending", "Hybrid Blending",
            "Accumulation Max", "Accumulation Mix"));
    private final ModeSetting profile =
            new ModeSetting("Profile", "How previous frames are blended", "Uniform", List.of("Uniform", "Weighted"));
    private final BooleanSetting refreshRateScaling =
            new BooleanSetting("Refresh Rate Scaling", "Adjust blur strength to the monitor refresh rate", true);
    private String lastConfigState;
    private boolean missingDependencyLogged;

    public MotionBlur() {
        super("Motion Blur", "Natural Motion Blur by ItsPasi", 'M');
        addSetting(strength);
        addSetting(algorithm);
        addSetting(profile);
        addSetting(refreshRateScaling);
    }

    /** Synchronizes OctoClient's controls with the bundled mod without saving every tick. */
    public void syncExternalConfig() {
        if (!FabricLoader.getInstance().isModLoaded("naturalmotionblur")) {
            if (!missingDependencyLogged) {
                System.err.println("[OctoClient] Natural Motion Blur is not loaded; its controls are inactive.");
                missingDependencyLogged = true;
            }
            return;
        }
        ConfigEntries config = ConfigManager.getConfig();
        config.enabled = isEnabled();
        config.motionBlurStrength = strength.getFloat();
        config.blurAlgorithm = switch (algorithm.getValue()) {
            case "Frame Blending" -> ConfigEntries.BlurAlgorithm.FRAME_BLENDING;
            case "Hybrid Blending" -> ConfigEntries.BlurAlgorithm.HYBRID_BLENDING;
            case "Accumulation Max" -> ConfigEntries.BlurAlgorithm.ACCUMULATION_MAX;
            case "Accumulation Mix" -> ConfigEntries.BlurAlgorithm.ACCUMULATION_MIX;
            default -> ConfigEntries.BlurAlgorithm.VELOCITY_BASED;
        };
        config.blurProfile = profile.getValue().equals("Weighted")
                ? ConfigEntries.BlurProfile.WEIGHTED : ConfigEntries.BlurProfile.UNIFORM;
        config.refreshRateScaling = refreshRateScaling.isEnabled();

        String state = config.enabled + "|" + config.motionBlurStrength + "|" + config.blurAlgorithm
                + "|" + config.blurProfile + "|" + config.refreshRateScaling;
        if (!state.equals(lastConfigState)) {
            ConfigManager.saveConfig();
            lastConfigState = state;
        }
    }
}
