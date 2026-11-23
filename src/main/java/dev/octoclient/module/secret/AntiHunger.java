package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.player.Player;

/**
 * Port of Meteor's AntiHunger. Reduces (does NOT remove) hunger consumption.
 * The packet transform is called from the send mixin.
 */
public class AntiHunger extends Module implements Module.Tickable {
    private static AntiHunger instance;

    private final BooleanSetting sprint = new BooleanSetting("sprint",
            "Spoofs sprinting packets.", true);
    private final BooleanSetting onGround = new BooleanSetting("on-ground",
            "Spoofs the onGround flag.", true);

    private boolean lastOnGround;
    private boolean ignorePacket;

    public AntiHunger() {
        super("AntiHunger", "Reduces hunger consumption.", 'H');
        addSetting(sprint);
        addSetting(onGround);
        instance = this;
    }

    @Override
    protected void onEnable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            lastOnGround = client.player.onGround();
        }
    }

    @Override
    public void onTick(Minecraft client) {
        Player player = client.player;
        if (player == null) {
            return;
        }
        if (player.onGround() && !lastOnGround && onGround.isEnabled()) {
            ignorePacket = true;
        }
        lastOnGround = player.onGround();
    }

    /**
     * @return the packet that should be sent instead, or {@code null} to cancel it.
     */
    public static Packet<?> transform(Packet<?> packet, Minecraft client) {
        AntiHunger module = instance;
        if (module == null || !module.isEnabled() || client.player == null) {
            return packet;
        }
        Player player = client.player;
        if (player.isPassenger() || player.isInWater() || player.isUnderWater()) {
            return packet;
        }
        if (packet instanceof ServerboundPlayerCommandPacket command && module.sprint.isEnabled()
                && command.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) {
            return null;
        }
        if (packet instanceof ServerboundMovePlayerPacket move && module.onGround.isEnabled()
                && player.onGround() && player.fallDistance <= 0.0
                && client.gameMode != null && !client.gameMode.isDestroying()) {
            if (module.ignorePacket) {
                module.ignorePacket = false;
                return packet;
            }
            if (move.isOnGround()) {
                return spoof(move);
            }
        }
        return packet;
    }

    private static Packet<?> spoof(ServerboundMovePlayerPacket move) {
        boolean collision = move.horizontalCollision();
        if (move instanceof ServerboundMovePlayerPacket.Pos pos) {
            return new ServerboundMovePlayerPacket.Pos(pos.getX(0), pos.getY(0), pos.getZ(0),
                    false, collision);
        }
        if (move instanceof ServerboundMovePlayerPacket.PosRot posRot) {
            return new ServerboundMovePlayerPacket.PosRot(posRot.getX(0), posRot.getY(0),
                    posRot.getZ(0), posRot.getYRot(0), posRot.getXRot(0), false, collision);
        }
        if (move instanceof ServerboundMovePlayerPacket.Rot rot) {
            return new ServerboundMovePlayerPacket.Rot(rot.getYRot(0), rot.getXRot(0), false, collision);
        }
        return move;
    }
}
