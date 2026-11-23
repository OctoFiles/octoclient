package dev.octoclient.mixin;

import dev.octoclient.render.SkinAtlasManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntitySkinAtlasMixin {
    @Unique
    private static final ThreadLocal<AvatarRenderState> octoclient$currentAvatar = new ThreadLocal<>();

    @Inject(method = "submit", at = @At("HEAD"))
    private void octoclient$captureAvatarState(LivingEntityRenderState state, PoseStack pose,
                                               SubmitNodeCollector collector, CameraRenderState camera,
                                               CallbackInfo ci) {
        octoclient$currentAvatar.set(state instanceof AvatarRenderState avatar ? avatar : null);
    }

    @ModifyArg(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
                    ordinal = 0),
            index = 7)
    private TextureAtlasSprite octoclient$remapSkinUvs(TextureAtlasSprite sprite) {
        AvatarRenderState state = octoclient$currentAvatar.get();
        if (state != null && state.skin != null && state.skin.body() != null) {
            TextureAtlasSprite atlasSprite = SkinAtlasManager.sprite(state.skin.body().texturePath());
            if (atlasSprite != null) {
                return atlasSprite;
            }
        }
        return sprite;
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void octoclient$clearAvatarState(LivingEntityRenderState state, PoseStack pose,
                                             SubmitNodeCollector collector, CameraRenderState camera,
                                             CallbackInfo ci) {
        octoclient$currentAvatar.remove();
    }
}
