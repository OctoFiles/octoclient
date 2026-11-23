package dev.octoclient.mixin;

import dev.octoclient.render.SkinAtlasManager;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarSkinAtlasMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$useSkinAtlas(AvatarRenderState state, CallbackInfoReturnable<Identifier> cir) {
        if (state.skin == null || state.skin.body() == null) {
            return;
        }
        Identifier atlas = SkinAtlasManager.texture(state.skin.body().texturePath());
        if (atlas != null) {
            cir.setReturnValue(atlas);
        }
    }
}
