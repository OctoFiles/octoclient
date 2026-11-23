package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.List;

/**
 * Port of Meteor's NoFall. The packet modes rewrite the on-ground flag of the
 * outgoing movement packets; the place modes fall back to the packet spoof.
 */
public class NoFall extends Module {
    private static NoFall instance;

    private final ModeSetting mode = new ModeSetting("mode",
            "The mode of NoFall.", "Packet",
            List.of("Packet", "NoGround", "AirPlace", "Place"));
    private final BooleanSetting resetOnDisable = new BooleanSetting("reset-on-disable",
            "Resets the fall distance when the module is disabled.", true);
    private final BooleanSetting pauseOnMace = new BooleanSetting("pause-on-mace",
            "Pauses while you are holding a mace.", true);

    public NoFall() {
        super("NoFall", "Prevents you from taking fall damage.", 'N');
        addSetting(mode);
        addSetting(resetOnDisable);
        addSetting(pauseOnMace);
        instance = this;
    }

    /** Rewrites outgoing movement packets while NoFall is active. */
    public static Packet<?> rewrite(Packet<?> packet, LocalPlayer player) {
        NoFall module = instance;
        if (module == null || !module.isEnabled() || player == null) {
            return packet;
        }
        if (!(packet instanceof ServerboundMovePlayerPacket move) || player.onGround()) {
            return packet;
        }
        if (module.pauseOnMace.isEnabled() && player.getMainHandItem().getItem() == net.minecraft.world.item.Items.MACE) {
            return packet;
        }
        boolean ground = !module.mode.getValue().equals("NoGround");
        if (move.isOnGround() == ground) {
            return packet;
        }
        if (move instanceof ServerboundMovePlayerPacket.Pos pos) {
            return new ServerboundMovePlayerPacket.Pos(pos.getX(0.0), pos.getY(0.0), pos.getZ(0.0),
                    ground, pos.horizontalCollision());
        }
        if (move instanceof ServerboundMovePlayerPacket.PosRot posRot) {
            return new ServerboundMovePlayerPacket.PosRot(posRot.getX(0.0), posRot.getY(0.0), posRot.getZ(0.0),
                    posRot.getYRot(0f), posRot.getXRot(0f), ground, posRot.horizontalCollision());
        }
        return packet;
    }
}
