package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

import java.util.Comparator;
import java.util.List;

/**
 * MultiAura: attacks several targets per swing window. Unlike KillAura it has
 * its own compact setting set and always works on the closest targets first.
 */
public class MultiAura extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final ModeSetting priority = new ModeSetting("priority",
            "How targets are sorted.", "Closest",
            List.of("Closest", "LowestHealth", "Angle"));
    private final ModeSetting rotate = new ModeSetting("rotate",
            "Rotates towards the targets.", "Always", List.of("Always", "OnHit", "None"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum attack distance.", 4.5, 0, 6, 0.1);
    private final NumberSetting maxTargets = new NumberSetting("max-targets",
            "How many targets are hit per window.", 3, 1, 10, 1);
    private final NumberSetting hitDelay = new NumberSetting("hit-delay",
            "Ticks between attack windows.", 11, 0, 20, 1);
    private final BooleanSetting noCooldown = new BooleanSetting("no-cooldown",
            "Every hit is fully charged, even below the vanilla cooldown.", false);
    private final BooleanSetting todesDamage = new BooleanSetting("todes-damage",
            "Spoofs a critical hit on every sword attack.", false);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only attacks while you hold the attack key.", false);
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Switches to the best weapon before attacking.", false);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Restores your hotbar slot afterwards.", true);
    private final BooleanSetting ignorePassive = new BooleanSetting("ignore-passive",
            "Ignores passive mobs.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private final HotbarSwap swap = new HotbarSwap();
    private int hitTimer;
    private int idleTicks;

    public MultiAura() {
        super("MultiAura", "Attacks several targets around you at once.", 'M');
        addSetting(targets);
        addSetting(priority);
        addSetting(rotate);
        addSetting(range);
        addSetting(maxTargets);
        addSetting(hitDelay);
        addSetting(noCooldown);
        addSetting(todesDamage);
        addSetting(onlyOnClick);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(ignorePassive);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        swap.release(Minecraft.getInstance(), swapBack.isEnabled());
        hitTimer = 0;
        idleTicks = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null || !client.player.isAlive()) {
            return;
        }
        swap.respectManualChoice(client);
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            idle(client);
            return;
        }
        List<LivingEntity> list = AuraUtil.targets(client, range.getValue(), targets.getValue(),
                ignoreFriends.isEnabled(), ignoreCreative.isEnabled());
        if (ignorePassive.isEnabled() && !targets.getValue().equals("Players")) {
            list.removeIf(entity -> !(entity instanceof net.minecraft.world.entity.player.Player)
                    && !(entity instanceof Enemy));
        }
        if (list.isEmpty()) {
            idle(client);
            return;
        }
        idleTicks = 0;
        Comparator<LivingEntity> comparator = switch (priority.getValue()) {
            case "LowestHealth" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Angle" -> Comparator.comparingDouble(e -> AuraUtil.angleTo(client.player, e));
            default -> Comparator.comparingDouble(e -> client.player.distanceToSqr(e));
        };
        list.sort(comparator);
        while (list.size() > maxTargets.getInt()) {
            list.remove(list.size() - 1);
        }
        if (autoSwitch.isEnabled()) {
            int slot = AuraUtil.bestWeaponSlot(client);
            if (slot >= 0) {
                swap.select(client, slot);
            }
        }
        LivingEntity primary = list.getFirst();
        if (rotate.getValue().equals("Always")) {
            AuraUtil.lookAt(client.player, primary);
        }
        if (hitTimer < hitDelay.getInt()) {
            hitTimer++;
            return;
        }
        boolean sword = AuraUtil.isSword(client.player.getMainHandItem());
        for (LivingEntity target : list) {
            if (rotate.getValue().equals("OnHit")) {
                AuraUtil.lookAt(client.player, target);
            }
            if (todesDamage.isEnabled() && sword) {
                AuraUtil.critSpoof(client);
            }
            client.gameMode.attack(client.player, target);
            if (noCooldown.isEnabled()) {
                client.player.swing(InteractionHand.MAIN_HAND, false);
                AuraUtil.refill(client.player);
            } else {
                client.player.swing(InteractionHand.MAIN_HAND);
            }
        }
        hitTimer = 0;
    }

    private void idle(Minecraft client) {
        hitTimer = 0;
        if (swap.swapped() && ++idleTicks > 60) {
            swap.release(client, swapBack.isEnabled());
            idleTicks = 0;
        }
    }
}
