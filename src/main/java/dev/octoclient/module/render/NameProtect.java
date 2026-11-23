package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleCategory;
import dev.octoclient.module.setting.ModeSetting;

import java.util.List;

/** Masks your username on client-rendered HUD elements. */
public class NameProtect extends Module {
    private final ModeSetting alias = new ModeSetting("Alias", "Name shown instead of yours",
            "You", List.of("You", "Player", "Protected"));

    public NameProtect() {
        super("Name Protect", "Hides your name on the HUD", 'N');
        addSetting(alias);
    }

    /** Returns the alias for the given name if it matches the local player. */
    public String protect(String name) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.getName().getString().equalsIgnoreCase(name)) {
            return alias.getValue();
        }
        return name;
    }
}
