package dev.octoclient.module.utility;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.KeybindSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;

/** Drops the complete stack in the selected hotbar slot when its key is pressed. */
public class DropStack extends Module implements Module.Tickable {
    private final KeybindSetting dropKey =
            new KeybindSetting("Drop Key", "Key that drops the selected stack", GLFW.GLFW_KEY_H);
    private final BooleanSetting dropInInventory =
            new BooleanSetting("Drop in Inventory", "Allow the drop key while the player inventory is open", false);
    private boolean wasDown;

    public DropStack() {
        super("Drop Stack", "Drop the entire selected item stack with one key", 'D');
        addSetting(dropKey);
        addSetting(dropInInventory);
    }

    @Override
    public void onTick(Minecraft client) {
        boolean inventoryOpen = client.gui.screen() instanceof InventoryScreen;
        if (client.player == null || dropKey.getKeyCode() == 0
                || (client.gui.screen() != null && (!dropInInventory.isEnabled() || !inventoryOpen))) {
            wasDown = false;
            return;
        }
        long window = client.getWindow().handle();
        boolean isDown = org.lwjgl.glfw.GLFW.glfwGetKey(window, dropKey.getKeyCode()) == GLFW.GLFW_PRESS;
        if (isDown && !wasDown && !client.player.getMainHandItem().isEmpty()) {
            client.player.drop(client.player.getMainHandItem(), true);
        }
        wasDown = isDown;
    }
}
