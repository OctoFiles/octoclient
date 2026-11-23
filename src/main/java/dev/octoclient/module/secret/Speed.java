package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Port of Meteor's Speed (Vanilla and Strafe modes). */
public class Speed extends Module implements Module.Tickable {
    private static final double DEFAULT_SPEED = 0.2873;

    private final ModeSetting mode = new ModeSetting("mode", "The speed mode.",
            "Vanilla", List.of("Vanilla", "Strafe"));
    private final NumberSetting vanillaSpeed = new NumberSetting("vanilla-speed",
            "The speed of the Vanilla mode.", 5.6, 0, 50, 0.1);
    private final NumberSetting strafeSpeed = new NumberSetting("strafe-speed",
            "The speed of the Strafe mode.", 1.6, 0, 10, 0.1);
    private final BooleanSetting speedLimit = new BooleanSetting("speed-limit",
            "Limits your speed on servers with powerful setbacks.", false);
    private final NumberSetting timer = new NumberSetting("timer",
            "Timer speed.", 1, 0.01, 10, 0.01);
    private final BooleanSetting inLiquids = new BooleanSetting("in-liquids",
            "Allows you to speed in liquids.", false);
    private final BooleanSetting whenSneaking = new BooleanSetting("when-sneaking",
            "Allows you to speed while sneaking.", false);
    private final BooleanSetting onlyOnGround = new BooleanSetting("only-on-ground",
            "Only speed while on the ground.", false);

    private int stage;
    private double strafeValue = DEFAULT_SPEED;
    private double distance;
    private long limitTimer;

    public Speed() {
        super("Speed", "Makes you go faster.", 'S');
        addSetting(mode);
        addSetting(vanillaSpeed);
        addSetting(strafeSpeed);
        addSetting(speedLimit);
        addSetting(timer);
        addSetting(inLiquids);
        addSetting(whenSneaking);
        addSetting(onlyOnGround);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (stopSpeed(client)) {
            return;
        }
        distance = Math.sqrt((client.player.getX() - client.player.xo) * (client.player.getX() - client.player.xo)
                + (client.player.getZ() - client.player.zo) * (client.player.getZ() - client.player.zo));
        if (mode.getValue().equals("Vanilla")) {
            tickVanilla(client);
        } else {
            tickStrafe(client);
        }
    }

    private boolean stopSpeed(Minecraft client) {
        if (client.player.isFallFlying() || client.player.onClimbable()
                || client.player.getVehicle() != null) {
            return true;
        }
        if (!whenSneaking.isEnabled() && client.player.isShiftKeyDown()) {
            return true;
        }
        if (onlyOnGround.isEnabled() && !client.player.onGround()) {
            return true;
        }
        return !inLiquids.isEnabled() && (client.player.isInWater() || client.player.isInLava());
    }

    private void tickVanilla(Minecraft client) {
        Vec2 input = client.player.input.getMoveVector();
        if (input.x == 0f && input.y == 0f) {
            return;
        }
        float yaw = client.player.getYRot();
        double yawRadians = Math.toRadians(yaw);
        double forward = input.y;
        double side = input.x;
        double dx = side * Math.cos(yawRadians) - forward * Math.sin(yawRadians);
        double dz = side * Math.sin(yawRadians) + forward * Math.cos(yawRadians);
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length <= 0) {
            return;
        }
        double magnitude = vanillaSpeed.getValue() / 20.0;
        if (client.player.hasEffect(MobEffects.SPEED)) {
            var effect = client.player.getEffect(MobEffects.SPEED);
            if (effect != null) {
                magnitude += magnitude * (effect.getAmplifier() + 1) * 0.205;
            }
        }
        dx = dx / length * magnitude;
        dz = dz / length * magnitude;
        Vec3 delta = client.player.getDeltaMovement();
        client.player.setDeltaMovement(dx, delta.y, dz);
    }

    private void tickStrafe(Minecraft client) {
        switch (stage) {
            case 0 -> {
                if (client.player.input.getMoveVector().x != 0f
                        || client.player.input.getMoveVector().y != 0f) {
                    stage = 1;
                    strafeValue = 1.18f * DEFAULT_SPEED - 0.01;
                }
            }
            case 1 -> {
                Vec2 input = client.player.input.getMoveVector();
                if ((input.x == 0f && input.y == 0f) || !client.player.onGround()) {
                    break;
                }
                Vec3 delta = client.player.getDeltaMovement();
                client.player.setDeltaMovement(delta.x, 0.40123128, delta.z);
                strafeValue = Math.max(strafeValue, DEFAULT_SPEED);
                stage = 2;
            }
            case 2 -> {
                strafeValue = distance - 0.76 * (distance - DEFAULT_SPEED);
                stage = 3;
            }
            case 3 -> {
                if (!client.level.noCollision(client.player.getBoundingBox()
                        .move(0.0, client.player.getDeltaMovement().y, 0.0))
                        || (client.player.verticalCollision && stage > 0)) {
                    stage = 0;
                }
                strafeValue = distance - (distance / 159.0);
            }
            default -> {
            }
        }
        strafeValue = Math.max(strafeValue, DEFAULT_SPEED);
        if (speedLimit.isEnabled()) {
            if (System.currentTimeMillis() - limitTimer > 2500L) {
                limitTimer = System.currentTimeMillis();
            }
            strafeValue = Math.min(strafeValue, System.currentTimeMillis() - limitTimer > 1250L ? 0.44 : 0.43);
        }
        Vec2 input = client.player.input.getMoveVector();
        float forward = Math.signum(input.y);
        float side = Math.signum(input.x);
        if (forward == 0f && side == 0f) {
            return;
        }
        float yaw = client.player.getYRot();
        float strafe = 90 * side;
        if (forward != 0) {
            strafe *= forward * 0.5f;
        }
        yaw -= strafe;
        if (forward < 0) {
            yaw -= 180;
        }
        double yawRadians = Math.toRadians(yaw);
        Vec3 delta = client.player.getDeltaMovement();
        client.player.setDeltaMovement(-Math.sin(yawRadians) * strafeValue, delta.y,
                Math.cos(yawRadians) * strafeValue);
    }
}
