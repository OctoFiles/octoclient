package dev.octoclient.module.setting;

import com.mojang.blaze3d.platform.InputConstants;

/** Rebindable keyboard key, stored as a GLFW key code. */
public class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String name, String description, int defaultKey) {
        super(name, description, defaultKey);
    }

    public int getKeyCode() {
        return getValue();
    }

    public String getDisplayName() {
        return getKeyCode() == 0 ? "Unbound"
                : InputConstants.Type.KEYSYM.getOrCreate(getKeyCode()).getDisplayName().getString();
    }
}
