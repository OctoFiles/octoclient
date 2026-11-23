package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

/**
 * Port of Meteor's Velocity: the velocity packet is rewritten so only the
 * configured fraction of the knockback actually reaches the player.
 */
public class Velocity extends Module implements Module.Tickable {
    private static Velocity instance;

    private final BooleanSetting knockback = new BooleanSetting("knockback",
            "Modifies the amount of knockback you take from attacks.", true);
    private final NumberSetting knockbackHorizontal = new NumberSetting("knockback-horizontal",
            "How much horizontal knockback you will take.", 0.0, 0.0, 1.0, 0.05);
    private final NumberSetting knockbackVertical = new NumberSetting("knockback-vertical",
            "How much vertical knockback you will take.", 0.0, 0.0, 1.0, 0.05);
    private final BooleanSetting sinking = new BooleanSetting("sinking",
            "Prevents you from sinking in liquids.", false);

    public Velocity() {
        super("Velocity", "Prevents you from being moved by external forces.", 'V');
        addSetting(knockback);
        addSetting(knockbackHorizontal);
        addSetting(knockbackVertical);
        addSetting(sinking);
        instance = this;
    }

    @Override
    public void onTick(Minecraft client) {
        if (!sinking.isEnabled() || client.player == null || client.options == null) {
            return;
        }
        if (client.options.keyJump.isDown() || client.options.keyShift.isDown()) {
            return;
        }
        LocalPlayer player = client.player;
        if ((player.isInWater() || player.isInLava()) && player.getDeltaMovement().y < 0) {
            Vec3 delta = player.getDeltaMovement();
            player.setDeltaMovement(delta.x, 0.0, delta.z);
        }
    }

    /**
     * Applies the Meteor velocity math: only the difference between the packet
     * velocity and the current velocity is scaled.
     *
     * @return {@code true} when the packet must be cancelled.
     */
    public static boolean handle(ClientboundSetEntityMotionPacket packet, LocalPlayer player) {
        Velocity module = instance;
        if (module == null || !module.isEnabled() || player == null
                || packet.id() != player.getId() || !module.knockback.isEnabled()) {
            return false;
        }
        Vec3 current = player.getDeltaMovement();
        Vec3 movement = packet.movement();
        double x = (movement.x - current.x) * module.knockbackHorizontal.getFloat() + current.x;
        double y = (movement.y - current.y) * module.knockbackVertical.getFloat() + current.y;
        double z = (movement.z - current.z) * module.knockbackHorizontal.getFloat() + current.z;
        player.setDeltaMovement(x, y, z);
        return true;
    }
}
