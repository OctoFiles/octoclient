package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.material.FogType;

/** Extends the visible fog range while optionally preserving underwater/lava fog. */
public class NoFog extends Module {
    private static NoFog instance;

    private final NumberSetting fogEnd =
            new NumberSetting("Fog Distance", "Distance at which fog fully fades out", 2048.0, 128.0, 8192.0, 128.0);
    private final BooleanSetting fluids = new BooleanSetting("Fluid Fog", "Also remove water and lava fog", false);

    public NoFog() {
        super("NoFog", "Reduce or remove environmental fog", 'F');
        addSetting(fogEnd);
        addSetting(fluids);
        instance = this;
    }

    public static void adjust(FogData fog, FogType fluidType) {
        if (instance == null || !instance.isEnabled() || fog == null) {
            return;
        }
        if (!instance.fluids.isEnabled() && fluidType != FogType.NONE) {
            return;
        }
        float distance = instance.fogEnd.getFloat();
        fog.environmentalStart = 0.0f;
        fog.environmentalEnd = distance;
        fog.renderDistanceStart = 0.0f;
        fog.renderDistanceEnd = distance;
        fog.skyEnd = distance;
        fog.cloudEnd = distance;
    }
}
