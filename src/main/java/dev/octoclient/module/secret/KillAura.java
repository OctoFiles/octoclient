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
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Port of Meteor's KillAura with the same setting names and defaults. */
public class KillAura extends Module implements Module.Tickable {
    private final ModeSetting attackWhenHolding = new ModeSetting("attack-when-holding",
            "Which items allow you to attack.", "Weapons", List.of("Weapons", "All"));
    private final ModeSetting rotate = new ModeSetting("rotate",
            "Rotates towards the target.", "Always", List.of("Always", "OnHit", "None"));
    private final BooleanSetting autoSwitch = new BooleanSetting("auto-switch",
            "Automatically swaps to a weapon before attacking.", false);
    private final BooleanSetting swapBack = new BooleanSetting("swap-back",
            "Swaps back to the previous slot after attacking.", false);
    private final ModeSetting shieldMode = new ModeSetting("shield-mode",
            "What to do when your target is blocking.", "None", List.of("Ignore", "Break", "None"));
    private final BooleanSetting onlyOnClick = new BooleanSetting("only-on-click",
            "Only attacks when you are holding down the attack key.", false);
    private final BooleanSetting onlyOnLook = new BooleanSetting("only-on-look",
            "Only attacks the entity you are looking at.", false);
    private final ModeSetting entities = new ModeSetting("entities",
            "Which entities to attack.", "Players", List.of("Players", "Mobs", "All"));
    private final ModeSetting priority = new ModeSetting("priority",
            "How targets are sorted.", "ClosestAngle", List.of("ClosestAngle", "Closest", "LowestHealth"));
    private final NumberSetting maxTargets = new NumberSetting("max-targets",
            "The maximum number of targets.", 1, 1, 10, 1);
    private final NumberSetting range = new NumberSetting("range",
            "The maximum range of the attacks.", 4.5, 0, 6, 0.1);
    private final NumberSetting wallsRange = new NumberSetting("walls-range",
            "The maximum range through walls.", 3.5, 0, 6, 0.1);
    private final BooleanSetting ignoreNamed = new BooleanSetting("ignore-named",
            "Ignores entities with a custom name.", false);
    private final BooleanSetting ignorePassive = new BooleanSetting("ignore-passive",
            "Ignores passive mobs.", true);
    private final BooleanSetting ignoreTamed = new BooleanSetting("ignore-tamed",
            "Ignores tamed mobs.", false);
    private final BooleanSetting pauseOnUse = new BooleanSetting("pause-on-use",
            "Pauses while you are using an item.", false);
    private final BooleanSetting customDelay = new BooleanSetting("custom-delay",
            "Uses a custom hit delay in ticks.", false);
    private final NumberSetting hitDelay = new NumberSetting("hit-delay",
            "The delay between hits, in ticks.", 11, 0, 20, 1);
    private final BooleanSetting noCooldown = new BooleanSetting("no-cooldown",
            "Sword hits ignore the attack cooldown and always deal full damage. "
                    + "The rate is set by hit-delay; 0 hits every tick.", false);
    private final BooleanSetting todesDamage = new BooleanSetting("todes-damage",
            "Spoofs a critical hit on every sword attack for extra 'Todes Damage'.", false);

    private final List<Entity> targets = new ArrayList<>();
    private int hitTimer;
    private int previousSlot = -1;
    private boolean swapped;

    public KillAura() {
        super("KillAura", "Automatically attacks entities around you.", 'K');
        addSetting(attackWhenHolding);
        addSetting(rotate);
        addSetting(autoSwitch);
        addSetting(swapBack);
        addSetting(shieldMode);
        addSetting(onlyOnClick);
        addSetting(onlyOnLook);
        addSetting(entities);
        addSetting(priority);
        addSetting(maxTargets);
        addSetting(range);
        addSetting(wallsRange);
        addSetting(ignoreNamed);
        addSetting(ignorePassive);
        addSetting(ignoreTamed);
        addSetting(pauseOnUse);
        addSetting(customDelay);
        addSetting(hitDelay);
        addSetting(noCooldown);
        addSetting(todesDamage);
    }

