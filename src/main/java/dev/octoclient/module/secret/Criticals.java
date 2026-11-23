package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MaceItem;

import java.util.List;

/**
 * Port of Meteor's Criticals. The packet modes spoof a short upward movement
 * right before the attack so the server accepts it as a critical hit.
 */
public class Criticals extends Module {
    private static Criticals instance;

    private final ModeSetting mode = new ModeSetting("mode",
            "The mode on how Criticals will function.", "Packet",
            List.of("None", "Packet", "UpdatedNCP", "OldNCP", "Jump", "MiniJump"));
    private final BooleanSetting onlyKillaura = new BooleanSetting("only-killaura",
            "Only performs crits when using killaura.", false);
    private final BooleanSetting smashAttack = new BooleanSetting("smash-attack",
            "Will always perform smash attacks when using a mace.", true);
    private final NumberSetting additionalHeight = new NumberSetting("additional-height",
            "The amount of additional height to spoof. More height means more damage.", 0.0, 0, 100, 0.1);

    public Criticals() {
        super("Criticals", "Performs critical attacks when you hit your target.", 'C');
        addSetting(mode);
        addSetting(onlyKillaura);
        addSetting(smashAttack);
        addSetting(additionalHeight);
        instance = this;
    }

    /** Called from the attack mixin right before the hit is sent. */
    public static void beforeAttack(Minecraft client, Player attacker) {
        Criticals module = instance;
        if (module == null || !module.isEnabled() || client.player == null || client.level == null) {
            return;
        }
        if (client.player != attacker || module.mode.getValue().equals("None")) {
            return;
        }
        if (module.onlyKillaura.isEnabled()
                && !dev.octoclient.module.ModuleManager.get().byName("KillAura")
                        .map(Module::isEnabled).orElse(false)) {
            return;
        }

        if (module.smashAttack.isEnabled()
                && client.player.getMainHandItem().getItem() instanceof MaceItem) {
            // MaceAura's simulated height is the authoritative spoof when set.
            if (MaceAura.simulationActive()) {
                return;
            }
            if (client.player.isFallFlying()) {
                return;
            }
            module.sendHeight(client, 0);
            module.sendHeight(client, 1.501 + module.additionalHeight.getValue());
            module.sendHeight(client, 0);
            return;
        }

        if (client.player.onGround() == false || client.player.isInWater()
                || client.player.isInLava() || client.player.onClimbable()) {
            return;
        }

        switch (module.mode.getValue()) {
            case "Packet", "Jump", "MiniJump" -> {
                module.sendHeight(client, 0.0625);
                module.sendHeight(client, 0);
            }
            case "UpdatedNCP" -> {
                module.sendHeight(client, 0.0000008);
                module.sendHeight(client, 0);
            }
            case "OldNCP" -> {
                module.sendHeight(client, 0.11);
                module.sendHeight(client, 0.1100013579);
                module.sendHeight(client, 0.0000013579);
            }
            default -> {
            }
        }
    }

    private void sendHeight(Minecraft client, double height) {
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                client.player.getX(), client.player.getY() + height, client.player.getZ(), false, false));
    }
}
