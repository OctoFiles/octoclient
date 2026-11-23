package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Anchor macro: performs the whole place/charge/detonate sequence on the block
 * you are looking at while the configured key is held.
 */
public class AnchorMacro extends Module implements Module.Tickable {
    private final ModeSetting trigger = new ModeSetting("trigger",
            "Which key starts the macro.", "Use", List.of("Use", "Attack", "Always"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance to the clicked block.", 4.5, 0, 6, 0.1);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates an anchor that would hurt you more than this.", 6, 0, 20, 0.5);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the block.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to anchors and glowstone automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between macro steps.", 1, 0, 20, 1);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;

    public AnchorMacro() {
        super("AnchorMacro", "Runs the full anchor sequence on the block you look at.", 'A');
        addSetting(trigger);
        addSetting(range);
        addSetting(maxSelfDamage);
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
        if (!triggered(client)) {
            timer = 0;
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        if (!(client.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        Level level = client.level;
        BlockPos clicked = hit.getBlockPos();
        if (client.player.distanceToSqr(
                clicked.getX() + 0.5, clicked.getY() + 0.5, clicked.getZ() + 0.5) > range.getValue() * range.getValue()) {
            return;
        }
        if (rotate.isEnabled()) {
            AuraUtil.lookAt(client.player, clicked);
        }
        if (AnchorUtil.isAnchor(level, clicked)) {
            if (AnchorUtil.charge(level, clicked) > 0) {
                if (AnchorUtil.selfDamage(client, clicked) > maxSelfDamage.getValue()) {
                    return;
                }
                if (AnchorUtil.detonate(client, swap, clicked)) {
                    timer = Math.max(1, delay.getInt());
                }
            } else if (AnchorUtil.chargeAnchor(client, swap, clicked)) {
                timer = Math.max(1, delay.getInt());
            }
            return;
        }
        BlockPos placePos = clicked.relative(hit.getDirection());
        if (AuraUtil.replaceable(level, placePos) && AuraUtil.solid(level, clicked)
                && AnchorUtil.placeWith(client, swap, net.minecraft.world.item.Items.RESPAWN_ANCHOR,
                new AnchorUtil.Spot(placePos, hit.getDirection()))) {
            timer = Math.max(1, delay.getInt());
        }
    }

    private boolean triggered(Minecraft client) {
        return switch (trigger.getValue()) {
            case "Attack" -> client.options.keyAttack.isDown();
            case "Always" -> true;
            default -> client.options.keyUse.isDown();
        };
    }
}
