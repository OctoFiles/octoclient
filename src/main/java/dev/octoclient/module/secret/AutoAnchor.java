package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Compact fully automatic anchor cycle: place, charge and detonate. */
public class AutoAnchor extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance for placing and using anchors.", 4.0, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Minimum predicted damage for a detonation.", 6, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates an anchor that would hurt you more than this.", 6, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Predicts the target position this many ticks ahead.", 2, 0, 20, 1);
    private final BooleanSetting charge = new BooleanSetting("charge",
            "Charges placed anchors with glowstone.", true);
    private final BooleanSetting explode = new BooleanSetting("explode",
            "Detonates charged anchors.", true);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the anchor.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to anchors and glowstone automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between actions.", 1, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public AutoAnchor() {
        super("AutoAnchor", "Automatically places, charges and detonates anchors.", 'A');
        addSetting(targets);
        addSetting(range);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
        addSetting(charge);
        addSetting(explode);
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
        LivingEntity target = AnchorTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                    ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        }
        if (target == null) {
            return;
        }
        if (explode.isEnabled()) {
            BlockPos charged = AnchorUtil.bestCharged(client, target, range.getValue(),
                    minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
            if (charged != null && AnchorUtil.detonate(client, swap, charged)) {
                if (rotate.isEnabled()) {
                    AuraUtil.lookAt(client.player, charged);
                }
                timer = Math.max(1, delay.getInt());
                return;
            }
        }
        if (charge.isEnabled()) {
            BlockPos uncharged = AnchorUtil.bestUncharged(client, target, range.getValue());
            if (uncharged != null && AnchorUtil.chargeAnchor(client, swap, uncharged)) {
                if (rotate.isEnabled()) {
                    AuraUtil.lookAt(client.player, uncharged);
                }
                timer = Math.max(1, delay.getInt());
                return;
            }
        }
        AnchorUtil.Spot spot = AnchorUtil.findSpot(client, target, range.getValue(),
                minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
        if (AnchorUtil.place(client, swap, spot)) {
            timer = Math.max(1, delay.getInt());
        }
    }
}
