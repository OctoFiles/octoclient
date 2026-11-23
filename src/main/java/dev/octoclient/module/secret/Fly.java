package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Port of Meteor's Flight (Abilities + Velocity modes with anti-kick). */
public class Fly extends Module implements Module.Tickable {
    private static Fly instance;

    private final ModeSetting mode = new ModeSetting("mode", "The mode for Flight.",
            "Abilities", List.of("Abilities", "Velocity"));
    private final NumberSetting speed = new NumberSetting("speed",
            "Your speed when flying.", 0.1, 0.0, 2.0, 0.01);
    private final BooleanSetting verticalSpeedMatch = new BooleanSetting("vertical-speed-match",
            "Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.", false);
    private final BooleanSetting noSneak = new BooleanSetting("no-sneak",
            "Prevents you from sneaking while flying.", false);
    private final ModeSetting antiKickMode = new ModeSetting("mode (anti kick)",
            "The mode for anti kick.", "Packet", List.of("Normal", "Packet", "None"));
    private final NumberSetting antiKickDelay = new NumberSetting("delay (anti kick)",
            "The amount of delay, in ticks, between flying down a bit and returning to the original position.",
            20, 1, 200, 1);
    private final NumberSetting offTime = new NumberSetting("off-time (anti kick)",
            "The amount of delay, in ticks, to fly down a bit to reset floating ticks.", 1, 1, 20, 1);

    private int delayLeft;
    private int offLeft;
    private float lastYaw;

    public Fly() {
        super("Flight", "FLYYYY! No Fall is recommended with this module.", 'F');
        addSetting(mode);
        addSetting(speed);
        addSetting(verticalSpeedMatch);
        addSetting(noSneak);
        addSetting(antiKickMode);
        addSetting(antiKickDelay);
        addSetting(offTime);
        instance = this;
    }

    public static Fly get() {
        return instance;
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        delayLeft = antiKickDelay.getInt();
        offLeft = offTime.getInt();
        if (client.player == null) {
            return;
        }
        if (mode.getValue().equals("Abilities") && !client.player.isSpectator()) {
            client.player.getAbilities().flying = true;
            if (client.player.getAbilities().instabuild) {
                return;
            }
            client.player.getAbilities().mayfly = true;
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (mode.getValue().equals("Abilities") && !client.player.isSpectator()) {
            abilitiesOff(client);
        }
    }

    private static void abilitiesOff(Minecraft client) {
        client.player.getAbilities().flying = false;
        client.player.getAbilities().setFlyingSpeed(0.05f);
        if (client.player.getAbilities().instabuild) {
            return;
        }
        client.player.getAbilities().mayfly = false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        float currentYaw = client.player.getYRot();
        lastYaw = currentYaw;

        if (delayLeft > 0) {
            delayLeft--;
        }
        boolean antiKick = !antiKickMode.getValue().equals("None");
        if (antiKick && offLeft <= 0 && delayLeft <= 0) {
            delayLeft = antiKickDelay.getInt();
            offLeft = offTime.getInt();
            if (antiKickMode.getValue().equals("Packet")) {
                // Resend position a tiny bit lower so the server resets its floating counter.
                Vec3 pos = client.player.position();
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                            pos.x, pos.y - 0.03130, pos.z,
                            client.player.onGround(), client.player.horizontalCollision));
                }
            }
        } else if (antiKick && delayLeft <= 0) {
            if (antiKickMode.getValue().equals("Normal") && mode.getValue().equals("Abilities")) {
                client.player.getAbilities().flying = false;
            }
            if (antiKickMode.getValue().equals("Packet") && offLeft == offTime.getInt()) {
                Vec3 pos = client.player.position();
                if (client.getConnection() != null) {
                    client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                            pos.x, pos.y - 0.03130, pos.z,
                            client.player.onGround(), client.player.horizontalCollision));
                }
            }
            offLeft--;
        }

        if (mode.getValue().equals("Velocity")) {
            client.player.getAbilities().flying = false;
            Vec3 velocity = new Vec3(0, 0, 0);
            float vertical = speed.getFloat() * (verticalSpeedMatch.isEnabled() ? 10f : 5f);
            if (client.options.keyJump.isDown()) {
                velocity = velocity.add(0, vertical, 0);
            }
            if (client.options.keyShift.isDown()) {
                velocity = velocity.subtract(0, vertical, 0);
            }
            client.player.setDeltaMovement(velocity);
            if (noSneak.isEnabled()) {
                client.player.setOnGround(false);
            }
        } else if (!client.player.isSpectator()) {
            client.player.getAbilities().setFlyingSpeed(speed.getFloat());
            client.player.getAbilities().flying = true;
            if (client.player.getAbilities().instabuild) {
                return;
            }
            client.player.getAbilities().mayfly = true;
        }
    }

    /** Meteor cancels the server ability packet in Abilities mode so flight is not toggled off. */
    public static boolean shouldCancelAbilities() {
        Fly module = instance;
        return module != null && module.isEnabled() && module.mode.getValue().equals("Abilities");
    }
}
