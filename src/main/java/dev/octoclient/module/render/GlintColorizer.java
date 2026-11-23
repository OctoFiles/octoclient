package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ColorSetting;

/** Recolors the vertex tint used by enchanted-item foil passes. */
public class GlintColorizer extends Module {
    private static GlintColorizer instance;
    private final ColorSetting color = new ColorSetting("Glint Color", "Enchantment glint color and opacity", 0xFFB267FF);

    public GlintColorizer() {
        super("Glint Colorizer", "Customize the enchantment glint color", 'G');
        addSetting(color);
        instance = this;
    }

    public static boolean active() {
        return instance != null && instance.isEnabled();
    }

    public static int color() {
        return instance == null ? 0xFFFFFFFF : instance.color.getValue();
    }
}
