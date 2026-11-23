package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;

public class HatDesign extends Module implements CosmeticPart {
    private final String designName;

    public HatDesign(String designName) {
        super(designName, "Wear this hat design", 'H');
        this.designName = designName;
    }

    public String designName() {
        return designName;
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.HAT;
    }

    @Override
    protected void onEnable() {
        CatalogCosmetic.disableCatalogType("HAT", this);
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof HatDesign other && other != this && other.isEnabled()) {
                other.setEnabled(false);
            } else if (module instanceof Hat hat && hat.isEnabled()) {
                hat.setEnabled(false);
            }
        }
    }
}
