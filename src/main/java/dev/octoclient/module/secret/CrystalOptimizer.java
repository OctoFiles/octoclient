package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;

/**
 * Crystal optimizer: destroys crystals on the same tick they become useful,
 * without waiting for the vanilla break delay.
 */
public class CrystalOptimizer extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to the crystal.", 4.5, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Only breaks crystals above this predicted damage.", 4, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never breaks a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Predicts the target position this many ticks ahead.", 1, 0, 20, 1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the crystal.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    public CrystalOptimizer() {
        super("CrystalOptimizer", "Breaks useful crystals instantly, every tick.", 'C');
        addSetting(targets);
        addSetting(range);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
        addSetting(rotate);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null) {
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
        if (crystal != null) {
            CrystalUtil.breakCrystal(client, crystal);
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, crystal);
            }
        }
    }
}
