package dev.octoclient.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import dev.octoclient.module.render.BadOptimization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPass.class)
public abstract class RenderPassBindSkipMixin {
    @Unique
    private RenderPipeline octoclient$lastPipeline;

    @Inject(method = "setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$skipRepeatedPipelineBind(RenderPipeline pipeline, CallbackInfo ci) {
        if (BadOptimization.skipsRepeatedRenderPassBinds() && octoclient$lastPipeline == pipeline) {
            ci.cancel();
            return;
        }
        octoclient$lastPipeline = pipeline;
    }
}
