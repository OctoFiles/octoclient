package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.NumberSetting;

/**
 * Feather fans on the back, drawn by {@link dev.octoclient.render.CosmeticLayers}.
 * Client-side only: nothing is sent to the server.
 */
public class Wings extends Module implements CosmeticPart {
    private final String designName;
    public final ColorSetting color = new ColorSetting("Color", "Wing colour", 0xFFE8ECF5);
    public final NumberSetting span = new NumberSetting("Span", "How far the wings spread", 1.0, 0.5, 2.0, 0.05);
    public final NumberSetting length = new NumberSetting("Length", "Wing length", 1.0, 0.5, 1.8, 0.05);
    public final NumberSetting flap = new NumberSetting("Flap", "How much the wings beat", 1.0, 0.0, 2.0, 0.05);
    public final NumberSetting speed = new NumberSetting("Speed", "Beats per second", 1.0, 0.2, 3.0, 0.1);

    public Wings() {
        this("Feather");
    }

    public Wings(String designName) {
        super(designName, "Wear these wings; select them like a cape", 'W');
        this.designName = designName;
        addSetting(color);
        addSetting(span);
        addSetting(length);
        addSetting(flap);
        addSetting(speed);
    }

    public String designName() {
        return designName;
    }

    public boolean isBeeStyle() {
        return designName.equals("Bee");
    }

    public boolean isProcedural() {
        return designName.equals("Feather") || isBeeStyle();
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.WINGS;
    }

    @Override
    protected void onEnable() {
        CatalogCosmetic.disableCatalogType("WINGS", this);
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof Wings other && other != this && other.isEnabled()) {
                other.setEnabled(false);
            }
        }
    }
}
