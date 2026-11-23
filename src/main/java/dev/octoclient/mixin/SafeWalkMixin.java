package dev.octoclient.mixin;

import dev.octoclient.module.secret.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Meteor's ClipAtLedgeEvent equivalent for SafeWalk. */
@Mixin(Player.class)
public class SafeWalkMixin {
    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void octoclient$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        if (SafeWalk.shouldClip(Minecraft.getInstance())) {
            cir.setReturnValue(true);
        }
    }
}
