package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * End-crystal aura: places crystals on obsidian next to the target and breaks
 * them at the best moment. All damage is applied by the server explosion.
 */
public class CrystalAura extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting placeRange = new NumberSetting("place-range",
            "Maximum distance for placing crystals.", 4.5, 0, 6, 0.1);
    private final NumberSetting breakRange = new NumberSetting("break-range",
            "Maximum distance for breaking crystals.", 4.5, 0, 6, 0.1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Only detonates when the crystal deals at least this much damage.", 6, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Aims the explosion where the target will be in this many ticks.", 2, 0, 20, 1);
    private final BooleanSetting place = new BooleanSetting("place",
            "Places end crystals.", true);
    private final BooleanSetting breakCrystals = new BooleanSetting("break",
            "Breaks placed end crystals.", true);
    private final BooleanSetting optimizer = new BooleanSetting("optimizer",
            "Breaks crystals every tick without a delay.", true);
    private final BooleanSetting surround = new BooleanSetting("surround",
            "Places obsidian around the target before attacking.", false);
    private final BooleanSetting trap = new BooleanSetting("trap",
            "Places obsidian above the target.", false);
    private final BooleanSetting antiTotem = new BooleanSetting("anti-totem",
            "Repeats the cycle immediately when the target is about to pop a totem.", true);
    private final NumberSetting antiTotemHits = new NumberSetting("anti-totem-hits",
            "Extra place/break cycles after a totem pop.", 2, 1, 5, 1);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the crystal or base block.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to crystals and obsidian automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot when the module stops.", true);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only works while you hold the attack key.", false);
    private final NumberSetting placeDelay = new NumberSetting("place-delay",
            "Ticks between placements.", 1, 0, 20, 1);
    private final NumberSetting breakDelay = new NumberSetting("break-delay",
            "Ticks between detonations when the optimizer is off.", 2, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Never attacks creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int delay;
    private int extraHits;
    private int idleTicks;

    public CrystalAura() {
        super("CrystalAura", "Places and breaks end crystals around your target.", 'C');
        addSetting(targets);
        addSetting(placeRange);
        addSetting(breakRange);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
        addSetting(place);
        addSetting(breakCrystals);
        addSetting(optimizer);
        addSetting(surround);
        addSetting(trap);
        addSetting(antiTotem);
        addSetting(antiTotemHits);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(onlyOnClick);
        addSetting(placeDelay);
        addSetting(breakDelay);
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
        LivingEntity target = AuraUtil.target(client, placeRange.getValue(), targets.getValue(),
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
            for (AnchorUtil.Spot spot : AnchorUtil.surroundSpots(client, target, placeRange.getValue())) {
                if (AnchorUtil.placeWith(client, swap, Items.OBSIDIAN, spot)) {
                    delay = Math.max(1, placeDelay.getInt());
                    return;
                }
            }
        }
        if (trap.isEnabled() && AuraUtil.slotOf(client, Items.OBSIDIAN) >= 0) {
            AnchorUtil.Spot spot = AnchorUtil.trapSpot(client, target, placeRange.getValue());
            if (spot != null && AnchorUtil.placeWith(client, swap, Items.OBSIDIAN, spot)) {
                delay = Math.max(1, placeDelay.getInt());
                return;
            }
        }

        if (breakCrystals.isEnabled()) {
            EndCrystal crystal = CrystalUtil.bestCrystal(client, target, breakRange.getValue(),
                    minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
            if (crystal != null && CrystalUtil.breakCrystal(client, crystal)) {
                boolean popping = target.getHealth() + target.getAbsorptionAmount()
                        <= CrystalUtil.damageTo(target, crystal.position(), predictTicks.getInt());
                extraHits = antiTotem.isEnabled() && popping ? antiTotemHits.getInt() : 0;
                delay = optimizer.isEnabled() || extraHits > 0 ? 0 : Math.max(1, breakDelay.getInt());
                return;
            }
        }
        if (place.isEnabled()) {
            BlockPos base = CrystalUtil.bestBase(client, target, placeRange.getValue(),
                    minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
            if (CrystalUtil.place(client, swap, base)) {
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
