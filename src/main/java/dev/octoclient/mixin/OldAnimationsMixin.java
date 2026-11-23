package dev.octoclient.mixin;

import com.mojang.math.Axis;
import dev.octoclient.module.render.OldAnimations;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public class OldAnimationsMixin {
    @Redirect(method = "submitArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void octoclient$legacySwordBlock(ItemInHandRenderer renderer, LivingEntity entity, ItemStack stack,
                                             ItemDisplayContext context, PoseStack pose,
                                             SubmitNodeCollector collector, int packedLight) {
        boolean blockingPose = OldAnimations.shouldUseLegacyBlockPose(entity, stack);
        if (!blockingPose) {
            OldAnimations.applyFirstPersonPose(entity, stack, context, pose);
            renderer.renderItem(entity, stack, context, pose, collector, packedLight);
            return;
        }

        pose.pushPose();
        try {
            OldAnimations.applyFirstPersonPose(entity, stack, context, pose);
            pose.translate(0.0, 0.12, -0.08);
            pose.mulPose(Axis.XP.rotationDegrees(-35f));
            pose.mulPose(Axis.YP.rotationDegrees(-18f));
            renderer.renderItem(entity, stack, context, pose, collector, packedLight);
        } finally {
            pose.popPose();
        }
    }
}
