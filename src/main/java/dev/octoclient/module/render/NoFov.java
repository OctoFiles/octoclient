package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Removes the sprint / damage FOV kicks by pinning the vanilla FOV-effect
 * scale to zero. The player's original value is restored on disable.
 */
public class NoFov extends Module {
    private Double original;

    public NoFov() {
        super("No FOV Effects", "Removes sprint and damage FOV kicks", 'O');
    }

    @Override
    protected void onEnable() {
        var options = Minecraft.getInstance().options;
        original = options.fovEffectScale().get();
        options.fovEffectScale().set(0.0);
    }

    @Override
    protected void onDisable() {
        if (original != null) {
            Minecraft.getInstance().options.fovEffectScale().set(original);
            original = null;
        }
    }
}