    @Override
    protected void onDisable() {
        targets.clear();
        stopAttacking();
        hitTimer = 0;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null || !client.player.isAlive()
                || client.player.isSpectator()) {
            stopAttacking();
            return;
        }
        if (pauseOnUse.isEnabled() && (client.gameMode.isDestroying() || client.player.isUsingItem())) {
            stopAttacking();
            return;
        }
        if (onlyOnClick.isEnabled() && !client.options.keyAttack.isDown()) {
            stopAttacking();
            return;
        }
        if (onlyOnLook.isEnabled()) {
            if (!(client.hitResult instanceof EntityHitResult hit) || !entityCheck(client, hit.getEntity())) {
                stopAttacking();
                return;
            }
            targets.clear();
            targets.add(hit.getEntity());
        } else {
            targets.clear();
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entityCheck(client, entity)) {
                    targets.add(entity);
                }
            }
            Comparator<Entity> comparator = switch (priority.getValue()) {
                case "Closest" -> Comparator.comparingDouble(e -> client.player.distanceToSqr(e));
                case "LowestHealth" -> Comparator.comparingDouble(e ->
                        e instanceof LivingEntity living ? living.getHealth() : Double.MAX_VALUE);
                default -> Comparator.comparingDouble(e -> angleTo(client.player, e));
            };
            targets.sort(comparator);
            while (targets.size() > maxTargets.getInt()) {
                targets.remove(targets.size() - 1);
            }
        }
        if (targets.isEmpty()) {
            stopAttacking();
            return;
        }

        Entity primary = targets.getFirst();
        if (autoSwitch.isEnabled()) {
            int weapon = findWeapon(client);
            if (weapon >= 0) {
                if (!swapped) {
                    previousSlot = client.player.getInventory().getSelectedSlot();
                    swapped = true;
                }
                client.player.getInventory().setSelectedSlot(weapon);
            }
        }
        if (attackWhenHolding.getValue().equals("Weapons") && !isWeapon(client.player.getMainHandItem())) {
            stopAttacking();
            return;
        }
        if (rotate.getValue().equals("Always")) {
            lookAt(client.player, primary);
        }
        if (delayCheck(client)) {
            boolean sword = AuraUtil.isSword(client.player.getMainHandItem());
            for (Entity target : new ArrayList<>(targets)) {
                if (rotate.getValue().equals("OnHit")) {
                    lookAt(client.player, target);
                }
                if (todesDamage.isEnabled() && sword) {
                    AuraUtil.critSpoof(client);
                }
                client.gameMode.attack(client.player, target);
                if (noCooldown.isEnabled()) {
                    // Local swing only: the swing packet would reset the
                    // server-side charge and turn the next hit into spam damage.
                    client.player.swing(InteractionHand.MAIN_HAND, false);
                    AuraUtil.refill(client.player);
                } else {
                    client.player.swing(InteractionHand.MAIN_HAND);
                }
                hitTimer = 0;
            }
        }
    }

    private void stopAttacking() {
        if (swapBack.isEnabled() && swapped && previousSlot >= 0 && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.getInventory().setSelectedSlot(previousSlot);
        }
        swapped = false;
    }

    private boolean delayCheck(Minecraft client) {
        if (noCooldown.isEnabled()) {
            // No charge requirement at all: hit-delay alone decides the rate.
            if (hitTimer < hitDelay.getInt()) {
                hitTimer++;
                return false;
            }
            return true;
        }
        if (!customDelay.isEnabled()) {
            return client.player.getAttackStrengthScale(0.5f) >= 1;
        }
        if (hitTimer < hitDelay.getInt()) {
            hitTimer++;
            return false;
        }
        return true;
    }

    private boolean entityCheck(Minecraft client, Entity entity) {
        if (entity == client.player || entity == client.getCameraEntity()) {
            return false;
        }
        if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        AABB box = entity.getBoundingBox();
        double cx = Math.max(box.minX, Math.min(client.player.getX(), box.maxX));
        double cy = Math.max(box.minY, Math.min(client.player.getY(), box.maxY));
        double cz = Math.max(box.minZ, Math.min(client.player.getZ(), box.maxZ));
        if (client.player.distanceToSqr(cx, cy, cz) > range.getValue() * range.getValue()) {
            return false;
        }
        boolean isPlayer = entity instanceof Player;
        if (entities.getValue().equals("Players") && !isPlayer) {
            return false;
        }
        if (entities.getValue().equals("Mobs") && isPlayer) {
            return false;
        }
        if (ignoreNamed.isEnabled() && entity.hasCustomName()) {
            return false;
        }
        if (!client.player.hasLineOfSight(entity)
                && client.player.distanceToSqr(entity) > wallsRange.getValue() * wallsRange.getValue()) {
            return false;
        }
        if (ignoreTamed.isEnabled() && entity instanceof OwnableEntity ownable
                && ownable.getOwner() != null && ownable.getOwner() == client.player) {
            return false;
        }
        if (isPlayer) {
            Player player = (Player) entity;
            if (player.isCreative()) {
                return false;
            }
            if (Friends.isFriend(player.getName().getString())) {
                return false;
            }
            if (shieldMode.getValue().equals("Ignore") && player.isBlocking()) {
                return false;
            }
        } else if (ignorePassive.isEnabled() && !(entity instanceof net.minecraft.world.entity.monster.Enemy)) {
            return false;
        }
        return true;
    }

    private static double angleTo(Player player, Entity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = entity.getBoundingBox().getCenter();
        Vec3 look = player.getLookAngle();
        Vec3 diff = center.subtract(eye).normalize();
        return Math.acos(Math.max(-1, Math.min(1, look.dot(diff))));
    }

    private int findWeapon(Minecraft client) {
        for (int slot = 0; slot < 9; slot++) {
            if (isWeapon(client.player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isWeapon(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(stack.getItem()).getPath();
        return path.endsWith("_sword") || path.endsWith("_axe") || path.equals("trident")
                || path.equals("mace") || path.endsWith("_spear");
    }

    private static void lookAt(Player player, Entity target) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = target.getBoundingBox().getCenter();
        double dx = center.x - eye.x;
        double dy = center.y - eye.y;
        double dz = center.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
    }
}
