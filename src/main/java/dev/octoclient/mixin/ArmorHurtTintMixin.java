package dev.octoclient.mixin;

import dev.octoclient.module.render.OldAnimations;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntityRenderer.class)
public class ArmorHurtTintMixin {
    @ModifyArg(method = "getOverlayCoords(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/OverlayTexture;v(Z)I"),
            index = 0)
    private static boolean octoclient$setArmorHurtTint(boolean hasRedOverlay) {
        return hasRedOverlay && OldAnimations.armorHurtTintEnabled();
    }
}
