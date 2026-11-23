package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * TargetStrafe: circles around the current target at a fixed radius, optionally
 * jumping. Handy together with KillAura or MultiAura.
 */
public class TargetStrafe extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to circle.", "Players", List.of("Players", "Mobs", "All"));
    private final ModeSetting direction = new ModeSetting("direction",
            "Which way to circle the target.", "Left", List.of("Left", "Right", "Switch"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to a target.", 6, 1, 12, 0.5);
    private final NumberSetting radius = new NumberSetting("radius",
            "Distance to keep from the target.", 2.5, 0.5, 6, 0.1);
    private final NumberSetting speed = new NumberSetting("speed",
            "Horizontal speed in blocks per tick.", 0.25, 0.05, 0.6, 0.01);
    private final BooleanSetting jump = new BooleanSetting("jump",
            "Jumps while strafing.", false);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only strafes while you hold the attack key.", true);
    private final NumberSetting switchDelay = new NumberSetting("switch-delay",
            "Ticks between direction changes in Switch mode.", 30, 5, 100, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never circles friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private int side = 1;
    private int switchTimer;

    public TargetStrafe() {
        super("TargetStrafe", "Circles around your target while attacking.", 'T');
        addSetting(targets);
        addSetting(direction);
        addSetting(range);
        addSetting(radius);
        addSetting(speed);
        addSetting(jump);
        addSetting(onlyOnClick);
        addSetting(switchDelay);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        switchTimer = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gui.screen() != null
                || !client.player.isAlive()) {
            return;
        }
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            return;
        }
        LivingEntity target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        if (target == null) {
            return;
        }
        if (direction.getValue().equals("Right")) {
            side = -1;
        } else if (direction.getValue().equals("Switch")) {
            if (--switchTimer <= 0) {
                switchTimer = Math.max(1, switchDelay.getInt());
                side = -side;
            }
        } else {
            side = 1;
        }
        double dx = client.player.getX() - target.getX();
        double dz = client.player.getZ() - target.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance < 1.0e-3) {
            return;
        }
        // Tangent of the target circle, plus a small radial correction that
        // keeps the configured distance instead of drifting away.
        double tangentX = -dz / distance * side;
        double tangentZ = dx / distance * side;
        double radial = (distance - radius.getValue()) * 0.2;
        double vx = tangentX * speed.getValue() + dx / distance * radial;
        double vz = tangentZ * speed.getValue() + dz / distance * radial;
        double vy = client.player.getDeltaMovement().y;
        if (jump.isEnabled() && client.player.onGround()) {
            vy = 0.42;
        }
        client.player.setDeltaMovement(vx, vy, vz);
    }
}
