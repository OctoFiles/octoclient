package dev.octoclient.mixin;

import dev.octoclient.module.secret.AntiKnockback;
import dev.octoclient.module.secret.Velocity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the Velocity and AntiKnockback modules scale or drop knockback. */
@Mixin(ClientPacketListener.class)
public class VelocityMixin {
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void octoclient$handleVelocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (Velocity.handle(packet, Minecraft.getInstance().player)) {
            ci.cancel();
            return;
        }
        if (AntiKnockback.handle(packet, Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }
}
