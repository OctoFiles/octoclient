package dev.octoclient.module.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.octoclient.config.ConfigManager;
import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ActionSetting;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Sends an editable list of saved chat messages or commands on key presses. */
public class Autotext extends Module implements Module.Tickable {
    private final List<Entry> entries = new ArrayList<>();
    private int nextEntryId = 1;
    private final ActionSetting addButton = new ActionSetting("Add", "Add a saved message or command",
            "+ Add", this::addEntry);

    public Autotext() {
        super("Autotext", "Send saved chat messages or commands with hotkeys", 'T');
        addSetting(addButton);
        addEntry("gg", GLFW.GLFW_KEY_F6, true, false);
    }

    private void addEntry() {
        addEntry("", 0, true, true);
    }

    private void addEntry(String text, int key, boolean enabled, boolean save) {
        int id = nextEntryId++;
        Entry entry = new Entry(id, text, key, enabled);
        entries.add(entry);
        addSetting(entry.message);
        addSetting(entry.key);
        addSetting(entry.enabled);
        addSetting(entry.deleteButton);
        if (save && addButton != null) {
            ConfigManager.save();
        }
    }

    private void deleteEntry(Entry entry) {
        entries.remove(entry);
        removeSetting(entry.message);
        removeSetting(entry.key);
        removeSetting(entry.enabled);
        removeSetting(entry.deleteButton);
        ConfigManager.save();
    }

    public JsonArray saveEntries() {
        JsonArray array = new JsonArray();
        for (Entry entry : entries) {
            JsonObject object = new JsonObject();
            object.addProperty("message", entry.message.getValue());
            object.addProperty("key", entry.key.getKeyCode());
            object.addProperty("enabled", entry.enabled.isEnabled());
            array.add(object);
        }
        return array;
    }

    public void loadEntries(JsonArray array) {
        for (Entry entry : List.copyOf(entries)) {
            removeSetting(entry.message);
            removeSetting(entry.key);
            removeSetting(entry.enabled);
            removeSetting(entry.deleteButton);
        }
        entries.clear();
        nextEntryId = 1;
        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            String message = object.has("message") ? object.get("message").getAsString() : "";
            int key = object.has("key") ? object.get("key").getAsInt() : 0;
            boolean enabled = !object.has("enabled") || object.get("enabled").getAsBoolean();
            addEntry(message, key, enabled, false);
        }
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.gui.screen() != null || client.getConnection() == null) {
            entries.forEach(entry -> entry.wasDown = false);
            return;
        }
        ClientPacketListener connection = client.getConnection();
        long window = client.getWindow().handle();
        for (Entry entry : entries) {
            int keyCode = entry.key.getKeyCode();
            boolean down = entry.enabled.isEnabled() && keyCode != 0
                    && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            if (down && !entry.wasDown) {
                String text = entry.message.getValue().trim();
                if (!text.isEmpty()) {
                    if (text.startsWith("/")) {
                        connection.sendCommand(text.substring(1));
                    } else {
                        connection.sendChat(text);
                    }
                }
            }
            entry.wasDown = down;
        }
    }

    private final class Entry {
        private final TextSetting message;
        private final KeybindSetting key;
        private final BooleanSetting enabled;
        private final ActionSetting deleteButton;
        private boolean wasDown;

        private Entry(int id, String initialMessage, int initialKey, boolean initialEnabled) {
            message = new TextSetting("Message " + id, "Chat text or command", initialMessage, 256);
            key = new KeybindSetting("Key " + id, "Send this saved message", initialKey);
            enabled = new BooleanSetting("Message " + id + " On/Off", "Enable this message binding", initialEnabled);
            deleteButton = new ActionSetting("Delete Message " + id, "Remove this message entry",
                    "Delete", () -> deleteEntry(this));
        }
    }
}
