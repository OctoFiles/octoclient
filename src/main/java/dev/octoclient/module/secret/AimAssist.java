package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;

/**
 * AimAssist: smoothly pulls the view towards a target inside a cone instead of
 * snapping to it like KillAura. It never attacks on its own.
 */
public class AimAssist extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to aim at.", "Players", List.of("Players", "Mobs", "All"));
    private final ModeSetting priority = new ModeSetting("priority",
            "How the aim target is chosen.", "Angle",
            List.of("Angle", "Closest", "LowestHealth"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum aim distance.", 4.5, 0, 8, 0.1);
    private final NumberSetting fov = new NumberSetting("fov",
            "Only aims at targets inside this cone (degrees). 360 disables it.", 120, 10, 360, 5);
    private final NumberSetting speed = new NumberSetting("speed",
            "How much of the remaining angle is turned per tick.", 0.35, 0.05, 1.0, 0.05);
    private final BooleanSetting vertical = new BooleanSetting("vertical",
            "Also turns up and down.", true);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only aims while you hold the attack key.", false);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between aim updates.", 0, 0, 10, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never aims at friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private int timer;

    public AimAssist() {
        super("AimAssist", "Smoothly aims at nearby targets without attacking.", 'A');
        addSetting(targets);
        addSetting(priority);
        addSetting(range);
        addSetting(fov);
        addSetting(speed);
        addSetting(vertical);
        addSetting(onlyOnClick);
        addSetting(delay);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        timer = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null) {
            return;
        }
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        timer = delay.getInt();
        List<LivingEntity> list = AuraUtil.targets(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        double cone = Math.toRadians(fov.getValue() / 2.0);
        list.removeIf(entity -> fov.getValue() < 360 && AuraUtil.angleTo(client.player, entity) > cone);
        if (list.isEmpty()) {
            return;
        }
        LivingEntity target = switch (priority.getValue()) {
            case "Closest" -> list.stream()
                    .min(Comparator.comparingDouble(e -> client.player.distanceToSqr(e))).orElse(null);
            case "LowestHealth" -> list.stream()
                    .min(Comparator.comparingDouble(LivingEntity::getHealth)).orElse(null);
            default -> list.stream()
                    .min(Comparator.comparingDouble(e -> AuraUtil.angleTo(client.player, e))).orElse(null);
        };
        if (target == null) {
            return;
        }
        AuraUtil.smoothLookAt(client.player, target.getBoundingBox().getCenter(),
                speed.getValue(), vertical.isEnabled());
    }
}
