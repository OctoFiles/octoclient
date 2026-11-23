package dev.octoclient.module.secret;

import dev.octoclient.mixin.MultiPlayerGameModeAccessor;
import dev.octoclient.module.Module;
import net.minecraft.client.Minecraft;

/** Removes the client-side pause before mining the next block. */
public class NoBreakDelay extends Module implements Module.Tickable {
    public NoBreakDelay() {
        super("No Break Delay", "Remove the client-side delay between mined blocks", 'B');
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null
                || client.gameMode == null || !client.options.keyAttack.isDown()) {
            return;
        }
        ((MultiPlayerGameModeAccessor) client.gameMode).octoclient$setDestroyDelay(0);
    }
}
