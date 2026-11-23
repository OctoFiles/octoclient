package dev.octoclient.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import dev.octoclient.module.render.GlintColorizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net.minecraft.client.renderer.feature.ItemFeatureRenderer")
public class GlintColorizerMixin {
    @ModifyArg(method = "prepareFoilSubmit",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBakedQuad(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"),
            index = 2)
    private QuadInstance octoclient$recolorGlint(QuadInstance quad) {
        if (GlintColorizer.active()) {
            quad.setColor(GlintColorizer.color());
        }
        return quad;
    }
}
