package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;

/**
 * A selectable aura design drawn by {@link dev.octoclient.render.CosmeticLayers}.
 */
public class Aura extends Module implements CosmeticPart {
    private final String designName;
    public final ColorSetting color = new ColorSetting("Color", "Aura colour", 0xFF8E97C4);
    public final NumberSetting width = new NumberSetting("Width", "How wide the aura is", 1.0, 0.5, 2.0, 0.05);
    public final NumberSetting height = new NumberSetting("Height", "How tall the aura is", 1.0, 0.3, 2.0, 0.05);
    public final NumberSetting speed = new NumberSetting("Speed", "How fast it swirls", 1.0, 0.0, 3.0, 0.1);

    public Aura(String designName) {
        super(designName, "Wear this aura", 'A');
        this.designName = designName;
        addSetting(color);
        addSetting(width);
        addSetting(height);
        addSetting(speed);
    }

    public String designName() {
        return designName;
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.AURA;
    }

    @Override
    protected void onEnable() {
        CatalogCosmetic.disableCatalogType("AURA", this);
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof Aura other && other != this && other.isEnabled()) {
                other.setEnabled(false);
            }
        }
    }
}
