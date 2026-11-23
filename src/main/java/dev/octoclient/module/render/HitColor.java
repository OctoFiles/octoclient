package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ColorSetting;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Replaces the vanilla red hurt tint with a configurable hit color. */
public class HitColor extends Module {
    private static HitColor instance;
    private final ColorSetting color = new ColorSetting("Color", "Tint applied while an entity is hurt", 0xFFFF4D6D);

    public HitColor() {
        super("Hitcolor", "Change the tint shown on recently hit entities", 'H');
        addSetting(color);
        instance = this;
    }

    public static int tint(LivingEntityRenderState state, int vanillaTint) {
        if (instance == null || !instance.isEnabled() || !state.hasRedOverlay) {
            return vanillaTint;
        }
        return instance.color.getValue();
    }
}
