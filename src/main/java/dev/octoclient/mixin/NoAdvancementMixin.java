package dev.octoclient.mixin;

import dev.octoclient.module.render.NoAdvancement;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class NoAdvancementMixin {
    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void octoclient$filterAdvancementToast(Toast toast, CallbackInfo ci) {
        if (NoAdvancement.shouldHide(toast)) {
            ci.cancel();
        }
    }
}
