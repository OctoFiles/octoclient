package dev.octoclient.module.secret;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * End-crystal helpers shared by CrystalAura and the Crystal* feature modules.
 * Crystals are placed with the normal right-click and broken with a normal
 * attack packet, so the server owns the explosion.
 */
final class CrystalUtil {
    /** Blast power of an end crystal. */
    static final double POWER = 6.0;

    private CrystalUtil() {
    }

    /** Crystals near the player, closest first. */
    static List<EndCrystal> crystals(Minecraft client, double range) {
        List<EndCrystal> found = new ArrayList<>();
        if (client.player == null || client.level == null) {
            return found;
        }
        for (EndCrystal crystal : client.level.getEntitiesOfClass(EndCrystal.class,
                client.player.getBoundingBox().inflate(range + 2))) {
            if (!crystal.isAlive()) {
                continue;
            }
            if (crystal.distanceToSqr(client.player) > range * range) {
                continue;
            }
            found.add(crystal);
        }
        found.sort(Comparator.comparingDouble(c -> c.distanceToSqr(client.player)));
        return found;
    }

    /** Crystal whose explosion hurts the target the most without hurting us. */
    static EndCrystal bestCrystal(Minecraft client, Entity target, double range,
                                  double minDamage, double maxSelf, int predict) {
        EndCrystal best = null;
        double bestDamage = 0;
        for (EndCrystal crystal : crystals(client, range)) {
            double damage = damageTo(target, crystal.position(), predict);
            if (damage < minDamage || damage <= bestDamage) {
                continue;
            }
            if (selfDamage(client, crystal.position()) > maxSelf) {
                continue;
            }
            best = crystal;
            bestDamage = damage;
        }
        return best;
    }

    /** Obsidian/bedrock block next to the target that has room for a crystal. */
    static BlockPos bestBase(Minecraft client, Entity target, double range,
                             double minDamage, double maxSelf, int predict) {
        Level level = client.level;
        if (level == null || client.player == null) {
            return null;
        }
        BlockPos center = target.blockPosition();
        BlockPos best = null;
        double bestDamage = 0;
        for (BlockPos base : BlockPos.betweenClosed(center.getX() - 3, center.getY() - 2, center.getZ() - 3,
                center.getX() + 3, center.getY() + 2, center.getZ() + 3)) {
            BlockState state = level.getBlockState(base);
            if (!isBase(state)) {
                continue;
            }
            BlockPos above = base.above();
            if (!AuraUtil.replaceable(level, above)) {
                continue;
            }
            if (!inRange(client, base, range)) {
                continue;
            }
            Vec3 blast = Vec3.atCenterOf(above);
            double damage = damageTo(target, blast, predict);
            if (damage < minDamage || damage <= bestDamage) {
                continue;
            }
            if (selfDamage(client, blast) > maxSelf) {
                continue;
            }
            best = base.immutable();
            bestDamage = damage;
        }
        return best;
    }

    static boolean isBase(BlockState state) {
        return state.getBlock() == Blocks.OBSIDIAN
                || state.getBlock() == Blocks.BEDROCK
                || state.getBlock() == Blocks.CRYING_OBSIDIAN;
    }

    static boolean place(Minecraft client, HotbarSwap swap, BlockPos base) {
        if (base == null || !swap.ensure(client, Items.END_CRYSTAL)) {
            return false;
        }
        AuraUtil.lookAt(client.player, base.above());
        return AuraUtil.useOn(client, base);
    }

    static boolean breakCrystal(Minecraft client, EndCrystal crystal) {
        if (crystal == null || client.player == null || client.gameMode == null) {
            return false;
        }
        AuraUtil.lookAt(client.player, crystal);
        client.gameMode.attack(client.player, crystal);
        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        return true;
    }

    static boolean hasCrystal(Minecraft client) {
        return AuraUtil.slotOf(client, Items.END_CRYSTAL) >= 0;
    }

    static boolean inRange(Minecraft client, BlockPos pos, double range) {
        return client.player != null && client.player.distanceToSqr(Vec3.atCenterOf(pos)) <= range * range;
    }

    static double damageTo(Entity target, Vec3 center, int predict) {
        return AuraUtil.explosionDamage(POWER, center,
                AuraUtil.predictedEye(target, Math.max(0, predict)));
    }

    static double selfDamage(Minecraft client, Vec3 center) {
        if (client.player == null) {
            return 0;
        }
        return AuraUtil.explosionDamage(POWER, center, client.player.getEyePosition());
    }
}
