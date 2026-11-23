package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Port of Meteor's LongJump. Vanilla boosts the normal jump, Burst uses the
 * staged sprint-jump acceleration and Glide the fixed glide jump arc.
 */
public class LongJump extends Module implements Module.Tickable {
    private static final float[] GLIDE_MOTION = {
            0.4206065F, 0.4179245F, 0.41525924F, 0.41261F, 0.409978F, 0.407361F, 0.404761F,
            0.402178F, 0.399611F, 0.39706F, 0.394525F, 0.392F, 0.3894F, 0.38644F, 0.383655F,
            0.381105F, 0.37867F, 0.37625F, 0.37384F, 0.37145F, 0.369F, 0.3666F, 0.3642F,
            0.3618F, 0.35945F, 0.357F, 0.354F, 0.351F, 0.348F, 0.345F, 0.342F, 0.339F,
            0.336F, 0.333F, 0.33F, 0.327F, 0.324F, 0.321F, 0.318F, 0.315F, 0.312F, 0.309F,
            0.307F, 0.305F, 0.303F, 0.3F, 0.297F, 0.295F, 0.293F, 0.291F, 0.289F, 0.287F,
            0.285F, 0.283F, 0.281F, 0.279F, 0.277F, 0.275F, 0.273F, 0.271F, 0.269F, 0.267F,
            0.265F, 0.263F, 0.261F, 0.259F, 0.257F, 0.255F, 0.253F, 0.251F, 0.249F, 0.247F,
            0.245F, 0.243F, 0.241F, 0.239F, 0.237F};
    private static final float[] GLIDE_RAMP = {
            0.3425F, 0.5445F, 0.65425F, 0.685F, 0.675F, 0.2F, 0.895F, 0.719F, 0.76F};

    private final ModeSetting jumpMode = new ModeSetting("mode",
            "The method of jumping.", "Vanilla", List.of("Vanilla", "Burst", "Glide"));
    private final NumberSetting vanillaBoostFactor = new NumberSetting("vanilla-boost-factor",
            "The amount by which to boost the jump.", 1.261, 0, 5, 0.001);
    private final NumberSetting burstInitialSpeed = new NumberSetting("burst-initial-speed",
            "The initial speed of the runup.", 6, 0, 20, 0.1);
    private final NumberSetting burstBoostFactor = new NumberSetting("burst-boost-factor",
            "The amount by which to boost the jump.", 2.149, 0, 20, 0.001);
    private final BooleanSetting onlyOnGround = new BooleanSetting("only-on-ground",
            "Only performs the jump if you are on the ground.", true);
    private final BooleanSetting onJump = new BooleanSetting("on-jump",
            "Whether the player needs to jump first or not.", false);
    private final NumberSetting glideMultiplier = new NumberSetting("glide-multiplier",
            "The amount by to multiply the glide velocity.", 1, 0, 5, 0.01);
    private final NumberSetting timer = new NumberSetting("timer",
            "Timer override.", 1, 0.01, 10, 0.01);
    private final BooleanSetting autoDisable = new BooleanSetting("auto-disable",
            "Automatically disables the module after jumping.", true);
    private final BooleanSetting disableOnRubberband = new BooleanSetting("disable-on-rubberband",
            "Disables the module when you get lagged back.", true);

    private int stage;
    private int airTicks;
    private int groundTicks;
    private double moveSpeed;
    private double lastX;
    private double lastZ;
    private boolean jumping;
    private boolean jumped;

    public LongJump() {
        super("LongJump", "Allows you to jump further.", 'L');
        addSetting(jumpMode);
        addSetting(vanillaBoostFactor);
        addSetting(burstInitialSpeed);
        addSetting(burstBoostFactor);
        addSetting(onlyOnGround);
        addSetting(onJump);
        addSetting(glideMultiplier);
        addSetting(timer);
        addSetting(autoDisable);
        addSetting(disableOnRubberband);
    }

