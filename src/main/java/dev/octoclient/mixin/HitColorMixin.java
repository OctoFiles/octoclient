package dev.octoclient.mixin;

import dev.octoclient.module.render.HitColor;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class HitColorMixin {
    @Inject(method = "getModelTint(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)I",
            at = @At("RETURN"), cancellable = true)
    private void octoclient$customHitTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(HitColor.tint(state, cir.getReturnValue()));
    }
}
