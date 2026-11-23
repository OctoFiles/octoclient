package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.TextSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Files;

/** Captures the current full-resolution frame into an OctoClient screenshot folder. */
public class ScreenshotModule extends Module implements Module.Tickable {
    private final KeybindSetting screenshotKey =
            new KeybindSetting("Screenshot Key", "Capture a screenshot", GLFW.GLFW_KEY_F12);
    private final TextSetting folderName =
            new TextSetting("Folder", "Subfolder under screenshots", "OctoClient", 32);
    private boolean wasDown;

    public ScreenshotModule() {
        super("Screenshot", "Capture and organize high-resolution screenshots", 'S');
        addSetting(screenshotKey);
        addSetting(folderName);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.gui.screen() != null || client.player == null || screenshotKey.getKeyCode() == 0) {
            wasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetKey(client.getWindow().handle(), screenshotKey.getKeyCode()) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            Path folder = screenshotDirectory();
            try {
                Files.createDirectories(folder);
            } catch (IOException error) {
                System.err.println("[OctoClient] Could not create screenshot folder: " + error.getMessage());
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal("Screenshot folder could not be created"));
                }
                wasDown = down;
                return;
            }
            Screenshot.grab(folder.toFile(), "OctoClient", client.gameRenderer.mainRenderTarget(), 1,
                    message -> {
                        if (client.player != null) {
                            client.player.sendSystemMessage(message);
                        }
                    });
        }
        wasDown = down;
    }

    private Path screenshotDirectory() {
        String safeName = folderName.getValue().replaceAll("[^A-Za-z0-9_-]", "");
        if (safeName.isBlank()) {
            safeName = "OctoClient";
        }
        return FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(safeName);
    }
}
