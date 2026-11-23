package dev.octoclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.octoclient.module.render.ItemModel;
import dev.octoclient.module.render.SideShield;
import dev.octoclient.module.hud.ShieldStatusHud;
import dev.octoclient.render.ShieldCosmetics;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemModelMixin {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void octoclient$adjustFirstPersonItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                                  PoseStack pose, SubmitNodeCollector collector, int light,
                                                  CallbackInfo ci) {
        ShieldStatusHud.beginFirstPersonRender(entity, stack, context);
        ShieldCosmetics.beginItemRender(entity, stack, context);
        // Custom shields are replaced in the item render state layer so they keep
        // the vanilla hand pose in both first and third person. Canceling the
        // whole item render here would hide third-person shields entirely.
        boolean adjustModel = ItemModel.shouldApply(stack, context);
        boolean adjustShield = SideShield.shouldApply(stack, context);
        if (adjustModel || adjustShield) {
            pose.pushPose();
            if (adjustModel) {
                ItemModel.transform(stack, pose);
            }
            if (adjustShield) {
                SideShield.transform(pose, entity);
            }
        }
    }

    @Inject(method = "renderItem", at = @At("RETURN"))
    private void octoclient$restoreFirstPersonItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                                   PoseStack pose, SubmitNodeCollector collector, int light,
                                                   CallbackInfo ci) {
        if (ItemModel.shouldApply(stack, context) || SideShield.shouldApply(stack, context)) {
            pose.popPose();
        }
        ShieldStatusHud.endFirstPersonRender();
        ShieldCosmetics.endItemRender();
    }
}
