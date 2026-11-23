package dev.octoclient.module.secret;

import com.mojang.blaze3d.platform.InputConstants;
import dev.octoclient.mixin.KeyMappingAccessor;
import dev.octoclient.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Holds the configured forward key while the player is in the world. */
public class AutoWalk extends Module implements Module.Tickable {
    public AutoWalk() {
        super("Auto Walk", "Automatically hold the forward movement key", 'W');
    }

    @Override
    public void onTick(Minecraft client) {
        KeyMapping forward = client.options.keyUp;
        if (client.getWindow() == null) {
            forward.setDown(false);
            return;
        }
        if (client.player == null || client.level == null || client.gui.screen() != null) {
            forward.setDown(isPhysicalKeyDown(client, forward));
            return;
        }
        forward.setDown(true);
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.options != null && client.getWindow() != null) {
            client.options.keyUp.setDown(isPhysicalKeyDown(client, client.options.keyUp));
        }
    }

    private static boolean isPhysicalKeyDown(Minecraft client, KeyMapping mapping) {
        InputConstants.Key key = ((KeyMappingAccessor) mapping).octoclient$getKey();
        return key.getType() == InputConstants.Type.KEYSYM
                && GLFW.glfwGetKey(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
    }
}
