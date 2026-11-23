package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.ModeSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.List;

/** Appends a suffix to everything you send in chat. */
public class ChatSuffix extends Module {
    private final ModeSetting suffix = new ModeSetting("Suffix", "Text appended to chat messages",
            ".octo", List.of(".octo", "[OCTO]", "~octo"));

    public ChatSuffix() {
        super("Chat Suffix", "Adds a tag to your chat messages", 'C');
        addSetting(suffix);
    }

    public void init() {
        ClientSendMessageEvents.MODIFY_CHAT.register(message -> {
            Module module = ModuleManager.get().byName("Chat Suffix").orElse(null);
            if (module == null || !module.isEnabled()) {
                return message;
            }
            if (message.startsWith("/")) {
                return message;
            }
            return message + " " + suffix.getValue();
        });
    }
}
