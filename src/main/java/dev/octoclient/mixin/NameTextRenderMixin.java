package dev.octoclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.module.cosmetics.NameTextRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class NameTextRenderMixin {
    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At("TAIL"))
    private void octoclient$submitNameText(EntityRenderState state, PoseStack poseStack,
                                           SubmitNodeCollector collector, CameraRenderState camera,
                                           int packedLight, CallbackInfo ci) {
        if (!(state instanceof NameTextRenderState nameTextState)) {
            return;
        }
        Component text = nameTextState.octoclient$getNameText();
        if (text == null || state.nameTagAttachment == null) {
            return;
        }

        double lineOffset = 9.0 * 1.15 * 0.025;
        double yOffset = nameTextState.octoclient$isNameTextAbove() ? lineOffset : -lineOffset;
        collector.submitNameTag(poseStack, state.nameTagAttachment.add(0.0, yOffset, 0.0), packedLight,
                text, !state.isDiscrete, state.lightCoords, camera);
    }
}
