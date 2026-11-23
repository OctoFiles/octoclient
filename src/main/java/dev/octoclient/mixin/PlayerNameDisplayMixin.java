package dev.octoclient.mixin;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.utility.StreamerMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class PlayerNameDisplayMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void octo$maskLocalName(Entity entity, CallbackInfoReturnable<Component> info) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity != minecraft.player || minecraft.player == null) {
            return;
        }
        StreamerMode streamerMode = ModuleManager.get().byName("Streamer Mode", StreamerMode.class).orElse(null);
        if (streamerMode == null || !streamerMode.isEnabled()) {
            return;
        }
        if (streamerMode.hidesPlayerName()) {
            info.setReturnValue(null);
            return;
        }
        String alias = streamerMode.displayedPlayerName();
        if (alias != null) {
            info.setReturnValue(Component.literal(alias));
        }
    }
}
