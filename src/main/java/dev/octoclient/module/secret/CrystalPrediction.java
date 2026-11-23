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

/** Breaks crystals where the target is predicted to be, not where it is now. */
public class CrystalPrediction extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to the crystal.", 4.5, 0, 6, 0.1);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "How far ahead the target position is predicted.", 4, 0, 20, 1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Only breaks crystals above this predicted damage.", 6, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never breaks a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final BooleanSetting announce = new BooleanSetting("announce",
            "Shows the predicted damage in the action bar.", false);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the crystal.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between detonations.", 1, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private int timer;

    public CrystalPrediction() {
        super("CrystalPrediction", "Detonates crystals at the target's predicted position.", 'C');
        addSetting(targets);
        addSetting(range);
        addSetting(predictTicks);
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
                minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
        if (crystal == null) {
            return;
        }
        double damage = CrystalUtil.damageTo(target, crystal.position(), predictTicks.getInt());
        if (CrystalUtil.breakCrystal(client, crystal)) {
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, crystal);
            }
            if (announce.isEnabled()) {
                client.player.sendOverlayMessage(Component.literal(
                        "Predicted crystal damage: " + String.format("%.1f", damage)));
            }
            timer = Math.max(1, delay.getInt());
        }
    }
}
