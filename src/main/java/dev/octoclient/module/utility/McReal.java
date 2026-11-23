package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.TextSetting;
import dev.octoclient.ui.McRealScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;

public class McReal extends Module implements Module.Tickable {
    private final TextSetting folder =
            new TextSetting("Screenshot Folder", "Subfolder under screenshots to browse", "OctoClient", 32);
    private final KeybindSetting openKey =
            new KeybindSetting("Open Gallery Key", "Open the local screenshot gallery", GLFW.GLFW_KEY_M);
    private boolean wasDown;

    public McReal() {
        super("McReal", "Browse and open your local screenshots", 'M');
        addSetting(folder);
        addSetting(openKey);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.gui.screen() != null || openKey.getKeyCode() == 0) {
            wasDown = false;
            return;
        }
        boolean down = GLFW.glfwGetKey(client.getWindow().handle(), openKey.getKeyCode()) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            client.setScreenAndShow(new McRealScreen(screenshotFolder()));
        }
        wasDown = down;
    }

    private Path screenshotFolder() {
        String safeName = folder.getValue().replaceAll("[^A-Za-z0-9_-]", "");
        if (safeName.isBlank()) {
            safeName = "OctoClient";
        }
        return FabricLoader.getInstance().getGameDir().resolve("screenshots").resolve(safeName);
    }
}
