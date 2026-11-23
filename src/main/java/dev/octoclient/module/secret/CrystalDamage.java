package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;

/** Damage-gated crystal detonation with an optional damage readout. */
public class CrystalDamage extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to the crystal.", 4.5, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Minimum predicted damage for a detonation.", 10, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final BooleanSetting announce = new BooleanSetting("announce",
            "Shows the predicted damage in the action bar.", true);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the crystal.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between detonations.", 1, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private int timer;

    public CrystalDamage() {
        super("CrystalDamage", "Only detonates crystals above a damage threshold.", 'C');
        addSetting(targets);
        addSetting(range);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(announce);
        addSetting(rotate);
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
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null) {
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        double reach = CrystalRange.breakRange(range.getValue());
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, reach, targets.getValue(),
                    ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        }
        if (target == null) {
            return;
        }
        EndCrystal crystal = CrystalUtil.bestCrystal(client, target, reach,
                minDamage.getValue(), maxSelfDamage.getValue(), 0);
        if (crystal == null) {
            return;
        }
        double damage = CrystalUtil.damageTo(target, crystal.position(), 0);
        if (CrystalUtil.breakCrystal(client, crystal)) {
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, crystal);
            }
            if (announce.isEnabled()) {
                client.player.sendOverlayMessage(Component.literal(
                        "Crystal damage: " + String.format("%.1f", damage)));
            }
            timer = Math.max(1, delay.getInt());
        }
    }
}
