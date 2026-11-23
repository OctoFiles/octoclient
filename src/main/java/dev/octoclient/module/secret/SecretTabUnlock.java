package dev.octoclient.module.secret;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Unlocks the extra-module dashboard tab with the requested arrow sequence. */
public final class SecretTabUnlock {
    private static final int[] SEQUENCE = {
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_LEFT
    };
    private static final int[] ARROW_KEYS = {
            GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT
    };

    private static boolean unlocked;
    private static final boolean[] wasDown = new boolean[ARROW_KEYS.length];
    private static int sequenceIndex;
    private static long sequenceStartedAt;

    /**
     * Dev-only: {@code -Doctoclient.secretTab=true} shows the hidden tab
     * without the arrow sequence, so dashboard captures can reach it.
     */
    private static final boolean DEV_UNLOCKED = Boolean.getBoolean("octoclient.secretTab");

    private SecretTabUnlock() {
    }

    public static boolean isUnlocked() {
        return unlocked || DEV_UNLOCKED;
    }

    public static void tick(Minecraft client) {
        if (client.getWindow() == null) {
            return;
        }
        long window = client.getWindow().handle();
        boolean canEnter = client.player != null && client.gui.screen() == null;
        long now = System.currentTimeMillis();
        if (sequenceIndex > 0 && now - sequenceStartedAt > 3_000L) {
            resetSequence();
        }
        for (int i = 0; i < ARROW_KEYS.length; i++) {
            boolean down = GLFW.glfwGetKey(window, ARROW_KEYS[i]) == GLFW.GLFW_PRESS;
            boolean pressed = down && !wasDown[i];
            wasDown[i] = down;
            if (pressed && canEnter) {
                acceptKey(ARROW_KEYS[i], now, client);
            }
        }
        if (!canEnter) {
            resetSequence();
        }
    }

    private static void acceptKey(int key, long now, Minecraft client) {
        if (key == SEQUENCE[sequenceIndex]) {
            if (sequenceIndex == 0) {
                sequenceStartedAt = now;
            }
            sequenceIndex++;
            if (sequenceIndex == SEQUENCE.length) {
                unlocked = !unlocked;
                resetSequence();
            }
            return;
        }
        sequenceIndex = key == SEQUENCE[0] ? 1 : 0;
        sequenceStartedAt = sequenceIndex == 1 ? now : 0L;
    }

    private static void resetSequence() {
        sequenceIndex = 0;
        sequenceStartedAt = 0L;
    }
}
