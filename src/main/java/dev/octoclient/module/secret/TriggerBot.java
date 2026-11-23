package dev.octoclient.module.secret;

import dev.octoclient.module.Module;
import dev.octoclient.module.hud.Friends;
import dev.octoclient.module.setting.BooleanSetting;
import dev.octoclient.module.setting.ModeSetting;
import dev.octoclient.module.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * TriggerBot: attacks the entity under the crosshair as soon as it is in reach.
 * It uses the normal attack packet, so the vanilla attack cooldown still
 * applies.
 */
public class TriggerBot extends Module implements Module.Tickable {
    private final ModeSetting targets = new ModeSetting("targets",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final ModeSetting shieldMode = new ModeSetting("shield-mode",
            "What to do when the target is blocking.", "None", List.of("Ignore", "Break", "None"));
    private final NumberSetting range = new NumberSetting("range",
            "Maximum attack distance.", 4.0, 0, 6, 0.1);
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only attacks while you hold the attack key.", false);
    private final BooleanSetting weaponsOnly = new BooleanSetting("weapons-only",
            "Only attacks while holding a weapon.", true);
    private final BooleanSetting noCooldown = new BooleanSetting("no-cooldown",
            "Ignores the attack cooldown; the delay setting decides the rate.", false);
    private final NumberSetting delay = new NumberSetting("delay",
            "Ticks between attacks when no-cooldown is on.", 11, 0, 20, 1);
    private final BooleanSetting critSpoof = new BooleanSetting("todes-damage",
            "Spoofs a critical hit on every attack.", false);
    private final BooleanSetting ignoreInvisible = new BooleanSetting("ignore-invisible",
            "Never attacks invisible entities.", true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("ignore-friends",
            "Never attacks friends.", true);
    private final BooleanSetting ignoreCreative = new BooleanSetting("ignore-creative",
            "Ignores creative players.", true);

    private int timer;

    public TriggerBot() {
        super("TriggerBot", "Attacks whatever is under your crosshair.", 'T');
        addSetting(targets);
        addSetting(shieldMode);
        addSetting(range);
        addSetting(onlyOnClick);
        addSetting(weaponsOnly);
        addSetting(noCooldown);
        addSetting(delay);
        addSetting(critSpoof);
        addSetting(ignoreInvisible);
        addSetting(ignoreFriends);
        addSetting(ignoreCreative);
    }

    @Override
    protected void onDisable() {
        timer = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null
                || client.gui.screen() != null || !client.player.isAlive()) {
            return;
        }
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            return;
        }
        if (weaponsOnly.isEnabled() && !AuraUtil.isWeapon(client.player.getMainHandItem())) {
            return;
        }
        Entity entity = client.crosshairPickEntity;
        if (!entityCheck(client, entity)) {
            return;
        }
        if (AuraUtil.hitboxDistance(client.player, entity) > range.getValue()) {
            return;
        }
        if (!client.player.isWithinAttackRange(client.player.getMainHandItem(),
                entity.getBoundingBox(), 3.0)) {
            return;
        }
        if (noCooldown.isEnabled()) {
            if (timer > 0) {
                timer--;
                return;
            }
        } else if (client.player.getAttackStrengthScale(0.5f) < 1) {
            return;
        }
        if (critSpoof.isEnabled()) {
            AuraUtil.critSpoof(client);
        }
        client.gameMode.attack(client.player, entity);
        if (noCooldown.isEnabled()) {
            client.player.swing(InteractionHand.MAIN_HAND, false);
            AuraUtil.refill(client.player);
            timer = delay.getInt();
        } else {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private boolean entityCheck(Minecraft client, Entity entity) {
        if (entity == null || entity == client.player || entity == client.getCameraEntity()) {
            return false;
        }
        if (!entity.isAlive() || !(entity instanceof LivingEntity living)) {
            return false;
        }
        if (living.isDeadOrDying() || ignoreInvisible.isEnabled() && entity.isInvisible()) {
            return false;
        }
        boolean isPlayer = entity instanceof Player;
        if (targets.getValue().equals("Players") && !isPlayer) {
            return false;
        }
        if (targets.getValue().equals("Mobs") && isPlayer) {
            return false;
        }
        if (isPlayer) {
            Player other = (Player) entity;
            if (other.isSpectator()) {
                return false;
            }
            if (ignoreCreative.isEnabled() && other.isCreative()) {
                return false;
            }
            if (ignoreFriends.isEnabled() && Friends.isFriend(other.getName().getString())) {
                return false;
            }
            if (shieldMode.getValue().equals("Ignore") && other.isBlocking()) {
                return false;
            }
        }
        return true;
    }
}
