package dev.octoclient.mixin;

import dev.octoclient.module.render.ClearBackground;
import dev.octoclient.module.settings.ClientSettings;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
abstract class ClearChatBackgroundMixin {
    @ModifyArg(method = "lambda$extractRenderState$1",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;fill(IIIII)V"),
            index = 4)
    private static int octoclient$clearChatBackdrop(int color) {
        return ClearBackground.clearsChat() ? 0 : color;
    }
}

@Mixin(PlayerTabOverlay.class)
abstract class ClearTabListBackgroundMixin {
    @ModifyArg(method = "extractRenderState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"),
            index = 4)
    private int octoclient$clearTabBackdrop(int color) {
        return ClearBackground.clearsTabList() ? 0 : color;
    }
}

@Mixin(EntityRenderer.class)
abstract class ClearNametagBackgroundMixin {
    @ModifyArg(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V"),
            index = 5)
    private int octoclient$clearNametagBackdrop(int backgroundColor) {
        return ClearBackground.clearsNametags() ? 0 : backgroundColor;
    }
}

@Mixin(Screen.class)
abstract class ClearInventoryBackgroundMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("HEAD"), cancellable = true)
    private void octoclient$clearInventoryBackdrop(
            net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        if (((Object) this instanceof InventoryScreen && ClearBackground.clearsInventory())
                || ((Object) this instanceof JoinMultiplayerScreen
                && ClientSettings.clearMultiplayerBackground())) {
            ci.cancel();
        }
    }
}
