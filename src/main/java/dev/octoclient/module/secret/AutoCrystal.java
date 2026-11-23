package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;

/** Compact fully automatic crystal cycle: place and break. */
public class AutoCrystal extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting placeRange = new NumberSetting("place-range",
            "Maximum placement distance.", 4.5, 0, 6, 0.1);
    private final NumberSetting breakRange = new NumberSetting("break-range",
            "Maximum distance to a crystal.", 4.5, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Minimum predicted damage for a detonation.", 6, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Predicts the target position this many ticks ahead.", 2, 0, 20, 1);
    private final BooleanSetting place = new BooleanSetting("place",
            "Places end crystals.", true);
    private final BooleanSetting breakCrystals = new BooleanSetting("break",
            "Breaks end crystals.", true);
    private final BooleanSetting optimizer = new BooleanSetting("optimizer",
            "Breaks crystals without a delay.", true);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the crystal or base block.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to crystals automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between placements.", 1, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public AutoCrystal() {
        super("AutoCrystal", "Automatically places and breaks end crystals.", 'C');
        addSetting(targets);
        addSetting(placeRange);
        addSetting(breakRange);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
        addSetting(place);
        addSetting(breakCrystals);
        addSetting(optimizer);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(delay);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        swap.release(Minecraft.getInstance(), swapBack.isEnabled());
        timer = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null) {
            return;
        }
        swap.respectManualChoice(client);
        if (timer > 0) {
            timer--;
            return;
        }
        double placeReach = CrystalRange.placeRange(placeRange.getValue());
        double breakReach = CrystalRange.breakRange(breakRange.getValue());
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, Math.max(placeReach, breakReach), targets.getValue(),
                    ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        }
        if (target == null) {
            return;
        }
        if (breakCrystals.isEnabled()) {
            EndCrystal crystal = CrystalUtil.bestCrystal(client, target, breakReach,
                    minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
            if (crystal != null && CrystalUtil.breakCrystal(client, crystal)) {
                if (rotate.isEnabled()) {
                    AuraUtil.lookAt(client.player, crystal);
                }
                timer = optimizer.isEnabled() ? 0 : Math.max(1, delay.getInt());
                return;
            }
        }
        if (place.isEnabled()) {
            BlockPos base = CrystalUtil.bestBase(client, target, placeReach,
                    minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
            if (CrystalUtil.place(client, swap, base)) {
                if (rotate.isEnabled() && base != null) {
                    AuraUtil.lookAt(client.player, base.above());
                }
                timer = Math.max(1, delay.getInt());
            }
        }
    }
}
