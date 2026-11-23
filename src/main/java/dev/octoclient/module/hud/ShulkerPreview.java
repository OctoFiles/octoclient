package dev.octoclient.module.hud;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.KeybindSetting;
import dev.octoclient.ui.ShulkerPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/** Shulker contents preview, opened with a keybind like kgriff0n/shulker-preview. */
public class ShulkerPreview extends Module implements Module.Tickable {
    private static ShulkerPreview instance;

    private final BooleanSetting showCounts =
            new BooleanSetting("Show Item Counts", "Draw stack counts in the preview", true);
    private final BooleanSetting alwaysPreview =
            new BooleanSetting("Always Preview", "Show shulker contents while hovering without a key", true);
    private final KeybindSetting previewKey =
            new KeybindSetting("Preview Key", "Open the shulker preview", GLFW.GLFW_KEY_V);
    private boolean keyWasDown;

    public ShulkerPreview() {
        super("Shulker Preview", "Preview shulker contents while hovering in an inventory", 'S');
        addSetting(showCounts);
        addSetting(alwaysPreview);
        addSetting(previewKey);
        instance = this;
    }

    public static boolean active() {
        return instance != null && instance.isEnabled();
    }

    public static boolean showCounts() {
        return instance == null || instance.showCounts.isEnabled();
    }

    public static boolean shouldPreview() {
        if (instance == null || !instance.isEnabled()) {
            return false;
        }
        if (instance.alwaysPreview.isEnabled()) {
            return true;
        }
        int key = instance.previewKey.getKeyCode();
        return key != 0 && GLFW.glfwGetKey(Minecraft.getInstance().getWindow().handle(), key) == GLFW.GLFW_PRESS;
    }

    /** Key code used to open the grid preview, or 0 when disabled. */
    public static int openKeyCode() {
        if (instance == null || !instance.isEnabled()) {
            return 0;
        }
        return instance.previewKey.getKeyCode();
    }

    /** Opens the grid preview for a held shulker (called every tick). */
    @Override
    public void onTick(Minecraft client) {
        int code = openKeyCode();
        boolean down = code != 0 && client.getWindow() != null
                && GLFW.glfwGetKey(client.getWindow().handle(), code) == GLFW.GLFW_PRESS;
        if (down && !keyWasDown && client.gui.screen() == null && client.player != null) {
            ItemStack main = client.player.getMainHandItem();
            ItemStack off = client.player.getOffhandItem();
            ItemStack stack = ShulkerPreviewScreen.isShulker(main) ? main
                    : ShulkerPreviewScreen.isShulker(off) ? off : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                client.setScreenAndShow(new ShulkerPreviewScreen(stack, null));
            }
        }
        keyWasDown = down;
    }
}
