package dev.octoclient.mixin;

import dev.octoclient.util.ServerTpsTracker;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ServerTpsMixin {
    @Inject(method = "handleSetTime", at = @At("HEAD"))
    private void octoclient$trackServerTime(ClientboundSetTimePacket packet, CallbackInfo ci) {
        ServerTpsTracker.onTimeUpdate(packet.gameTime());
    }
}
