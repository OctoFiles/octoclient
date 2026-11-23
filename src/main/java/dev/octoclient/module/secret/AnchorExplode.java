package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/** Detonates charged anchors when they deal enough damage to the target. */
public class AnchorExplode extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to the anchor.", 4.0, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Only detonates when the anchor deals at least this much damage.", 6, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates an anchor that would hurt you more than this.", 6, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Predicts the target position this many ticks ahead.", 2, 0, 20, 1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the anchor.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to a detonation item automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between detonations.", 2, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public AnchorExplode() {
        super("AnchorExplode", "Detonates charged respawn anchors.", 'A');
        addSetting(targets);
        addSetting(range);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
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
        BlockPos anchor = AnchorUtil.bestCharged(client, target, range.getValue(),
                minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
        if (anchor != null && AnchorUtil.detonate(client, swap, anchor)) {
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, anchor);
            }
            timer = Math.max(1, delay.getInt());
        }
    }
}
