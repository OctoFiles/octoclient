package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * AntiKnockback: drops or scales the velocity packet that carries knockback,
 * explosions and fishing-rod pulls. Unlike {@link Velocity} it defaults to a
 * full cancel instead of a partial scale.
 */
public class AntiKnockback extends Module {
    private static AntiKnockback instance;

    private final ModeSetting mode = new ModeSetting("mode",
            "How incoming motion is handled.", "Cancel", List.of("Cancel", "Reduce"));
    private final NumberSetting horizontal = new NumberSetting("horizontal",
            "How much horizontal knockback reaches you in Reduce mode.", 0.0, 0.0, 1.0, 0.05);
    private final NumberSetting vertical = new NumberSetting("vertical",
            "How much vertical knockback reaches you in Reduce mode.", 0.0, 0.0, 1.0, 0.05);

    public AntiKnockback() {
        super("AntiKnockback", "Cancels or reduces all knockback you take.", 'A');
        addSetting(mode);
        addSetting(horizontal);
        addSetting(vertical);
        instance = this;
    }

    /**
     * @return {@code true} when the packet must be cancelled.
     */
    public static boolean handle(ClientboundSetEntityMotionPacket packet, LocalPlayer player) {
        AntiKnockback module = instance;
        if (module == null || !module.isEnabled() || player == null
                || packet.id() != player.getId()) {
            return false;
        }
        Vec3 current = player.getDeltaMovement();
        if (module.mode.getValue().equals("Cancel")) {
            // Keep the motion the player already has, drop the packet's push.
            player.setDeltaMovement(current);
            return true;
        }
        Vec3 movement = packet.movement();
        double x = (movement.x - current.x) * module.horizontal.getFloat() + current.x;
        double y = (movement.y - current.y) * module.vertical.getFloat() + current.y;
        double z = (movement.z - current.z) * module.horizontal.getFloat() + current.z;
        player.setDeltaMovement(x, y, z);
        return true;
    }
}
