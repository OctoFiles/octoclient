package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Respawn-anchor aura: places anchors next to the target, charges them with
 * glowstone and detonates them. The explosion damage is always calculated by
 * the server; the module only aims and right-clicks.
 */
public class AnchorAura extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum distance for placing and using anchors.", 4.0, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Only detonates when the anchor deals at least this much damage.", 6, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates an anchor that would hurt you more than this.", 6, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Aims the explosion where the target will be in this many ticks.", 2, 0, 20, 1);
    private final BooleanSetting place = new BooleanSetting("place",
            "Places new respawn anchors.", true);
    private final BooleanSetting charge = new BooleanSetting("charge",
            "Charges placed anchors with glowstone.", true);
    private final BooleanSetting explode = new BooleanSetting("explode",
            "Detonates charged anchors.", true);
    private final BooleanSetting surround = new BooleanSetting("surround",
            "Places obsidian around the target before attacking.", false);
    private final BooleanSetting trap = new BooleanSetting("trap",
            "Places obsidian above the target.", false);
    private final BooleanSetting antiTotem = new BooleanSetting("anti-totem",
            "Repeats the cycle immediately when the target is about to pop a totem.", true);
    private final NumberSetting antiTotemHits = new NumberSetting("anti-totem-hits",
            "Extra place/charge/detonate cycles after a totem pop.", 2, 1, 5, 1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the anchor.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to anchors, glowstone and obsidian automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot when the module stops.", true);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only works while you hold the attack key.", false);
    private final NumberSetting placeDelay = new NumberSetting("place-delay",
            "Ticks between placing and charging.", 2, 0, 20, 1);
    private final NumberSetting hitDelay = new NumberSetting("hit-delay",
            "Ticks between detonations.", 2, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Never attacks creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int delay;
    private int extraHits;
    private int idleTicks;

    public AnchorAura() {
        super("AnchorAura", "Places, charges and detonates respawn anchors around your target.", 'A');
        addSetting(targets);
        addSetting(range);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
        addSetting(place);
        addSetting(charge);
        addSetting(explode);
        addSetting(surround);
        addSetting(trap);
        addSetting(antiTotem);
        addSetting(antiTotemHits);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(onlyOnClick);
        addSetting(placeDelay);
        addSetting(hitDelay);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        swap.release(Minecraft.getInstance(), swapBack.isEnabled());
        delay = 0;
        extraHits = 0;
        idleTicks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null || !client.player.isAlive()) {
            return;
        }
        if (!autoSwitch.isEnabled()) {
            swap.release(client, false);
        }
        swap.respectManualChoice(client);
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            idle(client);
            return;
        }
        if (delay > 0) {
            delay--;
            return;
        }
        LivingEntity target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        if (target == null) {
            idle(client);
            return;
        }
        idleTicks = 0;
        if (rotate.isEnabled()) {
            AuraUtil.lookAt(client.player, target);
        }

        if (surround.isEnabled() && AuraUtil.slotOf(client, Items.OBSIDIAN) >= 0) {
            List<AnchorUtil.Spot> spots = AnchorUtil.surroundSpots(client, target, range.getValue());
            for (AnchorUtil.Spot spot : spots) {
                if (AnchorUtil.placeWith(client, swap, Items.OBSIDIAN, spot)) {
                    delay = Math.max(1, placeDelay.getInt());
                    return;
                }
            }
        }
        if (trap.isEnabled() && AuraUtil.slotOf(client, Items.OBSIDIAN) >= 0) {
            AnchorUtil.Spot spot = AnchorUtil.trapSpot(client, target, range.getValue());
            if (spot != null && AnchorUtil.placeWith(client, swap, Items.OBSIDIAN, spot)) {
                delay = Math.max(1, placeDelay.getInt());
                return;
            }
        }

        if (explode.isEnabled()) {
            BlockPos charged = AnchorUtil.bestCharged(client, target, range.getValue(),
                    minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
            if (charged != null && AnchorUtil.detonate(client, swap, charged)) {
                boolean popping = target.getHealth() + target.getAbsorptionAmount()
                        <= AnchorUtil.damageTo(target, charged, predictTicks.getInt());
                extraHits = antiTotem.isEnabled() && popping ? antiTotemHits.getInt() : 0;
                delay = extraHits > 0 ? 0 : Math.max(1, hitDelay.getInt());
                return;
            }
        }
        if (charge.isEnabled()) {
            BlockPos uncharged = AnchorUtil.bestUncharged(client, target, range.getValue());
            if (uncharged != null && AnchorUtil.chargeAnchor(client, swap, uncharged)) {
                delay = Math.max(1, placeDelay.getInt());
                return;
            }
        }
        if (place.isEnabled()) {
            AnchorUtil.Spot spot = AnchorUtil.findSpot(client, target, range.getValue(),
                    minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
            if (AnchorUtil.place(client, swap, spot)) {
                delay = Math.max(1, placeDelay.getInt());
                return;
            }
        }
        if (extraHits > 0) {
            extraHits--;
        }
    }

    private void idle(Minecraft client) {
        if (swap.swapped() && ++idleTicks > 60) {
            swap.release(client, swapBack.isEnabled());
            idleTicks = 0;
        }
    }
}
