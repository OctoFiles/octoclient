package dev.octoclient.mixin;

import dev.octoclient.module.ModuleManager;
import dev.octoclient.module.hud.PingHud;
import dev.octoclient.module.utility.StreamerMode;
import dev.octoclient.util.NameBadges;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabNameMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void octo$maskLocalTabName(PlayerInfo info, CallbackInfoReturnable<Component> result) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !info.getProfile().name().equals(minecraft.player.getName().getString())) {
            return;
        }
        StreamerMode streamerMode = ModuleManager.get().byName("Streamer Mode", StreamerMode.class).orElse(null);
        if (streamerMode != null && streamerMode.isEnabled()) {
            if (streamerMode.hidesPlayerName()) {
                result.setReturnValue(Component.empty());
                return;
            }
            String alias = streamerMode.displayedPlayerName();
            if (alias != null) {
                result.setReturnValue(Component.literal(alias));
            }
        }
        String pingSuffix = PingHud.tabPingSuffix();
        if (pingSuffix != null && !result.getReturnValue().getString().isBlank()) {
            result.setReturnValue(result.getReturnValue().copy().append(pingSuffix));
        }
    }

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void octo$badgeTabName(PlayerInfo info, CallbackInfoReturnable<Component> result) {
        Component name = result.getReturnValue();
        if (name == null) {
            return;
        }
        Component badged = NameBadges.prefix(info.getProfile().id(), info.getProfile().name(), name);
        if (badged != name) {
            result.setReturnValue(badged);
        }
    }
}
