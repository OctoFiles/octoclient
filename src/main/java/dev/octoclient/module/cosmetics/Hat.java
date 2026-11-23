package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;

/** Coloured crown worn on top of the head, drawn by {@link dev.octoclient.render.CosmeticLayers}. */
public class Hat extends Module implements CosmeticPart {
    public final ColorSetting color = new ColorSetting("Color", "Hat colour", 0xFFFFC94A);
    public final NumberSetting size = new NumberSetting("Size", "How wide the hat sits", 1.0, 0.8, 1.4, 0.05);

    public Hat() {
        super("Hat", "Wear a client-side hat", 'H');
        addSetting(color);
        addSetting(size);
    }

    @Override
    protected void onEnable() {
        CatalogCosmetic.disableCatalogType("HAT", this);
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof HatDesign design && design.isEnabled()) {
                design.setEnabled(false);
            }
        }
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.HAT;
    }
}
