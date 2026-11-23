package dev.octoclient.module.secret;

import dev.octoclient.mixin.LivingEntityAttackStrengthAccessor;
import dev.octoclient.module.hud.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Shared helpers for the combat and explosion modules: target selection,
 * aiming, hotbar handling, block placement and a vanilla-style explosion
 * damage estimate.
 */
final class AuraUtil {
    private AuraUtil() {
    }

    /* ------------------------------------------------------------------ */
    /* Targets                                                            */
    /* ------------------------------------------------------------------ */

    /** Living targets in reach, closest first. */
    static List<LivingEntity> targets(Minecraft client, double range, String mode,
                                      boolean ignoreFriends, boolean ignoreCreative) {
        List<LivingEntity> found = new ArrayList<>();
        Player player = client.player;
        if (player == null || client.level == null) {
            return found;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (entity == player || entity == client.getCameraEntity()) {
                continue;
            }
            if (!living.isAlive() || living.isDeadOrDying() || living.isInvisible()) {
                continue;
            }
            if (living instanceof Player other) {
                if (other.isSpectator() || other.isCreative() && ignoreCreative) {
                    continue;
                }
                if (ignoreFriends && Friends.isFriend(other.getName().getString())) {
                    continue;
                }
            }
            if (!mode.equals("All")) {
                boolean isPlayer = living instanceof Player;
                if (mode.equals("Players") != isPlayer) {
                    continue;
                }
                if (mode.equals("Mobs") && isPlayer) {
                    continue;
                }
            }
            if (hitboxDistance(player, entity) > range) {
                continue;
            }
            found.add(living);
        }
        found.sort(Comparator.comparingDouble(e -> player.distanceToSqr(e)));
        return found;
    }

    /** Closest living target, or {@code null}. */
    static LivingEntity target(Minecraft client, double range, String mode,
                               boolean ignoreFriends, boolean ignoreCreative) {
        List<LivingEntity> list = targets(client, range, mode, ignoreFriends, ignoreCreative);
        return list.isEmpty() ? null : list.getFirst();
    }

    /** Distance from the player's eyes to the closest point of the hitbox. */
    static double hitboxDistance(Player player, Entity entity) {
        AABB box = entity.getBoundingBox();
        double cx = Math.max(box.minX, Math.min(player.getX(), box.maxX));
        double cy = Math.max(box.minY, Math.min(player.getY(), box.maxY));
        double cz = Math.max(box.minZ, Math.min(player.getZ(), box.maxZ));
        return Math.sqrt(player.distanceToSqr(cx, cy, cz));
    }

    /* ------------------------------------------------------------------ */
    /* Aiming                                                             */
    /* ------------------------------------------------------------------ */

