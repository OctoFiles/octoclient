package dev.octoclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.module.render.LowFire;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class LowFireMixin {
    @Inject(method = "submitFire", at = @At("HEAD"))
    private static void octoclient$lowerFire(PoseStack pose, SubmitNodeCollector collector,
                                             TextureAtlasSprite sprite, CallbackInfo ci) {
        if (LowFire.active()) {
            pose.pushPose();
            pose.translate(0, -LowFire.lowerAmount(), 0);
        }
    }

    @Inject(method = "submitFire", at = @At("RETURN"))
    private static void octoclient$restoreFire(PoseStack pose, SubmitNodeCollector collector,
                                               TextureAtlasSprite sprite, CallbackInfo ci) {
        if (LowFire.active()) {
            pose.popPose();
        }
    }

    @ModifyArg(method = "buildFireQuad",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ScreenEffectRenderer;buildSpriteQuad(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFFFFI)V"),
            index = 8)
    private static int octoclient$adjustFireOpacity(int color) {
        if (!LowFire.active()) {
            return color;
        }
        int alpha = (color >>> 24) & 0xFF;
        int adjustedAlpha = alpha * LowFire.opacityPercent() / 100;
        return (color & 0x00FFFFFF) | (adjustedAlpha << 24);
    }
}
