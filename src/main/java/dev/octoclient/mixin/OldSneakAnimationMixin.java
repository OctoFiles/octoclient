package dev.octoclient.mixin;

import dev.octoclient.module.render.OldAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class OldSneakAnimationMixin {
    @Shadow
    @Final
    public ModelPart body;

    @Shadow
    @Final
    public ModelPart head;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
            at = @At("RETURN"))
    private void octoclient$flattenLegacySneak(HumanoidRenderState state, CallbackInfo ci) {
        if (OldAnimations.oldSneakAnimationEnabled() && state.isCrouching) {
            body.xRot = 0f;
            head.y = 0f;
        }
    }
}