    static double angleTo(Player player, Entity entity) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = entity.getBoundingBox().getCenter();
        Vec3 look = player.getLookAngle();
        Vec3 diff = center.subtract(eye).normalize();
        return Math.acos(Math.max(-1, Math.min(1, look.dot(diff))));
    }

    static void lookAt(Player player, Entity target) {
        lookAt(player, target.getBoundingBox().getCenter());
    }

    static void lookAt(Player player, BlockPos pos) {
        lookAt(player, Vec3.atCenterOf(pos));
    }

    static void lookAt(Player player, Vec3 point) {
        Vec3 eye = player.getEyePosition();
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);
    }

    /** Rotates only part of the way towards the point, for AimAssist. */
    static void smoothLookAt(Player player, Vec3 point, double strength, boolean vertical) {
        Vec3 eye = player.getEyePosition();
        double dx = point.x - eye.x;
        double dy = point.y - eye.y;
        double dz = point.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        float yaw = player.getYRot() + wrapDegrees(targetYaw - player.getYRot()) * (float) strength;
        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        if (vertical) {
            float pitch = player.getXRot()
                    + (targetPitch - player.getXRot()) * (float) strength;
            player.setXRot(Math.max(-90, Math.min(90, pitch)));
        }
    }

    private static float wrapDegrees(float degrees) {
        float d = degrees % 360.0f;
        if (d >= 180.0f) {
            d -= 360.0f;
        }
        if (d < -180.0f) {
            d += 360.0f;
        }
        return d;
    }

    /* ------------------------------------------------------------------ */
    /* Hotbar                                                             */
    /* ------------------------------------------------------------------ */

    static int slotOf(Minecraft client, Item item) {
        return slotOf(client, stack -> !stack.isEmpty() && stack.is(item));
    }

    static int slotOf(Minecraft client, Predicate<ItemStack> predicate) {
        if (client.player == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; slot++) {
            if (predicate.test(client.player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    static int emptySlot(Minecraft client) {
        return slotOf(client, ItemStack::isEmpty);
    }

    static boolean holding(Player player, Item item) {
        return player != null && !player.getMainHandItem().isEmpty()
                && player.getMainHandItem().is(item);
    }

    static String pathOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    static boolean isTotem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING);
    }

    static boolean isSword(ItemStack stack) {
        return pathOf(stack).endsWith("_sword");
    }

    static boolean isAxe(ItemStack stack) {
        return pathOf(stack).endsWith("_axe");
    }

    static boolean isSpear(ItemStack stack) {
        String path = pathOf(stack);
        return path.equals("trident") || path.endsWith("_spear");
    }

    static boolean isWeapon(ItemStack stack) {
        return isSword(stack) || isAxe(stack) || isSpear(stack) || pathOf(stack).equals("mace");
    }

    /** Rough weapon ranking used by AutoWeapon and the auras. */
    static int weaponScore(ItemStack stack) {
        String path = pathOf(stack);
        if (path.isEmpty()) {
            return 0;
        }
        int score;
        if (path.endsWith("_sword")) {
            score = 30;
        } else if (path.endsWith("_axe")) {
            score = 20;
        } else if (path.equals("mace")) {
            score = 40;
        } else if (path.equals("trident") || path.endsWith("_spear")) {
            score = 25;
        } else {
            return 0;
        }
        if (path.startsWith("netherite")) {
            score += 6;
        } else if (path.startsWith("diamond")) {
            score += 5;
        } else if (path.startsWith("iron")) {
            score += 4;
        } else if (path.startsWith("golden") || path.startsWith("gold")) {
            score += 3;
        } else if (path.startsWith("stone")) {
            score += 2;
        } else if (path.startsWith("wooden") || path.startsWith("wood")) {
            score += 1;
        }
        return score;
    }

    /** Best weapon slot in the hotbar, or -1. */
    static int bestWeaponSlot(Minecraft client) {
        int best = -1;
        int bestScore = 0;
        if (client.player == null) {
            return -1;
        }
        for (int slot = 0; slot < 9; slot++) {
            int score = weaponScore(client.player.getInventory().getItem(slot));
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    static void refill(Player player) {
        ((LivingEntityAttackStrengthAccessor) player).octoclient$setAttackStrengthTicker(100);
    }

    /**
     * Spoofs a tiny fall right before an attack, so the server counts the hit
     * as a critical hit. Same packet trick as the Criticals module.
     */
    static void critSpoof(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.getConnection() == null || player.isFallFlying()) {
            return;
        }
        if (!player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable()) {
            return;
        }
        sendY(client, 0.0625);
        sendY(client, 0);
    }

    static void sendY(Minecraft client, double offset) {
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        client.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                client.player.getX(), client.player.getY() + offset, client.player.getZ(), false, false));
    }

    /* ------------------------------------------------------------------ */
    /* Blocks                                                             */
    /* ------------------------------------------------------------------ */

    static boolean replaceable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    static boolean solid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }

    /** Right-clicks a block (anchor, crystal base, ...) with the held item. */
    static boolean useOn(Minecraft client, BlockPos pos) {
        if (client.player == null || client.gameMode == null) {
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        return true;
    }

    /**
     * Places the held block/item against the given face of the support block.
     * The item has to be in the main hand already.
     */
    static boolean placeOn(Minecraft client, BlockPos pos, Direction face) {
        if (client.player == null || client.gameMode == null || client.level == null) {
            return false;
        }
        if (!replaceable(client.level, pos)) {
            return false;
        }
        BlockPos support = pos.relative(face.getOpposite());
        if (!solid(client.level, support)) {
            return false;
        }
        Vec3 hitVec = Vec3.atCenterOf(support).add(
                face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(hitVec, face, support, false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        client.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    /* ------------------------------------------------------------------ */
    /* Explosion estimate                                                 */
    /* ------------------------------------------------------------------ */

    /**
     * Vanilla explosion damage approximation without the block-density ray
     * check. Good enough to rank anchor and crystal spots.
     *
     * @param power blast power (anchor 5, crystal 6)
     */
    static float explosionDamage(double power, Vec3 center, Entity entity) {
        return explosionDamage(power, center, entity.getEyePosition());
    }

    static float explosionDamage(double power, Vec3 center, Vec3 point) {
        double distance = point.distanceTo(center) / (power * 2.0);
        if (distance > 1.0) {
            return 0;
        }
        double impact = 1.0 - distance;
        return (float) (((impact * impact + impact) / 2.0) * 7.0 * power + 1.0);
    }

    static Vec3 predicted(Entity entity, int ticks) {
        Vec3 delta = entity.getDeltaMovement();
        return entity.position().add(delta.x * ticks, delta.y * ticks, delta.z * ticks);
    }

    /** Eye position the entity is expected to have in {@code ticks} ticks. */
    static Vec3 predictedEye(Entity entity, int ticks) {
        return predicted(entity, ticks).add(entity.getEyePosition().subtract(entity.position()));
    }
}
