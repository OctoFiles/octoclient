package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import net.minecraft.client.Minecraft;

/**
 * Turns off the walking camera bob without destroying the player's own
 * setting: the previous value is captured on enable and put back on disable.
 */
public class NoBobbing extends Module {
    private Boolean original;

    public NoBobbing() {
        super("No Bobbing", "Stops the camera bob while walking", 'B');
    }

    @Override
    protected void onEnable() {
        var options = Minecraft.getInstance().options;
        original = options.bobView().get();
        options.bobView().set(false);
    }

    @Override
    protected void onDisable() {
        if (original != null) {
            Minecraft.getInstance().options.bobView().set(original);
            original = null;
        }
    }
}
