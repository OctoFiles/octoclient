package dev.octoclient.module.render;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ColorSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

public class HealthIndicator extends Module {
    private static HealthIndicator instance;

    private final NumberSetting range = new NumberSetting("Range", "Maximum distance for overhead health", 32, 4, 128, 1);
    private final BooleanSetting players = new BooleanSetting("Players", "Show health for other players", true);
    private final BooleanSetting mobs = new BooleanSetting("Mobs", "Show health for non-player living entities", true);
    private final ModeSetting display = new ModeSetting("Display", "Health label style", "Numeric", List.of("Numeric", "Bar"));
    private final ColorSetting color = new ColorSetting("Color", "Color of the health indicator", 0xFFFF5F6D);

    public HealthIndicator() {
        super("Health Indicator", "Display health above nearby living entities", 'H');
        addSetting(range);
        addSetting(players);
        addSetting(mobs);
        addSetting(display);
        addSetting(color);
        instance = this;
    }

    public static boolean shouldShow(LivingEntity entity, double distanceToCameraSq) {
        if (instance == null || !instance.isEnabled()
                || distanceToCameraSq > instance.range.getSnapped() * instance.range.getSnapped()) {
            return false;
        }
        if (entity == Minecraft.getInstance().player) {
            return false;
        }
        return entity instanceof Player ? instance.players.isEnabled() : instance.mobs.isEnabled();
    }

    public static Component appendHealth(LivingEntity entity, Component name) {
        float maxHealth = Math.max(1, entity.getMaxHealth());
        float health = Math.max(0, Math.min(entity.getHealth(), maxHealth));
        String label;
        if (instance.display.getValue().equals("Bar")) {
            int filled = Math.round(health / maxHealth * 10);
            label = "HP [" + "#".repeat(filled) + "-".repeat(10 - filled) + "]";
        } else {
            label = String.format(Locale.ROOT, "HP %.1f/%.1f", health, maxHealth);
        }
        Component healthText = Component.literal(label).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(instance.color.getRgb())));
        return name == null ? healthText
                : Component.empty().append(name).append(Component.literal(" ")).append(healthText);
    }
}
