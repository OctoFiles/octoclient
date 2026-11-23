package dev.octoclient.mixin;

import dev.octoclient.module.secret.Fly;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Meteor's Flight cancels the server ability packet so it cannot disable flight. */
@Mixin(ClientPacketListener.class)
public class FlightAbilitiesMixin {
    @Inject(method = "handlePlayerAbilities", at = @At("HEAD"), cancellable = true)
    private void octoclient$keepFlight(ClientboundPlayerAbilitiesPacket packet, CallbackInfo ci) {
        if (Fly.shouldCancelAbilities()) {
            ci.cancel();
        }
    }
}
