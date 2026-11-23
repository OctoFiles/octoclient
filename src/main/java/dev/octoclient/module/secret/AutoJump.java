package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import net.minecraft.client.Minecraft;

/** Automatically jumps when the player is moving forward and grounded. */
public class AutoJump extends Module implements Module.Tickable {
    public AutoJump() {
        super("Auto Jump", "Jump automatically while moving forward", 'J');
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null
                || !client.options.keyUp.isDown() || client.player.isPassenger()
                || !client.player.onGround()) {
            return;
        }
        client.player.jumpFromGround();
    }
}
