package dev.octoclient.mixin;

import dev.octoclient.render.LiquidGlass;
import dev.octoclient.module.render.OldAnimations;
import dev.octoclient.module.render.NoHurtCam;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Copies the finished world framebuffer into the glass backdrop right before
 * the batched GUI pass runs. At this point no render pass is open, which is the
 * only place a texture-to-texture copy is allowed.
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$damageAnimation(CameraRenderState camera, PoseStack pose, CallbackInfo ci) {
        if (NoHurtCam.shouldDisable() || !OldAnimations.damageAnimationEnabled()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V",
                    shift = At.Shift.BEFORE))
    private void octoclient$captureLiquidGlassBackdrop(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        LiquidGlass.captureBackdrop();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At("RETURN"))
    private void octoclient$devCapture(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        dev.octoclient.render.DevCapture.tick();
    }
}
