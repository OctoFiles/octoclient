package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.setting.BooleanSetting;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/** Sends "gg" automatically when a game ends. */
public class AutoGG extends Module {
    private final BooleanSetting autoSend = new BooleanSetting("Auto Send", "Send gg without asking", true);

    private final String[] triggers = {
            "victory", "defeat", "game over", "won the game", "you won", "you lost",
            "placement", "1st place", "2nd place", "3rd place"
    };

    public AutoGG() {
        super("Auto GG", "Says gg when a game ends", 'E');
        addSetting(autoSend);
    }

    public void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> check(message));
        ClientReceiveMessageEvents.CHAT.register(
                (message, playerChatMessage, profile, bound, instant) -> check(message));
    }

    private void check(Component message) {
        Module module = ModuleManager.get().byName("Auto GG").orElse(null);
        if (module == null || !module.isEnabled() || !autoSend.isEnabled()) {
            return;
        }
        String text = message.getString().toLowerCase(Locale.ROOT);
        for (String trigger : triggers) {
            if (text.contains(trigger)) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.getConnection() != null) {
                    mc.getConnection().sendChat("gg");
                }
                return;
            }
        }
    }
}
