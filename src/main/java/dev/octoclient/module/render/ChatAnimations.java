package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;

/** Adds an eased fade-in to newly displayed chat lines. */
public class ChatAnimations extends Module {
    private static ChatAnimations instance;
    private final NumberSetting duration =
            new NumberSetting("Duration", "Chat line entrance time in ticks", 6.0, 1.0, 20.0, 1.0);

    public ChatAnimations() {
        super("Chat Animations", "Smoothly fade newly received chat lines into view", 'C');
        addSetting(duration);
        instance = this;
    }

    public static float entranceAlpha(int addedAt, int currentTick, float originalAlpha) {
        if (instance == null || !instance.isEnabled()) {
            return originalAlpha;
        }
        float elapsed = Math.max(0, currentTick - addedAt);
        float t = Math.min(1f, elapsed / (float) instance.duration.getInt());
        float eased = t * t * (3f - 2f * t);
        return originalAlpha * eased;
    }
}
