package dev.octoclient.util;

import net.minecraft.client.Minecraft;

import java.util.ArrayDeque;

/** Tracks left/right mouse clicks per second (sampled every client frame). */
public final class ClickCounter {
    private static final ClickCounter INSTANCE = new ClickCounter();

    private final ArrayDeque<Long> leftClicks = new ArrayDeque<>();
    private final ArrayDeque<Long> rightClicks = new ArrayDeque<>();

    private boolean leftWasDown;
    private boolean rightWasDown;

    private ClickCounter() {
    }

    public static ClickCounter get() {
        return INSTANCE;
    }

    /** Sampled once per rendered frame while in-game. */
    public void sample() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.screen() != null) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean left = mc.mouseHandler.isLeftPressed();
        boolean right = mc.mouseHandler.isRightPressed();
        if (left && !leftWasDown) {
            leftClicks.addLast(now);
        }
        if (right && !rightWasDown) {
            rightClicks.addLast(now);
        }
        leftWasDown = left;
        rightWasDown = right;
        prune(now);
    }

    private void prune(long now) {
        while (!leftClicks.isEmpty() && now - leftClicks.peekFirst() > 1000L) {
            leftClicks.removeFirst();
        }
        while (!rightClicks.isEmpty() && now - rightClicks.peekFirst() > 1000L) {
            rightClicks.removeFirst();
        }
    }

    public int getLeftCps() {
        prune(System.currentTimeMillis());
        return leftClicks.size();
    }

    public int getRightCps() {
        prune(System.currentTimeMillis());
        return rightClicks.size();
    }
}
