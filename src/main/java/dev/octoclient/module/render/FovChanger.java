package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Applies a user-selected base field of view while preserving the original option on disable. */
public class FovChanger extends Module implements Module.Tickable {
    private final NumberSetting fov = new NumberSetting("FOV", "Base field of view", 90.0, 30.0, 110.0, 1.0);
    private Integer original;

    public FovChanger() {
        super("FOV Changer", "Set a custom base field of view", 'V');
        addSetting(fov);
    }

    @Override
    protected void onEnable() {
        original = Minecraft.getInstance().options.fov().get();
    }

    @Override
    protected void onDisable() {
        if (original != null) {
            Minecraft.getInstance().options.fov().set(original);
            original = null;
        }
    }

    @Override
    public void onTick(Minecraft client) {
        client.options.fov().set(fov.getInt());
    }
}
