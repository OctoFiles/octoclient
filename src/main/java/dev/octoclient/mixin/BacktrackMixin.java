package dev.octoclient.mixin;

import dev.octoclient.module.secret.Backtrack;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the Backtrack module withhold the local player's position packets. */
@Mixin(LocalPlayer.class)
public class BacktrackMixin {
    @Inject(method = "sendPosition", at = @At("HEAD"), cancellable = true)
    private void octoclient$backtrack(CallbackInfo ci) {
        if (Backtrack.hold()) {
            ci.cancel();
        }
    }
}
