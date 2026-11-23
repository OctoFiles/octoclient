package dev.octoclient.module.cosmetics;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.TextSetting;

public class Accessory extends Module implements CosmeticPart {
    private final String designName;
    private final TextSetting minecraftUser;

    public Accessory(String designName) {
        super(designName, "Wear this accessory", 'X');
        this.designName = designName;
        if (AccessoryDesigns.usesMinecraftSkin(designName)) {
            minecraftUser = new TextSetting("Minecraft User", "Minecraft username whose skin the MiniMe wears", "", 16);
            addSetting(minecraftUser);
        } else {
            minecraftUser = null;
        }
    }

    public String designName() {
        return designName;
    }

    public String minecraftUser() {
        return minecraftUser == null ? "" : minecraftUser.getValue().trim();
    }

    @Override
    public CosmeticKind kind() {
        return CosmeticKind.ACCESSORY;
    }

    @Override
    protected void onEnable() {
        for (Module module : ModuleManager.get().getModules()) {
            if (module instanceof Accessory other && other != this && other.isEnabled()) {
                other.setEnabled(false);
            }
        }
    }
}
