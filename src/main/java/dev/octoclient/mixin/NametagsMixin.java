package dev.octoclient.mixin;

import dev.octoclient.module.hud.SpotifyOverlay;
import dev.octoclient.module.cosmetics.ColoredName;
import dev.octoclient.module.cosmetics.NameText;
import dev.octoclient.module.cosmetics.NameTextRenderState;
import dev.octoclient.module.render.Nametags;
import dev.octoclient.util.NameBadges;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class NametagsMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$showConfiguredNames(LivingEntity entity, double distanceToCameraSq,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (Nametags.shouldShow(entity, distanceToCameraSq) || SpotifyOverlay.hasWorldTrackText(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void octoclient$customizeName(LivingEntity entity, LivingEntityRenderState state, float tickDelta,
                                          CallbackInfo ci) {
        if (entity instanceof Player player) {
            var worldTrackText = SpotifyOverlay.worldTrackText(player);
            if (worldTrackText != null) {
                state.nameTag = worldTrackText;
                ((NameTextRenderState) state).octoclient$setNameText(null, true);
            } else {
                state.nameTag = ColoredName.apply(player, state.nameTag);
                state.nameTag = Nametags.customize(player, state.nameTag);
                state.nameTag = NameBadges.prefix(player.getUUID(), player.getName().getString(),
                        state.nameTag);
                // Optional nametag animation (Nametags module setting); the
                // offset lives on the attachment so the vanilla renderer, the
                // badge and the extra name text all move together.
                Vec3 motion = Nametags.animationOffset(player);
                if (motion != null && state.nameTagAttachment != null) {
                    state.nameTagAttachment = state.nameTagAttachment.add(motion.x, motion.y, motion.z);
                }
                var nameText = NameText.renderTextFor(player);
                boolean nameTextAbove = NameText.isAbove();
                ((NameTextRenderState) state).octoclient$setNameText(nameText, nameTextAbove);
                if (nameText != null && !nameTextAbove && state.nameTagAttachment != null) {
                    state.nameTagAttachment = state.nameTagAttachment.add(0.0, 0.1, 0.0);
                }
            }
        }
    }
}
