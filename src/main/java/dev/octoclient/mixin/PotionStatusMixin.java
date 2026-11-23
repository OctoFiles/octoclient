package dev.octoclient.mixin;

import dev.octoclient.module.hud.PotionHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class PotionStatusMixin {
    @Inject(method = "extractEffects(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$hideVanillaEffects(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                               CallbackInfo ci) {
        if (PotionHud.shouldHideVanillaStatus()) {
            ci.cancel();
        }
    }
}
