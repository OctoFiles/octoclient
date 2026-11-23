package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

/** Charges already placed respawn anchors with glowstone. */
public class AnchorCharge extends Module implements Module.Tickable {
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to the anchor.", 4.0, 0, 6, 0.1);
    private final BooleanSetting targetOnly = new BooleanSetting("target-only",
            "Only charges anchors that are close to another player or mob.", true);
    private final NumberSetting targetRange = new NumberSetting("target-range",
            "How close an anchor has to be to a target.", 4, 1, 8, 0.5);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the anchor.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to glowstone automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between charges.", 1, 0, 20, 1);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public AnchorCharge() {
        super("AnchorCharge", "Charges nearby respawn anchors with glowstone.", 'A');
        addSetting(range);
        addSetting(targetOnly);
        addSetting(targetRange);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(delay);
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
            target = AuraUtil.target(client, targetRange.getValue(), "Players", true, true);
        }
        if (target == null && targetOnly.isEnabled()) {
            return;
        }
        BlockPos anchor = AnchorUtil.bestUncharged(client,
                target != null ? target : client.player, range.getValue());
        if (anchor != null && AnchorUtil.chargeAnchor(client, swap, anchor)) {
            if (rotate.isEnabled()) {
                AuraUtil.lookAt(client.player, anchor);
            }
            timer = Math.max(1, delay.getInt());
        }
    }
}
