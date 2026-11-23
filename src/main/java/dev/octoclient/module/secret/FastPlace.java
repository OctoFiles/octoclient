package dev.octoclient.module.secret;

import dev.octoclient.mixin.MinecraftAccessor;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;

/** Adjusts the client-side delay between item uses while use is held. */
public class FastPlace extends Module implements Module.Tickable {
    private final NumberSetting delay =
            new NumberSetting("Delay", "Ticks to wait between uses (vanilla is 4)", 0, 0, 4, 1);

    public FastPlace() {
        super("Fast Place", "Reduce the delay between placing blocks and using items", 'P');
        addSetting(delay);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null
                || !client.options.keyUse.isDown()) {
            return;
        }
        ((MinecraftAccessor) client).octoclient$setRightClickDelay(delay.getInt());
    }
}
