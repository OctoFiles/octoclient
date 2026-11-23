package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

import java.util.Map;

/**
 * Rejoins the server after a disconnect, ported from NoRisk's auto reconnect.
 * Only multiplayer: singleplayer reconnects are left alone.
 */
public class AutoReconnect extends Module implements Module.Tickable {
    private final NumberSetting delay =
            new NumberSetting("Delay", "Seconds to wait before reconnecting", 3.0, 1.0, 15.0, 1.0);

    private int ticks;

    public AutoReconnect() {
        super("Auto Reconnect", "Rejoin the server after a disconnect", 'A');
        addSetting(delay);
    }

    @Override
    protected void onDisable() {
        ticks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (!(client.gui.screen() instanceof DisconnectedScreen disconnect)) {
            ticks = 0;
            return;
        }
        ServerData server = client.getCurrentServer();
        if (server == null || server.ip == null || server.ip.isEmpty()) {
            ticks = 0;
            return;
        }
        if (++ticks < delay.getInt() * 20) {
            return;
        }
        ticks = 0;
        try {
            ConnectScreen.startConnecting(disconnect, client, ServerAddress.parseString(server.ip), server, false,
                    new TransferState(Map.of(), Map.of(), false));
        } catch (Exception e) {
            System.err.println("[OctoClient] Auto Reconnect failed: " + e);
        }
    }
}
