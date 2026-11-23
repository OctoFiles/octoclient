package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public class ServerQuickConnect extends Module implements Module.Tickable {
    private final TextSetting[] names = {
            new TextSetting("Server 1 Name", "Display name for server slot 1", "Server 1", 32),
            new TextSetting("Server 2 Name", "Display name for server slot 2", "Server 2", 32),
            new TextSetting("Server 3 Name", "Display name for server slot 3", "Server 3", 32)
    };
    private final TextSetting[] addresses = {
            new TextSetting("Server 1 Address", "Hostname or address for server slot 1", "", 255),
            new TextSetting("Server 2 Address", "Hostname or address for server slot 2", "", 255),
            new TextSetting("Server 3 Address", "Hostname or address for server slot 3", "", 255)
    };
    private final KeybindSetting[] keys = {
            new KeybindSetting("Server 1 Key", "Connect to server slot 1", GLFW.GLFW_KEY_F6),
            new KeybindSetting("Server 2 Key", "Connect to server slot 2", GLFW.GLFW_KEY_F7),
            new KeybindSetting("Server 3 Key", "Connect to server slot 3", GLFW.GLFW_KEY_F8)
    };
    private final boolean[] wasDown = new boolean[3];

    public ServerQuickConnect() {
        super("Server Quick Connect", "Configure three server slots and connect with hotkeys", 'Q');
        for (int i = 0; i < names.length; i++) {
            addSetting(names[i]);
            addSetting(addresses[i]);
            addSetting(keys[i]);
        }
    }

    @Override
    public void onTick(Minecraft client) {
        long window = client.getWindow().handle();
        for (int i = 0; i < keys.length; i++) {
            boolean down = keys[i].getKeyCode() != 0
                    && GLFW.glfwGetKey(window, keys[i].getKeyCode()) == GLFW.GLFW_PRESS;
            if (down && !wasDown[i]) {
                connect(client, i);
            }
            wasDown[i] = down;
        }
    }

    private void connect(Minecraft client, int index) {
        String address = addresses[index].getValue().trim();
        if (address.isEmpty()) {
            System.err.println("[OctoClient] Server slot " + (index + 1) + " has no address configured.");
            return;
        }
        if (client.level != null || client.gui.screen() == null) {
            System.err.println("[OctoClient] Quick Connect is available from menus, not while in a world.");
            return;
        }

        String name = names[index].getValue().trim();
        if (name.isEmpty()) {
            name = address;
        }
        ServerData server = new ServerData(name, address, ServerData.Type.OTHER);
        try {
            ConnectScreen.startConnecting(client.gui.screen(), client, ServerAddress.parseString(address), server,
                    false, new TransferState(Map.of(), Map.of(), false));
        } catch (IllegalArgumentException error) {
            System.err.println("[OctoClient] Invalid server address in slot " + (index + 1) + ": " + error.getMessage());
        }
    }
}
