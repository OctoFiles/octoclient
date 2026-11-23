package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * AutoBlock: raises a shield while an enemy is close.
 *
 * <p>{@code Shield} mode holds the shield in the main hand. {@code Sword} mode
 * keeps your weapon in the main hand and uses the offhand shield; the server
 * applies the block, the client only misses the raise animation (vanilla has no
 * sword blocking any more).
 */
public class AutoBlock extends Module implements Module.Tickable {
    private final ModeSetting mode = new ModeSetting("mode",
            "Where the shield is held.", "Shield", List.of("Shield", "Sword"));
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities trigger the block.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "How close a target has to be.", 4.5, 1, 8, 0.1);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only blocks while you hold the attack key.", false);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to a shield (Shield mode) or a weapon (Sword mode).", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot when the module stops.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between block refreshes in Sword mode.", 2, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never blocks for friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private boolean blocking;
    private int useTimer;
    private int idleTicks;

    public AutoBlock() {
        super("AutoBlock", "Raises a shield automatically while enemies are close.", 'A');
        addSetting(mode);
        addSetting(targets);
        addSetting(range);
        addSetting(onlyOnClick);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(delay);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        stopBlocking(Minecraft.getInstance());
        swap.release(Minecraft.getInstance(), swapBack.isEnabled());
        idleTicks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null || !client.player.isAlive()) {
            stopBlocking(client);
            return;
        }
        swap.respectManualChoice(client);
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            stopBlocking(client);
            idle(client);
            return;
        }
        LivingEntity target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        if (target == null) {
            stopBlocking(client);
            idle(client);
            return;
        }
        idleTicks = 0;
        if (mode.getValue().equals("Shield")) {
            if (autoSwitch.isEnabled() && !AuraUtil.holding(client.player, Items.SHIELD)) {
                swap.ensure(client, Items.SHIELD);
            }
            if (!AuraUtil.holding(client.player, Items.SHIELD)) {
                stopBlocking(client);
                return;
            }
            if (!client.player.isUsingItem()) {
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            }
            blocking = true;
        } else {
            if (autoSwitch.isEnabled() && !AuraUtil.isWeapon(client.player.getMainHandItem())) {
                int slot = AuraUtil.bestWeaponSlot(client);
                if (slot >= 0) {
                    swap.select(client, slot);
                }
            }
            if (!client.player.getOffhandItem().is(Items.SHIELD)) {
                stopBlocking(client);
                return;
            }
            if (useTimer > 0) {
                useTimer--;
            } else {
                // The main-hand sword cannot be used, so the server falls back
                // to the offhand shield. One packet is enough per refresh.
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                useTimer = Math.max(1, delay.getInt());
            }
            blocking = true;
        }
    }

    private void stopBlocking(Minecraft client) {
        if (blocking && client.player != null && client.player.isUsingItem()) {
            client.gameMode.releaseUsingItem(client.player);
        }
        blocking = false;
        useTimer = 0;
    }

    private void idle(Minecraft client) {
        if (swap.swapped() && ++idleTicks > 60) {
            swap.release(client, swapBack.isEnabled());
            idleTicks = 0;
        }
    }
}
