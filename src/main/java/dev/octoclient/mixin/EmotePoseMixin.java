package dev.octoclient.mixin;

import dev.octoclient.module.utility.Emotes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public class EmotePoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
            at = @At("TAIL"))
    private void octoclient$applyEmote(AvatarRenderState state, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            Emotes.applyPose((PlayerModel) (Object) this, state, player.getId());
        }
    }
}
