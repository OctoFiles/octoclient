package dev.octoclient.module.secret;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Respawn-anchor helpers shared by AnchorAura and the Anchor* feature modules.
 * Placement and detonation always go through the normal right-click packet, so
 * the server performs the real explosion and damage.
 */
final class AnchorUtil {
    /** Blast power of a charged respawn anchor. */
    static final double POWER = 5.0;
    /** Anchor with a support block below. */
    record Spot(BlockPos pos, Direction face) {
    }

    private AnchorUtil() {
    }

    static int charge(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() != Blocks.RESPAWN_ANCHOR) {
            return -1;
        }
        return state.getValue(RespawnAnchorBlock.CHARGE);
    }

    static boolean isAnchor(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR;
    }

    /** Highest-damage charged anchor near the target that is safe for us. */
    static BlockPos bestCharged(Minecraft client, Entity target, double range,
                                double minDamage, double maxSelf, int predict) {
        Level level = client.level;
        if (level == null || client.player == null) {
            return null;
        }
        BlockPos center = target.blockPosition();
        BlockPos best = null;
        double bestDamage = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - 4, center.getY() - 3, center.getZ() - 4,
                center.getX() + 4, center.getY() + 3, center.getZ() + 4)) {
            if (!isAnchor(level, pos) || charge(level, pos) <= 0) {
                continue;
            }
            if (!inRange(client, pos, range)) {
                continue;
            }
            double damage = damageTo(target, pos, predict);
            if (damage < minDamage || damage <= bestDamage) {
                continue;
            }
            if (selfDamage(client, pos) > maxSelf) {
                continue;
            }
            best = pos.immutable();
            bestDamage = damage;
        }
        return best;
    }

    /** Closest anchor near the target that still needs glowstone. */
    static BlockPos bestUncharged(Minecraft client, Entity target, double range) {
        Level level = client.level;
        if (level == null) {
            return null;
        }
        BlockPos center = target.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.getX() - 4, center.getY() - 3, center.getZ() - 4,
                center.getX() + 4, center.getY() + 3, center.getZ() + 4)) {
            int charge = charge(level, pos);
            if (charge < 0 || charge >= RespawnAnchorBlock.MAX_CHARGES) {
                continue;
            }
            if (!inRange(client, pos, range)) {
                continue;
            }
            double distance = target.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos.immutable();
            }
        }
        return best;
    }

    /** Best safe anchor placement spot around the target. */
    static Spot findSpot(Minecraft client, Entity target, double range,
                         double minDamage, double maxSelf, int predict) {
        Level level = client.level;
        if (level == null || client.player == null) {
            return null;
        }
        BlockPos center = target.blockPosition();
        Spot best = null;
        double bestDamage = 0;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!AuraUtil.replaceable(level, pos) || !inRange(client, pos, range)) {
                        continue;
                    }
                    Direction face = supportFace(level, pos);
                    if (face == null) {
                        continue;
                    }
                    if (selfDamage(client, pos) > maxSelf) {
                        continue;
                    }
                    double damage = damageTo(target, pos, predict);
                    if (damage < minDamage) {
                        continue;
                    }
                    // Prefer the strongest hit, then the closest spot.
                    if (damage > bestDamage + 0.01
                            || Math.abs(damage - bestDamage) <= 0.01 && best != null
                            && target.distanceToSqr(Vec3.atCenterOf(pos))
                            < target.distanceToSqr(Vec3.atCenterOf(best.pos))) {
                        bestDamage = damage;
                        best = new Spot(pos.immutable(), face);
                    }
                }
            }
        }
        return best;
    }

    /** Free blocks next to the target's feet, used by the surround features. */
    static List<Spot> surroundSpots(Minecraft client, Entity target, double range) {
        Level level = client.level;
        List<Spot> spots = new ArrayList<>();
        if (level == null) {
            return spots;
        }
        BlockPos feet = target.blockPosition();
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] offset : offsets) {
            for (int dy = 0; dy <= 1; dy++) {
                BlockPos pos = feet.offset(offset[0], dy, offset[1]);
                if (!AuraUtil.replaceable(level, pos) || !inRange(client, pos, range)) {
                    continue;
                }
                Direction face = supportFace(level, pos);
                if (face != null) {
                    spots.add(new Spot(pos.immutable(), face));
                    break;
                }
            }
        }
        spots.sort(Comparator.comparingDouble(s -> s.pos().distSqr(feet)));
        return spots;
    }

    /** Spot above the target's head for the trap features. */
    static Spot trapSpot(Minecraft client, Entity target, double range) {
        Level level = client.level;
        if (level == null) {
            return null;
        }
        BlockPos head = target.blockPosition().above(2);
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos pos = head.above(dy);
            if (!AuraUtil.replaceable(level, pos) || !inRange(client, pos, range)) {
                continue;
            }
            Direction face = supportFace(level, pos);
            if (face != null) {
                return new Spot(pos.immutable(), face);
            }
        }
        return null;
    }

    /** Direction from a solid neighbour to {@code pos}, floor first. */
    static Direction supportFace(Level level, BlockPos pos) {
        if (AuraUtil.solid(level, pos.below())) {
            return Direction.UP;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (AuraUtil.solid(level, pos.relative(direction))) {
                return direction.getOpposite();
            }
        }
        return null;
    }

    static boolean place(Minecraft client, HotbarSwap swap, Spot spot) {
        return placeWith(client, swap, Items.RESPAWN_ANCHOR, spot);
    }

    /** Places any block item from the hotbar on the spot's support face. */
    static boolean placeWith(Minecraft client, HotbarSwap swap, net.minecraft.world.item.Item item, Spot spot) {
        if (spot == null || !swap.ensure(client, item)) {
            return false;
        }
        AuraUtil.lookAt(client.player, spot.pos());
        return AuraUtil.placeOn(client, spot.pos(), spot.face());
    }

    static boolean chargeAnchor(Minecraft client, HotbarSwap swap, BlockPos pos) {
        if (pos == null || !swap.ensure(client, Items.GLOWSTONE)) {
            return false;
        }
        AuraUtil.lookAt(client.player, pos);
        return AuraUtil.useOn(client, pos);
    }

    static boolean detonate(Minecraft client, HotbarSwap swap, BlockPos pos) {
        if (pos == null) {
            return false;
        }
        // Glowstone would charge a partially filled anchor instead of setting
        // it off, so hold an empty slot (or any unrelated item) for the click.
        boolean detonator = swap.ensure(client, stack -> stack.isEmpty()
                || !stack.is(Items.GLOWSTONE) && !stack.is(Items.RESPAWN_ANCHOR));
        if (!detonator) {
            return false;
        }
        AuraUtil.lookAt(client.player, pos);
        return AuraUtil.useOn(client, pos);
    }

    static boolean hasAnchor(Minecraft client) {
        return AuraUtil.slotOf(client, Items.RESPAWN_ANCHOR) >= 0;
    }

    static boolean hasGlowstone(Minecraft client) {
        return AuraUtil.slotOf(client, Items.GLOWSTONE) >= 0;
    }

    static boolean inRange(Minecraft client, BlockPos pos, double range) {
        return client.player != null && client.player.distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    static double damageTo(Entity target, BlockPos pos, int predict) {
        return AuraUtil.explosionDamage(POWER, Vec3.atCenterOf(pos),
                AuraUtil.predictedEye(target, Math.max(0, predict)));
    }

    static double selfDamage(Minecraft client, BlockPos pos) {
        if (client.player == null) {
            return 0;
        }
        return AuraUtil.explosionDamage(POWER, Vec3.atCenterOf(pos), client.player.getEyePosition());
    }

    /** True when the anchor item is currently in the main hand. */
    static boolean holdingAnchor(Minecraft client) {
        return client.player != null && !client.player.getMainHandItem().isEmpty()
                && client.player.getMainHandItem().is(Items.RESPAWN_ANCHOR);
    }
}
