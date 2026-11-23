package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;

public class Bandana extends Module implements CosmeticPart {
    public Bandana(String name) {
        super(name, "Wear this bandana", 'B');
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.BANDANA;
    }

    @Override
    protected void onEnable() {
        CatalogCosmetic.disableCatalogType("BANDANA", this);
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof Bandana other && other != this && other.isEnabled()) {
                other.setEnabled(false);
            }
        }
    }
}
