package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/** Adjusts the local player's hand-swing animation without changing attack cooldowns. */
public class SwingSpeed extends Module {
    private static SwingSpeed instance;

    private final NumberSetting speed =
            new NumberSetting("Speed", "Visual hand swing speed multiplier", 1.5, 0.25, 4, 0.05);

    public SwingSpeed() {
        super("Swing Speed", "Change the visual speed of your hand swing", 'W');
        addSetting(speed);
        instance = this;
    }

    public static float speedFor(LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        return instance != null && instance.isEnabled() && entity == client.player
                ? instance.speed.getFloat() : 1f;
    }
}
