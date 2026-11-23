package dev.octoclient.mixin;

import dev.octoclient.module.secret.AntiHunger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets AntiHunger spoof sprint/onGround packets on their way out. */
@Mixin(ClientCommonPacketListenerImpl.class)
public class AntiHungerMixin {
    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void octoclient$antiHunger(Packet<?> packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Packet<?> replacement = AntiHunger.transform(packet, client);
        if (replacement != packet) {
            ci.cancel();
            if (replacement != null && client.getConnection() != null) {
                client.getConnection().send(replacement);
            }
        }
    }
}
