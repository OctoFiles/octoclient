package dev.octoclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.render.ShieldCosmetics;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tracks the player whose model is being submitted, so a selected catalog
 * shield can suppress the vanilla shield held in third person. The replacement
 * mesh itself is drawn by the catalog render layer on the same arm.
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityShieldMixin {
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void octoclient$beginShieldEntity(LivingEntityRenderState state, PoseStack pose,
                                              SubmitNodeCollector collector, CameraRenderState camera,
                                              CallbackInfo ci) {
        if (state instanceof AvatarRenderState avatar) {
            ShieldCosmetics.beginEntityRender(avatar);
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void octoclient$endShieldEntity(LivingEntityRenderState state, PoseStack pose,
                                            SubmitNodeCollector collector, CameraRenderState camera,
                                            CallbackInfo ci) {
        ShieldCosmetics.endEntityRender();
    }
}
