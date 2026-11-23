package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.module.setting.TextSetting;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.repository.PackRepository;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Stores named virtual folders of pack IDs and applies them as resource-pack presets. */
public class ResourcepackOrganizer extends Module implements Module.Tickable {
    private static ResourcepackOrganizer instance;
    private final TextSetting[] folderNames = {
            new TextSetting("Folder 1 Name", "Name of resource-pack preset 1", "Building", 32),
            new TextSetting("Folder 2 Name", "Name of resource-pack preset 2", "PvP", 32),
            new TextSetting("Folder 3 Name", "Name of resource-pack preset 3", "Default", 32)
    };
    private final TextSetting[] packIds = {
            new TextSetting("Folder 1 Pack IDs", "Comma-separated resource-pack IDs for preset 1", "", 512),
            new TextSetting("Folder 2 Pack IDs", "Comma-separated resource-pack IDs for preset 2", "", 512),
            new TextSetting("Folder 3 Pack IDs", "Comma-separated resource-pack IDs for preset 3", "", 512)
    };
    private final KeybindSetting[] keys = {
            new KeybindSetting("Folder 1 Key", "Apply preset 1 (unbound by default)", 0),
            new KeybindSetting("Folder 2 Key", "Apply preset 2 (unbound by default)", 0),
            new KeybindSetting("Folder 3 Key", "Apply preset 3 (unbound by default)", 0)
    };
    private final boolean[] wasDown = new boolean[3];
    private final BooleanSetting showIncompatible =
            new BooleanSetting("Show Incompatible Packs", "Allow applying packs with an incompatible version", false);
    private final BooleanSetting transparentBackground =
            new BooleanSetting("Transparent Background", "Use a transparent resource-pack screen backdrop", false);
    private final BooleanSetting packPreviews =
            new BooleanSetting("Pack Previews", "Show resource-pack icons and descriptions", true);

    public ResourcepackOrganizer() {
        super("Resourcepack Organizer", "Group pack IDs into named presets and switch between them", 'R');
        instance = this;
        for (int i = 0; i < folderNames.length; i++) {
            addSetting(folderNames[i]);
            addSetting(packIds[i]);
            addSetting(keys[i]);
        }
        addSetting(showIncompatible);
        addSetting(transparentBackground);
        addSetting(packPreviews);
    }

    public static boolean transparentBackgroundEnabled() {
        return instance != null && instance.isEnabled() && instance.transparentBackground.isEnabled();
    }

    public static boolean packPreviewsEnabled() {
        return instance == null || !instance.isEnabled() || instance.packPreviews.isEnabled();
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.level != null || client.gui.screen() == null) {
            java.util.Arrays.fill(wasDown, false);
            return;
        }
        long window = client.getWindow().handle();
        for (int i = 0; i < keys.length; i++) {
            int keyCode = keys[i].getKeyCode();
            boolean down = keyCode != 0
                    && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS;
            if (down && !wasDown[i]) {
                applyPreset(client, i);
            }
            wasDown[i] = down;
        }
    }

    private void applyPreset(Minecraft client, int index) {
        PackRepository repository = client.getResourcePackRepository();
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        selected.add("vanilla");
        List<String> unavailable = new ArrayList<>();
        for (String id : packIds[index].getValue().split(",")) {
            String packId = id.trim();
            if (packId.isEmpty() || packId.equals("vanilla")) {
                continue;
            }
            var pack = repository.getPack(packId);
            if (pack != null && (showIncompatible.isEnabled() || pack.getCompatibility().isCompatible())) {
                selected.add(packId);
            } else {
                unavailable.add(packId);
            }
        }
        if (!unavailable.isEmpty()) {
            System.err.println("[OctoClient] Unknown pack IDs in preset '" + folderNames[index].getValue()
                    + "': " + String.join(", ", unavailable));
        }
        repository.setSelected(selected);
        client.reloadResourcePacks().whenComplete((ignored, error) -> {
            if (error != null) {
                System.err.println("[OctoClient] Could not apply resource-pack preset '"
                        + folderNames[index].getValue() + "': " + error.getMessage());
            }
        });
    }
}
