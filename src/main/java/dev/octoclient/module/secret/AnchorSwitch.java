package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Keeps the right anchor item in the hand: glowstone while an anchor needs
 * charging, a detonator item for a charged anchor and an anchor otherwise.
 */
public class AnchorSwitch extends Module implements Module.Tickable {
    private final ModeSetting mode = new ModeSetting("mode",
            "Which item to keep in the hand.", "Auto",
            List.of("Auto", "Anchor", "Glowstone", "Detonator"));
    private final NumberSetting range = new NumberSetting("range",
            "How far to look for anchors.", 5.0, 0, 8, 0.1);
    private final BooleanSetting targetOnly = new BooleanSetting("target-only",
            "Only switches when a target is nearby.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot when the module stops.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between checks.", 2, 0, 20, 1);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public AnchorSwitch() {
        super("AnchorSwitch", "Automatically holds anchors, glowstone or a detonator.", 'A');
        addSetting(mode);
        addSetting(range);
        addSetting(targetOnly);
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
        timer = Math.max(1, delay.getInt());
        LivingEntity target = AnchorTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, 8, "Players", true, true);
        }
        if (target == null && targetOnly.isEnabled()) {
            return;
        }
        switch (mode.getValue()) {
            case "Anchor" -> swap.ensure(client, Items.RESPAWN_ANCHOR);
            case "Glowstone" -> swap.ensure(client, Items.GLOWSTONE);
            case "Detonator" -> swap.ensure(client, stack -> stack.isEmpty()
                    || !stack.is(Items.GLOWSTONE) && !stack.is(Items.RESPAWN_ANCHOR));
            default -> {
                BlockPos charged = target != null
                        ? AnchorUtil.bestCharged(client, target, range.getValue(), 0, 20, 0)
                        : null;
                BlockPos uncharged = target != null
                        ? AnchorUtil.bestUncharged(client, target, range.getValue())
                        : null;
                if (charged != null) {
                    swap.ensure(client, stack -> stack.isEmpty()
                            || !stack.is(Items.GLOWSTONE) && !stack.is(Items.RESPAWN_ANCHOR));
                } else if (uncharged != null) {
                    swap.ensure(client, Items.GLOWSTONE);
                } else {
                    swap.ensure(client, Items.RESPAWN_ANCHOR);
                }
            }
        }
    }

    /** Convenience for other modules that want to know the preferred item. */
    static Item preferred(boolean charged, boolean uncharged) {
        if (charged) {
            return Items.AIR;
        }
        return uncharged ? Items.GLOWSTONE : Items.RESPAWN_ANCHOR;
    }
}
