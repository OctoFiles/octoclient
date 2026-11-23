package dev.octoclient.mixin;

import dev.octoclient.module.render.HealthIndicator;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class HealthIndicatorMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$showHealthName(LivingEntity entity, double distanceToCameraSq,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (HealthIndicator.shouldShow(entity, distanceToCameraSq)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void octoclient$appendHealth(LivingEntity entity, LivingEntityRenderState state, float tickDelta,
                                         CallbackInfo ci) {
        net.minecraft.world.entity.Entity camera = net.minecraft.client.Minecraft.getInstance().getCameraEntity();
        if (camera != null && HealthIndicator.shouldShow(entity, entity.distanceToSqr(camera))) {
            state.nameTag = HealthIndicator.appendHealth(entity, state.nameTag);
        }
    }
}
