package dev.octoclient.mixin;

import dev.octoclient.module.render.BadOptimization;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShadowFeatureRenderer.class)
public class BadOptimizationMixin {
    @Inject(method = "buildGroup", at = @At("HEAD"), cancellable = true)
    private void octoclient$skipShadowBuild(CallbackInfo ci) {
        if (BadOptimization.hidesShadows()) {
            ci.cancel();
        }
    }
}
