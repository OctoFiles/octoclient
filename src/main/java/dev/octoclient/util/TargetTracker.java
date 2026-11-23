package dev.octoclient.util;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Remembers the last entity the player attacked, for the Target HUD. */
public final class TargetTracker {
    private static final TargetTracker INSTANCE = new TargetTracker();

    private LivingEntity target;
    private long lastAttackTime;

    private TargetTracker() {
    }

    public static TargetTracker get() {
        return INSTANCE;
    }

    public void init() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (entity instanceof LivingEntity living && entity != player) {
                target = living;
                lastAttackTime = System.currentTimeMillis();
            }
            return InteractionResult.PASS;
        });
    }

    /** Current target if still valid (alive and attacked within the last 5s). */
    public LivingEntity getTarget(Minecraft mc) {
        if (target == null) {
            return null;
        }
        if (!target.isAlive() || target.isRemoved() || System.currentTimeMillis() - lastAttackTime > 5000L) {
            target = null;
            return null;
        }
        return target;
    }
}
