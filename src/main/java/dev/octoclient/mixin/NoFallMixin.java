package dev.octoclient.mixin;

import dev.octoclient.module.secret.NoFall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Rewrites outgoing movement packets while NoFall is active. */
@Mixin(ClientCommonPacketListenerImpl.class)
public class NoFallMixin {
    @ModifyVariable(method = "send", at = @At("HEAD"), argsOnly = true)
    private Packet<?> octoclient$noFall(Packet<?> packet) {
        return NoFall.rewrite(packet, Minecraft.getInstance().player);
    }
}
