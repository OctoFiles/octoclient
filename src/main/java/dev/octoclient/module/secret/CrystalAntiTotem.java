package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.util.List;

/**
 * Repeats the crystal cycle without delay when the target is about to pop a
 * totem, so the next crystal hits the freshly revived target.
 */
public class CrystalAntiTotem extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum placement and breaking distance.", 4.5, 0, 6, 0.1);
    private final NumberSetting extraHits = new NumberSetting("extra-hits",
            "Extra place/break cycles after a totem pop.", 2, 1, 5, 1);
    private final NumberSetting minDamage = new NumberSetting("min-damage",
            "Only detonates crystals above this predicted damage.", 4, 0, 40, 0.5);
    private final NumberSetting maxSelfDamage = new NumberSetting("max-self-damage",
            "Never detonates a crystal that would hurt you more than this.", 8, 0, 20, 0.5);
    private final NumberSetting predictTicks = new NumberSetting("predict-ticks",
            "Predicts the target position this many ticks ahead.", 1, 0, 20, 1);
    private final BooleanSetting announce = new BooleanSetting("announce",
            "Shows a message when a totem pop is predicted.", true);
    private final BooleanSetting rotate = new BooleanSetting("rotate",
            "Rotates towards the crystal.", true);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to crystals and obsidian automatically.", true);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between normal cycles.", 1, 0, 20, 1);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int timer;
    private int extra;

    public CrystalAntiTotem() {
        super("CrystalAntiTotem", "Keeps hitting right after the target pops a totem.", 'C');
        addSetting(targets);
        addSetting(range);
        addSetting(extraHits);
        addSetting(minDamage);
        addSetting(maxSelfDamage);
        addSetting(predictTicks);
        addSetting(announce);
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
        extra = 0;
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
        LivingEntity target = CrystalTarget.lockedTarget(client);
        if (target == null) {
            target = AuraUtil.target(client, range.getValue(), targets.getValue(),
                    ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        }
        if (target == null) {
            return;
        }
        if (extra > 0) {
            extra--;
        }
        EndCrystal crystal = CrystalUtil.bestCrystal(client, target, range.getValue(),
                minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
        if (crystal != null) {
            double damage = CrystalUtil.damageTo(target, crystal.position(), predictTicks.getInt());
            boolean popping = target.getHealth() + target.getAbsorptionAmount() <= damage;
            if (CrystalUtil.breakCrystal(client, crystal)) {
                if (rotate.isEnabled()) {
                    AuraUtil.lookAt(client.player, crystal);
                }
                if (popping) {
                    extra = extraHits.getInt();
                    if (announce.isEnabled()) {
                        client.player.sendOverlayMessage(Component.literal("Totem pop predicted!"));
                    }
                }
                timer = extra > 0 ? 0 : Math.max(1, delay.getInt());
                return;
            }
        }
        BlockPos base = CrystalUtil.bestBase(client, target, range.getValue(),
                minDamage.getValue(), maxSelfDamage.getValue(), predictTicks.getInt());
        if (CrystalUtil.place(client, swap, base)) {
            timer = extra > 0 ? 0 : Math.max(1, delay.getInt());
        }
    }
}