    @Override
    protected void onEnable() {
        stage = 0;
        airTicks = 0;
        groundTicks = 0;
        moveSpeed = 0;
        jumping = false;
        jumped = false;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            lastX = client.player.getX();
            lastZ = client.player.getZ();
        }
    }

    @Override
    public void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        switch (jumpMode.getValue()) {
            case "Vanilla" -> vanilla(client, player);
            case "Burst" -> burst(client, player);
            case "Glide" -> glide(client, player);
            default -> {
            }
        }
        lastX = player.getX();
        lastZ = player.getZ();
    }

    private void vanilla(Minecraft client, Player player) {
        if (!isMoving(player) || !client.options.keyJump.isDown()) {
            return;
        }
        double dir = getDir(player);
        double xDir = Math.cos(Math.toRadians(dir + 90));
        double zDir = Math.sin(Math.toRadians(dir + 90));
        Vec3 delta = player.getDeltaMovement();

        if (player.verticalCollision
                || !client.level.noCollision(player, player.getBoundingBox().move(0, delta.y, 0))) {
            player.setDeltaMovement(xDir * 0.29, delta.y, zDir * 0.29);
        }
        if (Math.abs(delta.y - 0.33319999363422365) < 1.0E-9) {
            player.setDeltaMovement(xDir * vanillaBoostFactor.getValue(), delta.y,
                    zDir * vanillaBoostFactor.getValue());
        }
    }

    private void burst(Minecraft client, Player player) {
        if (stage != 0 && !player.onGround() && autoDisable.isEnabled()) {
            jumping = true;
        }
        if (jumping && (player.getY() - (int) player.getY() < 0.01)) {
            jumping = false;
            toggle();
            return;
        }
        if (onlyOnGround.isEnabled() && !player.onGround() && stage == 0) {
            return;
        }

        double xDist = player.getX() - lastX;
        double zDist = player.getZ() - lastZ;
        double lastDist = Math.sqrt(xDist * xDist + zDist * zDist);

        if (!isMoving(player) || (onJump.isEnabled() && !client.options.keyJump.isDown())
                || player.isInLava() || player.isInWater()) {
            return;
        }

        switch (stage) {
            case 0 -> moveSpeed = getMoveSpeed(player) * burstInitialSpeed.getValue();
            case 1 -> {
                Vec3 delta = player.getDeltaMovement();
                player.setDeltaMovement(delta.x, 0.42, delta.z);
                moveSpeed *= burstBoostFactor.getValue();
            }
            case 2 -> {
                double difference = lastDist - getMoveSpeed(player);
                moveSpeed = lastDist - difference;
            }
            default -> moveSpeed = lastDist - lastDist / 159;
        }

        setMoveSpeed(player, moveSpeed = Math.max(getMoveSpeed(player), moveSpeed));
        if (!player.verticalCollision
                && !client.level.noCollision(player, player.getBoundingBox().move(0, player.getDeltaMovement().y, 0))
                && !client.level.noCollision(player, player.getBoundingBox().move(0, -0.4, 0))) {
            Vec3 delta = player.getDeltaMovement();
            player.setDeltaMovement(delta.x, -0.001, delta.z);
        }
        stage++;
    }

    private void glide(Minecraft client, Player player) {
        if (!isMoving(player)) {
            return;
        }
        float forward = player.zza != 0 ? (player.zza > 0 ? 1 : -1) : 0;
        double yaw = Math.toRadians(player.getYRot() + 90);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double mult = glideMultiplier.getValue();
        Vec3 delta = player.getDeltaMovement();

        if (!player.verticalCollision && !player.onGround()) {
            jumped = true;
            airTicks++;
            groundTicks = -5;
            double velocityY = delta.y;

            if (airTicks - 6 >= 0 && airTicks - 6 < GLIDE_RAMP.length) {
                setY(player, velocityY * GLIDE_RAMP[airTicks - 6] * mult);
            }
            if (velocityY < -0.2 && velocityY > -0.24) {
                setY(player, velocityY * 0.7 * mult);
            } else if (velocityY < -0.25 && velocityY > -0.32) {
                setY(player, velocityY * 0.8 * mult);
            } else if (velocityY < -0.35 && velocityY > -0.8) {
                setY(player, velocityY * 0.98 * mult);
            }

            if (airTicks - 1 >= 0 && airTicks - 1 < GLIDE_MOTION.length) {
                double motion = GLIDE_MOTION[airTicks - 1] * 3 * mult;
                player.setDeltaMovement(forward * motion * cos, player.getDeltaMovement().y,
                        forward * motion * sin);
            } else {
                player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
            }
        } else {
            if (autoDisable.isEnabled() && jumped) {
                jumped = false;
                toggle();
                return;
            }
            airTicks = 0;
            groundTicks++;
            if (groundTicks <= 2) {
                double speed = forward * 0.01 * mult;
                player.setDeltaMovement(speed * cos, player.getDeltaMovement().y, speed * sin);
            } else {
                double speed = forward * 0.3 * mult;
                player.setDeltaMovement(speed * cos, 0.424, speed * sin);
            }
        }
    }

    private void setY(Player player, double y) {
        Vec3 delta = player.getDeltaMovement();
        player.setDeltaMovement(delta.x, y, delta.z);
    }

    private static boolean isMoving(Player player) {
        return player.zza != 0 || player.xxa != 0;
    }

    private static double getDir(Player player) {
        double dir = player.getYRot() + (player.zza < 0 ? 180 : 0);
        if (player.xxa > 0) {
            dir += -90F * (player.zza < 0 ? -0.5F : (player.zza > 0 ? 0.5F : 1F));
        } else if (player.xxa < 0) {
            dir += 90F * (player.zza < 0 ? -0.5F : (player.zza > 0 ? 0.5F : 1F));
        }
        return dir;
    }

    private static double getMoveSpeed(Player player) {
        double base = 0.2873;
        var speed = player.getEffect(MobEffects.SPEED);
        if (speed != null) {
            base *= 1.0 + 0.2 * (speed.getAmplifier() + 1);
        }
        return base;
    }

    private static void setMoveSpeed(Player player, double speed) {
        double forward = player.zza;
        double strafe = player.xxa;
        float yaw = player.getYRot();

        if (!isMoving(player)) {
            Vec3 delta = player.getDeltaMovement();
            player.setDeltaMovement(0, delta.y, 0);
            return;
        }
        if (forward != 0) {
            if (strafe > 0) {
                yaw += forward > 0 ? -45 : 45;
            } else if (strafe < 0) {
                yaw += forward > 0 ? 45 : -45;
            }
        }
        strafe = 0;
        if (forward > 0) {
            forward = 1;
        } else if (forward < 0) {
            forward = -1;
        }

        double cos = Math.cos(Math.toRadians(yaw + 90));
        double sin = Math.sin(Math.toRadians(yaw + 90));
        Vec3 delta = player.getDeltaMovement();
        player.setDeltaMovement(forward * speed * cos + strafe * speed * sin, delta.y,
                forward * speed * sin + strafe * speed * cos);
    }
}
