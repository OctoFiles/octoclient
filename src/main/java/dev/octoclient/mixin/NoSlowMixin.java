package dev.octoclient.mixin;

import dev.octoclient.module.secret.NoSlow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cancels the vanilla item-use slowdown while NoSlow is enabled. */
@Mixin(LocalPlayer.class)
public class NoSlowMixin {
    @Inject(method = "isMovingSlowly", at = @At("HEAD"), cancellable = true)
    private void octoclient$noSlow(CallbackInfoReturnable<Boolean> cir) {
        if (NoSlow.shouldRemoveSlowdown(Minecraft.getInstance().player)) {
            cir.setReturnValue(false);
        }
    }
}
