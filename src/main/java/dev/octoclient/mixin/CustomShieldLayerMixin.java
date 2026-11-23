package dev.octoclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.render.ShieldCosmetics;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla shield special model with the selected catalog mesh. The
 * item render state has already applied the item transform by this point, so the
 * catalog mesh ends up in exactly the same hand pose as the shield it replaces,
 * in both first and third person.
 */
@Mixin(targets = "net.minecraft.client.renderer.item.ItemStackRenderState$LayerRenderState")
public class CustomShieldLayerMixin {
    @Shadow
    private SpecialModelRenderer specialRenderer;

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;applyTransform(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void octoclient$submitCustomShield(PoseStack pose, SubmitNodeCollector collector, int light,
                                               int overlay, int tint, CallbackInfo ci) {
        boolean customShield = ShieldCosmetics.submitFirstPersonShield(pose, collector, light);
        if (!customShield) {
            // Third-person: suppress vanilla shield layer; catalog layer renders
            // the custom shield directly on the arm.
            customShield = ShieldCosmetics.submitThirdPersonShield(specialRenderer, pose, collector, light);
        }
        if (customShield) {
            // The original method pushed this pose before the injection point and
            // would normally pop it on return; cancelling skips that pop.
            pose.popPose();
            ci.cancel();
        }
    }
}
