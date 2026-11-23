package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;

/** Breaks end crystals when their explosion would hit the target. */
public class CrystalBreak extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting breakRange = new NumberSetting("break-range",
            "Maximum distance to the crystal.", 4.5, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Only breaks crystals above this predicted damage.", 6, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never breaks a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Predicts the target position this many ticks ahead.", 2, 0, 20, 1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the crystal.", true);
    private final BooleanSetting optimizer = new BooleanSetting("optimizer",
            "Breaks every tick without a delay.", false);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between detonations.", 2, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private int timer;

    public CrystalBreak() {
        super("CrystalBreak", "Breaks end crystals around your target.", 'C');
        addSetting(targets);
        addSetting(breakRange);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
        addSetting(rotate);
        addSetting(optimizer);
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
        double range = CrystalRange.breakRange(breakRange.getValue());
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, range, targets.getValue(),
                    ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        }
        if (target == null) {
            return;
        }
        EndCrystal crystal = CrystalUtil.bestCrystal(client, target, range,
                minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
        if (crystal != null && CrystalUtil.breakCrystal(client, crystal)) {
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, crystal);
            }
            timer = optimizer.isEnabled() ? 0 : Math.max(1, delay.getInt());
        }
    }
}
